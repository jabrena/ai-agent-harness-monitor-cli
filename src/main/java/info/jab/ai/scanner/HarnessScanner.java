package info.jab.ai.scanner;

import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Harness;
import java.io.IOException;
import java.util.List;

public interface HarnessScanner {

    Harness harness();

    List<Finding> scan(AuditConfig config) throws IOException;
}
