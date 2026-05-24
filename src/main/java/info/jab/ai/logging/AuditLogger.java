package info.jab.ai.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.ChangeEvent;
import info.jab.ai.model.Snapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AuditLogger {

    private final ObjectMapper objectMapper;

    public AuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void logScan(AuditConfig config, Snapshot snapshot, List<ChangeEvent> changes) throws IOException {
        Files.createDirectories(config.outputDirectory());
        append(config.outputDirectory(), Map.of(
            "eventType", "scan",
            "createdAt", snapshot.createdAt(),
            "projectsDirectory", config.projectsDirectory().toString(),
            "projectsDirectories", config.projectsDirectories(),
            "projectScanRoots", config.projectScanRoots(),
            "findingCount", snapshot.findings().size(),
            "changeCount", changes.size(),
            "harnesses", config.harnesses()
        ));
        for (ChangeEvent change : changes) {
            append(config.outputDirectory(), Map.of(
                "eventType", "change",
                "createdAt", change.detectedAt(),
                "change", change
            ));
        }
    }

    private void append(Path outputDirectory, Object event) throws IOException {
        String json = objectMapper.writeValueAsString(event).replace(System.lineSeparator(), "");
        Files.writeString(
            logPath(outputDirectory),
            json + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public Path logPath(Path outputDirectory) {
        return outputDirectory.resolve("audit-" + LocalDate.now() + ".jsonl");
    }
}
