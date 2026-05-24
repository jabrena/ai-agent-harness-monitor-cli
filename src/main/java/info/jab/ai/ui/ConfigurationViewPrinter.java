package info.jab.ai.ui;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.Harness;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConfigurationViewPrinter {

    private final PrintWriter out;

    public ConfigurationViewPrinter(PrintWriter out) {
        this.out = out;
    }

    public void print(AuditConfig config) {
        out.println("AI Agent Harness Configuration");
        out.println();
        out.println("Runtime");
        out.println("- Projects folders: " + config.projectsDirectories());
        out.println("- Primary project: " + config.projectRoot());
        out.println("- Projects discovered: " + config.projectScanRoots().size());
        out.println("- Output directory: " + config.outputDirectory());
        out.println("- Watch interval: " + config.interval().toSeconds() + "s");
        out.println("- TamboUI enabled: " + config.uiEnabled());
        out.println("- Auto confirm: " + config.autoConfirm());
        out.println("- Verbose: " + config.verbose());
        out.println("- Privacy mode: " + config.privacyMode());
        out.println("- Missing locations: " + config.missingLocationBehavior());
        out.println();

        out.println("Enabled Harnesses");
        config.harnesses().stream()
            .sorted()
            .forEach(harness -> out.println("- " + harness.displayName()));
        out.println();

        out.println("Enabled Asset Categories");
        config.assetTypes().stream()
            .sorted()
            .forEach(assetType -> out.println("- " + assetType));
        out.println();

        out.println("User-Level Locations");
        for (Map.Entry<Harness, Path> entry : config.userRoots().entrySet()) {
            out.println("- " + entry.getKey().displayName() + ": " + withStatus(entry.getValue()));
        }
        out.println();

        out.println("Project-Level Locations");
        for (Path projectRoot : config.projectScanRoots()) {
            out.println("- " + projectRoot);
            out.println("  - Cursor: " + withStatus(projectRoot.resolve(".cursor")));
            out.println("  - Agents: " + withStatus(projectRoot.resolve(".agents")) + ", skills: " + withStatus(projectRoot.resolve(".agents").resolve("skills")));
            out.println("  - Skills: " + withStatus(projectRoot.resolve("skills")));
            out.println("  - Claude: " + withStatus(projectRoot.resolve(".claude")) + ", " + withStatus(projectRoot.resolve("CLAUDE.md")));
            out.println("  - Codex: " + withStatus(projectRoot.resolve(".codex")) + ", " + withStatus(projectRoot.resolve("AGENTS.md")));
        }
        out.flush();
    }

    private String withStatus(Path path) {
        return path + " (" + (Files.exists(path) ? "found" : "missing") + ")";
    }
}
