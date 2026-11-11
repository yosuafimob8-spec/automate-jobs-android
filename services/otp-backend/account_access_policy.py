"""Server-side entitlement reader; importing performs no database access.

No registration/login replacement, no credential files, migrations, grants,
revocations, DDL or data writes. The existing bearer-session format is reused.
"""
from contextlib import closing
from dataclasses import dataclass
import hashlib
from pathlib import Path
import sqlite3
import time

BASE_CAPABILITIES = frozenset({'profile.read_self', 'profile.write_self', 'jobs.search'})
ROLE_CAPABILITIES = {
    'user': BASE_CAPABILITIES,
    'admin': BASE_CAPABILITIES | {'admin.read'},
    'super_admin': BASE_CAPABILITIES | {'admin.read', 'accounts.manage_roles', 'accounts.manage_pro'},
}


def nonnegative_integer(value):
    return value if type(value) is int and value >= 0 else 0


@dataclass(frozen=True)
class AccessState:
    account_id: int
    role: str
    is_pro: bool
    pro_permanent: bool
    pro_until: int | None
    capabilities: frozenset
    origin: str

    def require(self, capability):
        if capability not in self.capabilities:
            raise PermissionError('Server permission denied')

    def public_fields(self):
        return {'role': self.role, 'is_pro': self.is_pro, 'isPro': self.is_pro,
                'membership': 'pro' if self.is_pro else 'free',
                'pro_permanent': self.pro_permanent, 'pro_until': self.pro_until,
                'capabilities': sorted(self.capabilities),
                'entitlements': {'plan': 'pro' if self.is_pro else 'free',
                                 'permanent': self.pro_permanent, 'expires_at': self.pro_until,
                                 'verified_by': 'server', 'origin': self.origin}}


def resolve_access(account_id, grant=None, legacy_pro_until=0, *, now=None):
    """Input grant MUST come from trusted storage, never request JSON/email flags.

    Role and Pro are independent. A super-admin role alone does not fabricate a
    paid/permanent entitlement. Invalid grants fail closed; a valid pre-existing
    linked Telegram subscription remains independently valid.
    """
    now = int(time.time()) if now is None else now
    role, permanent, explicit_until = 'user', False, 0
    legacy = nonnegative_integer(legacy_pro_until)
    valid_grant = (isinstance(grant, dict) and type(grant.get('account_id')) is int
        and grant['account_id'] == account_id and isinstance(grant.get('role'), str)
        and grant['role'] in ROLE_CAPABILITIES
        and type(grant.get('pro_permanent')) is int and grant['pro_permanent'] in (0, 1)
        and type(grant.get('pro_until')) is int and grant['pro_until'] >= 0)
    if valid_grant:
        role = grant['role']
        permanent = grant['pro_permanent'] == 1
        explicit_until = grant['pro_until']
    until = max(legacy, explicit_until)
    is_pro = permanent or until > now
    capabilities = ROLE_CAPABILITIES[role] | ({'pro.features'} if is_pro else set())
    origin = ('account_access' if valid_grant and (permanent or explicit_until >= legacy and explicit_until > now)
              else 'linked_telegram' if legacy > now else 'account_access' if valid_grant else 'default_free')
    return AccessState(account_id, role, is_pro, permanent, None if permanent else until,
                       frozenset(capabilities), origin)


class ReadOnlyAccessRepository:
    """Every lookup rechecks the existing server session and current entitlement.

    Constructing is inert. SQLite opens ONLY mode=ro; unknown accounts, expired
    sessions and missing legacy users are never inserted automatically. No
    backend modules are imported because some initialise/write on import.
    """
    def __init__(self, database, *, connection_factory=None):
        self.database = Path(database)
        self.connection_factory = connection_factory

    def _connect(self):
        if self.connection_factory is not None:
            connection = self.connection_factory()
        else:
            connection = sqlite3.connect(self.database.resolve().as_uri() + '?mode=ro', uri=True, timeout=5)
        connection.row_factory = sqlite3.Row
        connection.execute('PRAGMA query_only=ON')
        return connection

    @staticmethod
    def _has_table(connection, name):
        return connection.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", (name,)).fetchone() is not None

    def auth_me(self, authorization, *, now=None):
        now = int(time.time()) if now is None else now
        if not isinstance(authorization, str) or not authorization.startswith('Bearer '):
            return None
        token = authorization[7:]
        if not token or len(token) > 1024 or any(c.isspace() for c in token):
            return None
        digest = hashlib.sha256(token.encode('utf-8')).hexdigest()
        with closing(self._connect()) as connection:
            connection.execute('BEGIN')  # consistent READ transaction, not DDL/write
            row = connection.execute('''
                SELECT a.id, a.email, a.email_verified, a.telegram_user_id
                FROM android_sessions s JOIN android_accounts a ON a.id=s.account_id
                WHERE s.token_hash=? AND s.expires_at>?
            ''', (digest, now)).fetchone()
            if row is None:
                return None
            account = dict(row)
            grant = None
            if self._has_table(connection, 'account_access'):
                entitlement = connection.execute('''
                    SELECT account_id, role, pro_permanent, pro_until
                    FROM account_access WHERE account_id=?
                ''', (account['id'],)).fetchone()
                grant = dict(entitlement) if entitlement else None
            legacy = 0
            if account['telegram_user_id'] is not None and self._has_table(connection, 'users'):
                telegram = connection.execute('SELECT pro_until FROM users WHERE id=?',
                                               (account['telegram_user_id'],)).fetchone()
                legacy = telegram['pro_until'] if telegram else 0
            access = resolve_access(account['id'], grant, legacy, now=now)

            # Admin override: only the Telegram ID stored in .admin_id.
            admin_id = None
            try:
                admin_id = int((Path(__file__).resolve().parent / '.admin_id').read_text().strip())
            except (OSError, ValueError):
                pass

            telegram_id = account.get('telegram_user_id')
            if admin_id is not None and telegram_id is not None:
                try:
                    is_admin_account = int(telegram_id) == admin_id
                except (TypeError, ValueError):
                    is_admin_account = False

                if is_admin_account:
                    access = AccessState(
                        access.account_id,
                        access.role,
                        True,
                        True,
                        None,
                        frozenset(set(access.capabilities) | {'pro.features'}),
                        'admin_override'
                    )

            return account | access.public_fields()

    def require(self, authorization, capability, *, now=None):
        account = self.auth_me(authorization, now=now)
        if account is None:
            raise PermissionError('Unauthenticated or expired session')
        if capability not in account['capabilities']:
            raise PermissionError('Server permission denied')
        return account
