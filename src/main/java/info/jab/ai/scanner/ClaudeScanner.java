package info.jab.ai.scanner;

import static info.jab.ai.scanner.ScannerSupport.addDirectoryChildren;
import static info.jab.ai.scanner.ScannerSupport.addDirectoryIfExists;
import static info.jab.ai.scanner.ScannerSupport.addFileIfExists;
import static info.jab.ai.scanner.ScannerSupport.hasExtension;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.AssetType;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Harness;
import info.jab.ai.model.Scope;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClaudeScanner implements HarnessScanner {

    @Override
    public Harness harness() {
        return Harness.CLAUDE;
    }

    @Override
    public List<Finding> scan(AuditConfig config) throws IOException {
        List<Finding> findings = new ArrayList<>();
        Path userRoot = config.userRoots().get(Harness.CLAUDE);

        if (config.assetTypes().contains(AssetType.SKILL)) {
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.SKILL, userRoot.resolve("skills"), path -> hasExtension(path, ".md"));
        }
        if (config.assetTypes().contains(AssetType.MCP)) {
            addFileIfExists(findings, harness(), Scope.USER, AssetType.MCP, "claude-user-mcp", userRoot.resolve("mcp.json"));
            addFileIfExists(findings, harness(), Scope.USER, AssetType.MCP, "claude-desktop-config", userRoot.resolve("claude_desktop_config.json"));
        }
        if (config.assetTypes().contains(AssetType.RULE)) {
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.RULE, userRoot.resolve("commands"), path -> hasExtension(path, ".md"));
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.RULE, userRoot.resolve("agents"), path -> hasExtension(path, ".md", ".json"));
        }
        if (config.assetTypes().contains(AssetType.CONFIG)) {
            addDirectoryIfExists(findings, harness(), Scope.USER, AssetType.CONFIG, "claude-home", userRoot);
            addFileIfExists(findings, harness(), Scope.USER, AssetType.CONFIG, "claude-settings", userRoot.resolve("settings.json"));
        }

        for (Path projectRoot : config.projectScanRoots()) {
            if (config.assetTypes().contains(AssetType.SKILL)) {
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.SKILL, projectRoot.resolve(".claude").resolve("skills"), path -> hasExtension(path, ".md"));
            }
            if (config.assetTypes().contains(AssetType.MCP)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.MCP, projectRoot.getFileName() + ":claude-project-mcp", projectRoot.resolve(".claude").resolve("mcp.json"));
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.MCP, projectRoot.resolve(".claude").resolve("mcps"), path -> hasExtension(path, ".json", ".yaml", ".yml"));
            }
            if (config.assetTypes().contains(AssetType.RULE)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.RULE, projectRoot.getFileName() + ":claude-guidance", projectRoot.resolve("CLAUDE.md"));
            }
            if (config.assetTypes().contains(AssetType.CONFIG)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.CONFIG, projectRoot.getFileName() + ":claude-project-settings", projectRoot.resolve(".claude").resolve("settings.json"));
            }
        }

        return findings;
    }
}
