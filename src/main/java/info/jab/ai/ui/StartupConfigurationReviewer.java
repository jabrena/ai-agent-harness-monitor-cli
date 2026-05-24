package info.jab.ai.ui;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.config.DefaultPathResolver;
import info.jab.ai.config.ProjectDiscovery;
import java.io.Console;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class StartupConfigurationReviewer {

    private final PrintWriter out;
    private final ProjectDiscovery projectDiscovery;

    public StartupConfigurationReviewer(PrintWriter out) {
        this.out = out;
        this.projectDiscovery = new ProjectDiscovery();
    }

    public Optional<AuditConfig> review(AuditConfig config) {
        return review(config, true);
    }

    public Optional<AuditConfig> review(AuditConfig config, boolean promptProjectsDirectory) {
        if (config.autoConfirm()) {
            return Optional.of(config);
        }
        Console console = System.console();
        AuditConfig reviewedConfig = promptProjectsDirectory ? promptProjectsDirectory(config, console) : config;
        if (!config.uiEnabled() || console == null) {
            new SummaryPrinter(out).printConfiguration(reviewedConfig);
            return confirm(console) ? Optional.of(reviewedConfig) : Optional.empty();
        }

        try {
            new TamboConfigurationReviewApp(reviewedConfig).run();
        } catch (Exception e) {
            out.println("TamboUI review failed, falling back to plain console: " + e.getMessage());
            new SummaryPrinter(out).printConfiguration(reviewedConfig);
        }
        return confirm(console) ? Optional.of(reviewedConfig) : Optional.empty();
    }

    private AuditConfig promptProjectsDirectory(AuditConfig config, Console console) {
        if (console == null) {
            return config;
        }
        String response = console.readLine("Projects folder to scan [%s]: ", config.projectsDirectory());
        if (response == null || response.isBlank()) {
            return config;
        }
        Path projectsDirectory = Path.of(response).toAbsolutePath().normalize();
        try {
            List<Path> projectScanRoots = projectDiscovery.discover(projectsDirectory);
            Path primaryProjectRoot = projectScanRoots.isEmpty() ? projectsDirectory : projectScanRoots.getFirst();
            return new AuditConfig(
                primaryProjectRoot,
                projectsDirectory,
                List.of(projectsDirectory),
                projectScanRoots,
                config.outputDirectory(),
                config.harnesses(),
                config.assetTypes(),
                config.interval(),
                config.uiEnabled(),
                config.autoConfirm(),
                config.verbose(),
                config.privacyMode(),
                config.missingLocationBehavior(),
                config.userRoots(),
                new DefaultPathResolver(Path.of(System.getProperty("user.home"))).projectRoots(primaryProjectRoot)
            );
        } catch (Exception e) {
            out.println("Could not scan projects folder, keeping previous value: " + e.getMessage());
            return config;
        }
    }

    private boolean confirm(Console console) {
        if (console == null) {
            return true;
        }
        String response = console.readLine("Run with this configuration? [Y/n] ");
        return response == null || response.isBlank() || response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes");
    }
}
