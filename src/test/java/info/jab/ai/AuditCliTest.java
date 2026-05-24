package info.jab.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.ai.cli.AuditApplication;
import info.jab.ai.cli.AuditCli;
import info.jab.ai.config.AuditConfig;
import info.jab.ai.config.ConfigOverrides;
import info.jab.ai.config.ConfigurationFactory;
import info.jab.ai.config.ConfigurationStore;
import info.jab.ai.config.DefaultPathResolver;
import info.jab.ai.config.InitRequest;
import info.jab.ai.config.JsonMapper;
import info.jab.ai.config.ReportOptions;
import info.jab.ai.model.AssetType;
import info.jab.ai.model.ChangeType;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Harness;
import info.jab.ai.model.Scope;
import info.jab.ai.model.Snapshot;
import info.jab.ai.scanner.AuditScanner;
import info.jab.ai.state.SnapshotDiffer;
import info.jab.ai.state.SnapshotStore;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class AuditCliTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesDefaultsWhenHarnessLocationsAreMissing() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home);
        Files.createDirectories(project);

        AuditConfig config = factory(home).create(new ConfigOverrides(project, null, output, null, null, true, true, false));

        assertEquals(project.toAbsolutePath().normalize(), config.projectRoot());
        assertEquals(project.toAbsolutePath().normalize(), config.projectsDirectory());
        assertEquals(List.of(project.toAbsolutePath().normalize()), config.projectScanRoots());
        assertEquals(output.toAbsolutePath().normalize(), config.outputDirectory());
        assertTrue(config.userRoots().get(Harness.CURSOR).endsWith(".cursor"));
        assertTrue(config.projectRoots().get("agentsFile").endsWith("AGENTS.md"));
    }

    @Test
    void detectsProjectAgentsAndSkillsFolders() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home);
        Files.createDirectories(project.resolve(".agents/skills/reviewer"));
        Files.writeString(project.resolve(".agents/skills/reviewer/SKILL.md"), "Review from agents");
        Files.createDirectories(project.resolve("skills/planner"));
        Files.writeString(project.resolve("skills/planner/SKILL.md"), "Plan work");

        AuditConfig config = factory(home).create(new ConfigOverrides(project, null, output, null, null, true, true, false));
        Snapshot snapshot = AuditScanner.defaultScanner().scan(config);

        assertTrue(snapshot.findings().stream().anyMatch(finding ->
            finding.harness() == Harness.CURSOR
                && finding.assetType() == AssetType.SKILL
                && finding.path().endsWith(".agents/skills/reviewer/SKILL.md")));
        assertTrue(snapshot.findings().stream().anyMatch(finding ->
            finding.harness() == Harness.CURSOR
                && finding.assetType() == AssetType.SKILL
                && finding.path().endsWith("skills/planner/SKILL.md")));
    }

    @Test
    void detectsCursorClaudeAndCodexFixtures() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home.resolve(".cursor/skills-cursor/reviewer"));
        Files.writeString(home.resolve(".cursor/skills-cursor/reviewer/SKILL.md"), "Review code");
        Files.createDirectories(project.resolve(".cursor"));
        Files.writeString(project.resolve(".cursor/mcp.json"), "{}");
        Files.writeString(project.resolve("CLAUDE.md"), "Claude guidance");
        Files.writeString(project.resolve("AGENTS.md"), "Codex guidance");

        AuditConfig config = factory(home).create(new ConfigOverrides(project, null, output, null, null, true, true, false));
        Snapshot snapshot = AuditScanner.defaultScanner().scan(config);

        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.harness() == Harness.CURSOR && finding.assetType() == AssetType.SKILL));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.harness() == Harness.CURSOR && finding.assetType() == AssetType.MCP));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.harness() == Harness.CLAUDE && finding.assetType() == AssetType.GUIDANCE));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.harness() == Harness.CODEX && finding.assetType() == AssetType.GUIDANCE));
    }

    @Test
    void detectsClaudeProjectRootMcpJson() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home);
        Files.createDirectories(project);
        Files.writeString(project.resolve(".mcp.json"), "{}");

        AuditConfig config = factory(home).create(new ConfigOverrides(project, null, output, null, "claude", true, true, false));
        Snapshot snapshot = AuditScanner.defaultScanner().scan(config);

        assertTrue(snapshot.findings().stream().anyMatch(finding ->
            finding.harness() == Harness.CLAUDE
                && finding.assetType() == AssetType.MCP
                && finding.path().endsWith(".mcp.json")));
    }

    @Test
    void skipsConfiguredFileNamesDuringScan() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home);
        Files.createDirectories(project.resolve(".cursor"));
        Files.writeString(project.resolve(".cursor/mcp.json"), "{}");
        Files.writeString(project.resolve("AGENTS.md"), "Codex guidance");
        Files.writeString(project.resolve("CLAUDE.md"), "Claude guidance");

        AuditConfig config = factory(home).create(
            new ConfigOverrides(project, null, List.of(), output, null, null, List.of("AGENTS.md", "mcp.json"), true, true, false)
        );
        Snapshot snapshot = AuditScanner.defaultScanner().scan(config);

        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.path().endsWith("CLAUDE.md")));
        assertFalse(snapshot.findings().stream().anyMatch(finding -> finding.path().endsWith("AGENTS.md")));
        assertFalse(snapshot.findings().stream().anyMatch(finding -> finding.path().endsWith("mcp.json")));
    }

    @Test
    void discoversProjectsFolderAndScansLocalAgentFilesAcrossChildren() throws Exception {
        Path home = tempDir.resolve("home");
        Path projects = tempDir.resolve("projects");
        Path output = tempDir.resolve("out");
        Path alpha = projects.resolve("alpha");
        Path beta = projects.resolve("beta");
        Files.createDirectories(home);
        Files.createDirectories(alpha.resolve(".cursor"));
        Files.writeString(alpha.resolve(".cursor/mcp.json"), "{}");
        Files.createDirectories(beta);
        Files.writeString(beta.resolve("AGENTS.md"), "Codex guidance");

        AuditConfig config = factory(home).create(new ConfigOverrides(null, projects, output, null, null, true, true, false));
        Snapshot snapshot = AuditScanner.defaultScanner().scan(config);

        assertEquals(projects.toAbsolutePath().normalize(), config.projectsDirectory());
        assertTrue(config.projectScanRoots().contains(alpha.toAbsolutePath().normalize()));
        assertTrue(config.projectScanRoots().contains(beta.toAbsolutePath().normalize()));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.path().contains("alpha/.cursor/mcp.json")));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.path().contains("beta/AGENTS.md")));
    }

    @Test
    void discoversProjectsAcrossMultipleConfiguredFolders() throws Exception {
        Path home = tempDir.resolve("home");
        Path workspaceA = tempDir.resolve("workspace-a");
        Path workspaceB = tempDir.resolve("workspace-b");
        Path output = tempDir.resolve("out");
        Path alpha = workspaceA.resolve("alpha");
        Path beta = workspaceB.resolve("beta");
        Files.createDirectories(home);
        Files.createDirectories(alpha.resolve(".cursor"));
        Files.writeString(alpha.resolve(".cursor/mcp.json"), "{}");
        Files.createDirectories(beta);
        Files.writeString(beta.resolve("AGENTS.md"), "Codex guidance");

        AuditConfig config = factory(home).create(
            new ConfigOverrides(null, workspaceA, List.of(workspaceA, workspaceB), output, null, null, true, true, false)
        );
        Snapshot snapshot = AuditScanner.defaultScanner().scan(config);

        assertEquals(List.of(workspaceA.toAbsolutePath().normalize(), workspaceB.toAbsolutePath().normalize()), config.projectsDirectories());
        assertTrue(config.projectScanRoots().contains(alpha.toAbsolutePath().normalize()));
        assertTrue(config.projectScanRoots().contains(beta.toAbsolutePath().normalize()));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.path().contains("alpha/.cursor/mcp.json")));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.path().contains("beta/AGENTS.md")));
    }

    @Test
    void excludesConfiguredProjectFoldersFromIncludedDirectories() throws Exception {
        Path home = tempDir.resolve("home");
        Path workspace = tempDir.resolve("workspace");
        Path output = tempDir.resolve("out");
        Path included = workspace.resolve("included");
        Path excluded = workspace.resolve("excluded-project");
        Files.createDirectories(home);
        Files.createDirectories(included.resolve(".cursor"));
        Files.writeString(included.resolve(".cursor/mcp.json"), "{}");
        Files.createDirectories(excluded.resolve(".cursor"));
        Files.writeString(excluded.resolve(".cursor/mcp.json"), "{}");

        AuditConfig config = factory(home).create(
            new ConfigOverrides(null, workspace, List.of(workspace), output, null, null, List.of(), List.of("excluded-project"), true, true, false)
        );
        Snapshot snapshot = AuditScanner.defaultScanner().scan(config);

        assertTrue(config.projectScanRoots().contains(included.toAbsolutePath().normalize()));
        assertFalse(config.projectScanRoots().contains(excluded.toAbsolutePath().normalize()));
        assertTrue(snapshot.findings().stream().anyMatch(finding -> finding.path().contains("included/.cursor/mcp.json")));
        assertFalse(snapshot.findings().stream().anyMatch(finding -> finding.path().contains("excluded-project/.cursor/mcp.json")));
    }

    @Test
    void parsesInitRequestJsonConfiguration() throws Exception {
        Path projects = tempDir.resolve("projects");
        ObjectMapper mapper = JsonMapper.create();

        InitRequest request = mapper.readValue(
            """
            {
              "user": "jabrena",
              "include-dirs": ["%s"],
              "internal-analysis": true,
              "harness": "cursor,codex",
              "exclude-files": ["AGENTS.md", "mcp.json"],
              "exclude-dirs": ["cursor-rules-agile"],
              "report-type": ["json"],
              "report-output-dir": "%s"
            }
            """.formatted(projects, tempDir.resolve("reports")),
            InitRequest.class
        );

        assertEquals("jabrena", request.user());
        assertEquals(List.of(projects), request.projectsDirectories());
        assertTrue(request.shouldShowInternalAnalysis(false));
        assertTrue(request.hasProjectsDirectories());
        assertEquals("cursor,codex", request.harnessCsv());
        assertEquals(List.of("AGENTS.md", "mcp.json"), request.skipFiles());
        assertEquals(List.of("cursor-rules-agile"), request.excludeDirectories());
        assertTrue(request.reportOptions().jsonEnabled());
    }

    @Test
    void cliOnlyExposesInitCommand() {
        CommandLine commandLine = new CommandLine(new AuditCli());

        assertEquals(java.util.Set.of("init"), commandLine.getSubcommands().keySet());
    }

    @Test
    void appPreloadsRuntimeClassesBeforeExecutingCli() {
        int exitCode = App.execute("--version");

        assertEquals(0, exitCode);
    }

    @Test
    void configFileInitRunsAsApprovedConfiguration() throws Exception {
        Path projects = tempDir.resolve("projects");
        Path output = tempDir.resolve("out");
        Path configFile = tempDir.resolve("audit-config.json");
        Files.createDirectories(projects.resolve("project"));
        Files.writeString(
            configFile,
            """
            {
              "include-dirs": ["%s"],
              "output-dir": "%s"
            }
            """.formatted(projects, output)
        );

        int exitCode = new CommandLine(new AuditCli()).execute("init", "--config-file", configFile.toString());
        AuditConfig saved = new ConfigurationStore(JsonMapper.create()).load(output.toAbsolutePath().normalize()).orElseThrow();

        assertEquals(0, exitCode);
        assertTrue(saved.autoConfirm());
        assertEquals(List.of(projects.toAbsolutePath().normalize()), saved.projectsDirectories());
    }

    @Test
    void mergesSavedConfigurationAndCliOverridesDeterministically() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home);
        Files.createDirectories(project);

        ObjectMapper mapper = JsonMapper.create();
        ConfigurationStore store = new ConfigurationStore(mapper);
        AuditConfig saved = factory(home).create(new ConfigOverrides(project, null, output, 90, "cursor", true, false, false));
        AuditConfig legacySaved = new AuditConfig(
            saved.projectRoot(),
            saved.projectsDirectory(),
            saved.projectsDirectories(),
            saved.projectScanRoots(),
            saved.outputDirectory(),
            saved.harnesses(),
            java.util.EnumSet.of(AssetType.SKILL, AssetType.MCP, AssetType.RULE, AssetType.CONFIG),
            saved.interval(),
            saved.uiEnabled(),
            saved.autoConfirm(),
            saved.verbose(),
            saved.privacyMode(),
            saved.missingLocationBehavior(),
            saved.skipFiles(),
            saved.excludeDirectories(),
            saved.userRoots(),
            saved.projectRoots()
        );
        store.save(legacySaved);

        AuditConfig merged = new ConfigurationFactory(new DefaultPathResolver(home), store)
            .create(new ConfigOverrides(null, null, output, 15, "claude,codex", true, true, true));

        assertEquals(project.toAbsolutePath().normalize(), merged.projectRoot());
        assertEquals(15, merged.interval().toSeconds());
        assertEquals(java.util.Set.of(Harness.CLAUDE, Harness.CODEX), merged.harnesses());
        assertTrue(merged.assetTypes().contains(AssetType.GUIDANCE));
        assertTrue(merged.autoConfirm());
        assertTrue(merged.verbose());
        assertFalse(merged.uiEnabled());
    }

    @Test
    void diffsAddedRemovedAndModifiedFindings() {
        Finding unchangedBefore = finding("same", "aaa");
        Finding unchangedAfter = finding("same", "aaa");
        Finding modifiedBefore = finding("modified", "old");
        Finding modifiedAfter = finding("modified", "new");
        Finding removed = finding("removed", "gone");
        Finding added = finding("added", "new");

        Snapshot previous = new Snapshot(Instant.parse("2026-01-01T00:00:00Z"), List.of(unchangedBefore, modifiedBefore, removed));
        Snapshot current = new Snapshot(Instant.parse("2026-01-01T00:01:00Z"), List.of(unchangedAfter, modifiedAfter, added));

        var changes = new SnapshotDiffer().diff(previous, current);

        assertEquals(3, changes.size());
        assertTrue(changes.stream().anyMatch(change -> change.type() == ChangeType.ADDED));
        assertTrue(changes.stream().anyMatch(change -> change.type() == ChangeType.REMOVED));
        assertTrue(changes.stream().anyMatch(change -> change.type() == ChangeType.MODIFIED));
    }

    @Test
    void scanWritesSnapshotAndAuditLogWithoutTerminalUi() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home.resolve(".cursor/skills-cursor/reviewer"));
        Files.writeString(home.resolve(".cursor/skills-cursor/reviewer/SKILL.md"), "Review code");
        Files.createDirectories(project);

        AuditConfig config = factory(home).create(new ConfigOverrides(project, null, output, null, "cursor", true, true, false));
        StringWriter console = new StringWriter();

        int exitCode = new AuditApplication(new PrintWriter(console, true)).scan(config);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(new SnapshotStore(JsonMapper.create()).snapshotPath(output.toAbsolutePath().normalize())));
        try (var logs = Files.list(output.toAbsolutePath().normalize())) {
            assertTrue(logs.anyMatch(path -> path.getFileName().toString().startsWith("audit-")));
        }
    }

    @Test
    void initWritesConfigurationSnapshotAuditLogAndInternalAnalysis() throws Exception {
        Path home = tempDir.resolve("home");
        Path projects = tempDir.resolve("projects");
        Path output = tempDir.resolve("out");
        Path project = projects.resolve("project");
        Files.createDirectories(home.resolve(".cursor/skills-cursor/reviewer"));
        Files.writeString(home.resolve(".cursor/skills-cursor/reviewer/SKILL.md"), "Review code");
        Files.createDirectories(project.resolve(".cursor"));
        Files.writeString(project.resolve(".cursor/mcp.json"), "{}");
        Files.writeString(project.resolve("AGENTS.md"), "Codex guidance");

        AuditConfig config = factory(home).create(new ConfigOverrides(null, projects, output, null, null, true, true, false));
        StringWriter console = new StringWriter();

        int exitCode = new AuditApplication(new PrintWriter(console, true)).init(config, true);
        String outputText = console.toString();

        assertEquals(0, exitCode);
        assertTrue(Files.exists(new ConfigurationStore(JsonMapper.create()).configPath(output.toAbsolutePath().normalize())));
        assertTrue(Files.exists(new SnapshotStore(JsonMapper.create()).snapshotPath(output.toAbsolutePath().normalize())));
        assertTrue(outputText.contains("AI Agent Harness Monitor"));
        assertTrue(outputText.contains("Skills (1)"));
        assertTrue(outputText.contains("Rules (0)"));
        assertTrue(outputText.contains("Guidance files (1)"));
        assertTrue(outputText.contains("MCPs (1)"));
    }

    @Test
    void initWritesJsonReportWithUserTimestampFileName() throws Exception {
        Path home = tempDir.resolve("home");
        Path projects = tempDir.resolve("projects");
        Path output = tempDir.resolve("out");
        Path reports = tempDir.resolve("reports");
        Path project = projects.resolve("project");
        Path emptyProject = projects.resolve("empty-project");
        Files.createDirectories(home);
        Files.createDirectories(project.resolve(".cursor"));
        Files.createDirectories(emptyProject);
        Files.writeString(project.resolve(".cursor/mcp.json"), "{}");

        AuditConfig config = factory(home).create(new ConfigOverrides(null, projects, output, null, null, true, true, false));
        StringWriter console = new StringWriter();

        int exitCode = new AuditApplication(new PrintWriter(console, true))
            .init(config, false, false, new ReportOptions("jabrena", List.of("json"), reports));

        assertEquals(0, exitCode);
        try (var reportFiles = Files.list(reports)) {
            List<Path> files = reportFiles.toList();
            assertEquals(1, files.size());
            Path report = files.getFirst();
            assertTrue(report.getFileName().toString().matches("jabrena-\\d{12}\\.json"));
            var json = JsonMapper.create().readTree(report.toFile());
            assertEquals("jabrena", json.get("user").asText());
            assertTrue(json.get("findingCount").asInt() >= 1);
            assertEquals(1, json.get("projectDetections").size());
            assertEquals(project.toAbsolutePath().normalize().toString(), json.get("projectDetections").get(0).get("path").asText());
            assertTrue(json.get("projectDetections").get(0).get("findings").size() >= 1);
        }
    }

    @Test
    void initWritesJsonReportWhenReportTypeIsConfiguredWithoutOutputDirectory() throws Exception {
        Path home = tempDir.resolve("home");
        Path projects = tempDir.resolve("projects");
        Path output = tempDir.resolve("out");
        Path project = projects.resolve("project");
        Files.createDirectories(home);
        Files.createDirectories(project.resolve(".cursor"));
        Files.writeString(project.resolve(".cursor/mcp.json"), "{}");

        AuditConfig config = factory(home).create(new ConfigOverrides(null, projects, output, null, null, true, true, false));
        StringWriter console = new StringWriter();

        int exitCode = new AuditApplication(new PrintWriter(console, true))
            .init(config, false, false, new ReportOptions("jabrena", List.of("json"), null));

        assertEquals(0, exitCode);
        try (var reportFiles = Files.list(output.resolve("reports").toAbsolutePath().normalize())) {
            List<Path> files = reportFiles.toList();
            assertEquals(1, files.size());
            assertTrue(files.getFirst().getFileName().toString().matches("jabrena-\\d{12}\\.json"));
        }
    }

    @Test
    void statusPrintsUserAndProjectInventoryView() throws Exception {
        Path home = tempDir.resolve("home");
        Path project = tempDir.resolve("project");
        Path output = tempDir.resolve("out");
        Files.createDirectories(home.resolve(".cursor/skills-cursor/reviewer"));
        Files.writeString(home.resolve(".cursor/skills-cursor/reviewer/SKILL.md"), "Review code");
        Files.createDirectories(project.resolve(".cursor"));
        Files.writeString(project.resolve(".cursor/mcp.json"), "{}");
        Files.writeString(project.resolve("AGENTS.md"), "Codex guidance");

        AuditConfig config = factory(home).create(new ConfigOverrides(project, null, output, null, null, true, true, false));
        StringWriter console = new StringWriter();

        int exitCode = new AuditApplication(new PrintWriter(console, true)).status(config);
        String statusView = console.toString();

        assertEquals(0, exitCode);
        assertTrue(statusView.contains("USER LEVEL"));
        assertTrue(statusView.contains("PROJECT LEVEL"));
        assertTrue(statusView.contains("Skills"));
        assertTrue(statusView.contains("MCPs"));
        assertTrue(statusView.contains("Guidance files"));
        assertTrue(statusView.contains("Others"));
        assertTrue(statusView.contains("SKILL.md"));
        assertTrue(statusView.contains("mcp.json"));
        assertTrue(statusView.contains("AGENTS.md"));
    }

    @Test
    void configurationViewPrintsEffectiveConfigurationAndLocationStatus() throws Exception {
        Path home = tempDir.resolve("home");
        Path projects = tempDir.resolve("projects");
        Path output = tempDir.resolve("out");
        Path alpha = projects.resolve("alpha");
        Files.createDirectories(home.resolve(".cursor"));
        Files.createDirectories(alpha.resolve(".cursor"));
        Files.writeString(alpha.resolve("AGENTS.md"), "Codex guidance");

        AuditConfig config = factory(home).create(new ConfigOverrides(null, projects, output, 30, "cursor,codex", true, true, true));
        StringWriter console = new StringWriter();

        int exitCode = new AuditApplication(new PrintWriter(console, true)).configurationView(config);
        String configView = console.toString();

        assertEquals(0, exitCode);
        assertTrue(configView.contains("AI Agent Harness Configuration"));
        assertTrue(configView.contains("Runtime"));
        assertTrue(configView.contains("Enabled Harnesses"));
        assertTrue(configView.contains("User-Level Locations"));
        assertTrue(configView.contains("Project-Level Locations"));
        assertTrue(configView.contains("Projects discovered: 1"));
        assertTrue(configView.contains("Watch interval: 30s"));
        assertTrue(configView.contains(".cursor"));
        assertTrue(configView.contains("AGENTS.md"));
        assertTrue(configView.contains("(found)"));
    }

    private ConfigurationFactory factory(Path home) {
        return new ConfigurationFactory(new DefaultPathResolver(home), new ConfigurationStore(JsonMapper.create()));
    }

    private Finding finding(String name, String fingerprint) {
        return new Finding(
            Harness.CURSOR,
            Scope.USER,
            AssetType.SKILL,
            name,
            "/tmp/" + name,
            fingerprint,
            Instant.EPOCH,
            0,
            Map.of()
        );
    }
}
