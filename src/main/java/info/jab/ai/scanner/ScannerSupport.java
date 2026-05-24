package info.jab.ai.scanner;

import info.jab.ai.model.AssetType;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Harness;
import info.jab.ai.model.Scope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class ScannerSupport {

    private ScannerSupport() {
    }

    static void addFileIfExists(List<Finding> findings, Harness harness, Scope scope, AssetType type, String name, Path path)
        throws IOException {
        if (Files.isRegularFile(path)) {
            findings.add(toFinding(harness, scope, type, name, path, Map.of("source", "file")));
        }
    }

    static void addDirectoryIfExists(List<Finding> findings, Harness harness, Scope scope, AssetType type, String name, Path path)
        throws IOException {
        if (Files.isDirectory(path)) {
            findings.add(toFinding(harness, scope, type, name, path, Map.of("source", "directory")));
        }
    }

    static void addDirectoryChildren(
        List<Finding> findings,
        Harness harness,
        Scope scope,
        AssetType type,
        Path directory,
        Predicate<Path> include
    ) throws IOException {
        if (Files.notExists(directory)) {
            return;
        }
        if (Files.isRegularFile(directory) && include.test(directory)) {
            findings.add(toFinding(harness, scope, type, fileName(directory), directory, Map.of("source", "file")));
            return;
        }
        if (!Files.isDirectory(directory)) {
            return;
        }

        List<Path> children = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            children.addAll(stream.sorted().toList());
        }

        for (Path child : children) {
            if (Files.isDirectory(child)) {
                Path skillFile = child.resolve("SKILL.md");
                Path descriptor = Files.isRegularFile(skillFile) ? skillFile : child;
                findings.add(toFinding(harness, scope, type, fileName(child), descriptor, Map.of("source", "directory-child")));
            } else if (include.test(child)) {
                findings.add(toFinding(harness, scope, type, fileName(child), child, Map.of("source", "file-child")));
            }
        }
    }

    static boolean hasExtension(Path path, String... extensions) {
        String name = fileName(path).toLowerCase();
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    static Finding toFinding(
        Harness harness,
        Scope scope,
        AssetType type,
        String name,
        Path path,
        Map<String, String> metadata
    ) throws IOException {
        long size = Files.isRegularFile(path) ? Files.size(path) : 0L;
        Instant modifiedAt = Files.getLastModifiedTime(path).toInstant();
        return new Finding(
            harness,
            scope,
            type,
            name,
            path.toAbsolutePath().normalize().toString(),
            FileFingerprint.hash(path),
            modifiedAt,
            size,
            metadata
        );
    }

    private static String fileName(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
