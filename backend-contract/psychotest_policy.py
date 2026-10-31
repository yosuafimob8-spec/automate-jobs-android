"""Training configuration shared by catalog and session creation (no network)."""


def basic_mode(config):
    explicit = config.get('access_tier')
    if explicit in ('free', 'pro'):
        return explicit == 'free'
    title = config.get('title', '').lower()
    return (config.get('family') in ('private', 'cpns')
            and len(config.get('categories', {})) == 1
            and not config.get('simulation')
            and not any(word in title for word in ('simulasi', 'cat ', 'premium', 'lengkap')))


def training_categories(config, requested=None):
    """Scale category counts to a real 10–20-question training session.

    Bank sufficiency is checked by the service before sampling; no repeated or
    fabricated questions are used to fill an undersized bank.
    """
    categories = dict(config['categories'])
    default = config.get('training_question_count', min(20, max(10, sum(categories.values()))))
    count = default if requested is None else requested
    if type(count) is not int or not 10 <= count <= 20:
        raise ValueError('Latihan harus berisi 10–20 soal')
    if not categories or any(type(n) is not int or n <= 0 for n in categories.values()) or len(categories) > count:
        raise ValueError('Konfigurasi kategori tidak valid')
    total = sum(categories.values())
    result = {key: 1 for key in categories}
    for _ in range(count - len(result)):
        # Allocate proportionally, deterministic tie-breaking in config order.
        key = max(categories, key=lambda k: categories[k] / total - result[k] / count)
        result[key] += 1
    return result
