"""Conservative selection from available company records; never invent contacts."""
import time
from phase512_sender import valid_target

def select_for_profile(rows,positions,locations,now=None):
    now=int(time.time()) if now is None else now
    positions=[str(p).strip().casefold() for p in positions if str(p).strip()]
    locations=[str(p).strip().casefold() for p in locations if str(p).strip()]
    if not positions or not locations: return []
    result=[];seen=set()
    candidates=[]
    for row in rows:
        item=dict(row);email=str(item.get('email') or '').strip().lower()
        if not valid_target(email) or not item.get('company') or not item.get('source_url') or not item.get('contact_source'): continue
        title=str(item.get('position') or '').casefold();location=str(item.get('location') or '').casefold()
        if not any(p in title for p in positions) or not any(loc in location for loc in locations): continue
        verified=item.get('kind')=='vacancy' and item.get('active') is True and item.get('vacancy_verified') is True and item.get('evidence_url') and isinstance(item.get('expires_at'),(int,float)) and item['expires_at']>now
        if item.get('kind')=='vacancy' and not verified: continue
        item.update(email=email,kind='vacancy' if verified else 'spontaneous',vacancy_verified=bool(verified),active=bool(verified))
        candidates.append(item)
    for item in sorted(candidates,key=lambda c:(c['kind']!='vacancy',c['company'],c['email'])):
        if item['email'] in seen: continue
        seen.add(item['email']);result.append(item)
        if len(result)==5:break
    return result
