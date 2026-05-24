package info.jab.ai.model;

import java.time.Instant;
import java.util.List;

public record Snapshot(
    Instant createdAt,
    List<Finding> findings
) {

    public static Snapshot empty() {
        return new Snapshot(Instant.EPOCH, List.of());
    }
}
