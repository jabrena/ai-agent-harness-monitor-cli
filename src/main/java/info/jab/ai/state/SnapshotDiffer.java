package info.jab.ai.state;

import info.jab.ai.model.ChangeEvent;
import info.jab.ai.model.ChangeType;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Snapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SnapshotDiffer {

    public List<ChangeEvent> diff(Snapshot previous, Snapshot current) {
        Instant detectedAt = current.createdAt();
        Map<String, Finding> previousByKey = byKey(previous);
        Map<String, Finding> currentByKey = byKey(current);
        List<ChangeEvent> changes = new ArrayList<>();

        for (Map.Entry<String, Finding> entry : currentByKey.entrySet()) {
            Finding before = previousByKey.get(entry.getKey());
            Finding after = entry.getValue();
            if (before == null) {
                changes.add(new ChangeEvent(ChangeType.ADDED, detectedAt, null, after));
            } else if (!before.fingerprint().equals(after.fingerprint())) {
                changes.add(new ChangeEvent(ChangeType.MODIFIED, detectedAt, before, after));
            }
        }

        for (Map.Entry<String, Finding> entry : previousByKey.entrySet()) {
            if (!currentByKey.containsKey(entry.getKey())) {
                changes.add(new ChangeEvent(ChangeType.REMOVED, detectedAt, entry.getValue(), null));
            }
        }

        return changes;
    }

    private Map<String, Finding> byKey(Snapshot snapshot) {
        Map<String, Finding> findings = new LinkedHashMap<>();
        for (Finding finding : snapshot.findings()) {
            findings.put(finding.key(), finding);
        }
        return findings;
    }
}
