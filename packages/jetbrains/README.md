# Codag JetBrains Plugin

> Visualize AI/LLM workflows in your codebase as interactive DAG graphs — inside any JetBrains IDE.

Port of the [Codag VS Code extension](https://github.com/michaelzixizhou/codag) to JetBrains Platform (IntelliJ IDEA, PHPStorm, WebStorm, PyCharm, GoLand, etc.).

## Features

- **Interactive DAG Visualization** — ELK.js layout engine with D3.js SVG rendering
- **Gemini-Powered Analysis** — detect LLM calls, agent workflows, function chains
- **Source Navigation** — click graph nodes to jump to source code
- **File Analysis Pipeline** — analyze selected files via `Ctrl+Shift+G` or context menu
- **LRU Cache** — skip re-analysis when files haven't changed
- **File Watching** — auto-detect file changes with debouncing
- **Theme Support** — auto-detects IDE dark/light/high-contrast theme
- **Status Bar Widget** — real-time backend connectivity status
- **Periodic Health Check** — monitors backend every 30 seconds

## Requirements

- JetBrains IDE 2024.1+ (with JCEF support)
- [Codag Backend](../../README.md) running on `localhost:52104` (configurable)
- Gemini API key configured in backend `.env`

## Installation

### From ZIP (manual)

1. Download `Codag-0.1.0.zip` from [Releases](https://github.com/Teranveril/codag-jetbrains/releases)
2. IDE → Settings → Plugins → ⚙️ → Install Plugin from Disk → select ZIP
3. Restart IDE

### From Source

```bash
cd packages/jetbrains
export JAVA_HOME="/path/to/jbr"  # JetBrains Runtime with JCEF
./gradlew buildPlugin
# ZIP at build/distributions/Codag-0.1.0.zip
```

## Usage

1. Start Codag backend: `cd backend && python -m uvicorn app:app --port 52104`
2. Open a project in your JetBrains IDE
3. Select source files → Right-click → **"Analyze with Codag"** (or `Ctrl+Shift+G`)
4. View the graph in the **Codag** tool window (right panel)
5. Click nodes to navigate to source code

## Configuration

Settings → Tools → Codag:

| Setting | Default | Description |
|---------|---------|-------------|
| Backend URL | `http://localhost:52104` | Codag backend address |
| Timeout | `30000` ms | HTTP request timeout |
| Cache Enabled | `true` | Skip re-analysis when content unchanged |
| Auto Analyze | `false` | Automatically analyze on file save |

## Architecture

```
┌─────────────────────────────────────────────┐
│ JetBrains IDE (Kotlin Plugin)               │
│                                             │
│  ┌──────────┐  ┌───────────┐  ┌──────────┐ │
│  │ Settings │  │ API Client│  │ Pipeline  │ │
│  │   DTO    │  │  HTTP/JSON│  │ + Cache   │ │
│  └──────────┘  └─────┬─────┘  └────┬─────┘ │
│                      │             │        │
│  ┌───────────────────┴─────────────┘        │
│  │ JCEF WebView + Message Bridge            │
│  │  codag-jcef-shim.js ↔ JBCefJSQuery      │
│  └───────────────────┬──────────────────────┘
│                      │                       │
└──────────────────────┼───────────────────────┘
                       │
┌──────────────────────┴───────────────────────┐
│ Codag Backend (Python, FastAPI, port 52104)  │
│  Gemini API → AST Analysis → Graph Builder   │
└──────────────────────────────────────────────┘
```

## Development

```bash
# Build
./gradlew buildPlugin

# Test (113 tests)
./gradlew test

# Verify compatibility
./gradlew verifyPlugin

# Run in sandbox IDE
./gradlew runIde
```

## Tests

| Package | Tests | Coverage |
|---------|-------|----------|
| `dto` | 13 | DTOs, settings, defaults |
| `api` | 9 | HTTP client, JSON parsing |
| `webview` | 27 | Resources, bridge, HTML, tool window |
| `pipeline` | 28 | Pipeline, navigator, cache, action |
| `watch` | 23 | File filter, health monitor, theme, registration |
| **scaffold** | 13 | Build config, plugin.xml |
| **Total** | **113** | All passing ✅ |

## License

MIT — see [LICENSE](../../LICENSE)
