package info.jab.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class ConfigurationStore {

    private final ObjectMapper objectMapper;

    public ConfigurationStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<AuditConfig> load(Path outputDirectory) throws IOException {
        Path configPath = configPath(outputDirectory);
        if (Files.notExists(configPath)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(configPath.toFile(), AuditConfig.class));
    }

    public void save(AuditConfig config) throws IOException {
        Files.createDirectories(config.outputDirectory());
        objectMapper.writeValue(configPath(config.outputDirectory()).toFile(), config);
    }

    public Path configPath(Path outputDirectory) {
        return outputDirectory.resolve("config.json");
    }
}
