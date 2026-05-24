package info.jab.ai.ui;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.AssetType;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Scope;
import info.jab.ai.model.Snapshot;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class InventoryViewPrinter {

    private final PrintWriter out;

    public InventoryViewPrinter(PrintWriter out) {
        this.out = out;
    }

    public void print(AuditConfig config, Snapshot snapshot) {
        out.println("AI Agent Harness Status");
        out.println("Projects folders: " + config.projectsDirectories());
        out.println("Projects discovered: " + config.projectScanRoots().size());
        out.println("Findings: " + snapshot.findings().size());
        out.println();

        printScope("USER LEVEL", snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.USER)
            .sorted(this::compareFinding)
            .toList());

        out.println();
        out.println("PROJECT LEVEL");
        for (Path projectRoot : config.projectScanRoots()) {
            List<Finding> projectFindings = snapshot.findings().stream()
                .filter(finding -> finding.scope() == Scope.PROJECT)
                .filter(finding -> belongsToProject(finding, projectRoot))
                .sorted(this::compareFinding)
                .toList();
            printProject(projectRoot, projectFindings);
        }
        out.flush();
    }

    private void printScope(String title, List<Finding> findings) {
        out.println(title);
        printCategory("Skills", findings, AssetCategory.SKILLS);
        printCategory("MCPs", findings, AssetCategory.MCPS);
        printCategory("Guidance files", findings, AssetCategory.GUIDANCE);
        printCategory("Others", findings, AssetCategory.OTHERS);
    }

    private void printProject(Path projectRoot, List<Finding> findings) {
        out.println("- " + projectRoot);
        printCategory("  Skills", findings, AssetCategory.SKILLS);
        printCategory("  MCPs", findings, AssetCategory.MCPS);
        printCategory("  Guidance files", findings, AssetCategory.GUIDANCE);
        printCategory("  Others", findings, AssetCategory.OTHERS);
    }

    private void printCategory(String label, List<Finding> findings, AssetCategory category) {
        List<Finding> filtered = findings.stream()
            .filter(finding -> category.matches(finding.assetType()))
            .toList();
        out.println(label + " (" + filtered.size() + ")");
        if (filtered.isEmpty()) {
            out.println("  - none");
            return;
        }
        for (Finding finding : filtered) {
            out.println("  - " + formatFinding(finding));
        }
    }

    private String formatFinding(Finding finding) {
        return finding.harness().displayName()
            + " "
            + finding.assetType().name().toLowerCase(Locale.ROOT)
            + " "
            + finding.name()
            + " -> "
            + finding.path();
    }

    private boolean belongsToProject(Finding finding, Path projectRoot) {
        try {
            return Path.of(finding.path()).toAbsolutePath().normalize().startsWith(projectRoot.toAbsolutePath().normalize());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private int compareFinding(Finding left, Finding right) {
        return Comparator
            .comparing(Finding::harness)
            .thenComparing(Finding::assetType)
            .thenComparing(Finding::name)
            .thenComparing(Finding::path)
            .compare(left, right);
    }

    private enum AssetCategory {
        SKILLS {
            @Override
            boolean matches(AssetType assetType) {
                return assetType == AssetType.SKILL;
            }
        },
        MCPS {
            @Override
            boolean matches(AssetType assetType) {
                return assetType == AssetType.MCP;
            }
        },
        GUIDANCE {
            @Override
            boolean matches(AssetType assetType) {
                return assetType == AssetType.GUIDANCE;
            }
        },
        OTHERS {
            @Override
            boolean matches(AssetType assetType) {
                return assetType != AssetType.SKILL && assetType != AssetType.MCP && assetType != AssetType.GUIDANCE;
            }
        };

        abstract boolean matches(AssetType assetType);
    }
}
