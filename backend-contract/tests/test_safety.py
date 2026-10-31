import concurrent.futures
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from auto_apply_safety import AutoApplyGate, select_candidates
from psychotest_policy import basic_mode, training_categories

NOW = 1793448000

class AutoApplyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.gate = AutoApplyGate(Path(self.temp.name) / 'test.sqlite')
        self.gate.consent('account', explicit=True, now=NOW)

    def test_requires_explicit_consent(self):
        with self.assertRaises(ValueError): self.gate.consent('x', explicit=False, now=NOW)
        with self.assertRaises(PermissionError): self.gate.reserve('x', 'hr@example.com', now=NOW)

    def test_five_per_jakarta_calendar_day_with_next_day_reset(self):
        for n in range(5): self.gate.reserve('account', f'hr{n}@example.com', now=NOW)
        with self.assertRaisesRegex(ValueError, 'Batas 5'): self.gate.reserve('account', 'six@example.com', now=NOW)
        self.gate.reserve('account', 'tomorrow@example.com', now=NOW+86400)
        self.assertEqual(6, len(self.gate.history('account')))

    def test_case_insensitive_persistent_duplicate_prevention(self):
        self.gate.send('account', ' HR@Example.com ', now=NOW, gmail_sender=lambda _: {'id':'gmail-confirmed-id'})
        fresh = AutoApplyGate(self.gate.database)
        with self.assertRaisesRegex(ValueError, 'duplikat'): fresh.reserve('account', 'hr@example.com', now=NOW+86400)
        self.assertEqual('sent', fresh.history('account')[0]['status'])

    def test_ambiguous_outcome_never_becomes_sent_or_retries(self):
        calls = []
        def timeout(recipient):
            calls.append(recipient)
            raise TimeoutError('test: no network used')
        with self.assertRaises(TimeoutError): self.gate.send('account', 'hr@example.com', now=NOW, gmail_sender=timeout)
        self.assertEqual('unknown', self.gate.history('account')[0]['status'])
        with self.assertRaises(ValueError): self.gate.send('account', 'hr@example.com', now=NOW, gmail_sender=timeout)
        self.assertEqual(1, len(calls))

    def test_missing_gmail_id_is_not_delivery_confirmation(self):
        with self.assertRaises(ValueError): self.gate.send('account', 'hr@example.com', now=NOW, gmail_sender=lambda _: {'success':True})
        self.assertEqual('unknown', self.gate.history('account')[0]['status'])

    def test_parallel_reservations_cannot_exceed_limit(self):
        def reserve(n):
            try: self.gate.reserve('account', f'hr{n}@example.com', now=NOW); return True
            except ValueError: return False
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as pool:
            self.assertEqual(5, sum(pool.map(reserve, range(20))))

    def test_candidates_match_profile_location_and_prioritize_verified_active(self):
        base = dict(company_id='stored-company', contact_source='company-database', position='Admin', location='Jakarta', email='hr@example.com')
        spontaneous = dict(base, kind='spontaneous')
        active = dict(base, company_id='vacancy-company', email='jobs@example.com', kind='vacancy', active=True, vacancy_verified=True, evidence_url='https://example.com/job', expires_at=NOW+10)
        bad = [dict(active, email='unverified@example.com', vacancy_verified=False), dict(active, email='expired@example.com', expires_at=NOW-1), dict(spontaneous, email='remote@example.com', location='Bandung'), dict(spontaneous, email='invented@example.com', contact_source='')]
        selected = select_candidates([spontaneous, *bad, active], positions=['Admin'], locations=['Jakarta'], now=NOW)
        self.assertEqual(['jobs@example.com', 'hr@example.com'], [c['email'] for c in selected])
        self.assertFalse(selected[1]['vacancy_verified'])
        self.assertEqual([], select_candidates([active], positions=[], locations=['Jakarta'], now=NOW))

class PsychotestTests(unittest.TestCase):
    def test_basic_modes_remain_free(self):
        self.assertTrue(basic_mode(dict(family='private', categories={'logic':8}, title='Logika')))
        self.assertFalse(basic_mode(dict(family='skb', categories={'logic':10}, title='SKB')))
        self.assertFalse(basic_mode(dict(family='cpns', categories={'twk':30}, title='Simulasi CAT')))

    def test_training_has_ten_to_twenty_unique_sample_slots(self):
        for n in (3,8,10,15,20,110):
            cfg = dict(categories={'a':n})
            self.assertTrue(10 <= sum(training_categories(cfg).values()) <= 20)
        self.assertEqual(20, sum(training_categories(dict(categories={'a':30,'b':35,'c':45}), 20).values()))
        for n in (0,9,21,True,'10'):
            with self.assertRaises(ValueError): training_categories(dict(categories={'a':10}), n)

if __name__ == '__main__': unittest.main()
