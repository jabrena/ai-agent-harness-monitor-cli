package info.jab.ai.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InitRequest(
    String user,
    @JsonProperty("projects-dirs")
    @JsonAlias({"project-dirs", "projectsDirs", "projectsDirectories"})
    List<Path> projectsDirectories,
    @JsonProperty("output-dir")
    @JsonAlias("outputDirectory")
    Path outputDirectory,
    @JsonProperty("interval-seconds")
    @JsonAlias("intervalSeconds")
    Integer intervalSeconds,
    @JsonProperty("harness")
    @JsonAlias("harnesses")
    String harnessCsv,
    @JsonProperty("no-ui")
    @JsonAlias("noUi")
    Boolean noUi,
    Boolean yes,
    Boolean verbose,
    @JsonProperty("internal-analysis")
    @JsonAlias({"internalAnalysis", "analysis"})
    Boolean internalAnalysis,
    @JsonProperty("report-type")
    @JsonAlias({"reportType", "report-types", "reportTypes"})
    List<String> reportTypes,
    @JsonProperty("report-output-dir")
    @JsonAlias("reportOutputDirectory")
    Path reportOutputDirectory
) {

    public InitRequest {
        projectsDirectories = projectsDirectories == null ? List.of() : List.copyOf(projectsDirectories);
        reportTypes = reportTypes == null ? List.of() : List.copyOf(reportTypes);
    }

    public static InitRequest empty() {
        return new InitRequest(null, List.of(), null, null, null, null, null, null, null, List.of(), null);
    }

    public ConfigOverrides toOverrides(boolean noUiOverride, boolean yesOverride, boolean verboseOverride) {
        Path projectsDirectory = projectsDirectories.isEmpty() ? null : projectsDirectories.getFirst();
        return new ConfigOverrides(
            null,
            projectsDirectory,
            projectsDirectories,
            outputDirectory,
            intervalSeconds,
            harnessCsv,
            Boolean.TRUE.equals(noUi) || noUiOverride,
            Boolean.TRUE.equals(yes) || yesOverride,
            Boolean.TRUE.equals(verbose) || verboseOverride
        );
    }

    public boolean shouldShowInternalAnalysis(boolean override) {
        return override || Boolean.TRUE.equals(internalAnalysis);
    }

    public boolean hasProjectsDirectories() {
        return !projectsDirectories.isEmpty();
    }

    public ReportOptions reportOptions() {
        return new ReportOptions(user, reportTypes, reportOutputDirectory);
    }
}
