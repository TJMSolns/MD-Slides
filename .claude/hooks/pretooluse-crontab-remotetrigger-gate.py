#!/usr/bin/env python3
"""PreToolUse gate: block creating live, unattended infrastructure (a crontab
entry, or a RemoteTrigger `create`/`update`) unless the command/trigger body
itself names a WORK-QUEUE item whose evidence artifact already has
`Verifier-verdict: PASS` (DN-023/HE-028).

Real incident this closes: an earlier version of HE-027's driver script was
installed as a live crontab entry before being independently reviewed at all
— caught only because a human/agent happened to notice, not because anything
structural stopped it. Mirrors pretooluse-done-gate.py's own "no
self-certification" principle, applied to CREATION of unattended live
infrastructure instead of WORK-QUEUE Done-transitions.

Scope answered by Tony (GL-032/HL-047, 2026-07-14): (1) correlation is an
explicit WORK-QUEUE tag in the content, fail-closed if absent; (2)
`RemoteTrigger action: update` is in scope alongside `create`; (3) this gate
is repo-scoped to harness-evolution only, not machine-wide (Tony's own
unrelated JobHunt crontab entries must stay untouched)."""
import sys
import json
import re
import os
import shlex


def repo_root(path):
    """Same walk-up-to-.git idiom as stop-git-durability-gate.py/
    pretooluse-done-gate.py's own repo_root() — not a new pattern."""
    d = os.path.abspath(path)
    if os.path.isfile(d):
        d = os.path.dirname(d)
    while True:
        if os.path.exists(os.path.join(d, ".git")):
            return d
        parent = os.path.dirname(d)
        if parent == d:
            return None
        d = parent


# DN-023/INV-2: same conservative, metacharacter-based philosophy as
# stop-git-durability-gate.py's is_readonly_command() (DN-019/HE-045) —
# reused, not reinvented. An embedded newline/redirect/pipe can hide a second
# statement or a write exactly as it can there.
METACHAR_RE = re.compile(r"[;|&`<>\n\r]|\$\(")
CRON_PATH_RE = re.compile(r"/var/spool/cron/|/etc/cron\.d/|/etc/crontab\b")


def _shell_tokens(command):
    """POSIX-aware tokenization via `shlex`, so a quoted (`'crontab'`,
    `"crontab"`) or backslash-escaped (`\\crontab`) invocation is recognized
    identically to a bare `crontab` token — closing a SECOND real VETO
    (round 2, an independent verifier's own adversarial testing) that a
    naive whitespace-only `.split()` couldn't see: ordinary, common shell
    quoting defeated the round-1 fix exactly the way path-qualification
    defeated the original version, and patching one quoting style at a time
    would just be the same whack-a-mole again. Returns None if the command
    can't be parsed as valid shell tokens (e.g. unbalanced quotes) — callers
    must fail toward blocking on None, per INV-4, not toward silently
    passing."""
    try:
        return shlex.split(command)
    except ValueError:
        return None


def _crontab_token_index(tokens):
    """Index of the first token that INVOKES the `crontab` binary — matched
    on the token's BASENAME, not string equality, so a path-qualified
    invocation (`/usr/bin/crontab -e`, `./crontab -e`) is recognized exactly
    like a bare `crontab -e`. Returns None if no token invokes it.

    Found live, empirically, via this item's own independent verifier
    (real VETO, not smoothed over): an EARLIER fix here required an exact
    string match on the token `crontab` (to stop a DIFFERENT bug — see
    is_crontab_write's own docstring below — where a regex word-boundary
    match false-positived on hyphenated filenames merely containing the
    substring "crontab"). That fix over-corrected: `/usr/bin/crontab -e`
    has no token equal to the bare string "crontab", so it was never
    recognized as a write at all — a complete, trivial bypass of this
    gate's entire purpose. `os.path.basename(tok) == "crontab"` fixes both
    bugs at once: a hyphenated filename's basename is the whole filename
    (never equal to "crontab"), while a path-qualified binary's basename is
    exactly "crontab". Callers must pass `_shell_tokens()`-derived tokens,
    not a naive `.split()`, so quoted/escaped invocations are also caught."""
    for i, tok in enumerate(tokens):
        if os.path.basename(tok) == "crontab":
            return i
    return None


def is_crontab_write(command):
    """True unless `command` is PROVABLY a read-only `crontab [-u <user>] -l`
    invocation. Anything not provably safe returns True (fail toward
    blocking/gating, per INV-4 — the opposite default from HE-045's own
    read/write heuristic, because the stakes here — unattended, unreviewed
    live infrastructure — justify gating on ambiguity rather than passing.

    Found live, empirically, dispatching an unrelated diagnostic command
    during this item's own build (self-caught, not smoothed over): matching
    `crontab` with a regex word-boundary (`\\bcrontab\\b`) false-positives on
    ANY command whose TEXT merely contains "crontab" as a hyphen-delimited
    substring — including this very hook's own file path
    (`pretooluse-crontab-remotetrigger-gate.py`), since `\\b` treats `-` as a
    word boundary too. See _crontab_token_index()/_shell_tokens() for how
    this is resolved together with two further real bugs an independent
    verifier found across 2 VETO rounds: path-qualified invocations, and
    quoted/escaped invocations, both bypassing the gate entirely.

    KNOWN, DISCLOSED LIMITATION, deliberately NOT patched (round 3 tried and
    reverted a broader fix within this same build — gating any
    metacharacter-laden command merely containing the substring "crontab"
    anywhere — because it false-positived on totally ordinary `&&`-chained
    commands, which are far more common than any of the evasions below):
    a `crontab` invocation is NOT reliably detected whenever it's embedded
    inside a NESTED SHELL CONTEXT this token-based heuristic doesn't parse
    into — confirmed real, distinct instances so far:
      - command substitution: `$(crontab evil.txt)`, `` `crontab evil.txt` ``
      - a wrapper command's own quoted argument: `bash -c "crontab -e"`,
        `sh -c "crontab -e"`, `ssh myhost "crontab -e"` — an independent
        verifier's own round-3 adversarial testing found this class is, if
        anything, MORE realistic than command substitution (`ssh host
        'crontab -e'` is the ordinary, textbook way to edit a REMOTE
        crontab — not a deliberate evasion attempt).
    Both are the same underlying gap, not two separate ones: `shlex` only
    tokenizes the OUTER command; text nested inside a subshell or a
    wrapper's own argument string is invisible to it. Closing this
    completely would mean recursively re-parsing arbitrary nested shell
    syntax — i.e. writing an actual shell interpreter, not a heuristic —
    which is explicitly out of scope (same documented-gap posture as
    `PATH_TOKEN_RE` in stop-git-durability-gate.py, and the same
    decline-to-build-speculatively discipline as HE-034: this project's own
    precedent is to accept a narrow, DISCLOSED heuristic gap rather than
    chase an open-ended, unbounded set of nested-invocation forms one at a
    time — `python3 -c "...crontab..."`, `perl -e '...'`, `xargs -I{}
    crontab {}`, and more would all follow the identical pattern). Grounded
    in DN-023's own threat model (a real, NON-adversarial incident — an
    accidental early crontab install, not a deliberate evasion attempt):
    this gate substantially raises the bar for the realistic failure mode
    (a driver script installed via a bare or simply-formed command) without
    claiming to be adversarial-proof against a determined evasion attempt,
    which no string-level heuristic in this position could honestly claim
    anyway."""
    if CRON_PATH_RE.search(command):
        return True
    has_metachar = bool(METACHAR_RE.search(command))
    tokens = _shell_tokens(command)
    if tokens is None:
        # Can't even parse the command's quoting — fail toward blocking only
        # if it plausibly mentions crontab at all; an unparseable command
        # with no such mention isn't this gate's concern.
        return "crontab" in command
    idx = _crontab_token_index(tokens)
    if idx is None:
        return False
    if has_metachar:
        return True
    i = idx + 1
    saw_l = False
    only_safe_flags = True
    while i < len(tokens):
        t = tokens[i]
        if t == "-u" and i + 1 < len(tokens):
            i += 2
            continue
        if t == "-l":
            saw_l = True
            i += 1
            continue
        only_safe_flags = False
        i += 1
    return not (saw_l and only_safe_flags)


def crontab_file_argument(command):
    """Best-effort: if `command`'s crontab invocation names a file argument
    (`crontab <path>`, no leading `-`), return it — so its ON-DISK content
    can also be searched for a tag. Returns None for `-e`/`-l`/`-r`/stdin
    (`-`) forms, or piped/multi-statement commands with no single clean file
    argument; those rely on the direct command-text tag search instead. This
    is explicitly a heuristic, not a shell parser — same documented-gap
    posture as PATH_TOKEN_RE in stop-git-durability-gate.py."""
    tokens = _shell_tokens(command)
    if tokens is None:
        return None
    idx = _crontab_token_index(tokens)
    if idx is None:
        return None
    i = idx + 1
    while i < len(tokens):
        tok = tokens[i]
        if tok == "-u" and i + 1 < len(tokens):
            i += 2  # -u <user> is a flag+value pair, not the file argument
            continue
        if tok.startswith("-"):
            i += 1
            continue
        if tok == "-":
            return None
        return tok
    return None


TAG_RE = re.compile(r"\b([A-Z]{2,8}-[0-9]+)\b")


def find_tags(text):
    if not text:
        return []
    seen = []
    for tag in TAG_RE.findall(text):
        if tag not in seen:
            seen.append(tag)
    return seen


def evidence_verifier_pass(repo_root_path, item_id):
    """Same field-value convention as pretooluse-done-gate.py's
    field_value()/verifier_veto() — a real, checked prior PASS, not just the
    tag's presence."""
    ev_path = os.path.join(repo_root_path, "docs", "agents", "evidence", f"{item_id}.md")
    if not os.path.isfile(ev_path):
        return False
    with open(ev_path) as f:
        text = f.read()
    m = re.search(r"^Verifier-verdict:[ \t]*(.*)$", text, re.MULTILINE)
    if not m:
        return False
    return m.group(1).strip().upper().startswith("PASS")


def main():
    try:
        data = json.loads(sys.stdin.read())
    except Exception:
        sys.exit(0)

    tool_name = data.get("tool_name")
    tool_input = data.get("tool_input", {}) or {}

    if tool_name not in ("Bash", "RemoteTrigger"):
        sys.exit(0)

    # Open question 3 (Tony): repo-scoped, not machine-wide. Fail open if the
    # session's cwd isn't inside this same repo, or either root can't be
    # determined — same fail-open discipline as HE-042's own repo-scope fix.
    this_repo = repo_root(os.path.abspath(__file__))
    cwd = data.get("cwd") or os.getcwd()
    session_repo = repo_root(cwd)
    if this_repo is None or session_repo is None or this_repo != session_repo:
        sys.exit(0)

    if tool_name == "Bash":
        command = tool_input.get("command", "") or ""
        if not is_crontab_write(command):
            sys.exit(0)
        searchable_texts = [command]
        file_arg = crontab_file_argument(command)
        if file_arg:
            file_path = file_arg if os.path.isabs(file_arg) else os.path.join(cwd, file_arg)
            try:
                with open(file_path) as f:
                    searchable_texts.append(f.read())
            except OSError:
                pass
        action_desc = f"Bash command installing/modifying a crontab: {command!r}"
    else:  # RemoteTrigger
        action = tool_input.get("action")
        if action not in ("create", "update"):
            sys.exit(0)
        body = tool_input.get("body", {}) or {}
        searchable_texts = [json.dumps(body)]
        action_desc = f"RemoteTrigger action={action}"

    tags = []
    for text in searchable_texts:
        for tag in find_tags(text):
            if tag not in tags:
                tags.append(tag)

    if not tags:
        print(
            "BLOCKED — live-infrastructure creation without a WORK-QUEUE tag "
            f"(DN-023/HE-028): {action_desc}",
            file=sys.stderr,
        )
        print(
            "No HE-NNN (or project-equivalent) tag found in the command/trigger content — "
            "cannot correlate this to a prior reviewed WORK-QUEUE item. Embed the item's own "
            "tag in the crontab entry's content (or the file it installs) or the RemoteTrigger "
            "body, after that item has a Verifier-verdict: PASS evidence file.",
            file=sys.stderr,
        )
        sys.exit(2)

    passing = [t for t in tags if evidence_verifier_pass(this_repo, t)]
    if not passing:
        print(
            "BLOCKED — live-infrastructure creation tagged "
            f"{', '.join(tags)}, but none has a Verifier-verdict: PASS evidence file "
            f"(DN-023/HE-028): {action_desc}",
            file=sys.stderr,
        )
        print(
            "Check docs/agents/evidence/<ID>.md exists for one of these tags and its "
            "Verifier-verdict: field is PASS before creating this crontab entry / RemoteTrigger.",
            file=sys.stderr,
        )
        sys.exit(2)

    sys.exit(0)


if __name__ == "__main__":
    main()
