"""Entitlements affect responses, never stored results. No data is removed from the DB."""
from copy import deepcopy

def is_pro(identity):
    return identity.get('is_pro') is True or (identity.get('is_pro') is None and str(identity.get('membership', '')).lower() == 'pro')

def result_for_access(response, identity):
    value = deepcopy(response)
    result = value.get('result')
    if not isinstance(result, dict): return value
    result['deep_analysis_available'] = is_pro(identity)
    if not is_pro(identity):
        # Summary score/motivation and persisted results remain available to FREE.
        for field in ('questions', 'analysis', 'categories'):
            result.pop(field, None)
    return value
