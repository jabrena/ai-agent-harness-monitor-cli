# Developer commands

## Essential maven commands

```bash
# Analyze dependencies
./mvnw dependency:tree
./mvnw dependency:analyze
./mvnw dependency:resolve

./mvnw clean validate -U
./mvnw buildplan:list-plugin
./mvnw buildplan:list-phase
./mvnw help:all-profiles
./mvnw help:active-profiles
./mvnw license:third-party-report

# Clean the project
./mvnw clean

# Run unit tests
./mvnw clean test

# Run integration tests
./mvnw clean verify

# Clean and package in one command
./mvnw clean package

# Check for dependency updates
./mvnw versions:display-property-updates
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates

# Generate project reports
./mvnw site
jwebserver -p 8005 -d "$(pwd)/target/site/"
```

## Plugin Goals Reference

The following sections list useful goals for each plugin configured in this project's pom.xml.

### maven-clean-plugin

| Goal                 | Description                       |
| -------------------- | --------------------------------- |
| `./mvnw clean:clean` | Delete the build output directory |

### maven-compiler-plugin

| Goal                          | Description               |
| ----------------------------- | ------------------------- |
| `./mvnw compiler:compile`     | Compile main source files |
| `./mvnw compiler:testCompile` | Compile test source files |

### maven-surefire-plugin

| Goal                   | Description              |
| ---------------------- | ------------------------ |
| `./mvnw surefire:test` | Run unit tests           |
| `./mvnw surefire:help` | Display help information |

### maven-dependency-plugin

| Goal                                  | Description                               |
| ------------------------------------- | ----------------------------------------- |
| `./mvnw dependency:tree`              | Display dependency tree                   |
| `./mvnw dependency:analyze`           | Analyse used/unused declared dependencies |
| `./mvnw dependency:resolve`           | Resolve and list all dependencies         |
| `./mvnw dependency:copy-dependencies` | Copy dependencies to a target directory   |
| `./mvnw dependency:go-offline`        | Download all dependencies for offline use |

### maven-jar-plugin

| Goal                  | Description                     |
| --------------------- | ------------------------------- |
| `./mvnw jar:jar`      | Build the JAR for the project   |
| `./mvnw jar:test-jar` | Build a JAR of the test classes |

### maven-shade-plugin

| Goal                 | Description                              |
| -------------------- | ---------------------------------------- |
| `./mvnw shade:shade` | Create an uber-JAR with all dependencies |
| `./mvnw shade:help`  | Display help information                 |

### maven-install-plugin

| Goal                     | Description                            |
| ------------------------ | -------------------------------------- |
| `./mvnw install:install` | Install artifact into local repository |

### maven-deploy-plugin

| Goal                   | Description                          |
| ---------------------- | ------------------------------------ |
| `./mvnw deploy:deploy` | Deploy artifact to remote repository |

### maven-enforcer-plugin

| Goal                              | Description                          |
| --------------------------------- | ------------------------------------ |
| `./mvnw enforcer:enforce`         | Execute configured enforcer rules    |
| `./mvnw enforcer:display-info`    | Display current platform information |

### versions-maven-plugin

| Goal                                         | Description                            |
| -------------------------------------------- | -------------------------------------- |
| `./mvnw versions:display-dependency-updates` | Show available dependency updates      |
| `./mvnw versions:display-plugin-updates`     | Show available plugin updates          |
| `./mvnw versions:display-property-updates`   | Show available property updates        |
| `./mvnw versions:use-latest-releases`        | Update dependencies to latest releases |
| `./mvnw versions:set -DnewVersion=X.Y.Z`     | Set the project version                |
