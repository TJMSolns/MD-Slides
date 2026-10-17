#!/usr/bin/env python3
"""PreToolUse gate: block moving a WORK-QUEUE item into ## Done without a valid
evidence artifact at docs/agents/evidence/<ID>.md (DN-001/GATE-004; DN-002 E2
extension; DN-006 transcript corroboration, closing Gap 1)."""
import sys
import json
import re
import os


def repo_root(path):
    """Same walk-up-to-.git idiom as stop-git-durability-gate.py's own repo_root()
    (DN-003) — not a new pattern. Returns None if no .git is found (e.g. the org
    root, a plain Dropbox directory per DN-017 — this hook was never intended to
    gate that queue and has no copy installed there anyway)."""
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

REQUIRED_FIELDS = [
    "Commit:",
    "Run-count:",
    "Invariance-recheck:",
    "Verified-by:",
    "Verifier-tier:",
    "Verifier-verdict:",
]

TIER_NAMES = ("haiku", "sonnet", "opus")  # tier NAMES not model IDs — see draw-verifier-tier.py


def done_ids(text):
    """Item-ID prefix is intentionally generic (not hardcoded to HE-) so this
    hook works unmodified when propagated to another project's WORK-QUEUE.md
    with its own convention (e.g. mdslides' MS-NNN) — caught while attempting
    HE-005's propagation: the original HE-only regex would have made this gate
    a silent no-op on any other project's queue, never matching any ID."""
    m = re.search(r"^## Done\b(.*)", text, re.S | re.M)
    if not m:
        return set()
    section = m.group(1)
    m2 = re.search(r"^## ", section, re.M)
    if m2:
        section = section[: m2.start()]
    return set(re.findall(r"\|\s*([A-Z]{2,8}-[A-Za-z0-9]+)\s*\|", section))


def field_value(evidence_text, field):
    """Match the field's value on ITS OWN LINE only. `[ \\t]*` (not `\\s*`) and the
    `$`/MULTILINE anchor prevent a blank value from capturing the next line's
    content — `\\s*(.+)` without anchoring did exactly that (found by the E2
    verifier's independent re-check on HE-002: an empty `Commit:` line was being
    satisfied by the following `Run-count:` line's text)."""
    m = re.search(r"^" + re.escape(field) + r"[ \t]*(.*)$", evidence_text, re.MULTILINE)
    if not m:
        return None
    return m.group(1).strip()


def missing_field(evidence_text):
    for field in REQUIRED_FIELDS:
        value = field_value(evidence_text, field)
        if not value:
            return field
    return None


def verifier_veto(evidence_text):
    """DN-002/DN-004: structural veto — anything other than PASS blocks regardless
    of how complete the other fields are. Distinguishes VETO (doer should fix and
    re-verify) from ESCALATE (DN-004: do NOT loop fix->re-verify — this needs
    Tony; log to docs/agents/ESCALATIONS.md instead) purely in the message text —
    both block identically at the hook layer."""
    verdict = field_value(evidence_text, "Verifier-verdict:")
    if not verdict:
        return "Verifier-verdict: field missing", "missing"
    if verdict.upper().startswith("ESCALATE"):
        return verdict, "escalate"
    if not verdict.upper().startswith("PASS"):
        return verdict, "veto"
    return None, None


def claimed_tier(evidence_text):
    """Extracts just the tier NAME from Verifier-tier:, which typically also
    carries explanatory prose (e.g. 'opus (drawn via real python3 random...)')."""
    raw = field_value(evidence_text, "Verifier-tier:")
    if not raw:
        return None
    m = re.match(r"(" + "|".join(TIER_NAMES) + r")", raw, re.IGNORECASE)
    return m.group(1).lower() if m else None


def iter_transcript_entries(transcript_text):
    """Decodes each transcript.jsonl line as JSON and yields the parsed entry.
    CRITICAL: all downstream regex/text matching must operate on values
    pulled from these decoded entries, never on transcript_text directly —
    the raw file bytes contain JSON-escaped sequences (e.g. a literal
    backslash-n, not a real newline byte) inside string values. A prior
    version of this hook regex-matched the raw file text directly; it
    happened to work for an `.endswith("PASS")` check (which doesn't care
    about line structure) but silently broke a later, more precise
    line-based check (`result_is_pass`) that requires real newlines to
    exist. Caught only because that later check was tested against this
    session's own real transcript, not just synthetic data."""
    for line in transcript_text.splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            yield json.loads(line)
        except Exception:
            continue


def iter_message_texts(transcript_text):
    """Yields the JSON-DECODED text content of every text-type content block
    across the transcript — real newlines, not escape sequences.

    HE-043: a completed Agent-tool sub-task's notification can also arrive as a
    top-level `{"type": "queue-operation", "operation": "enqueue", ...,
    "content": "<task-notification>..."}` entry — no "message" key at all.
    Confirmed live (MS-017, 2026-07-11): a dispatched implementation subagent's
    own nested verifier dispatch delivered its completion this way, and this
    function's prior message-content-only path never yielded it, making a
    genuine PASS invisible to transcript_corroborates_pass() (which returns
    False, not None, for a readable-but-blind transcript — a hard, silent
    false-negative block on a real Done-transition). Additive only — the
    existing message.content path is unchanged."""
    for entry in iter_transcript_entries(transcript_text):
        if entry.get("type") == "queue-operation":
            content = entry.get("content")
            if isinstance(content, str):
                yield content
        content = entry.get("message", {}).get("content", [])
        if isinstance(content, str):
            yield content
            continue
        if not isinstance(content, list):
            continue
        for block in content:
            if isinstance(block, dict) and block.get("type") == "text":
                yield block.get("text", "")


FALLBACK_MARKER = "VERIFIER-FALLBACK (DN-010):"


def parse_verifier_agent_calls(transcript_text):
    """Yields (tool_use_id, model) for every Agent tool_use entry in the
    transcript with subagent_type == 'verifier' — OR (DN-010, HE-016) a
    'general-purpose' call whose prompt starts with FALLBACK_MARKER, used
    when subagent_type: 'verifier' itself is unavailable (not the same as
    DN-007's model-tier-unavailable case, which is a different axis: model
    still resolves to 'verifier', just a different tier). Without this
    second branch, the documented fallback procedure (next SKILL.md) would
    let a real, independent verification happen that DN-006 still couldn't
    corroborate — closing a gap of the same shape DN-008 found for
    Workflow-dispatched verifiers, before it ever shipped silently broken."""
    calls = []
    for entry in iter_transcript_entries(transcript_text):
        content = entry.get("message", {}).get("content", [])
        if not isinstance(content, list):
            continue
        for block in content:
            if not isinstance(block, dict) or block.get("type") != "tool_use":
                continue
            if block.get("name") != "Agent":
                continue
            inp = block.get("input", {}) or {}
            if inp.get("subagent_type") == "verifier":
                calls.append((block.get("id"), inp.get("model")))
            elif inp.get("subagent_type") == "general-purpose":
                prompt = inp.get("prompt", "") or ""
                if prompt.startswith(FALLBACK_MARKER):
                    calls.append((block.get("id"), inp.get("model")))
    return calls


NOTIFICATION_RE = re.compile(
    r"<task-notification>.*?<tool-use-id>(?P<tid>[^<]*)</tool-use-id>.*?"
    r"<status>(?P<status>[^<]*)</status>.*?<result>(?P<result>.*?)</result>.*?"
    r"</task-notification>",
    re.S,
)


def find_notification_result(transcript_text, tool_use_id):
    """Searches DECODED message texts (see iter_message_texts) for a
    completed <task-notification> matching tool_use_id. Returns the result
    text, or None if no completed match is found."""
    for text in iter_message_texts(transcript_text):
        if tool_use_id not in text:
            continue
        for m in NOTIFICATION_RE.finditer(text):
            if m.group("tid") == tool_use_id and m.group("status") == "completed":
                return m.group("result")
    return None


FENCE_LINE_RE = re.compile(r"^```\w*$")


def result_is_pass(result_text):
    """Requires the LAST NON-EMPTY, NON-FENCE-DELIMITER LINE to be exactly
    PASS (case-insensitive) per the verifier's own output contract ('exactly
    one of PASS/VETO:.../ESCALATE:...', nothing else) — NOT just any text
    ending in the substring 'PASS', which would wrongly match a result
    ending in a word like 'bypass' ("bypass".upper().endswith("PASS") is
    True in Python).

    Bare markdown code-fence delimiter lines (```` ``` ```` or ```` ```lang ````)
    are stripped before checking the last line — found live in production
    (HE-012's own verifier run): a model sometimes wraps its otherwise-bare
    `PASS` output in a fence purely as a formatting habit, making the fence
    marker the literal last line. Only bare delimiter lines are stripped,
    never fenced CONTENT — a VETO reason legitimately quoting a code block
    keeps that content; only the ``` lines themselves are ignored."""
    lines = [line.strip() for line in result_text.splitlines() if line.strip()]
    lines = [line for line in lines if not FENCE_LINE_RE.match(line)]
    if not lines:
        return False
    return lines[-1].upper() == "PASS"


def transcript_corroborates_pass(transcript_path, expected_tier):
    """DN-006: cross-check a claimed PASS against the session transcript
    rather than trusting the evidence file's Verifier-verdict field alone.

    Returns True/False when the transcript could actually be read and
    checked (False = readable but does NOT corroborate — a real finding).
    Returns None ONLY when the transcript is genuinely unavailable (missing
    path, missing file, unreadable) — infrastructure absence degrades to the
    pre-DN-006 evidence-field-only check rather than blocking on a systemic
    condition unrelated to this specific claim."""
    if not transcript_path or not os.path.isfile(transcript_path):
        return None
    try:
        with open(transcript_path, "r") as f:
            text = f.read()
    except OSError:
        return None

    for tool_use_id, model in parse_verifier_agent_calls(text):
        if model != expected_tier:
            continue
        result = find_notification_result(text, tool_use_id)
        if result and result_is_pass(result):
            return True
    return False


def main():
    try:
        data = json.loads(sys.stdin.read())
    except Exception:
        sys.exit(0)

    tool_name = data.get("tool_name")
    tool_input = data.get("tool_input", {}) or {}
    file_path = tool_input.get("file_path", "")

    if tool_name not in ("Edit", "Write"):
        sys.exit(0)
    if not file_path.replace("\\", "/").endswith("docs/agents/WORK-QUEUE.md"):
        sys.exit(0)

    # HE-042: this gate must only apply within the SAME repo this hook file lives
    # in — never to a same-named file in a different repo just because the
    # current session's --add-dir grants make it reachable (confirmed live,
    # GL-030 2026-07-10: a groom subagent editing keystone's own WORK-QUEUE.md
    # was wrongly blocked by harness-evolution's own Done-gate policy). Fail
    # open (never block) if the two repo roots don't match, or if either can't
    # be determined (e.g. a non-git root — DN-017's org-root case).
    this_repo = repo_root(os.path.abspath(__file__))
    target_repo = repo_root(file_path)
    if this_repo is None or target_repo is None or this_repo != target_repo:
        sys.exit(0)

    try:
        with open(file_path, "r") as f:
            old_content = f.read()
    except FileNotFoundError:
        old_content = ""

    if tool_name == "Edit":
        old_string = tool_input.get("old_string", "")
        new_string = tool_input.get("new_string", "")
        if not old_string or old_string not in old_content:
            sys.exit(0)  # can't reliably reconstruct post-edit state; fail open
        if tool_input.get("replace_all"):
            new_content = old_content.replace(old_string, new_string)
        else:
            new_content = old_content.replace(old_string, new_string, 1)
    else:
        new_content = tool_input.get("content", "")

    newly_done = done_ids(new_content) - done_ids(old_content)
    if not newly_done:
        sys.exit(0)

    evidence_dir = os.path.join(os.path.dirname(os.path.abspath(file_path)), "evidence")
    problems = []
    for item_id in sorted(newly_done):
        ev_path = os.path.join(evidence_dir, f"{item_id}.md")
        if not os.path.isfile(ev_path):
            problems.append(f"{item_id}: no evidence file at docs/agents/evidence/{item_id}.md")
            continue
        with open(ev_path) as f:
            ev_text = f.read()
        field = missing_field(ev_text)
        if field:
            problems.append(f"{item_id}: evidence file missing/empty required field '{field}'")
            continue
        veto, kind = verifier_veto(ev_text)
        if veto:
            if kind == "escalate":
                problems.append(
                    f"{item_id}: verifier ESCALATED, not VETOed — {veto} "
                    f"(do NOT fix-and-retry; log to docs/agents/ESCALATIONS.md and wait for Tony)"
                )
            else:
                problems.append(f"{item_id}: verifier did not PASS — {veto}")
            continue

        # DN-006: corroborate a claimed PASS against the session transcript —
        # closes Gap 1 (DN-002), a doer could otherwise hand-write PASS
        # without ever spawning a verifier. Infrastructure absence (None)
        # degrades to the pre-DN-006 check; an actual mismatch (False) blocks.
        tier = claimed_tier(ev_text)
        corroborated = transcript_corroborates_pass(data.get("transcript_path"), tier)
        if corroborated is False:
            problems.append(
                f"{item_id}: Verifier-verdict: PASS is not corroborated by the session transcript "
                f"(DN-006) — no completed 'verifier' Agent call at tier '{tier}' with a matching PASS "
                f"result was found. This could mean the verifier was never actually spawned."
            )

    if problems:
        print("BLOCKED — Done transition without valid evidence artifact (DN-001/GATE-004; DN-002 E2; DN-004 E4; DN-006):", file=sys.stderr)
        for p in problems:
            print(f"  - {p}", file=sys.stderr)
        print(
            "Create docs/agents/evidence/<ID>.md with Commit:/Run-count:/Invariance-recheck:/Verified-by:/"
            "Verifier-tier:/Verifier-verdict: (PASS required) before marking Done. An ESCALATE verdict means "
            "stop and log to docs/agents/ESCALATIONS.md instead of retrying.",
            file=sys.stderr,
        )
        sys.exit(2)

    sys.exit(0)


if __name__ == "__main__":
    main()
