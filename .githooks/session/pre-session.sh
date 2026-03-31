#!/usr/bin/env bash
set -euo pipefail

# Session PRE-HOOK: runs at session start
# Audits previous session state, builds plan for current session

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JETBRAINS_DIR="$PROJECT_ROOT/packages/jetbrains"
SESSION_LOG="$PROJECT_ROOT/.githooks/session/session.log"
PLAN_FILE="$PROJECT_ROOT/.githooks/session/current-plan.md"

echo "═══════════════════════════════════════════════"
echo "🔍 SESSION PRE-HOOK — $(date '+%Y-%m-%d %H:%M')"
echo "═══════════════════════════════════════════════"

# 1. Git state audit
echo ""
echo "📋 Git State:"
echo "  Branch: $(git -C "$PROJECT_ROOT" branch --show-current)"
echo "  Remote: $(git -C "$PROJECT_ROOT" remote get-url origin)"
echo "  Upstream: $(git -C "$PROJECT_ROOT" remote get-url upstream 2>/dev/null || echo 'not set')"
echo "  Last commit: $(git -C "$PROJECT_ROOT" --no-pager log -1 --oneline 2>/dev/null || echo 'none')"
echo "  Dirty files: $(git -C "$PROJECT_ROOT" status --short | wc -l | tr -d ' ')"

# 2. Build state audit
echo ""
echo "📦 Build State:"
if [ -f "$JETBRAINS_DIR/gradlew" ]; then
    echo "  Gradle wrapper: ✅ present"
else
    echo "  Gradle wrapper: ❌ missing"
fi

if [ -d "$JETBRAINS_DIR/build/distributions" ]; then
    echo "  Plugin artifact: ✅ $(ls "$JETBRAINS_DIR/build/distributions/" 2>/dev/null | head -1)"
else
    echo "  Plugin artifact: ⚠️  not built yet"
fi

# 3. Test state audit
echo ""
echo "🧪 Test Files:"
TEST_COUNT=$(find "$JETBRAINS_DIR/src/test" -name "*.kt" 2>/dev/null | wc -l | tr -d ' ')
echo "  Test files: $TEST_COUNT"
find "$JETBRAINS_DIR/src/test" -name "*.kt" 2>/dev/null | while read -r f; do
    echo "    - $(basename "$f")"
done

# 4. Source files audit
echo ""
echo "📁 Source Files:"
SRC_COUNT=$(find "$JETBRAINS_DIR/src/main" -name "*.kt" 2>/dev/null | wc -l | tr -d ' ')
echo "  Source files: $SRC_COUNT"
find "$JETBRAINS_DIR/src/main" -name "*.kt" 2>/dev/null | while read -r f; do
    echo "    - $(basename "$f")"
done

# 5. CI state (last workflow run)
echo ""
echo "🔄 CI State:"
if command -v gh &>/dev/null; then
    LAST_RUN=$(gh run list --repo Teranveril/codag-jetbrains --workflow=jetbrains-plugin.yml --limit 1 --json status,conclusion,headBranch,createdAt 2>/dev/null || echo "[]")
    if [ "$LAST_RUN" != "[]" ] && [ -n "$LAST_RUN" ]; then
        echo "  $LAST_RUN"
    else
        echo "  No CI runs yet"
    fi
else
    echo "  gh CLI not available"
fi

# 6. Session plan
echo ""
echo "═══════════════════════════════════════════════"
echo "📝 Ready for session work. Audit complete."
echo "═══════════════════════════════════════════════"

# Log audit
echo "[PRE] $(date -u '+%Y-%m-%dT%H:%M:%SZ') branch=$(git -C "$PROJECT_ROOT" branch --show-current) tests=$TEST_COUNT src=$SRC_COUNT" >> "$SESSION_LOG"
