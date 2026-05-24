package info.jab.ai.config;

import info.jab.ai.model.AssetType;
import info.jab.ai.model.Harness;
import info.jab.ai.model.MissingLocationBehavior;
import info.jab.ai.model.PrivacyMode;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConfigurationFactory {

    private final DefaultPathResolver pathResolver;
    private final ConfigurationStore configurationStore;
    private final ProjectDiscovery projectDiscovery;

    public ConfigurationFactory(DefaultPathResolver pathResolver, ConfigurationStore configurationStore) {
        this.pathResolver = pathResolver;
        this.configurationStore = configurationStore;
        this.projectDiscovery = new ProjectDiscovery();
    }

    public AuditConfig create(ConfigOverrides overrides) throws IOException {
        Path projectRoot = normalize(overrides.projectRoot() == null ? Path.of("").toAbsolutePath() : overrides.projectRoot());
        Path projectsDirectory = normalize(overrides.projectsDirectory() == null ? projectRoot : overrides.projectsDirectory());
        List<Path> projectsDirectories = normalizeProjectsDirectories(overrides.projectsDirectories(), projectsDirectory);
        Path outputDirectory = normalize(overrides.outputDirectory() == null
            ? Path.of(System.getProperty("user.home"), ".ai-agent-team-auditory-cli")
            : overrides.outputDirectory());

        Path defaultProjectRoot = projectRoot;
        Path defaultProjectsDirectory = projectsDirectory;
        List<Path> defaultProjectsDirectories = projectsDirectories;
        Path defaultOutputDirectory = outputDirectory;
        AuditConfig base = configurationStore.load(outputDirectory)
            .orElseGet(() -> defaults(defaultProjectRoot, defaultProjectsDirectory, defaultProjectsDirectories, defaultOutputDirectory));
        projectRoot = overrides.projectRoot() == null ? fallback(base.projectRoot(), projectRoot) : projectRoot;
        projectsDirectory = overrides.projectsDirectory() == null ? fallback(base.projectsDirectory(), projectsDirectory) : projectsDirectory;
        projectsDirectories = overrides.projectsDirectories().isEmpty() ? fallbackProjectsDirectories(base.projectsDirectories(), projectsDirectories) : projectsDirectories;
        outputDirectory = overrides.outputDirectory() == null ? fallback(base.outputDirectory(), outputDirectory) : outputDirectory;
        if (overrides.projectRoot() != null && overrides.projectsDirectory() == null) {
            projectsDirectory = projectRoot;
            projectsDirectories = List.of(projectRoot);
        }

        Set<Harness> harnesses = overrides.harnessCsv() == null ? base.harnesses() : parseHarnesses(overrides.harnessCsv());
        Duration interval = overrides.intervalSeconds() == null ? base.interval() : Duration.ofSeconds(overrides.intervalSeconds());
        List<Path> projectScanRoots = overrides.projectRoot() == null
            ? discoverProjects(projectsDirectories)
            : List.of(projectRoot);
        Path primaryProjectRoot = projectScanRoots.isEmpty() ? projectRoot : projectScanRoots.getFirst();

        return new AuditConfig(
            primaryProjectRoot,
            projectsDirectory,
            projectsDirectories,
            projectScanRoots,
            outputDirectory,
            harnesses,
            base.assetTypes(),
            interval,
            overrides.noUi() ? false : base.uiEnabled(),
            overrides.yes() || base.autoConfirm(),
            overrides.verbose() || base.verbose(),
            base.privacyMode(),
            base.missingLocationBehavior(),
            pathResolver.userRoots(),
            pathResolver.projectRoots(primaryProjectRoot)
        );
    }

    private AuditConfig defaults(Path projectRoot, Path projectsDirectory, List<Path> projectsDirectories, Path outputDirectory) {
        List<Path> projectScanRoots;
        try {
            projectScanRoots = discoverProjects(projectsDirectories);
        } catch (IOException e) {
            projectScanRoots = List.of(projectRoot);
        }
        Path primaryProjectRoot = projectScanRoots.isEmpty() ? projectRoot : projectScanRoots.getFirst();
        return new AuditConfig(
            primaryProjectRoot,
            projectsDirectory,
            projectsDirectories,
            projectScanRoots,
            outputDirectory,
            EnumSet.allOf(Harness.class),
            EnumSet.allOf(AssetType.class),
            Duration.ofSeconds(60),
            true,
            false,
            false,
            PrivacyMode.METADATA_ONLY,
            MissingLocationBehavior.WARN_AND_CONTINUE,
            pathResolver.userRoots(),
            pathResolver.projectRoots(primaryProjectRoot)
        );
    }

    private Set<Harness> parseHarnesses(String csv) {
        Set<Harness> parsed = Arrays.stream(csv.split(","))
            .map(Harness::fromToken)
            .flatMap(OptionalSupport::stream)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Harness.class)));
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("No valid harnesses found in --harness value: " + csv);
        }
        return parsed;
    }

    private List<Path> discoverProjects(List<Path> projectsDirectories) throws IOException {
        List<Path> projectScanRoots = new ArrayList<>();
        for (Path directory : projectsDirectories) {
            projectScanRoots.addAll(projectDiscovery.discover(directory));
        }
        return projectScanRoots.stream().distinct().toList();
    }

    private List<Path> normalizeProjectsDirectories(List<Path> projectsDirectories, Path fallback) {
        if (projectsDirectories == null || projectsDirectories.isEmpty()) {
            return List.of(fallback);
        }
        return projectsDirectories.stream()
            .map(this::normalize)
            .distinct()
            .toList();
    }

    private List<Path> fallbackProjectsDirectories(List<Path> candidate, List<Path> fallback) {
        return candidate == null || candidate.isEmpty() ? fallback : candidate;
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private Path fallback(Path candidate, Path fallback) {
        return candidate == null ? fallback : candidate;
    }

    private static final class OptionalSupport {
        private OptionalSupport() {
        }

        static <T> java.util.stream.Stream<T> stream(java.util.Optional<T> optional) {
            return optional.stream();
        }
    }
}
