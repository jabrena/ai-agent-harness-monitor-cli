package info.jab.ai.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.ai.config.AuditConfig;
import info.jab.ai.config.ConfigurationStore;
import info.jab.ai.config.JsonMapper;
import info.jab.ai.config.ReportOptions;
import info.jab.ai.logging.AuditLogger;
import info.jab.ai.model.ChangeEvent;
import info.jab.ai.model.Snapshot;
import info.jab.ai.report.JsonReportWriter;
import info.jab.ai.scanner.AuditScanner;
import info.jab.ai.state.SnapshotDiffer;
import info.jab.ai.state.SnapshotStore;
import info.jab.ai.ui.ConfigurationViewPrinter;
import info.jab.ai.ui.InitAnalysisPrinter;
import info.jab.ai.ui.InventoryViewPrinter;
import info.jab.ai.ui.StartupConfigurationReviewer;
import info.jab.ai.ui.SummaryPrinter;
import info.jab.ai.ui.TamboInitAnalysisApp;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Console;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class AuditApplication {

    private final AuditScanner scanner;
    private final SnapshotStore snapshotStore;
    private final SnapshotDiffer snapshotDiffer;
    private final AuditLogger auditLogger;
    private final ConfigurationStore configurationStore;
    private final StartupConfigurationReviewer reviewer;
    private final SummaryPrinter summaryPrinter;
    private final InventoryViewPrinter inventoryViewPrinter;
    private final ConfigurationViewPrinter configurationViewPrinter;
    private final InitAnalysisPrinter initAnalysisPrinter;
    private final JsonReportWriter reportWriter;

    public AuditApplication(PrintWriter out) {
        ObjectMapper objectMapper = JsonMapper.create();
        this.scanner = AuditScanner.defaultScanner();
        this.snapshotStore = new SnapshotStore(objectMapper);
        this.snapshotDiffer = new SnapshotDiffer();
        this.auditLogger = new AuditLogger(objectMapper);
        this.configurationStore = new ConfigurationStore(objectMapper);
        this.reviewer = new StartupConfigurationReviewer(out);
        this.summaryPrinter = new SummaryPrinter(out);
        this.inventoryViewPrinter = new InventoryViewPrinter(out);
        this.configurationViewPrinter = new ConfigurationViewPrinter(out);
        this.initAnalysisPrinter = new InitAnalysisPrinter(out);
        this.reportWriter = new JsonReportWriter(objectMapper);
    }

    public int init(AuditConfig config, boolean internalAnalysisRequested) throws IOException {
        return init(config, internalAnalysisRequested, true, ReportOptions.none());
    }

    public int init(AuditConfig config, boolean internalAnalysisRequested, boolean promptProjectsDirectory) throws IOException {
        return init(config, internalAnalysisRequested, promptProjectsDirectory, ReportOptions.none());
    }

    public int init(
        AuditConfig config,
        boolean internalAnalysisRequested,
        boolean promptProjectsDirectory,
        ReportOptions reportOptions
    ) throws IOException {
        Optional<AuditConfig> reviewedConfig = reviewer.review(config, promptProjectsDirectory);
        if (reviewedConfig.isEmpty()) {
            return 2;
        }
        AuditConfig activeConfig = reviewedConfig.get();
        configurationStore.save(activeConfig);
        boolean useUi = shouldUseTamboUi(activeConfig);
        ScanResult result = runSingleScan(activeConfig, !useUi);
        Optional<Path> reportPath = reportWriter.write(activeConfig, result.snapshot(), result.changes(), reportOptions);
        boolean shownInUi = useUi && reviewInitAnalysis(activeConfig, result, reportPath, reportOptions);
        if (!shownInUi) {
            if (useUi) {
                summaryPrinter.printScanResult(result.snapshot(), result.changes());
            }
            reportPath.ifPresent(path -> summaryPrinter.printReport(path));
            initAnalysisPrinter.printSummary(activeConfig, result.snapshot());
            if (internalAnalysisRequested || promptForInternalAnalysis(activeConfig)) {
                initAnalysisPrinter.printInternalAnalysis(result.snapshot());
            }
        }
        return 0;
    }

    public int scan(AuditConfig config) throws IOException {
        Optional<AuditConfig> reviewedConfig = reviewer.review(config);
        if (reviewedConfig.isEmpty()) {
            return 2;
        }
        runSingleScan(reviewedConfig.get(), true);
        return 0;
    }

    public int watch(AuditConfig config) throws IOException, InterruptedException {
        Optional<AuditConfig> reviewedConfig = reviewer.review(config);
        if (reviewedConfig.isEmpty()) {
            return 2;
        }
        AuditConfig activeConfig = reviewedConfig.get();
        while (!Thread.currentThread().isInterrupted()) {
            runSingleScan(activeConfig, true);
            Thread.sleep(sleepMillis(activeConfig.interval()));
        }
        return 0;
    }

    public int saveConfig(AuditConfig config) throws IOException {
        Optional<AuditConfig> reviewedConfig = reviewer.review(config);
        if (reviewedConfig.isEmpty()) {
            return 2;
        }
        configurationStore.save(reviewedConfig.get());
        summaryPrinter.printConfiguration(reviewedConfig.get());
        return 0;
    }

    public int status(AuditConfig config) throws IOException {
        Optional<AuditConfig> reviewedConfig = reviewer.review(config);
        if (reviewedConfig.isEmpty()) {
            return 2;
        }
        AuditConfig activeConfig = reviewedConfig.get();
        Snapshot current = scanner.scan(activeConfig);
        inventoryViewPrinter.print(activeConfig, current);
        return 0;
    }

    public int configurationView(AuditConfig config) {
        configurationViewPrinter.print(config);
        return 0;
    }

    private ScanResult runSingleScan(AuditConfig config, boolean printSummary) throws IOException {
        Snapshot previous = snapshotStore.load(config.outputDirectory()).orElse(Snapshot.empty());
        Snapshot current = scanner.scan(config);
        List<ChangeEvent> changes = snapshotDiffer.diff(previous, current);
        auditLogger.logScan(config, current, changes);
        snapshotStore.save(config.outputDirectory(), current);
        if (printSummary) {
            summaryPrinter.printScanResult(current, changes);
        }
        return new ScanResult(current, changes);
    }

    private boolean reviewInitAnalysis(
        AuditConfig config,
        ScanResult result,
        Optional<Path> reportPath,
        ReportOptions reportOptions
    ) {
        if (!shouldUseTamboUi(config)) {
            return false;
        }
        try {
            new TamboInitAnalysisApp(config, result.snapshot(), result.changes(), reportPath, () -> refresh(config, reportOptions)).run();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private TamboInitAnalysisApp.RefreshResult refresh(AuditConfig config, ReportOptions reportOptions) throws IOException {
        ScanResult result = runSingleScan(config, false);
        Optional<Path> reportPath = reportWriter.write(config, result.snapshot(), result.changes(), reportOptions);
        return new TamboInitAnalysisApp.RefreshResult(result.snapshot(), result.changes(), reportPath);
    }

    private boolean promptForInternalAnalysis(AuditConfig config) {
        if (config.autoConfirm()) {
            return false;
        }
        Console console = System.console();
        if (console == null) {
            return false;
        }
        String response = console.readLine("Open internal analysis details? [y/N] ");
        return response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
    }

    private boolean shouldUseTamboUi(AuditConfig config) {
        return config.uiEnabled() && System.console() != null;
    }

    private long sleepMillis(Duration interval) {
        return Math.max(1_000L, interval.toMillis());
    }

    private record ScanResult(Snapshot snapshot, List<ChangeEvent> changes) {
    }
}
