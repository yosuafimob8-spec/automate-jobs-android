"""KarirMate OTP HTML email design. No network calls or email sending here."""
from __future__ import annotations

import base64
from email.utils import parseaddr
from html import escape
from pathlib import Path
import re

ASSET_FILES = {
    "karirmate-envelope": "envelope.png",
    "karirmate-wave": "wave.png",
    "karirmate-clock": "clock.png",
    "karirmate-shield": "shield.png",
    "karirmate-info": "info.png",
    "karirmate-support": "support.png",
    # Dedicated logo CID; add it last for better Gmail Android compatibility.
    "karirmate-otp-logo-v3": "brand.png",
}
ASSETS = Path(__file__).with_name("otp_mail_assets")


def render_otp_html(code: str, support_address: str, minutes: int = 10, preview: bool = False) -> str:
    """Render an email-friendly table layout, with cid images for genuine emails."""
    if not re.fullmatch(r"[0-9]{6}", code):
        raise ValueError("Kode OTP harus tepat 6 digit.")
    _, address = parseaddr(support_address)
    if not address or "@" not in address:
        raise ValueError("Email pengirim/support tidak valid.")
    if not isinstance(minutes, int) or not 1 <= minutes <= 60:
        raise ValueError("Durasi OTP di luar batas.")
    address = escape(address, quote=True)
    href = escape("mailto:" + parseaddr(support_address)[1] + "?subject=Bantuan%20Verifikasi%20KarirMate", quote=True)
    def asset(cid):
        if not preview:
            return "cid:" + cid
        data = (ASSETS / ASSET_FILES[cid]).read_bytes()
        return "data:image/png;base64," + base64.b64encode(data).decode("ascii")

    return f'''<!doctype html>
<html lang="id">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1" />
<meta name="color-scheme" content="light only" />
<meta name="supported-color-schemes" content="light" />
<style>:root{{color-scheme:light only;supported-color-schemes:light;}} body, table, td {{ -webkit-text-size-adjust:100%; }}</style>
<title>Kode Verifikasi Email KarirMate</title>
</head>
<body style="margin:0;padding:0;background-color:#eef7fd;color:#112c4c;font-family:Arial,Helvetica,sans-serif;-webkit-text-size-adjust:100%;">
<div style="display:none;font-size:1px;line-height:1px;color:#eef7fd;max-height:0;max-width:0;opacity:0;overflow:hidden;">Kode verifikasi akun KarirMate kamu berlaku {minutes} menit. Jangan bagikan kepada siapa pun.</div>
<table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" bgcolor="#eef7fd" style="border-collapse:collapse;background:#eef7fd;background-color:#eef7fd;">
<tr><td align="center" style="padding:36px 14px 44px;">
<table role="presentation" cellpadding="0" cellspacing="0" border="0" width="600" style="border-collapse:separate;width:100%;max-width:600px;">
<tr><td align="center" style="padding:0 14px 26px;">
<img src="{asset('karirmate-otp-logo-v3')}" width="94" height="94" alt="KarirMate" style="width:94px;height:94px;border:0;display:block;border-radius:22px;" />
<div style="font-family:Arial,Helvetica,sans-serif;font-style:italic;font-size:39px;font-weight:800;letter-spacing:-1.3px;line-height:1.18;color:#0c2850;padding-top:10px;">KarirMate</div>
<div style="color:#55667a;font-size:16px;letter-spacing:.2px;margin-top:3px;">Your Career Journey</div>
</td></tr>
<tr><td bgcolor="#ffffff" style="background:#ffffff;background-color:#ffffff;border:1px solid #cfe7f3;border-radius:27px;overflow:hidden;box-shadow:0 12px 34px rgba(28,105,152,.12);">
<table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="border-collapse:collapse;">
<tr><td style="line-height:0;font-size:0;"><img src="{asset('karirmate-wave')}" width="600" alt="" style="border:0;width:100%;height:auto;display:block;" /></td></tr>
<tr><td align="center" style="padding:0 28px 0;">
<img src="{asset('karirmate-envelope')}" width="184" height="127" alt="Ikon email dan kunci" style="border:0;width:184px;height:auto;display:block;margin-top:-20px;" />
<h1 style="font-size:30px;line-height:1.23;margin:14px 0 10px;color:#102b4e;font-weight:800;letter-spacing:-.5px;">Kode Verifikasi Email</h1>
<p style="font-size:16px;line-height:1.6;color:#607086;margin:0 auto 27px;max-width:425px;">Gunakan kode berikut untuk memverifikasi akun <strong style="color:#17365a;">KarirMate</strong> kamu.</p>
<table role="presentation" cellpadding="0" cellspacing="0" width="100%" bgcolor="#edfcf9" style="max-width:410px;border-collapse:separate;border-radius:20px;border:3px solid #61dec8;background:#edfcf9;background-color:#edfcf9;">
<tr><td align="center" style="padding:19px 8px 17px;font-size:46px;line-height:1.25;white-space:nowrap;font-weight:800;letter-spacing:8px;color:#0c2a54;font-family:Arial,Helvetica,sans-serif;">{code}</td></tr>
</table>
</td></tr>
<tr><td style="padding:28px 38px 10px;">
<table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="border-collapse:collapse;">
<tr><td width="50" valign="middle" style="padding:0 12px 16px 0;"><img src="{asset('karirmate-clock')}" width="43" height="43" alt="" style="display:block;border:0;" /></td><td valign="middle" style="padding:0 0 16px 0;font-size:15px;line-height:1.5;color:#3f526e;">Kode berlaku <strong style="color:#0a2b59;">{minutes} menit.</strong></td></tr>
<tr><td width="50" valign="middle" style="padding:0 12px 16px 0;"><img src="{asset('karirmate-shield')}" width="43" height="43" alt="" style="display:block;border:0;" /></td><td valign="middle" style="padding:0 0 16px 0;font-size:15px;line-height:1.5;font-weight:700;color:#0e315c;">Jangan bagikan kode ini kepada siapa pun.</td></tr>
</table>
</td></tr>
<tr><td style="padding:0 26px 20px;">
<table role="presentation" cellpadding="0" cellspacing="0" width="100%" bgcolor="#e4f2ff" style="border-collapse:separate;border-radius:16px;background:#e4f2ff;background-color:#e4f2ff;">
<tr><td width="47" valign="middle" style="padding:15px 0 15px 15px;"><img src="{asset('karirmate-info')}" width="37" height="37" alt="Info" style="display:block;border:0;" /></td><td valign="middle" style="padding:15px 13px 15px 7px;color:#3f536f;font-size:14px;line-height:1.55;">Kode ini hanya untuk akun <strong style="color:#10335d;">KarirMate</strong> milikmu. Jika kamu tidak meminta kode ini, abaikan email ini.</td></tr>
</table>
</td></tr>
<tr><td style="padding:0 28px;"><div style="height:1px;line-height:1px;background:#e0eaf1;font-size:0;">&nbsp;</div></td></tr>
<tr><td align="center" style="padding:19px 12px 25px;">
<table role="presentation" cellpadding="0" cellspacing="0" border="0" style="border-collapse:collapse;"><tr>
<td style="padding:0 8px 0 0;"><img src="{asset('karirmate-support')}" width="28" height="28" alt="" style="border:0;display:block;" /></td>
<td style="font-size:14px;line-height:1.5;color:#607086;">Butuh bantuan? <a href="{href}" style="font-weight:700;text-decoration:underline;color:#076dc5;">Hubungi tim KarirMate</a></td>
</tr></table>
</td></tr>
</table>
</td></tr>
<tr><td align="center" style="padding:17px 12px 0;font-size:12px;line-height:1.6;color:#8da0b5;">Email otomatis dari KarirMate &bull; {address}<br />Your Career Journey</td></tr>
</table>
</td></tr></table>
</body></html>'''


def attach_otp_assets(html_part):
    """Attach branding as CID image/related subparts, usable with Gmail SMTP."""
    for cid, filename in ASSET_FILES.items():
        image_file = ASSETS / filename
        raw = image_file.read_bytes()
        html_part.add_related(
            raw, maintype="image", subtype="png", cid=f"<{cid}>",
            disposition="inline", filename=filename
        )
