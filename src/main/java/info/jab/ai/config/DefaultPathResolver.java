package info.jab.ai.config;

import info.jab.ai.model.Harness;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultPathResolver {

    private final Path homeDirectory;

    public DefaultPathResolver(Path homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public Map<Harness, Path> userRoots() {
        EnumMap<Harness, Path> roots = new EnumMap<>(Harness.class);
        roots.put(Harness.CURSOR, homeDirectory.resolve(".cursor"));
        roots.put(Harness.CLAUDE, homeDirectory.resolve(".claude"));
        roots.put(Harness.CODEX, homeDirectory.resolve(".codex"));
        return roots;
    }

    public Map<String, Path> projectRoots(Path projectRoot) {
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("cursorProject", projectRoot.resolve(".cursor"));
        roots.put("claudeProject", projectRoot.resolve(".claude"));
        roots.put("codexProject", projectRoot.resolve(".codex"));
        roots.put("agentsFile", projectRoot.resolve("AGENTS.md"));
        roots.put("claudeFile", projectRoot.resolve("CLAUDE.md"));
        return roots;
    }
}
