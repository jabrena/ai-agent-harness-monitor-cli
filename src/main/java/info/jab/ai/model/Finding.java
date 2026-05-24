package info.jab.ai.model;

import java.time.Instant;
import java.util.Map;

public record Finding(
    Harness harness,
    Scope scope,
    AssetType assetType,
    String name,
    String path,
    String fingerprint,
    Instant modifiedAt,
    long sizeBytes,
    Map<String, String> metadata
) {

    public String key() {
        return harness + "|" + scope + "|" + assetType + "|" + name + "|" + path;
    }
}
