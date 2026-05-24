package info.jab.ai.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProjectDiscovery {

    private static final List<String> LOCAL_AGENT_MARKERS = List.of(".cursor", ".claude", ".codex", "AGENTS.md", "CLAUDE.md");

    public List<Path> discover(Path projectsDirectory) throws IOException {
        Path normalized = projectsDirectory.toAbsolutePath().normalize();
        List<Path> projects = new ArrayList<>();

        if (Files.notExists(normalized)) {
            return List.of(normalized);
        }
        if (containsLocalAgentFiles(normalized)) {
            projects.add(normalized);
        }
        if (Files.isDirectory(normalized)) {
            try (var stream = Files.list(normalized)) {
                stream
                    .filter(Files::isDirectory)
                    .filter(path -> !isHidden(path))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(projects::add);
            }
        }
        if (projects.isEmpty()) {
            projects.add(normalized);
        }
        return projects.stream().distinct().toList();
    }

    private boolean containsLocalAgentFiles(Path directory) {
        return LOCAL_AGENT_MARKERS.stream().anyMatch(marker -> Files.exists(directory.resolve(marker)));
    }

    private boolean isHidden(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().startsWith(".");
    }
}
