#!/usr/bin/env python3
"""Canonical, deterministic verifier-tier draw (DN-002 E2 coin). Replaces the
hand-transcribed prose in planner.md/next SKILL.md that produced one real
process slip (HE-004: P1 stake-weighting mistakenly applied to a P2 draw,
caught before submission but not by any mechanism — this script exists so it
can't happen again). Uses real randomness (python3's random module, seeded
from the OS), not a value picked by the calling agent.

DN-007 (HE-012 Part 2): optional 3rd CLI arg is a comma-separated exclude
list, for retrying after a drawn tier turned out unavailable in this
environment. The doer's own tier is the guaranteed fallback floor (it must
be callable, since the doer itself is running) — if even that's excluded,
the draw signals ESCALATE (exit 2) rather than retry indefinitely; that's a
genuinely broken environment, a job for Tony (E4), not a retry loop."""
import sys
import random

# Tier NAMES, not model versions/IDs — the Agent tool's `model` param resolves
# "sonnet"/"opus"/"haiku" to whatever the current model in that tier is, so a
# routine version bump (e.g. Sonnet 5 -> Sonnet 6) needs no change here.
# KNOWN LIMITATION (not fixed, documented instead — no API exists to discover
# tiers at runtime): if Anthropic adds a new graded tier, retires one of these
# three, or renames the category itself, this list needs a manual edit. Fable
# is deliberately excluded, not missing by oversight — DN-002/ADR-002 scoped
# it as "separate" from the graded ±1 ladder (research-findings-parcel1.md:
# "Haiku < Sonnet < Opus [Fable separate]") since it isn't a consistently
# weaker/stronger tier the way Haiku/Sonnet/Opus are.
LADDER = ["haiku", "sonnet", "opus"]


def draw(priority, doer_tier, excluded=None):
    """Returns (tier, raw_offset, final_offset, resampled). `tier` is None only
    if every tier (including doer_tier) is in `excluded` — signals ESCALATE,
    not a retryable condition (DN-007)."""
    excluded = set(excluded or [])
    doer_idx = LADDER.index(doer_tier)
    offset = random.choice([-1, 0, 1])
    raw_offset = offset
    resampled = False
    if priority == "P1" and offset == -1:
        if random.random() < 0.5:
            offset = random.choice([0, 1])
            resampled = True
    idx = max(0, min(2, doer_idx + offset))
    candidate = LADDER[idx]

    if candidate not in excluded:
        return candidate, raw_offset, offset, resampled

    # DN-007: candidate is unavailable. Fall back to the doer's own tier
    # (guaranteed callable — the doer itself is running) rather than
    # re-rolling indefinitely. If even that's excluded, there's nothing left
    # to try — signal ESCALATE.
    if doer_tier not in excluded:
        return doer_tier, raw_offset, offset, resampled
    remaining = [t for t in LADDER if t not in excluded]
    if remaining:
        return remaining[0], raw_offset, offset, resampled
    return None, raw_offset, offset, resampled


def main():
    if len(sys.argv) not in (3, 4):
        print(
            "Usage: draw-verifier-tier.py <priority-label, e.g. P1/P2/P3/P4> "
            "<doer-tier: haiku|sonnet|opus> [exclude-tiers-comma-separated]",
            file=sys.stderr,
        )
        sys.exit(1)

    priority = sys.argv[1].upper()
    doer_tier = sys.argv[2].lower()
    excluded = set()
    if len(sys.argv) == 4 and sys.argv[3].strip():
        excluded = {t.strip().lower() for t in sys.argv[3].split(",")}

    if doer_tier not in LADDER:
        print(f"Invalid doer tier: '{doer_tier}'. Must be one of {LADDER}.", file=sys.stderr)
        sys.exit(1)
    if not priority or not priority[0].isalpha():
        # Deliberately NOT hardcoded to a fixed set (P1/P2/P3) — different
        # projects use different priority-label conventions (CNX: P1/P2/P4,
        # no P3). Found live during HE-024's CNX propagation: this script,
        # copied byte-identical, hard-rejected CNX's real "P4" lane, silently
        # breaking E2 for that entire lane — the same class of gap HE-005
        # already fixed once for done_ids()'s ID-prefix regex. The ONLY
        # priority value this script treats specially is the literal "P1"
        # stake-weighting rule below; every other label is accepted and
        # treated uniformly (unweighted), so this never needs a per-project
        # edit again. Minimal sanity check only: must start with a letter.
        print(f"Invalid priority: '{priority}'. Must be a label like P1/P2/P3/P4.", file=sys.stderr)
        sys.exit(1)
    invalid_excludes = excluded - set(LADDER)
    if invalid_excludes:
        print(f"Invalid excluded tier(s): {sorted(invalid_excludes)}. Must be from {LADDER}.", file=sys.stderr)
        sys.exit(1)

    tier, raw_offset, final_offset, resampled = draw(priority, doer_tier, excluded)

    if tier is None:
        print(
            f"tier=ESCALATE raw_offset={raw_offset} final_offset={final_offset} "
            f"resampled={resampled} priority={priority} doer={doer_tier} excluded={sorted(excluded)} "
            f"(DN-007: every tier including the doer's own is excluded — not retryable, "
            f"escalate to Tony via E4 instead)"
        )
        sys.exit(2)  # distinct from the exit-1 usage-error path

    suffix = f" excluded={sorted(excluded)}" if excluded else ""
    print(
        f"tier={tier} raw_offset={raw_offset} final_offset={final_offset} "
        f"resampled={resampled} priority={priority} doer={doer_tier}{suffix}"
    )


if __name__ == "__main__":
    main()
