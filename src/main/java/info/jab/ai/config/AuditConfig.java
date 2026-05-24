package info.jab.ai.config;

import info.jab.ai.model.AssetType;
import info.jab.ai.model.Harness;
import info.jab.ai.model.MissingLocationBehavior;
import info.jab.ai.model.PrivacyMode;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record AuditConfig(
    Path projectRoot,
    Path projectsDirectory,
    List<Path> projectsDirectories,
    List<Path> projectScanRoots,
    Path outputDirectory,
    Set<Harness> harnesses,
    Set<AssetType> assetTypes,
    Duration interval,
    boolean uiEnabled,
    boolean autoConfirm,
    boolean verbose,
    PrivacyMode privacyMode,
    MissingLocationBehavior missingLocationBehavior,
    Map<Harness, Path> userRoots,
    Map<String, Path> projectRoots
) {
    public AuditConfig {
        projectsDirectories = projectsDirectories == null ? List.of(projectsDirectory) : List.copyOf(projectsDirectories);
        projectScanRoots = projectScanRoots == null ? List.of(projectRoot) : List.copyOf(projectScanRoots);
    }
}
