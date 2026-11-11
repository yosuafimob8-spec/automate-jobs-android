"""Paths can be overridden for isolated tests; defaults preserve Termux locations."""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DB = Path(os.environ.get('KARIRMATE_DB_PATH', str(ROOT / 'automate.db'))).expanduser()
CV_DIR = Path(os.environ.get('KARIRMATE_CV_DIR', str(ROOT / 'phase5_cv'))).expanduser()
