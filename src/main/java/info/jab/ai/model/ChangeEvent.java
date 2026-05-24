package info.jab.ai.model;

import java.time.Instant;

public record ChangeEvent(
    ChangeType type,
    Instant detectedAt,
    Finding before,
    Finding after
) {
}
