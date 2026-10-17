#!/usr/bin/env python3
"""Stop hook: block ending the session while any git repo touched this session
is uncommitted or unpushed (DN-003 / E3). Deterministic replacement for the
advisory-only session-stop.sh nag, scoped to this repo's own session.

DN-005 extension: a repo may carry docs/agents/GIT-DURABILITY-DEFER.md to
explicitly, visibly, and TEMPORARILY defer blocking on pre-existing debt this
session isn't responsible for (e.g. a backlog predating rails propagation).
Every defer entry requires an expiry — on parse failure or expiry, the gate
fails toward BLOCKING, never toward silently passing."""
import sys
import json
import os
import re
import datetime
import subprocess

# Best-effort extraction of absolute-path-like tokens from a Bash command string
# (e.g. `git -C /path/to/repo ...`, `cat /path/to/repo/file`). This is a heuristic,
# not a parser — it cannot resolve shell variables, relative paths without cwd
# context, or paths built up across multiple commands. Residual gap, documented
# in docs/agents/evidence/HE-003.md rather than assumed closed: a Bash command
# that touches a repo with no absolute-path token in its own text (e.g. `cd repo
# && make` in a prior command, then a bare `git commit` in a later one with no
# path) can still be missed. False positives (checking an irrelevant path) are
# harmless here; the design goal is to minimize false NEGATIVES (missed repos).
PATH_TOKEN_RE = re.compile(r"(?:/[\w.\-]+){2,}")


def repo_root(path):
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


def touched_paths_from_transcript(transcript_path):
    paths = []
    try:
        with open(transcript_path, "r") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    entry = json.loads(line)
                except Exception:
                    continue
                message = entry.get("message", {})
                content = message.get("content", [])
                if not isinstance(content, list):
                    continue
                for block in content:
                    if not isinstance(block, dict) or block.get("type") != "tool_use":
                        continue
                    name = block.get("name")
                    tool_input = block.get("input", {}) or {}
                    if name in ("Edit", "Write"):
                        fp = tool_input.get("file_path")
                        if fp:
                            paths.append(fp)
                    elif name == "Bash":
                        command = tool_input.get("command", "") or ""
                        paths.extend(PATH_TOKEN_RE.findall(command))
    except (FileNotFoundError, OSError):
        pass
    return paths


def strip_fenced_code_blocks(text):
    return re.sub(r"```.*?```", "", text, flags=re.S)


def field_value(text, field):
    m = re.search(r"^" + re.escape(field) + r"[ \t]*(.*)$", text, re.MULTILINE)
    if not m:
        return None
    return m.group(1).strip()


def active_defer_scopes(repo):
    """Returns (scopes: set of 'working-tree'/'unpushed', notes: list[str]) for
    non-expired, well-formed DEFER entries in <repo>/docs/agents/GIT-DURABILITY-DEFER.md.
    Any parse failure (missing fields, bad date, expired) drops that entry silently
    from the deferred set — fails toward blocking, never toward passing."""
    defer_path = os.path.join(repo, "docs", "agents", "GIT-DURABILITY-DEFER.md")
    if not os.path.isfile(defer_path):
        return set(), []
    try:
        with open(defer_path, "r") as f:
            text = f.read()
    except OSError:
        return set(), []

    text = strip_fenced_code_blocks(text)
    entries = re.split(r"^## DEFER-", text, flags=re.M)[1:]
    today = datetime.date.today()
    scopes = set()
    notes = []
    for entry in entries:
        scope_raw = field_value(entry, "**Scope:**")
        expires_raw = field_value(entry, "**Expires:**")
        if not scope_raw or not expires_raw:
            continue
        try:
            expires = datetime.date.fromisoformat(expires_raw)
        except ValueError:
            continue
        if expires < today:
            continue
        scope_norm = scope_raw.lower()
        if scope_norm == "both":
            scopes.update({"working-tree", "unpushed"})
        elif scope_norm in ("working-tree", "unpushed"):
            scopes.add(scope_norm)
        else:
            continue
        notes.append(f"{scope_norm} deferred until {expires.isoformat()}")
    return scopes, notes


def git(repo, *args):
    result = subprocess.run(
        ["git", "-C", repo] + list(args), capture_output=True, text=True
    )
    return result.returncode, result.stdout.strip(), result.stderr.strip()


def check_repo(repo):
    """Returns (dirty, unpushed, no_upstream, dirty_detail, unpushed_detail)."""
    _, status_out, _ = git(repo, "status", "--short")
    dirty = bool(status_out)
    dirty_detail = f"{len(status_out.splitlines())} uncommitted file(s)" if dirty else ""

    rc, upstream, _ = git(repo, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")
    if rc != 0:
        return dirty, False, True, dirty_detail, ""

    _, ahead_out, _ = git(repo, "log", f"{upstream}..HEAD", "--oneline")
    unpushed = bool(ahead_out)
    unpushed_detail = (
        f"{len(ahead_out.splitlines())} unpushed commit(s) ahead of {upstream}" if unpushed else ""
    )
    return dirty, unpushed, False, dirty_detail, unpushed_detail


def main():
    try:
        data = json.loads(sys.stdin.read())
    except Exception:
        sys.exit(0)

    if data.get("stop_hook_active"):
        sys.exit(0)  # already blocked once this turn; don't loop (block-cap guard)

    cwd = data.get("cwd") or os.getcwd()
    transcript_path = data.get("transcript_path")

    repos = set()
    cwd_repo = repo_root(cwd)
    if cwd_repo:
        repos.add(cwd_repo)

    if transcript_path:
        for p in touched_paths_from_transcript(transcript_path):
            r = repo_root(p)
            if r:
                repos.add(r)

    blocking = []
    warnings = []
    for repo in sorted(repos):
        dirty, unpushed, no_upstream, dirty_detail, unpushed_detail = check_repo(repo)
        deferred_scopes, defer_notes = active_defer_scopes(repo)
        defer_note = f" [DEFERRED: {'; '.join(defer_notes)}]" if defer_notes else ""

        if no_upstream:
            warnings.append(f"{repo}: no upstream configured — cannot verify push state")
            if dirty:
                if "working-tree" in deferred_scopes:
                    warnings.append(f"{repo}: dirty (upstream unknown) but{defer_note}")
                else:
                    blocking.append(f"{repo}: {dirty_detail} (upstream unknown, but working tree is dirty)")
            continue

        problems = []
        if dirty and "working-tree" not in deferred_scopes:
            problems.append(dirty_detail)
        elif dirty:
            warnings.append(f"{repo}: dirty but{defer_note}")
        if unpushed and "unpushed" not in deferred_scopes:
            problems.append(unpushed_detail)
        elif unpushed:
            warnings.append(f"{repo}: unpushed but{defer_note}")
        if problems:
            blocking.append(f"{repo}: {'; '.join(problems)}")

    for w in warnings:
        print(f"WARNING (git-durability gate): {w}", file=sys.stderr)

    if blocking:
        reason = (
            "Git-durability gate (DN-003/E3): the following repo(s) touched this session are not "
            "committed and pushed:\n"
            + "\n".join(f"  - {b}" for b in blocking)
            + "\nCommit and push before stopping."
        )
        print(json.dumps({"decision": "block", "reason": reason}))
        sys.exit(0)

    sys.exit(0)


if __name__ == "__main__":
    main()
