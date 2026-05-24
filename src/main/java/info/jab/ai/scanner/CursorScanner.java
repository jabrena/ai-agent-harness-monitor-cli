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

public final class CursorScanner implements HarnessScanner {

    @Override
    public Harness harness() {
        return Harness.CURSOR;
    }

    @Override
    public List<Finding> scan(AuditConfig config) throws IOException {
        List<Finding> findings = new ArrayList<>();
        Path userRoot = config.userRoots().get(Harness.CURSOR);

        if (config.assetTypes().contains(AssetType.SKILL)) {
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.SKILL, userRoot.resolve("skills-cursor"), path -> hasExtension(path, ".md"));
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.SKILL, userRoot.resolve("skills"), path -> hasExtension(path, ".md"));
        }
        if (config.assetTypes().contains(AssetType.MCP)) {
            addFileIfExists(findings, harness(), Scope.USER, AssetType.MCP, "cursor-user-mcp", userRoot.resolve("mcp.json"));
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.MCP, userRoot.resolve("mcps"), path -> hasExtension(path, ".json", ".yaml", ".yml"));
        }
        if (config.assetTypes().contains(AssetType.CONFIG)) {
            addDirectoryIfExists(findings, harness(), Scope.USER, AssetType.CONFIG, "cursor-home", userRoot);
        }

        for (Path projectRoot : config.projectScanRoots()) {
            if (config.assetTypes().contains(AssetType.SKILL)) {
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.SKILL, projectRoot.resolve(".cursor").resolve("skills"), path -> hasExtension(path, ".md"));
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.SKILL, projectRoot.resolve(".agents").resolve("skills"), path -> hasExtension(path, ".md"));
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.SKILL, projectRoot.resolve("skills"), path -> hasExtension(path, ".md"));
            }
            if (config.assetTypes().contains(AssetType.MCP)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.MCP, projectRoot.getFileName() + ":cursor-project-mcp", projectRoot.resolve(".cursor").resolve("mcp.json"));
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.MCP, projectRoot.resolve(".cursor").resolve("mcps"), path -> hasExtension(path, ".json", ".yaml", ".yml"));
            }
            if (config.assetTypes().contains(AssetType.RULE)) {
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.RULE, projectRoot.resolve(".cursor").resolve("rules"), path -> hasExtension(path, ".md", ".mdc"));
            }
            if (config.assetTypes().contains(AssetType.CONFIG)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.CONFIG, projectRoot.getFileName() + ":cursor-project-settings", projectRoot.resolve(".cursor").resolve("settings.json"));
            }
        }

        return findings;
    }
}
