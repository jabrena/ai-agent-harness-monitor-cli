package info.jab.ai.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.ai.config.AuditConfig;
import info.jab.ai.config.ReportOptions;
import info.jab.ai.model.AssetType;
import info.jab.ai.model.ChangeEvent;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Harness;
import info.jab.ai.model.Scope;
import info.jab.ai.model.Snapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JsonReportWriter {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("ddMMyyyyHHmm");

    private final ObjectMapper objectMapper;

    public JsonReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Path> write(AuditConfig config, Snapshot snapshot, List<ChangeEvent> changes, ReportOptions options)
        throws IOException {
        if (options == null || !options.jsonEnabled()) {
            return Optional.empty();
        }
        Path outputDirectory = outputDirectory(config, options);
        Files.createDirectories(outputDirectory);
        Path reportPath = outputDirectory.resolve(fileName(options.user()));
        objectMapper.writeValue(reportPath.toFile(), report(config, snapshot, changes, options.user()));
        return Optional.of(reportPath.toAbsolutePath().normalize());
    }

    private Path outputDirectory(AuditConfig config, ReportOptions options) {
        return options.outputDirectory() == null ? config.outputDirectory().resolve("reports") : options.outputDirectory();
    }

    private String fileName(String user) {
        String normalizedUser = user == null || user.isBlank() ? "user" : user.trim();
        return normalizedUser + "-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".json";
    }

    private Map<String, Object> report(AuditConfig config, Snapshot snapshot, List<ChangeEvent> changes, String user) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("user", user);
        report.put("generatedAt", LocalDateTime.now());
        report.put("projectsDirectories", config.projectsDirectories());
        report.put("projectsDiscovered", config.projectScanRoots().size());
        report.put("projectDetections", projectDetections(config, snapshot));
        report.put("findingCount", snapshot.findings().size());
        report.put("changeCount", changes.size());
        report.put("counts", counts(snapshot));
        report.put("findings", snapshot.findings());
        report.put("changes", changes);
        return report;
    }

    private Map<String, Object> counts(Snapshot snapshot) {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("byAssetType", countByAssetType(snapshot));
        counts.put("byHarness", countByHarness(snapshot));
        counts.put("byScope", countByScope(snapshot));
        return counts;
    }

    private List<Map<String, Object>> projectDetections(AuditConfig config, Snapshot snapshot) {
        return config.projectScanRoots().stream()
            .map(projectRoot -> projectDetection(config, snapshot, projectRoot))
            .filter(project -> ((long) project.get("detections")) > 0)
            .toList();
    }

    private Map<String, Object> projectDetection(AuditConfig config, Snapshot snapshot, Path projectRoot) {
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("path", projectRoot.toAbsolutePath().normalize().toString());
        project.put("detections", countProjectFindings(config, snapshot, projectRoot));
        project.put("findings", projectFindings(config, snapshot, projectRoot));
        return project;
    }

    private long countProjectFindings(AuditConfig config, Snapshot snapshot, Path projectRoot) {
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        return snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.PROJECT)
            .filter(finding -> belongsToProject(config, finding.path(), normalizedProjectRoot))
            .count();
    }

    private List<Finding> projectFindings(AuditConfig config, Snapshot snapshot, Path projectRoot) {
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        return snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.PROJECT)
            .filter(finding -> belongsToProject(config, finding.path(), normalizedProjectRoot))
            .toList();
    }

    private boolean belongsToProject(AuditConfig config, String findingPath, Path normalizedProjectRoot) {
        return assignedProjectRoot(config, findingPath)
            .map(normalizedProjectRoot::equals)
            .orElse(false);
    }

    private Optional<Path> assignedProjectRoot(AuditConfig config, String findingPath) {
        try {
            Path normalizedFindingPath = Path.of(findingPath).toAbsolutePath().normalize();
            return config.projectScanRoots().stream()
                .map(path -> path.toAbsolutePath().normalize())
                .filter(normalizedFindingPath::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private Map<AssetType, Long> countByAssetType(Snapshot snapshot) {
        Map<AssetType, Long> counts = new EnumMap<>(AssetType.class);
        for (AssetType assetType : AssetType.values()) {
            counts.put(assetType, snapshot.findings().stream().filter(finding -> finding.assetType() == assetType).count());
        }
        return counts;
    }

    private Map<Harness, Long> countByHarness(Snapshot snapshot) {
        Map<Harness, Long> counts = new EnumMap<>(Harness.class);
        for (Harness harness : Harness.values()) {
            counts.put(harness, snapshot.findings().stream().filter(finding -> finding.harness() == harness).count());
        }
        return counts;
    }

    private Map<Scope, Long> countByScope(Snapshot snapshot) {
        Map<Scope, Long> counts = new EnumMap<>(Scope.class);
        for (Scope scope : Scope.values()) {
            counts.put(scope, snapshot.findings().stream().filter(finding -> finding.scope() == scope).count());
        }
        return counts;
    }
}
