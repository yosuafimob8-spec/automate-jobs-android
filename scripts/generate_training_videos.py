#!/usr/bin/env python3
"""Build short, original, offline MP4 lesson title videos from course data."""
import pathlib
import re
import subprocess
import textwrap

root = pathlib.Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/com/example/automatesmartbot/data/career/TrainingCourses.kt").read_text()
dest = root / "app/src/main/res/raw"
dest.mkdir(parents=True, exist_ok=True)
blocks = re.split(r'TrainingCourse\("', source)[1:]
for block in blocks:
    course = block.split('"', 1)[0]
    lesson_section = block.split('listOf(', 1)[1].split('), listOf(', 1)[0]
    lessons = re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', lesson_section)
    if not lessons:
        raise RuntimeError("No lessons found for " + course)
    for index, (title, body) in enumerate(lessons):
        target = dest / f"training_{course}_{index}.mp4"
        if target.exists():
            continue
        title_text = "\\n".join(textwrap.wrap(title, 24))
        description = "\\n".join(textwrap.wrap(body, 38)[:7])
        filter_text = (
            "drawbox=x=24:y=24:w=592:h=312:color=white@0.10:t=fill,"
            "drawtext=text='KarirMate | PELATIHAN KERJA':fontcolor=0xFFB86B:fontsize=23:x=45:y=45,"
            f"drawtext=text='{title_text}':fontcolor=white:fontsize=29:x=45:y=102:line_spacing=9,"
            f"drawtext=text='{description}':fontcolor=white:fontsize=17:x=45:y=208:line_spacing=7"
        )
        cmd = [
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-f", "lavfi", "-i", "color=c=0x152D43:s=640x360:r=12",
            "-t", "9", "-vf", filter_text,
            "-c:v", "libx264", "-preset", "ultrafast", "-crf", "34",
            "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(target)
        ]
        subprocess.run(cmd, check=True)
        if not target.exists() or target.stat().st_size < 1000:
            raise RuntimeError("Video generation failed: " + str(target))
print("Generated", len(list(dest.glob("training_*.mp4"))), "training videos")
