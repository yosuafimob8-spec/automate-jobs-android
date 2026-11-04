"""Check the real paid API boundary with isolated identity/bank adapters; no server or payment."""
import importlib.util
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import Mock, patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

class SubscriptionAuthorizationTests(unittest.TestCase):
    def setUp(self):
        service_module = types.ModuleType('psychotest_service')
        service_module.API_VERSION = 'test'
        service_module.PsychotestError = type('PsychotestError', (Exception,), {})
        service_module.PsychotestService = Mock
        service_module.TESTS = {'premium': {'access_tier': 'pro'}}
        identity_module = types.ModuleType('search_identity')
        identity_module.IdentityError = type('IdentityError', (Exception,), {})
        identity_module.android_subject = lambda authorization: None
        patches = patch.dict(sys.modules, {'psychotest_service': service_module, 'search_identity': identity_module})
        patches.start(); self.addCleanup(patches.stop)
        spec = importlib.util.spec_from_file_location('subscription_authorization_api', ROOT / 'psychotest_api.py')
        self.module = importlib.util.module_from_spec(spec); spec.loader.exec_module(self.module)
        self.service = Mock()
        self.identity = {'is_pro': False, 'account_id': 'server-account'}
        self.api = self.module.PsychotestApi(self.service, identity_resolver=lambda _: ('android:server-account', self.identity))

    def test_demo_and_client_pro_claims_cannot_authorize_paid_endpoint(self):
        with self.assertRaises(self.module.ApiError) as failure:
            self.api.start('Bearer test-fixture', {'test_id': 'premium', 'is_pro': True, 'membership': 'pro', 'pro_demo': True})
        self.assertEqual(403, failure.exception.status)
        self.service.start_session.assert_not_called()

    def test_server_identity_grants_access_and_client_account_override_is_ignored(self):
        self.identity['is_pro'] = True
        self.api.start('Bearer test-fixture', {'test_id': 'premium', 'account_id': 'different-account'})
        self.service.start_session.assert_called_once_with('premium', 'android:server-account', 'server-account', question_count=None)

    def test_missing_authentication_cannot_use_demo_claim(self):
        with self.assertRaises(self.module.ApiError) as failure:
            self.api.start(None, {'test_id': 'premium', 'pro_demo': True})
        self.assertEqual(401, failure.exception.status)
        self.service.start_session.assert_not_called()
