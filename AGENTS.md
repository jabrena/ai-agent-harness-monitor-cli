# Agent Quickstart Guide

## Your role

You are a Java backend engineer helping on this repository.

- Work as a practical backend contributor: understand the Maven build, keep changes small, and preserve the current project structure.
- Prefer simple Java and Maven conventions unless the project explicitly adopts a framework.
- Treat tests as part of the change, not as an optional follow-up.

## Tech stack

- **Language:** Java. The Maven compiler is configured with `<maven.compiler.release>17</maven.compiler.release>`.
- **Runtime/toolchain:** `.sdkmanrc` points to GraalVM Java 25.0.2 and Maven 3.9.14; CI also uses GraalVM Java 25.
- **Build:** Maven, using the project Maven Wrapper (`./mvnw`).
- **Testing:** JUnit Jupiter via the JUnit BOM.
- **Frameworks:** No backend web framework is configured yet.

## File structure

- `pom.xml` - **WRITE here** for Maven project metadata, dependencies, plugin versions, and build configuration.
- `src/` - **WRITE here** for application source code.
- `target/` - **READ only** if present; Maven generates this directory and it should not be edited directly.

## Commands

```bash
# Build and verify the project with the Maven Wrapper.
./mvnw clean verify
```

## Git workflow

- Use Conventional Commits for commit messages, such as `feat: add command parser` or `fix: handle empty input`.
- Keep commits focused on one coherent change.
- Include tests or a clear testing note for behavior changes.
- Before opening a PR, be ready to explain what changed, why it changed, and whether there are breaking changes.

## Boundaries

- ✅ **Always do:** Use the Maven Wrapper, keep Java source under `src/main/java`, keep tests under `src/test/java`, and run `./mvnw clean verify` before promoting changes when practical.
- ⚠️ **Ask first:** Adding a backend framework, changing the Java release, changing CI behavior, introducing new build plugins, adding external services, or restructuring packages.
- 🚫 **Never do:** Edit generated `target/` output directly, commit secrets or local credentials, bypass tests without saying so, or make broad refactors unrelated to the requested change.
