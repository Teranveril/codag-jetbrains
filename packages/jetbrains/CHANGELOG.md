# Changelog

All notable changes to the Codag JetBrains Plugin are documented in this file.
Format follows [Conventional Commits](https://www.conventionalcommits.org/).

## [0.1.0] — 2026-03-31

### Added

- **feat(jetbrains):** Scaffold plugin project with TDD, CI/CD and session hooks
- **feat(dto):** WorkflowGraph DTOs, API request/response models, project settings
- **feat(api):** CodagApiClient with JSON parsing, health check, workflow analysis
- **feat(webview):** JCEF panel with VS Code ↔ JCEF message bridge and bundled web resources
  - `codag-jcef-shim.js` replaces `acquireVsCodeApi()` with `cefQuery` bridge
  - Bundled ELK.js + D3.js graph renderer (3.6 MB)
- **feat(analysis):** File analysis pipeline, source navigation, LRU caching, IDE action
  - `Ctrl+Shift+G` / context menu: "Analyze with Codag"
  - Supported: `.py`, `.ts`, `.js`, `.kt`, `.java`, `.go`, `.rs`, `.rb`, `.php` + more
- **feat(watch):** File watcher, status bar widget, theme detection, health monitoring
  - BulkFileListener with debouncing and excluded-directory filtering
  - Status bar: real-time backend connectivity (connected / disconnected / api key invalid)
  - IDE theme auto-detection → webview CSS class (dark / light / high-contrast)
  - Periodic health check (30s interval) via PostStartupActivity

### Infrastructure

- GitHub Actions CI: build → test → verify → plugin verifier
- Git hooks: pre-commit (lint + test), commit-msg (conventional commits)
- Session hooks: pre-session audit, post-session verification
- TDD: 113 tests across 6 sessions, all passing

### Compatibility

- JetBrains Platform 2024.1+ (IntelliJ, PHPStorm, WebStorm, PyCharm, etc.)
- Plugin Verifier: **Compatible** with IC-241
- Dynamic Plugin: can be enabled/disabled without IDE restart
