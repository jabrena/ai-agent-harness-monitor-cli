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

public final class CodexScanner implements HarnessScanner {

    @Override
    public Harness harness() {
        return Harness.CODEX;
    }

    @Override
    public List<Finding> scan(AuditConfig config) throws IOException {
        List<Finding> findings = new ArrayList<>();
        Path userRoot = config.userRoots().get(Harness.CODEX);

        if (config.assetTypes().contains(AssetType.SKILL)) {
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.SKILL, userRoot.resolve("skills"), path -> hasExtension(path, ".md"));
        }
        if (config.assetTypes().contains(AssetType.MCP)) {
            addFileIfExists(findings, harness(), Scope.USER, AssetType.MCP, "codex-user-mcp", userRoot.resolve("mcp.json"));
            addDirectoryChildren(findings, harness(), Scope.USER, AssetType.MCP, userRoot.resolve("mcps"), path -> hasExtension(path, ".json", ".yaml", ".yml", ".toml"));
        }
        if (config.assetTypes().contains(AssetType.GUIDANCE)) {
            addFileIfExists(findings, harness(), Scope.USER, AssetType.GUIDANCE, "codex-user-agents", userRoot.resolve("AGENTS.md"));
        }
        if (config.assetTypes().contains(AssetType.CONFIG)) {
            addDirectoryIfExists(findings, harness(), Scope.USER, AssetType.CONFIG, "codex-home", userRoot);
            addFileIfExists(findings, harness(), Scope.USER, AssetType.CONFIG, "codex-config", userRoot.resolve("config.toml"));
        }

        for (Path projectRoot : config.projectScanRoots()) {
            if (config.assetTypes().contains(AssetType.SKILL)) {
                addDirectoryChildren(findings, harness(), Scope.PROJECT, AssetType.SKILL, projectRoot.resolve(".codex").resolve("skills"), path -> hasExtension(path, ".md"));
            }
            if (config.assetTypes().contains(AssetType.MCP)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.MCP, projectRoot.getFileName() + ":codex-project-mcp", projectRoot.resolve(".codex").resolve("mcp.json"));
            }
            if (config.assetTypes().contains(AssetType.GUIDANCE)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.GUIDANCE, projectRoot.getFileName() + ":codex-project-agents", projectRoot.resolve("AGENTS.md"));
            }
            if (config.assetTypes().contains(AssetType.CONFIG)) {
                addFileIfExists(findings, harness(), Scope.PROJECT, AssetType.CONFIG, projectRoot.getFileName() + ":codex-project-config", projectRoot.resolve(".codex").resolve("config.toml"));
            }
        }

        return findings;
    }
}
