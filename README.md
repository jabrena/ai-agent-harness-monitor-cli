# AI Agent Harness Monitor CLI

Java CLI tool designed to increase awareness about local AI-agent harness tool files like skills, rules, guidance files, MCPs, and enhanced security scanning capabilities.

[![CI Builds](https://github.com/jabrena/ai-agent-harness-monitor-cli/actions/workflows/maven.yaml/badge.svg)](https://github.com/jabrena/ai-agent-harness-monitor-cli/actions/workflows/maven.yaml)

```bash
# Main view

╭AI Agent Harness Monitor────────────────────────────────────────────────────────────────╮
│Dynamic analysis                                                                        │
│Scan complete: 1318 findings, 1 changes                                                 │
│Configuration: audit-config.example.json                                                │
│                                                                                        │
│User harness discovery                                                                  │
│Cursor: /Users/jabrena/.cursor -> 15 detections                                         │
│Claude: /Users/jabrena/.claude -> 1 detections                                          │
│Codex: /Users/jabrena/.codex -> 0 detections                                            │
│                                                                                        │
│Project folders: [/Users/jabrena/IdeaProjects]                                          │
│Projects with detections: 30                                                            │
│                                                                                        │
│Navigation: U user, P projects, Esc quit                                                │
│                                                                                        │
│                                                                                        │
╰────────────────────────────────────────────────────────────────────────────────────────╯

# User view

╭AI Agent Harness Monitor───────────────────────────────────────────────────────────────────────────╮
│User Level Discovery                                                                               │
│B/Esc back, H overview, S Scan, N/PageDown next, R/PageUp previous                                 │
│                                                                                                   │
│Cursor: /Users/jabrena/.cursor -> 14 detections                                                    │
│Claude: /Users/jabrena/.claude -> 1 detections                                                     │
│Codex: /Users/jabrena/.codex -> 0 detections                                                       │
│                                                                                                   │
│Detected assets                                                                                    │
│Skills: 13                                                                                         │
│Rules: 0                                                                                           │
│Guidance files: 0                                                                                  │
│MCPs: 0                                                                                            │
│Config files: 2                                                                                    │
│Details (15)                                                                                       │
│Page 1/2 - press 1-9 to open detection details                                                     │
│1. Skill: babysit                                                                                  │
│2. Skill: canvas                                                                                    │
│3. Skill: create-hook                                                                              │
│4. Skill: create-rule                                                                              │
│5. Skill: create-skill                                                                             │
│6. Skill: create-subagent                                                                          │
│7. Skill: migrate-to-skills                                                                        │
│8. Skill: sdk                                                                                      │
│9. Skill: shell                                                                                    │
╰───────────────────────────────────────────────────────────────────────────────────────────────────╯

# Skill view

╭AI Agent Harness Monitor──────────────────────────────────────────────────────────────────────────╮
│Skill Details                                                                                     │
│B/Esc back, 1-9 open file, S scanner, N/PageDown next, R/PageUp previous, D delete whole skill    │
│                                                                                                  │
│Harness: Cursor                                                                                   │
│Skill: babysit                                                                                    │
│Skill root: /Users/jabrena/.cursor/skills-cursor/babysit                                          │
│Delete target: /Users/jabrena/.cursor/skills-cursor/babysit                                       │
│                                                                                                  │
│Skill files (1)                                                                                   │
│Page 1/1 - press 1-9 to open file                                                                 │
│1. SKILL.md                                                                                       │
│                                                                                                  │
│                                                                                                  │
╰──────────────────────────────────────────────────────────────────────────────────────────────────╯

# Skill view / Scanning skill

╭AI Agent Harness Monitor─────────────────────────────────────────────────────────────────────────╮
│Skill Details                                                                                    │
│B/Esc back, 1-9 open file, S scanner, N/PageDown next, R/PageUp previous, D delete whole skill   │
│                                                                                                 │
│Harness: Cursor                                                                                  │
│Skill: babysit                                                                                   │
│Skill root: /Users/jabrena/.cursor/skills-cursor/babysit                                         │
│Delete target: /Users/jabrena/.cursor/skills-cursor/babysit                                      │
│                                                                                                 │
│Skill files (1)                                                                                  │
│Page 1/1 - press 1-9 to open file                                                                │
│1. SKILL.md                                                                                      │
│                                                                                                 │
│Skill Scanner Output                                                                             │
│B/Esc back, 1-9 open file, N/PageDown next, R/PageUp previous, D delete whole skill              │
│                                                                                                 │
│Command: skill-scanner scan /Users/jabrena/.cursor/skills-cursor/babysit --use-behavioral --polic│
│Exit code: 0                                                                                     │
│14:22:33 - LiteLLM:WARNING: common_utils.py:979 - litellm: could not pre-load bedrock-runtime res│
│14:22:34 - LiteLLM:WARNING: common_utils.py:24 - litellm: could not pre-load sagemaker-runtime re│
│============================================================                                     │
│Skill: babysit                                                                                   │
│============================================================                                     │
│Status: [OK] SAFE                                                                                │
│Max Severity: INFO                                                                               │
│Total Findings: 1                                                                                │
│Scan Duration: 0.31s                                                                             │
│Findings Summary:                                                                                │
│  CRITICAL: 0                                                                                    │
│      HIGH: 0                                                                                    │
│    MEDIUM: 0                                                                                    │
│       LOW: 0                                                                                    │
│      INFO: 1                                                                                    │
│                                                                                                 │
╰─────────────────────────────────────────────────────────────────────────────────────────────────╯

# Projects view

╭AI Agent Harness Monitor───────────────────────────────────────────────────────────────────────────╮
│Project Discovery                                                                                  │
│B/Esc back, H overview, S Scan, N/PageDown next, R/PageUp previous                                 │
│                                                                                                   │
│Project folders: [/Users/jabrena/IdeaProjects]                                                     │
│Projects with detections: 29 -> 1288 detections                                                    │
│                                                                                                   │
│                                                                                                   │
│Details (29)                                                                                       │
│Page 1/3 - press 1-9 to open project details                                                       │
│1. /Users/jabrena/IdeaProjects/ai-agent-harness-monitor-cli -> 83 detections                       │
│2. /Users/jabrena/IdeaProjects/codemotion-madrid-2026-demos -> 204 detections                      │
│3. /Users/jabrena/IdeaProjects/context-mapper-cli -> 11 detections                                 │
│4. /Users/jabrena/IdeaProjects/cursor-agents-api-java-client -> 26 detections                      │
│5. /Users/jabrena/IdeaProjects/cursor-mcp-config-cli -> 8 detections                               │
│6. /Users/jabrena/IdeaProjects/java-cursor-rules -> 233 detections                                 │
│7. /Users/jabrena/IdeaProjects/jbang.dev -> 1 detections                                           │
│8. /Users/jabrena/IdeaProjects/jvm-flags -> 86 detections                                          │
│9. /Users/jabrena/IdeaProjects/plantuml-to-png-mcp -> 34 detections                                │
│                                                                                                   │
╰───────────────────────────────────────────────────────────────────────────────────────────────────╯

╭AI Agent Harness Monitor──────────────────────────────────────────────────────────────────────╮
│Project Detection Details                                                                     │
│B/Esc back, H overview, N/PageDown next, R/PageUp previous                                    │
│                                                                                              │
│Project: /Users/jabrena/IdeaProjects/cursor-rules-java                                        │
│Detections: 236                                                                               │
│Detected assets                                                                               │
│Skills: 166                                                                                   │
│Rules: 67                                                                                     │
│Guidance files: 2                                                                             │
│MCPs: 1                                                                                       │
│Config files: 0                                                                               │
│                                                                                              │
│Details (236)                                                                                 │
│Page 1/27 - press 1-9 to open detection details                                               │
│1. Skill: 001-skills-inventory                                                                │
│2. Skill: 001-skills-inventory                                                                │
│3. Skill: 002-agents-inventory                                                                │
│4. Skill: 002-agents-inventory                                                                │
│5. Skill: 003-agents-installation                                                             │
│6. Skill: 003-agents-installation                                                             │
│7. Skill: 012-agile-epic                                                                      │
│8. Skill: 012-agile-epic                                                                      │
│9. Skill: 013-agile-feature                                                                   │
╰──────────────────────────────────────────────────────────────────────────────────────────────╯

╭AI Agent Harness Monitor──────────────────────────────────────────────────────────────────────────────────────╮
│Skill Details                                                                                                 │
│B/Esc back, 1-9 open file, S scanner, N/PageDown next, R/PageUp previous, D delete whole skill                │
│                                                                                                              │
│Harness: Cursor                                                                                               │
│Skill: 001-skills-inventory                                                                                   │
│Skill root: /Users/jabrena/IdeaProjects/cursor-rules-java/.agents/skills/001-skills-inventory                 │
│Delete target: /Users/jabrena/IdeaProjects/cursor-rules-java/.agents/skills/001-skills-inventory              │
│                                                                                                              │
│Skill files (2)                                                                                               │
│Page 1/1 - press 1-9 to open file                                                                             │
│1. SKILL.md                                                                                                   │
│2. references/001-skills-inventory.md                                                                         │
│                                                                                                              │
│                                                                                                              │
╰──────────────────────────────────────────────────────────────────────────────────────────────────────────────╯
```

## How to run in local

```bash
./mvnw clean verify
./mvnw clean package
java -jar target/ai-agent-harness-monitor-cli-0.1.0-SNAPSHOT.jar init --config ./audit-config.example.json
```

## Example about configuration

```json
{
  "user": "jabrena",
  "include-dirs": [
    "/Users/jabrena/IdeaProjects/"
  ],
  "exclude-dirs": [ 
    "cursor-rules-agile",
    "cursor-rules-spring-boot"
  ],
  "exclude-files": [
    "AGENTS.md",
    "CLAUDE.md",
    "mcp.json"
  ],
  "interval-seconds": 60,
  "report-type": ["json"],
  "report-output-dir": "./target/audit-test-output"
}
```

## How to install cisco-ai-skill-scanner

```bash
uv tool install cisco-ai-skill-scanner
skill-scanner scan ./my-skill --use-behavioral
```

## References

- [cisco-ai-defense/skill-scanner](https://github.com/cisco-ai-defense/skill-scanner)
