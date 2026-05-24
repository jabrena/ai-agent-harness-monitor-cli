# Maintenance

Some **User prompts** designed to help in the maintenance of this repository.

## Begin a new release

### Bump a new Snapshot version

```bash
# Maven command to update the maven version to next minor version
./mvnw versions:set -DnewVersion=0.1.0
./mvnw versions:commit
```

## Finish a release

```bash
# Prompt to provide a release changelog
Can you update the current changelog for 0.1.0 comparing git commits in relation to 0.1.0 tag. Use  @https://keepachangelog.com/en/1.1.0/  rules

```

## Release process

- [ ] Update CHANGELOG.md
- [ ] Remove SNAPSHOT in pom.xml
- [ ] Last review in docs (Manual)
- [ ] Review git changes for hidden issues (Manual) <https://github.com/jabrena/ai-agent-harness-monitor-cli/compare/0.1.0...feature/release-020>
- [ ] Tag repository
- [ ] Publish JBang
`
---

```bash

# Maven command to update the maven version to next minor version
./mvnw versions:set -DnewVersion=0.15.0-SNAPSHOT
./mvnw versions:commit

## Note: Refactor a bit more to include all pom.xml

## Tagging process
git tag --list
git tag 0.15.0
git push --tags
```
