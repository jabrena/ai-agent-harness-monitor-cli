package info.jab.ai.ui;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.AssetType;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Snapshot;
import java.io.PrintWriter;
import java.util.List;

public final class InitAnalysisPrinter {

    private final PrintWriter out;

    public InitAnalysisPrinter(PrintWriter out) {
        this.out = out;
    }

    public void printSummary(AuditConfig config, Snapshot snapshot) {
        out.println("AI Agent Harness Monitor");
        out.println("Projects folders: " + config.projectsDirectories());
        out.println("Projects discovered: " + config.projectScanRoots().size());
        out.println("Detected assets:");
        out.println("- Skills: " + count(snapshot, AssetType.SKILL));
        out.println("- Rules: " + count(snapshot, AssetType.RULE));
        out.println("- MCPs: " + count(snapshot, AssetType.MCP));
        out.println("- Config files: " + count(snapshot, AssetType.CONFIG));
        out.flush();
    }

    public void printInternalAnalysis(Snapshot snapshot) {
        out.println();
        out.println("Internal Analysis");
        printCount("Skills", snapshot, AssetType.SKILL);
        printCount("Rules", snapshot, AssetType.RULE);
        printCount("MCPs", snapshot, AssetType.MCP);
        out.flush();
    }

    private void printCount(String label, Snapshot snapshot, AssetType assetType) {
        List<Finding> findings = snapshot.findings().stream()
            .filter(finding -> finding.assetType() == assetType)
            .toList();
        out.println(label + " (" + findings.size() + ")");
        if (findings.isEmpty()) {
            out.println("  - none");
            return;
        }
        findings.forEach(finding -> out.println("  - " + finding.harness().displayName() + " " + finding.scope() + " " + finding.name()));
    }

    private long count(Snapshot snapshot, AssetType assetType) {
        return snapshot.findings().stream()
            .filter(finding -> finding.assetType() == assetType)
            .count();
    }
}
