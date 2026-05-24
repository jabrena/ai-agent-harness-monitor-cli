package info.jab.ai.config;

import java.nio.file.Path;
import java.util.List;

public record ConfigOverrides(
    Path projectRoot,
    Path projectsDirectory,
    List<Path> projectsDirectories,
    Path outputDirectory,
    Integer intervalSeconds,
    String harnessCsv,
    List<String> skipFiles,
    List<String> excludeDirectories,
    boolean noUi,
    boolean yes,
    boolean verbose
) {
    public ConfigOverrides(
        Path projectRoot,
        Path projectsDirectory,
        Path outputDirectory,
        Integer intervalSeconds,
        String harnessCsv,
        boolean noUi,
        boolean yes,
        boolean verbose
    ) {
        this(projectRoot, projectsDirectory, List.of(), outputDirectory, intervalSeconds, harnessCsv, List.of(), List.of(), noUi, yes, verbose);
    }

    public ConfigOverrides(
        Path projectRoot,
        Path projectsDirectory,
        List<Path> projectsDirectories,
        Path outputDirectory,
        Integer intervalSeconds,
        String harnessCsv,
        boolean noUi,
        boolean yes,
        boolean verbose
    ) {
        this(projectRoot, projectsDirectory, projectsDirectories, outputDirectory, intervalSeconds, harnessCsv, List.of(), List.of(), noUi, yes, verbose);
    }

    public ConfigOverrides(
        Path projectRoot,
        Path projectsDirectory,
        List<Path> projectsDirectories,
        Path outputDirectory,
        Integer intervalSeconds,
        String harnessCsv,
        List<String> skipFiles,
        boolean noUi,
        boolean yes,
        boolean verbose
    ) {
        this(projectRoot, projectsDirectory, projectsDirectories, outputDirectory, intervalSeconds, harnessCsv, skipFiles, List.of(), noUi, yes, verbose);
    }

    public ConfigOverrides {
        projectsDirectories = projectsDirectories == null ? List.of() : List.copyOf(projectsDirectories);
        skipFiles = skipFiles == null ? List.of() : List.copyOf(skipFiles);
        excludeDirectories = excludeDirectories == null ? List.of() : List.copyOf(excludeDirectories);
    }
}
