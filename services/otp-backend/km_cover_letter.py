"""Draft surat lamaran dari data lamaran; tidak mengirim email."""
import re


def _clean(value, limit=130):
    text = str(value or "")
    text = re.sub(r"[\x00-\x1f\x7f]+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text[:limit].strip()


def compose(applicant_name, job_title, company, application_kind):
    name = _clean(applicant_name, 100)
    position = _clean(job_title, 130)
    firm = _clean(company, 130)
    kind = _clean(application_kind, 30).casefold()
    if not name or not position or not firm:
        raise ValueError("Nama, posisi, dan perusahaan wajib tersedia")

    spontaneous = kind == "spontaneous"
    subject = (
        f"Permohonan Kesempatan Kerja - {position} - {name}"
        if spontaneous else f"Lamaran Kerja - {position} - {name}"
    )
    opening = (
        f"Saya ingin mengajukan lamaran untuk kesempatan kerja pada posisi {position} "
        f"di {firm}, apabila tersedia kebutuhan rekrutmen yang sesuai."
        if spontaneous else
        f"Saya bermaksud melamar posisi {position} di {firm}."
    )
    body = (
        f"Yth. Tim Rekrutmen {firm},\n\n"
        "Dengan hormat,\n\n"
        f"Perkenalkan, saya {name}. {opening}\n\n"
        "Sebagai bahan pertimbangan, saya melampirkan CV dalam format PDF "
        "yang berisi riwayat pendidikan dan pengalaman saya. "
        "Saya bersedia mengikuti proses seleksi serta memberikan "
        "informasi tambahan apabila diperlukan.\n\n"
        "Terima kasih atas waktu dan perhatiannya.\n\n"
        f"Hormat saya,\n{name}"
    )
    return subject, body
