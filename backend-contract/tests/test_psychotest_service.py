"""Exercise the actual service with an isolated synthetic bank, not production data.
The real bank and identity bridge are absent from this repository.
"""
import importlib.util
from pathlib import Path
import random
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

class PsychotestServiceTests(unittest.TestCase):
    def setUp(self):
        bank = types.ModuleType('psychotest_bank')
        bank.CATEGORY_LABELS = {'logic':'Logika'}
        bank.FAMILY_LABELS = {'private':'Swasta'}
        bank.FEATURES = []; bank.PERSONALITY_DIMENSIONS = {}
        bank.TESTS = {'logic':dict(family='private', categories={'logic':8}, title='Logika', kind='objective', duration=600, score_scale='percent', leaderboard=True)}
        bank.TEST_ORDER = ['logic']; bank.build_questions = lambda: []
        skb = types.ModuleType('psychotest_skb')
        skb.SKB_FORMATIONS = []; skb.SKB_SOURCE = {}; skb.SKB_VALIDATION = {}
        with patch.dict(sys.modules, psychotest_bank=bank, psychotest_skb=skb):
            spec = importlib.util.spec_from_file_location('isolated_psychotest_service', ROOT/'psychotest_service.py')
            self.module = importlib.util.module_from_spec(spec); spec.loader.exec_module(self.module)
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.module.TEMP = Path(self.temp.name)
        self.store = self.module.PsychotestStore(Path(self.temp.name)/'test.sqlite')
        questions = [dict(id=f'logic-{n:02}', family='private', category='logic', difficulty='mudah', kind='objective', prompt=f'Synthetic test question number {n}', options=[dict(id=c, text=c) for c in 'abcd'], answer_key={'correct':'a'}, explanation='Synthetic explanation for automated verification.') for n in range(20)]
        self.store.seed(questions)
        self.service = self.module.PsychotestService(self.store, seed=False, clock=lambda:1000, rng=random.Random(1))

    def test_free_catalog_and_ten_question_session_use_real_unique_bank_rows(self):
        mode = self.service.catalog()['modes'][0]
        self.assertEqual('free', mode['access_tier']); self.assertEqual(10, mode['training_question_count'])
        session = self.service.start_session('logic', 'account', question_count=10)['session']
        questions = self.service.questions(session['id'], 'account')['questions']
        self.assertEqual(10, len(questions)); self.assertEqual(10, len({q['question_id'] for q in questions}))

    def test_answer_revision_score_and_history_are_persisted(self):
        session = self.service.start_session('logic', 'account', question_count=10)['session']
        questions = self.service.questions(session['id'], 'account')['questions']
        answers = [dict(question_id=q['question_id'], option_id='a' if i<5 else 'b') for i,q in enumerate(questions)]
        self.service.save_answers(session['id'], 'account', answers, expected_revision=0)
        with self.assertRaises(self.module.PsychotestError): self.service.save_answers(session['id'], 'account', answers, expected_revision=0)
        result = self.service.submit(session['id'], 'account')['result']
        self.assertEqual(50, result['practice_score']); self.assertEqual(5, result['correct_count'])
        fresh = self.module.PsychotestService(self.store, seed=False, clock=lambda:1001)
        self.assertEqual(1, len(fresh.history('account')['items']))
        self.assertEqual(50, fresh.result(session['id'], 'account')['result']['practice_score'])

    def test_insufficient_bank_never_pads_fake_or_duplicate_questions(self):
        with self.store.connect() as db: db.execute("DELETE FROM questions WHERE id >= 'logic-05'")
        self.assertFalse(self.service.catalog()['modes'][0]['training_available'])
        with self.assertRaises(self.module.PsychotestError): self.service.start_session('logic', 'account', question_count=10)

    def test_existing_full_session_contract_remains_unchanged(self):
        self.assertEqual(8, self.service.start_session('logic', 'legacy')['session']['question_count'])

    def test_session_data_cannot_be_read_by_another_account(self):
        session = self.service.start_session('logic', 'owner-a', question_count=10)['session']
        with self.assertRaises(self.module.PsychotestError):
            self.service.questions(session['id'], 'owner-b')
        with self.assertRaises(self.module.PsychotestError):
            self.service.get_session(session['id'], 'owner-b')
        self.assertEqual([], self.service.history('owner-b')['items'])

if __name__ == '__main__': unittest.main()
