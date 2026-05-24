package info.jab.ai.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public record ReportOptions(
    String user,
    List<String> reportTypes,
    Path outputDirectory
) {

    public ReportOptions {
        reportTypes = reportTypes == null ? List.of() : List.copyOf(reportTypes);
    }

    public static ReportOptions none() {
        return new ReportOptions(null, List.of(), null);
    }

    public boolean jsonEnabled() {
        return reportTypes.stream()
            .map(type -> type.toLowerCase(Locale.ROOT))
            .anyMatch("json"::equals);
    }
}
