#!/usr/bin/env python3
"""Count open entries in docs/agents/ESCALATIONS.md (DN-004 / E4).

Prints "<total> <open>" to stdout. Deliberately NOT a bash grep one-liner: an
independent verifier's second VETO on HE-004 found that naive whole-file
grep for `^## ESC-` / `^\\*\\*Resolved:\\*\\*` (a) matched the file's own
embedded template example, and (b) could be fooled by a `**Resolved:**`-
looking line in an entry's own prose that wasn't the real status field.
Fixed by: stripping fenced code blocks before parsing, splitting into real
entries only, and counting the single unambiguous `**Status:**` field per
entry (open/resolved) rather than inferring status from free text.
"""
import sys
import re


def strip_fenced_code_blocks(text):
    return re.sub(r"```.*?```", "", text, flags=re.S)


def count(text):
    text = strip_fenced_code_blocks(text)
    entries = re.split(r"^## ESC-", text, flags=re.M)[1:]
    total = len(entries)
    open_count = 0
    for entry in entries:
        m = re.search(r"^\*\*Status:\*\*[ \t]*(.*)$", entry, re.M)
        status = m.group(1).strip().lower() if m else ""
        if status != "resolved":
            open_count += 1  # missing/malformed Status field fails open (visible), not silently dropped
    return total, open_count


def main():
    if len(sys.argv) != 2:
        print("0 0")
        return
    try:
        with open(sys.argv[1], "r") as f:
            text = f.read()
    except (FileNotFoundError, OSError):
        print("0 0")
        return
    total, open_count = count(text)
    print(f"{total} {open_count}")


if __name__ == "__main__":
    main()
