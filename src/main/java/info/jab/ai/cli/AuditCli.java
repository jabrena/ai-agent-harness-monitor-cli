package info.jab.ai.cli;

import info.jab.ai.config.ConfigOverrides;
import info.jab.ai.config.ConfigurationFactory;
import info.jab.ai.config.ConfigurationStore;
import info.jab.ai.config.DefaultPathResolver;
import info.jab.ai.config.InitRequest;
import info.jab.ai.config.JsonMapper;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "ai-agent-audit",
    description = "Audit AI agent harness skills, MCP servers, rules, and config.",
    mixinStandardHelpOptions = true,
    version = "ai-agent-audit 1.0-SNAPSHOT",
    subcommands = {
        AuditCli.InitCommand.class
    }
)
public final class AuditCli implements Runnable {

    @Override
    public void run() {
        new picocli.CommandLine(this).usage(System.out);
    }

    @Command(name = "init", description = "Initialize the audit from a JSON configuration.")
    static final class InitCommand implements Callable<Integer> {
        @Option(
            names = "--config",
            description = "Inline JSON or path to a JSON file. Example: '{\"projects-dirs\":[\"/Users/me/IdeaProjects\"]}'"
        )
        String config;

        @Option(names = "--config-file", description = "Path to a JSON configuration file.")
        Path configFile;

        @Option(names = "--internal-analysis", description = "Print the internal skills, rules, and MCP drill-down.")
        boolean internalAnalysis;

        @Option(names = "--no-ui", description = "Disable TamboUI and use plain console output.")
        boolean noUi;

        @Option(names = "--yes", description = "Accept the computed configuration without prompting.")
        boolean yes;

        @Option(names = "--verbose", description = "Print additional scan details.")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            InitRequest initRequest = readRequest();
            boolean configurationProvided = config != null || configFile != null;
            ConfigOverrides overrides = initRequest.toOverrides(noUi, yes || configurationProvided, verbose);
            return application().init(
                configurationFactory().create(overrides),
                initRequest.shouldShowInternalAnalysis(internalAnalysis),
                !initRequest.hasProjectsDirectories(),
                initRequest.reportOptions()
            );
        }

        private InitRequest readRequest() throws Exception {
            if (config != null && configFile != null) {
                throw new IllegalArgumentException("Use either --config or --config-file, not both.");
            }
            if (configFile != null) {
                return JsonMapper.create().readValue(configFile.toFile(), InitRequest.class);
            }
            if (config == null || config.isBlank()) {
                return InitRequest.empty();
            }
            String trimmedConfig = config.trim();
            if (trimmedConfig.startsWith("{")) {
                return JsonMapper.create().readValue(trimmedConfig, InitRequest.class);
            }
            return JsonMapper.create().readValue(Files.readString(Path.of(trimmedConfig)), InitRequest.class);
        }
    }

    private static AuditApplication application() {
        return new AuditApplication(new PrintWriter(System.out, true));
    }

    private static ConfigurationFactory configurationFactory() {
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        return new ConfigurationFactory(new DefaultPathResolver(home), new ConfigurationStore(JsonMapper.create()));
    }
}
