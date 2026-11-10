#!/usr/bin/env python3
"""Fail GitHub Actions if Android APK is signed by the wrong certificate."""
import re
import shutil
import subprocess
import sys
from pathlib import Path

EXPECTED_SHA1 = 'D263B4E01CB34E203F3B2EA1779D033720A33F2A'


def locate_apksigner() -> Path:
    tool = shutil.which('apksigner')
    if tool: return Path(tool)
    from os import environ
    for env in ('ANDROID_HOME', 'ANDROID_SDK_ROOT'):
        sdk = environ.get(env)
        if sdk:
            matches = sorted((Path(sdk)/'build-tools').glob('*/apksigner'), reverse=True)
            if matches: return matches[0]
    raise RuntimeError('Android apksigner tidak ditemukan')


def fingerprint(output: str) -> str:
    m = re.search(r'(?im)^\s*(?:V[1234]\s+)?Signer: certificate SHA-1 digest:\s*([0-9a-f:]+)\s*$',output)
    if not m:
        raise RuntimeError('SHA-1 tidak ada dalam hasil apksigner')
    return m.group(1).replace(':','').upper()


def main() -> int:
    if len(sys.argv)!=2:
        print('Usage: python3 verify_karirmate_apk_signing.py APK_DIR',file=sys.stderr)
        return 2
    files = sorted(Path(sys.argv[1]).glob('*.apk'))
    if not files:
        print('STOP: file APK hasil build tidak ditemukan',file=sys.stderr)
        return 2
    try:
        tool = locate_apksigner()
        for apk in files:
            p=subprocess.run([str(tool),'verify','--print-certs',str(apk)],capture_output=True,text=True)
            if p.returncode:
                raise RuntimeError('APK tidak lolos verifikasi signature: '+apk.name)
            sha=fingerprint(p.stdout)
            if sha!=EXPECTED_SHA1:
                raise RuntimeError('STOP APK '+apk.name+': SHA-1 berbeda dari sertifikat Google OAuth KarirMate.')
            print('OK APK ditandatangani dengan kunci tetap:',apk.name)
        return 0
    except RuntimeError as e:
        print(str(e),file=sys.stderr)
        return 2

if __name__=='__main__':sys.exit(main())
