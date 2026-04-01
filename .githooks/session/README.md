# Session Hooks

## Usage

Before starting work on the plugin:
```bash
bash .githooks/session/pre-session.sh
```

After completing work:
```bash
bash .githooks/session/post-session.sh
```

## What they do

### pre-session.sh
- Audits git state (branch, remote, dirty files)
- Checks build state (gradlew, artifacts)
- Counts test and source files
- Checks last CI run status via `gh`
- Logs audit to `session.log`

### post-session.sh
- Runs `./gradlew build`
- Runs `./gradlew test`
- Reports pass/fail counts
- Verifies plugin structure
- Logs results to `session.log`

## Git Hooks (automatic)

### .githooks/pre-commit
Runs tests on staged JetBrains plugin changes before commit.

### .githooks/commit-msg
Validates Conventional Commits format:
```
<type>(<scope>): <description>
```
Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
