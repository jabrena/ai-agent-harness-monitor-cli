# AI Agent Harness Monitor CLI

Java CLI for auditing local AI-agent harness configuration.

```
╭AI Agent Harness Monitor───────────────────────────────────────────────────────────────────╮
│Dynamic analysis                                                                           │
│Scan complete: 945 findings, 1 changes                                                     │
│Scan update: 60s                                                                           │
│Report: /Users/jabrena/IdeaProjects/ai-agent-harness-monitor-cli/target/audit-test-output/ │
│                                                                                           │
│User harness discovery                                                                     │
│Cursor: /Users/jabrena/.cursor -> 2 detections                                             │
│Claude: /Users/jabrena/.claude -> 1 detections                                             │
│Codex: /Users/jabrena/.codex -> 0 detections                                               │
│                                                                                           │
│Project folders: [/Users/jabrena/IdeaProjects]                                             │
│Projects discovered: 116 -> 942 detections                                                 │
│                                                                                           │
│Navigation: 1 Cursor, 2 Claude, 3 Codex, U user, P projects, Esc quit                      │
│                                                                                           │
│                                                                                           │
╰───────────────────────────────────────────────────────────────────────────────────────────╯
```

## How to run in local:

```bash
./mvnw clean verify
./mvnw clean package
java -jar target/ai-agent-harness-monitor-cli-0.1.0-SNAPSHOT.jar init --config ./audit-config.example.json
```

## Example about configuration:

```json
{
  "user": "jabrena",
  "projects-dirs": [
    "/Users/jabrena/IdeaProjects/"
  ],
  "interval-seconds": 60,
  "report-type": ["json"],
  "report-output-dir": "./target/audit-test-output"
}
```