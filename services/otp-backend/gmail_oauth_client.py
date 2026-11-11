"""Gmail worker client. Refresh tokens stay at the existing OAuth worker.

Import has no network/file side effects. There is no automatic retry of sends.
"""
import os
from pathlib import Path
from urllib.parse import urlsplit
import requests

BASE = os.environ.get('KARIRMATE_GMAIL_WORKER_URL', 'https://red-king-9c6cautomatesmartbot-auth.ilhamramdhani763-dc9.workers.dev').rstrip('/')
KEY_FILE = Path(os.environ.get('KARIRMATE_GMAIL_KEY_FILE', str(Path(__file__).resolve().parent / '.gmail_oauth_key'))).expanduser()

class GmailUnavailable(RuntimeError):
    pass

class GmailReauthorizationRequired(GmailUnavailable):
    pass

def headers():
    key = KEY_FILE.read_text(encoding='utf-8').strip() if KEY_FILE.is_file() else ''
    if not key:
        raise GmailUnavailable('Kunci layanan Gmail belum dikonfigurasi.')
    if urlsplit(BASE).scheme != 'https' or not urlsplit(BASE).hostname or urlsplit(BASE).username:
        raise GmailUnavailable('Alamat layanan Gmail harus HTTPS.')
    return {'Authorization': 'Bearer ' + key}

def _uid(uid):
    if isinstance(uid, bool) or not str(uid).isdigit() or int(uid) <= 0:
        raise ValueError('Identitas Gmail tidak valid.')
    return str(int(uid))

def _json(response):
    if response.status_code in (401, 403):
        raise GmailReauthorizationRequired('Izin Gmail/kunci layanan ditolak. Hubungkan ulang akun atau periksa konfigurasi layanan.')
    response.raise_for_status()
    data = response.json()
    if not isinstance(data, dict):
        raise GmailUnavailable('Respons layanan Gmail tidak valid.')
    return data

def begin(uid):
    with requests.post(BASE + '/api/oauth/start', json={'telegram_user_id': _uid(uid)}, headers=headers(), timeout=(10, 20), allow_redirects=False) as response:
        data = _json(response)
    url = data.get('url', '')
    parsed = urlsplit(url)
    if parsed.scheme != 'https' or parsed.hostname != 'accounts.google.com' or parsed.username or parsed.password or parsed.port not in (None, 443):
        raise GmailUnavailable('URL otorisasi Google tidak valid.')
    return url

def status(uid):
    expected = _uid(uid)
    with requests.get(BASE + '/api/oauth/status', params={'uid': expected}, headers=headers(), timeout=(10, 20), allow_redirects=False) as response:
        data = _json(response)
    if data.get('telegram_user_id') is not None and str(data['telegram_user_id']) != expected:
        raise GmailUnavailable('Identitas Gmail tidak sesuai akun.')
    return dict(data, connected=data.get('connected') is True)

def send_application(payload):
    # Same worker contract as the supplied Termux sender. A timeout is ambiguous.
    with requests.post(BASE + '/api/gmail/send-application', json=payload, headers={**headers(), 'Idempotency-Key': payload['job_key']}, timeout=(10, 55), allow_redirects=False) as response:
        data = _json(response)
        message_id = data.get('message_id')
        if response.status_code != 200 or data.get('success') is not True or not isinstance(message_id, str) or not message_id.strip() or message_id.strip().lower() == 'null':
            raise GmailUnavailable('Gmail belum mengonfirmasi message ID. Periksa riwayat sebelum mengirim ulang.')
        return message_id


# karirmate_gmail_account_oauth_v1: account IDs, NOT Telegram user IDs.
def _karirmate_account_id(value):
    if isinstance(value, bool) or not isinstance(value, int) or not 0 < value < 2**53:
        raise ValueError("ID akun KarirMate tidak valid")
    return str(value)


def begin_karirmate(account_id):
    """Only called from a validated Android bearer session in auto_apply_api."""
    with requests.post(
        BASE + "/api/karirmate/oauth/start",
        json={"account_id": _karirmate_account_id(account_id)},
        headers=headers(), timeout=(10, 20), allow_redirects=False,
    ) as response:
        data = _json(response)
    uri = data.get("url", "")
    parsed = urlsplit(uri)
    if (parsed.scheme != "https" or parsed.hostname != "accounts.google.com"
            or parsed.username or parsed.password or parsed.port not in (None, 443)):
        raise GmailUnavailable("Tautan otorisasi Gmail tidak valid")
    return uri


def status_karirmate(account_id):
    aid = _karirmate_account_id(account_id)
    with requests.get(
        BASE + "/api/karirmate/oauth/status",
        params={"account_id": aid}, headers=headers(),
        timeout=(10, 20), allow_redirects=False,
    ) as response:
        data = _json(response)
    if str(data.get("account_id")) != aid:
        raise GmailUnavailable("Respons Gmail tidak cocok dengan akun KarirMate")
    return {"connected": data.get("connected") is True,
            "connected_at": data.get("connected_at")}
