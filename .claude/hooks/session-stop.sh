#!/usr/bin/env bash
QUEUE="docs/agents/WORK-QUEUE.md"
if [ -f "$QUEUE" ] && grep -q "in_progress" "$QUEUE" 2>/dev/null; then
  COUNT=$(grep -c "in_progress" "$QUEUE" 2>/dev/null || echo "some")
  echo "⚠  You have $COUNT in_progress item(s). Run /handoff before closing."
fi
