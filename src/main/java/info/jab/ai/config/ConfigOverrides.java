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
        this(projectRoot, projectsDirectory, List.of(), outputDirectory, intervalSeconds, harnessCsv, noUi, yes, verbose);
    }

    public ConfigOverrides {
        projectsDirectories = projectsDirectories == null ? List.of() : List.copyOf(projectsDirectories);
    }
}
