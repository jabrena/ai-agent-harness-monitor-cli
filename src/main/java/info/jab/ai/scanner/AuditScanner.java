package info.jab.ai.scanner;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Snapshot;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditScanner {

    private final List<HarnessScanner> scanners;

    public AuditScanner(List<HarnessScanner> scanners) {
        this.scanners = List.copyOf(scanners);
    }

    public Snapshot scan(AuditConfig config) throws IOException {
        Set<String> skippedFileNames = skippedFileNames(config);
        List<Finding> findings = scanners.stream()
            .filter(scanner -> config.harnesses().contains(scanner.harness()))
            .flatMap(scanner -> scanHarness(scanner, config).stream())
            .filter(finding -> !isSkipped(finding, skippedFileNames))
            .sorted(Comparator.comparing(Finding::key))
            .toList();
        return new Snapshot(Instant.now(), findings);
    }

    private List<Finding> scanHarness(HarnessScanner scanner, AuditConfig config) {
        try {
            return scanner.scan(config);
        } catch (IOException e) {
            throw new ScannerException("Failed to scan " + scanner.harness().displayName(), e);
        }
    }

    private Set<String> skippedFileNames(AuditConfig config) {
        return config.skipFiles().stream()
            .map(String::trim)
            .filter(skipFile -> !skipFile.isEmpty())
            .collect(Collectors.toSet());
    }

    private boolean isSkipped(Finding finding, Set<String> skippedFileNames) {
        if (skippedFileNames.isEmpty()) {
            return false;
        }
        try {
            Path fileName = Path.of(finding.path()).getFileName();
            return fileName != null && skippedFileNames.contains(fileName.toString());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static AuditScanner defaultScanner() {
        return new AuditScanner(List.of(new CursorScanner(), new ClaudeScanner(), new CodexScanner()));
    }

    public static final class ScannerException extends RuntimeException {
        public ScannerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
