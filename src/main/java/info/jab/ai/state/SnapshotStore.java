package info.jab.ai.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.jab.ai.model.Snapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class SnapshotStore {

    private final ObjectMapper objectMapper;

    public SnapshotStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Snapshot> load(Path outputDirectory) throws IOException {
        Path path = snapshotPath(outputDirectory);
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(path.toFile(), Snapshot.class));
    }

    public void save(Path outputDirectory, Snapshot snapshot) throws IOException {
        Files.createDirectories(outputDirectory);
        objectMapper.writeValue(snapshotPath(outputDirectory).toFile(), snapshot);
    }

    public Path snapshotPath(Path outputDirectory) {
        return outputDirectory.resolve("last-snapshot.json");
    }
}
