package info.jab.ai;

import info.jab.ai.cli.AuditCli;
import picocli.CommandLine;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    static int execute(String... args) {
        try {
            preloadRuntimeClasses();
            return new CommandLine(new AuditCli()).execute(args);
        } catch (NoClassDefFoundError e) {
            System.err.println("Runtime class loading failed. Please stop any running CLI instance, rebuild, and rerun: " + e.getMessage());
            return 1;
        }
    }

    private static void preloadRuntimeClasses() {
        try {
            Class.forName("picocli.CommandLine$IExitCodeGenerator");
            Class.forName("picocli.CommandLine$RunLast");
            Class.forName("dev.tamboui.tui.event.KeyEvent");
            Class.forName("dev.tamboui.tui.event.KeyModifiers");
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }
}
