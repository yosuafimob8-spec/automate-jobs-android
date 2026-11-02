#!/usr/bin/env python3
"""Audit KarirMate's server-owned assessment bank. Read-only; never edits catalogs."""
import argparse
import collections
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1] / "certificate-registry"
FILES = [ROOT / "trusted_catalog.json", *sorted((ROOT / "role_catalog").glob("category_*.json"))]

def normalize(value):
    return re.sub(r"\s+", " ", re.sub(r"[^\w\s]", " ", value.casefold())).strip()

def audit():
    courses = []
    for path in FILES:
        if not path.exists():
            raise FileNotFoundError(path)
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, list):
            raise ValueError(f"{path}: expected array")
        courses.extend(data)
    ids = collections.Counter(c["courseId"] for c in courses)
    issues = []
    prompts = collections.defaultdict(list)
    option_sets = collections.defaultdict(list)
    for course in courses:
        cid = course["courseId"]
        qs = course.get("questions", [])
        if len(qs) != 20:
            issues.append(f"{cid}: {len(qs)} questions; expected 20")
        local = set()
        for i, q in enumerate(qs, 1):
            prompt = q.get("prompt", "")
            options = q.get("options", [])
            correct = q.get("correct")
            key = normalize(prompt)
            if not key:
                issues.append(f"{cid} Q{i}: empty prompt")
            if key in local:
                issues.append(f"{cid} Q{i}: repeated prompt within course")
            local.add(key)
            if not isinstance(options, list) or len(options) < 3 or len({normalize(o) for o in options}) != len(options):
                issues.append(f"{cid} Q{i}: invalid or repeated answer choices")
            if type(correct) is not int or not 0 <= correct < len(options):
                issues.append(f"{cid} Q{i}: invalid answer key")
            prompts[key].append(f"{cid} Q{i}")
            option_sets[tuple(normalize(o) for o in options)].append(f"{cid} Q{i}")
    for cid, n in ids.items():
        if n > 1:
            issues.append(f"{cid}: duplicate course ID ({n})")
    repeated_prompts = {p: places for p, places in prompts.items() if len(places) > 1}
    repeated_options = {p: places for p, places in option_sets.items() if len(places) > 1}
    print(f"Courses: {len(courses)}; questions: {sum(len(c['questions']) for c in courses)}")
    print(f"Duplicate prompt groups (global): {len(repeated_prompts)}")
    print(f"Duplicate prompt excess (global): {sum(len(v)-1 for v in repeated_prompts.values())}")
    print(f"Repeated ordered answer-choice groups: {len(repeated_options)}")
    print(f"Repeated ordered answer-choice excess: {sum(len(v)-1 for v in repeated_options.values())}")
    print(f"Structural issues: {len(issues)}")
    for p, locations in sorted(repeated_prompts.items(), key=lambda x: -len(x[1]))[:25]:
        print(f"DUP x{len(locations)}: {p[:120]} :: {', '.join(locations[:5])}")
    for item in issues[:40]:
        print("INVALID:", item)
    return len(issues), len(repeated_prompts), len(repeated_options)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="Fail on global prompt or option reuse")
    args = parser.parse_args()
    invalid, repeated, options = audit()
    sys.exit(1 if invalid or (args.strict and (repeated or options)) else 0)
