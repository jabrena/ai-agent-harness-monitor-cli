package info.jab.ai.ui;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import info.jab.ai.config.AuditConfig;
import java.nio.file.Files;
import java.nio.file.Path;

final class TamboConfigurationReviewApp extends ToolkitApp {

    private final AuditConfig config;

    TamboConfigurationReviewApp(AuditConfig config) {
        this.config = config;
    }

    @Override
    protected Element render() {
        return panel("AI Agent Harness Audit",
            column(
                text("Startup configuration review").bold().cyan(),
                text("Projects folders: " + config.projectsDirectories()),
                text("Projects discovered: " + config.projectScanRoots().size()),
                text("Output: " + config.outputDirectory()),
                text("Harnesses: " + config.harnesses()),
                text("Assets: " + config.assetTypes()),
                text("Interval: " + config.interval().toSeconds() + "s"),
                text("Privacy: " + config.privacyMode()),
                spacer(),
                text("User roots").bold(),
                text(rootStatus("Cursor", config.userRoots().get(info.jab.ai.model.Harness.CURSOR))),
                text(rootStatus("Claude", config.userRoots().get(info.jab.ai.model.Harness.CLAUDE))),
                text(rootStatus("Codex", config.userRoots().get(info.jab.ai.model.Harness.CODEX))),
                spacer(),
                text("Press q to accept this review, then confirm in the prompt.").dim()
            )
        ).rounded();
    }

    private String rootStatus(String label, Path path) {
        return label + ": " + path + " (" + (Files.exists(path) ? "found" : "missing") + ")";
    }
}
