package info.jab.ai.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Harness {
    CURSOR,
    CLAUDE,
    CODEX;

    public static Optional<Harness> fromToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalized = token.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
            .filter(value -> value.name().equals(normalized))
            .findFirst();
    }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
