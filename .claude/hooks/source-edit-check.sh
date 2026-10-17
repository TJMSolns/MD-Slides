#!/usr/bin/env bash
# PreToolUse hook: warn before editing source code outside of /next

INPUT=$(cat)
FILE=$(echo "$INPUT" | grep -o '"file_path":"[^"]*"' | head -1 | cut -d'"' -f4)

[ -z "$FILE" ] && exit 0

if echo "$FILE" | grep -qE "(^|/)(src|frontend/src|test)/|build\.sc$|/V[0-9]+__[^/]+\.sql$"; then
  echo ""
  echo "⚠️  HARNESS: Editing source code — $FILE"
  echo "   Was this triggered by /next? If not: use /bug to queue it and STOP."
  echo ""
fi

exit 0
