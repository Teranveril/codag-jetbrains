#!/usr/bin/env bash
set -euo pipefail

# Session POST-HOOK: runs at session end
# Verifies build, tests, commits changes, pushes, logs progress

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JETBRAINS_DIR="$PROJECT_ROOT/packages/jetbrains"
SESSION_LOG="$PROJECT_ROOT/.githooks/session/session.log"
JAVA_HOME="${JAVA_HOME:-/Applications/PhpStorm.app/Contents/jbr/Contents/Home}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

echo "═══════════════════════════════════════════════"
echo "✅ SESSION POST-HOOK — $(date '+%Y-%m-%d %H:%M')"
echo "═══════════════════════════════════════════════"

ERRORS=0

# 1. Run build
echo ""
echo "📦 Building plugin..."
if [ -f "$JETBRAINS_DIR/gradlew" ]; then
    (cd "$JETBRAINS_DIR" && ./gradlew build --quiet 2>&1) && {
        echo "  Build: ✅ PASSED"
    } || {
        echo "  Build: ❌ FAILED"
        ERRORS=$((ERRORS + 1))
    }
else
    echo "  Build: ⚠️  No gradlew — skipping"
fi

# 2. Run tests
echo ""
echo "🧪 Running tests..."
if [ -f "$JETBRAINS_DIR/gradlew" ]; then
    TEST_OUTPUT=$( (cd "$JETBRAINS_DIR" && ./gradlew test 2>&1) ) && {
        PASSED=$(echo "$TEST_OUTPUT" | grep -c "PASSED" || true)
        FAILED=$(echo "$TEST_OUTPUT" | grep -c "FAILED" || true)
        echo "  Tests: ✅ $PASSED passed, $FAILED failed"
    } || {
        echo "  Tests: ❌ FAILED"
        echo "$TEST_OUTPUT" | tail -20
        ERRORS=$((ERRORS + 1))
    }
fi

# 3. Check dirty state
echo ""
echo "📋 Git Status:"
DIRTY=$(git -C "$PROJECT_ROOT" status --short | wc -l | tr -d ' ')
echo "  Uncommitted files: $DIRTY"
if [ "$DIRTY" -gt 0 ]; then
    git -C "$PROJECT_ROOT" --no-pager status --short | head -20
fi

# 4. Verify plugin structure
echo ""
echo "🔍 Plugin Structure Verification:"
if [ -f "$JETBRAINS_DIR/gradlew" ]; then
    (cd "$JETBRAINS_DIR" && ./gradlew verifyPluginStructure --quiet 2>&1) && {
        echo "  Structure: ✅ VALID"
    } || {
        echo "  Structure: ⚠️  Issues found (non-blocking)"
    }
fi

# 5. Summary
echo ""
echo "═══════════════════════════════════════════════"
if [ "$ERRORS" -eq 0 ]; then
    echo "🎉 Session completed successfully. Safe to push."
else
    echo "⚠️  Session completed with $ERRORS error(s). Fix before push."
fi
echo "═══════════════════════════════════════════════"

# 6. Log
SRC_COUNT=$(find "$JETBRAINS_DIR/src/main" -name "*.kt" 2>/dev/null | wc -l | tr -d ' ')
TEST_COUNT=$(find "$JETBRAINS_DIR/src/test" -name "*.kt" 2>/dev/null | wc -l | tr -d ' ')
LAST_COMMIT=$(git -C "$PROJECT_ROOT" --no-pager log -1 --oneline 2>/dev/null || echo "none")
echo "[POST] $(date -u '+%Y-%m-%dT%H:%M:%SZ') branch=$(git -C "$PROJECT_ROOT" branch --show-current) tests=$TEST_COUNT src=$SRC_COUNT errors=$ERRORS commit=\"$LAST_COMMIT\"" >> "$SESSION_LOG"
