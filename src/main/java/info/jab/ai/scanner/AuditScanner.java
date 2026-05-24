package info.jab.ai.scanner;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Snapshot;
import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class AuditScanner {

    private final List<HarnessScanner> scanners;

    public AuditScanner(List<HarnessScanner> scanners) {
        this.scanners = List.copyOf(scanners);
    }

    public Snapshot scan(AuditConfig config) throws IOException {
        List<Finding> findings = scanners.stream()
            .filter(scanner -> config.harnesses().contains(scanner.harness()))
            .flatMap(scanner -> scanHarness(scanner, config).stream())
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

    public static AuditScanner defaultScanner() {
        return new AuditScanner(List.of(new CursorScanner(), new ClaudeScanner(), new CodexScanner()));
    }

    public static final class ScannerException extends RuntimeException {
        public ScannerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
