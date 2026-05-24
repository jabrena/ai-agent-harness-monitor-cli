package info.jab.ai.ui;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.ChangeEvent;
import info.jab.ai.model.ChangeType;
import info.jab.ai.model.Snapshot;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class SummaryPrinter {

    private final PrintWriter out;

    public SummaryPrinter(PrintWriter out) {
        this.out = out;
    }

    public void printConfiguration(AuditConfig config) {
        out.println("AI Agent Harness Audit configuration");
        out.println("Projects folders: " + config.projectsDirectories());
        out.println("Projects discovered: " + config.projectScanRoots().size());
        if (config.verbose()) {
            config.projectScanRoots().forEach(project -> out.println("  - " + project));
        }
        out.println("Output: " + config.outputDirectory());
        out.println("Harnesses: " + config.harnesses());
        out.println("Assets: " + config.assetTypes());
        out.println("Interval: " + config.interval().toSeconds() + "s");
        out.println("Privacy: " + config.privacyMode());
        out.flush();
    }

    public void printScanResult(Snapshot snapshot, List<ChangeEvent> changes) {
        out.println("Scan complete: " + snapshot.findings().size() + " findings, " + changes.size() + " changes");
        if (!changes.isEmpty()) {
            out.println("Changes: " + summarize(changes));
        }
        out.flush();
    }

    public void printReport(Path reportPath) {
        out.println("Report written: " + reportPath);
        out.flush();
    }

    private String summarize(List<ChangeEvent> changes) {
        return changes.stream()
            .collect(Collectors.groupingBy(ChangeEvent::type, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .map(entry -> label(entry.getKey()) + "=" + entry.getValue())
            .collect(Collectors.joining(", "));
    }

    private String label(ChangeType type) {
        return type.name().toLowerCase();
    }
}
