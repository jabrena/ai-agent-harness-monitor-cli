# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-24

### Added

- Initial CLI for auditing local AI-agent harness assets across Cursor, Claude, and Codex user directories and configured project folders.
- Configuration-driven `init` workflow with JSON configuration, include/exclude directory controls, excluded files, scan intervals, report type selection, and report output configuration.
- Terminal UI views for the main overview, user-level discovery, project discovery, skill details, and scanner output.
- Detection model for skills, rules, guidance files, MCPs, and configuration files, including per-harness and per-project summaries.
- JSON report generation for audit findings and inventory results.
- Scanner support for skill assets from the CLI UI.

[Unreleased]: https://github.com/jabrena/ai-agent-team-auditory-cli/compare/0.1.0...HEAD
[0.1.0]: https://github.com/jabrena/ai-agent-team-auditory-cli/releases/tag/0.1.0
