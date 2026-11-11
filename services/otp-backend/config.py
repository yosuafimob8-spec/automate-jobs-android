"""Commercial AutoMate AI configuration (local secret files stay outside ZIP)."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def secret(name):
    path = ROOT / name
    return path.read_text(encoding="utf-8").strip() if path.exists() else ""

BOT_TOKEN = secret('.bot_token')
SERPER_KEY = secret('.serper_key')
ADMIN_ID = int(secret('.admin_id') or 0)
CONFIG_PATH = ROOT / 'commercial_settings.json'
DEFAULTS = {
    'free_daily_searches': 3,
    'pro_daily_searches': 30,
    'free_pairs': 2,
    'pro_pairs': 6,
    'daily_api_budget': 24,
    'cache_hours': 12,
    'max_auto_refresh_queries': 6,
    'refresh_interval_hours': 6,
    'stars_price': 0,
    'payments_enabled': False,
    'support_contact': '',
    'terms_url': '',
    'privacy_url': '',
    'refund_url': '',
}

def settings():
    out = dict(DEFAULTS)
    if CONFIG_PATH.exists():
        try:
            loaded = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
            if isinstance(loaded, dict):
                out.update(loaded)
        except (ValueError, OSError):
            pass
    return out

def save_settings(overrides):
    current = settings()
    current.update(overrides)
    tmp = CONFIG_PATH.with_suffix('.json.tmp')
    tmp.write_text(json.dumps(current, indent=2, ensure_ascii=False), encoding='utf-8')
    tmp.replace(CONFIG_PATH)
