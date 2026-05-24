package info.jab.ai.ui;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import info.jab.ai.config.AuditConfig;
import info.jab.ai.model.AssetType;
import info.jab.ai.model.ChangeEvent;
import info.jab.ai.model.Finding;
import info.jab.ai.model.Harness;
import info.jab.ai.model.Scope;
import info.jab.ai.model.Snapshot;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class TamboInitAnalysisApp extends ToolkitApp {

    private static final int PAGE_SIZE = 12;
    private static final int PROJECT_PAGE_SIZE = 9;
    private static final int PREVIEW_LINES = 12;
    private static final int SCANNER_OUTPUT_LINES = 16;
    private static final Duration SKILL_SCANNER_TIMEOUT = Duration.ofMinutes(2);
    private static final String SKILL_SCANNER_REPOSITORY = "https://github.com/cisco-ai-defense/skill-scanner";

    private final AuditConfig config;
    private Snapshot snapshot;
    private List<ChangeEvent> changes;
    private final Optional<String> configurationSource;
    private final RefreshSource refreshSource;
    private Optional<String> refreshError = Optional.empty();
    private View view = View.OVERVIEW;
    private int page;
    private Optional<Path> selectedProject = Optional.empty();
    private Optional<Finding> selectedFinding = Optional.empty();
    private Optional<Path> selectedSkillFile = Optional.empty();
    private View findingReturnView = View.OVERVIEW;
    private boolean deleteConfirmationPending;
    private Optional<String> actionTitle = Optional.empty();
    private List<String> actionMessages = List.of();
    private long scannerRunId;
    private long scanRunId;
    private Thread.UncaughtExceptionHandler previousUncaughtExceptionHandler;

    public TamboInitAnalysisApp(AuditConfig config, Snapshot snapshot) {
        this(config, snapshot, List.of());
    }

    public TamboInitAnalysisApp(AuditConfig config, Snapshot snapshot, List<ChangeEvent> changes) {
        this(config, snapshot, changes, Optional.empty(), null);
    }

    public TamboInitAnalysisApp(
        AuditConfig config,
        Snapshot snapshot,
        List<ChangeEvent> changes,
        Optional<String> configurationSource,
        RefreshSource refreshSource
    ) {
        this.config = config;
        this.snapshot = snapshot;
        this.changes = List.copyOf(changes);
        this.configurationSource = configurationSource == null ? Optional.empty() : configurationSource;
        this.refreshSource = refreshSource;
    }

    @Override
    protected void onStart() {
        preloadTuiKeyClasses();
        previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
        runner().eventRouter().addGlobalHandler(event -> {
            if (!(event instanceof KeyEvent key)) {
                return EventResult.UNHANDLED;
            }
            return handleKey(key);
        });
        if (refreshSource != null) {
            runner().scheduleWithFixedDelay(this::refreshFromInterval, config.interval());
        }
    }

    @Override
    protected void onStop() {
        Thread.setDefaultUncaughtExceptionHandler(previousUncaughtExceptionHandler);
    }

    @Override
    protected Element render() {
        return panel("AI Agent Harness Monitor", column(elementsForView())).rounded();
    }

    private EventResult handleKey(KeyEvent key) {
        if (key.isCharIgnoreCase('q')) {
            return EventResult.HANDLED;
        }
        if (key.isQuit()) {
            quit();
            return EventResult.HANDLED;
        }
        if (key.isCharIgnoreCase('b') || key.isKey(KeyCode.ESCAPE)) {
            if (view == View.OVERVIEW && key.isKey(KeyCode.ESCAPE)) {
                quit();
                return EventResult.HANDLED;
            }
            deleteConfirmationPending = false;
            if (view == View.PROJECT_DETAIL) {
                show(View.PROJECTS);
                return EventResult.HANDLED;
            }
            if (view == View.FINDING_DETAIL) {
                show(findingReturnView);
                return EventResult.HANDLED;
            }
            if (view == View.SKILL_FILE_DETAIL) {
                show(View.FINDING_DETAIL);
                return EventResult.HANDLED;
            }
            show(View.OVERVIEW);
            return EventResult.HANDLED;
        }
        if (key.isCharIgnoreCase('h')) {
            deleteConfirmationPending = false;
            show(View.OVERVIEW);
            return EventResult.HANDLED;
        }
        if ((view == View.FINDING_DETAIL || view == View.SKILL_FILE_DETAIL) && key.isCharIgnoreCase('d')) {
            deleteConfirmationPending = true;
            showActionMessage("Delete requested. Press Y to confirm, or B/Esc to cancel.");
            return EventResult.HANDLED;
        }
        if ((view == View.FINDING_DETAIL || view == View.SKILL_FILE_DETAIL) && key.isCharIgnoreCase('y') && deleteConfirmationPending) {
            deleteSelectedFinding();
            return EventResult.HANDLED;
        }
        if ((view == View.FINDING_DETAIL || view == View.SKILL_FILE_DETAIL) && isSelectedSkill() && key.isCharIgnoreCase('s')) {
            scanSelectedSkill();
            return EventResult.HANDLED;
        }
        if ((view == View.USER || view == View.PROJECTS) && key.isCharIgnoreCase('s')) {
            scanOnDemand();
            return EventResult.HANDLED;
        }
        int digitOffset = digitOffset(key);
        if (view == View.FINDING_DETAIL && isSelectedSkill() && digitOffset >= 0) {
            return showSkillFileDetail(digitOffset);
        }
        if (view == View.PROJECTS && digitOffset >= 0) {
            return showProjectDetail(digitOffset);
        }
        if (view == View.PROJECT_DETAIL && digitOffset >= 0) {
            return showFindingDetail(digitOffset);
        }
        if (isFindingMenuView(view) && digitOffset >= 0) {
            return showFindingMenuDetail(digitOffset);
        }
        if (key.isCharIgnoreCase('u')) {
            show(View.USER);
            return EventResult.HANDLED;
        }
        if (key.isCharIgnoreCase('p')) {
            show(View.PROJECTS);
            return EventResult.HANDLED;
        }
        if (key.isPageDown() || key.isCharIgnoreCase('n')) {
            page = Math.min(page + 1, maxPage());
            return EventResult.HANDLED;
        }
        if (key.isPageUp() || key.isCharIgnoreCase('r')) {
            page = Math.max(0, page - 1);
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private int digitOffset(KeyEvent key) {
        for (int digit = 1; digit <= 9; digit++) {
            if (key.isChar((char) ('0' + digit))) {
                return digit - 1;
            }
        }
        return -1;
    }

    private void preloadTuiKeyClasses() {
        KeyEvent.ofChar(' ');
        KeyModifiers.NONE.toString();
    }

    private void refreshFromInterval() {
        try {
            applyRefresh(refreshSource.refresh());
        } catch (Exception e) {
            refreshError = Optional.of("Refresh failed: " + e.getMessage());
        }
    }

    private void applyRefresh(RefreshResult result) {
        snapshot = result.snapshot();
        changes = List.copyOf(result.changes());
        refreshError = Optional.empty();
        page = Math.min(page, maxPage());
    }

    private void handleUncaughtException(Thread thread, Throwable throwable) {
        if (thread.getName().contains("tui-input-reader")) {
            quit();
            return;
        }
        if (previousUncaughtExceptionHandler != null) {
            previousUncaughtExceptionHandler.uncaughtException(thread, throwable);
            return;
        }
        throwable.printStackTrace();
    }

    private void show(View nextView) {
        view = nextView;
        page = 0;
        scannerRunId++;
        actionTitle = Optional.empty();
        actionMessages = List.of();
        deleteConfirmationPending = false;
    }

    private EventResult showProjectDetail(int pageOffset) {
        List<ProjectDetection> projects = projectDetections();
        int index = (page * PROJECT_PAGE_SIZE) + pageOffset;
        if (index >= projects.size()) {
            return EventResult.HANDLED;
        }
        selectedProject = Optional.of(projects.get(index).path());
        selectedFinding = Optional.empty();
        show(View.PROJECT_DETAIL);
        return EventResult.HANDLED;
    }

    private EventResult showFindingDetail(int pageOffset) {
        if (selectedProject.isEmpty()) {
            return EventResult.HANDLED;
        }
        List<Finding> findings = projectFindings(selectedProject.get());
        int index = (page * PROJECT_PAGE_SIZE) + pageOffset;
        if (index >= findings.size()) {
            return EventResult.HANDLED;
        }
        selectedFinding = Optional.of(findings.get(index));
        findingReturnView = View.PROJECT_DETAIL;
        selectedSkillFile = Optional.empty();
        show(View.FINDING_DETAIL);
        return EventResult.HANDLED;
    }

    private EventResult showFindingMenuDetail(int pageOffset) {
        List<Finding> findings = sorted(findingsForView(view));
        int index = (page * PROJECT_PAGE_SIZE) + pageOffset;
        if (index >= findings.size()) {
            return EventResult.HANDLED;
        }
        selectedFinding = Optional.of(findings.get(index));
        findingReturnView = view;
        selectedSkillFile = Optional.empty();
        show(View.FINDING_DETAIL);
        return EventResult.HANDLED;
    }

    private EventResult showSkillFileDetail(int pageOffset) {
        if (selectedFinding.isEmpty()) {
            return EventResult.HANDLED;
        }
        List<Path> files = skillFiles(selectedFinding.get());
        int index = (page * PROJECT_PAGE_SIZE) + pageOffset;
        if (index >= files.size()) {
            return EventResult.HANDLED;
        }
        selectedSkillFile = Optional.of(files.get(index));
        show(View.SKILL_FILE_DETAIL);
        return EventResult.HANDLED;
    }

    private Element[] elementsForView() {
        return switch (view) {
            case OVERVIEW -> overview();
            case USER -> userView();
            case PROJECTS -> projectsView();
            case PROJECT_DETAIL -> projectDetailView();
            case FINDING_DETAIL -> findingDetailView();
            case SKILL_FILE_DETAIL -> skillFileDetailView();
        };
    }

    private Element[] overview() {
        List<Element> elements = new ArrayList<>();
        elements.add(text("Dynamic analysis").bold().cyan());
        elements.add(text("Scan complete: " + snapshot.findings().size() + " findings, " + changes.size() + " changes"));
        configurationSource.ifPresent(source -> elements.add(text("Configuration: " + source)));
        refreshError.ifPresent(error -> elements.add(text(error)));
        elements.add(spacer());
        elements.add(text("User harness discovery").bold());
        for (Harness harness : Harness.values()) {
            elements.add(text(harness.displayName() + ": " + userHarnessSummary(harness)));
        }
        elements.add(spacer());
        elements.add(text("Project folders: " + config.projectsDirectories()));
        elements.add(text("Projects with detections: " + projectDetections().size()));
        elements.add(spacer());
        elements.add(text("Navigation: U user, P projects, Esc quit").dim());
        return elements.toArray(Element[]::new);
    }

    private Element[] userView() {
        List<Finding> findings = sorted(snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.USER)
            .toList());
        List<Element> elements = scanHeader("User Level Discovery");
        for (Harness harness : Harness.values()) {
            elements.add(text(harness.displayName() + ": " + userHarnessSummary(harness)));
        }
        elements.add(spacer());
        addCounts(elements, findings);
        addPagedFindingMenu(elements, findings);
        addFooterMessage(elements);
        return elements.toArray(Element[]::new);
    }

    private Element[] projectsView() {
        List<Element> elements = scanHeader("Project Discovery");
        elements.add(text("Project folders: " + config.projectsDirectories()));
        List<ProjectDetection> projectDetections = projectDetections();
        elements.add(text(
            "Projects with detections: " + projectDetections.size() + " -> " + totalProjectDetections(projectDetections) + " detections"
        ));
        elements.add(spacer());
        addPagedProjects(elements, projectDetections);
        addFooterMessage(elements);
        return elements.toArray(Element[]::new);
    }

    private Element[] projectDetailView() {
        Optional<Path> projectRoot = selectedProject;
        if (projectRoot.isEmpty()) {
            show(View.PROJECTS);
            return projectsView();
        }
        List<Finding> findings = projectFindings(projectRoot.get());
        List<Element> elements = header("Project Detection Details");
        elements.add(text("Project: " + projectRoot.get()));
        elements.add(text("Detections: " + findings.size()));
        addCounts(elements, findings);
        addPagedFindingMenu(elements, findings);
        return elements.toArray(Element[]::new);
    }

    private Element[] findingDetailView() {
        Optional<Finding> finding = selectedFinding;
        if (finding.isEmpty()) {
            show(View.PROJECT_DETAIL);
            return projectDetailView();
        }
        Finding selected = finding.get();
        if (isSkillFinding(selected)) {
            return skillDetailView(selected);
        }
        List<Element> elements = new ArrayList<>();
        elements.add(text("Detection Details").bold().cyan());
        elements.add(text("B/Esc back, D delete, Y confirm delete, H overview").dim());
        elements.add(spacer());
        elements.add(text("Harness: " + selected.harness().displayName()));
        elements.add(text("Type: " + selected.assetType().name().toLowerCase(Locale.ROOT)));
        elements.add(text("Name: " + selected.name()));
        elements.add(text("Path: " + selected.path()));
        elements.add(text("Delete target: " + deleteTarget(selected)));
        elements.add(spacer());
        elements.add(text("Preview").bold());
        preview(selected).forEach(line -> elements.add(text(line)));
        addFooterMessage(elements);
        return elements.toArray(Element[]::new);
    }

    private Element[] skillDetailView(Finding skill) {
        List<Element> elements = new ArrayList<>();
        Path skillRoot = skillRoot(skill);
        elements.add(text("Skill Details").bold().cyan());
        elements.add(text("B/Esc back, 1-9 open file, S scanner, N/PageDown next, R/PageUp previous, D delete whole skill").dim());
        elements.add(spacer());
        elements.add(text("Harness: " + skill.harness().displayName()));
        elements.add(text("Skill: " + skill.name()));
        elements.add(text("Skill root: " + skillRoot));
        elements.add(text("Delete target: " + skillRoot));
        addPagedSkillFiles(elements, skill);
        addFooterMessage(elements);
        return elements.toArray(Element[]::new);
    }

    private Element[] skillFileDetailView() {
        if (selectedSkillFile.isEmpty()) {
            show(View.FINDING_DETAIL);
            return findingDetailView();
        }
        Path file = selectedSkillFile.get();
        List<Element> elements = new ArrayList<>();
        elements.add(text("Skill File Preview").bold().cyan());
        elements.add(text("B/Esc back, S scanner, D delete whole skill, Y confirm delete, H overview").dim());
        elements.add(spacer());
        elements.add(text("File: " + file));
        selectedFinding.map(this::skillRoot).ifPresent(root -> elements.add(text("Skill root: " + root)));
        selectedFinding.map(this::deleteTarget).ifPresent(target -> elements.add(text("Delete target: " + target)));
        elements.add(spacer());
        elements.add(text("Preview").bold());
        preview(file).forEach(line -> elements.add(text(line)));
        addFooterMessage(elements);
        return elements.toArray(Element[]::new);
    }

    private List<Element> header(String title) {
        List<Element> elements = new ArrayList<>();
        elements.add(text(title).bold().cyan());
        elements.add(text("B/Esc back, H overview, N/PageDown next, R/PageUp previous").dim());
        elements.add(spacer());
        return elements;
    }

    private List<Element> scanHeader(String title) {
        List<Element> elements = new ArrayList<>();
        elements.add(text(title).bold().cyan());
        elements.add(text("B/Esc back, H overview, S Scan, N/PageDown next, R/PageUp previous").dim());
        elements.add(spacer());
        return elements;
    }

    private String userHarnessSummary(Harness harness) {
        return config.userRoots().get(harness) + " -> " + count(harness, Scope.USER) + " detections";
    }

    private void addCounts(List<Element> elements, List<Finding> findings) {
        Map<AssetType, Long> counts = new EnumMap<>(AssetType.class);
        for (AssetType assetType : AssetType.values()) {
            counts.put(assetType, count(findings, assetType));
        }
        elements.add(text("Detected assets").bold());
        elements.add(text("Skills: " + counts.get(AssetType.SKILL)));
        elements.add(text("Rules: " + counts.get(AssetType.RULE)));
        elements.add(text("Guidance files: " + counts.get(AssetType.GUIDANCE)));
        elements.add(text("MCPs: " + counts.get(AssetType.MCP)));
        elements.add(text("Config files: " + counts.get(AssetType.CONFIG)));
    }

    private void addPagedFindingMenu(List<Element> elements, List<Finding> findings) {
        List<Finding> sortedFindings = sorted(findings);
        elements.add(spacer());
        elements.add(text("Details (" + sortedFindings.size() + ")").bold());
        if (sortedFindings.isEmpty()) {
            elements.add(text("- none"));
            return;
        }
        int maxPage = maxPage(sortedFindings.size(), PROJECT_PAGE_SIZE);
        page = Math.min(page, maxPage);
        int start = page * PROJECT_PAGE_SIZE;
        int end = Math.min(sortedFindings.size(), start + PROJECT_PAGE_SIZE);
        elements.add(text("Page " + (page + 1) + "/" + (maxPage + 1) + " - press 1-9 to open detection details"));
        int displayIndex = 1;
        for (Finding finding : sortedFindings.subList(start, end)) {
            elements.add(text(displayIndex + ". " + formatFinding(finding)));
            displayIndex++;
        }
    }

    private void addPagedSkillFiles(List<Element> elements, Finding skill) {
        List<Path> files = skillFiles(skill);
        elements.add(spacer());
        elements.add(text("Skill files (" + files.size() + ")").bold());
        if (files.isEmpty()) {
            elements.add(text("- none"));
            return;
        }
        int maxPage = maxPage(files.size(), PROJECT_PAGE_SIZE);
        page = Math.min(page, maxPage);
        int start = page * PROJECT_PAGE_SIZE;
        int end = Math.min(files.size(), start + PROJECT_PAGE_SIZE);
        elements.add(text("Page " + (page + 1) + "/" + (maxPage + 1) + " - press 1-9 to open file"));
        Path root = skillRoot(skill);
        int displayIndex = 1;
        for (Path file : files.subList(start, end)) {
            elements.add(text(displayIndex + ". " + root.relativize(file)));
            displayIndex++;
        }
    }

    private void addPagedProjects(List<Element> elements, List<ProjectDetection> projects) {
        elements.add(spacer());
        elements.add(text("Details (" + projects.size() + ")").bold());
        if (projects.isEmpty()) {
            elements.add(text("- none"));
            return;
        }
        int maxPage = maxPage(projects.size(), PROJECT_PAGE_SIZE);
        page = Math.min(page, maxPage);
        int start = page * PROJECT_PAGE_SIZE;
        int end = Math.min(projects.size(), start + PROJECT_PAGE_SIZE);
        elements.add(text("Page " + (page + 1) + "/" + (maxPage + 1) + " - press 1-9 to open project details"));
        int displayIndex = 1;
        for (ProjectDetection project : projects.subList(start, end)) {
            elements.add(text(displayIndex + ". " + project.path() + " -> " + project.detections() + " detections"));
            displayIndex++;
        }
    }

    private void addFooterMessage(List<Element> elements) {
        if (!actionMessages.isEmpty()) {
            elements.add(spacer());
            actionTitle.ifPresent(title -> {
                elements.add(text(title).bold().cyan());
                elements.add(text("B/Esc back, 1-9 open file, N/PageDown next, R/PageUp previous, D delete whole skill").dim());
                elements.add(spacer());
            });
            actionMessages.forEach(message -> elements.add(actionTitle.isPresent() ? text(message) : text(message).dim()));
        }
    }

    private void showActionMessage(String message) {
        scannerRunId++;
        actionTitle = Optional.empty();
        actionMessages = List.of(message);
    }

    private void showActionSection(String title, List<String> messages) {
        actionTitle = Optional.of(title);
        actionMessages = List.copyOf(messages);
    }

    private void scanOnDemand() {
        if (refreshSource == null) {
            showActionMessage("Scan is unavailable in this mode.");
            return;
        }
        long runId = ++scanRunId;
        showActionMessage("Scan started...");
        Thread scanThread = new Thread(() -> runOnDemandScan(runId), "on-demand-scan-runner");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private void runOnDemandScan(long runId) {
        try {
            RefreshResult result = refreshSource.refresh();
            runner().runOnRenderThread(() -> {
                if (runId == scanRunId) {
                    applyRefresh(result);
                    showActionMessage("Scan complete: " + snapshot.findings().size() + " findings.");
                }
            });
        } catch (Exception e) {
            runner().runOnRenderThread(() -> {
                if (runId == scanRunId) {
                    refreshError = Optional.of("Refresh failed: " + e.getMessage());
                    showActionMessage("Scan failed: " + e.getMessage());
                }
            });
        }
    }

    private String formatFinding(Finding finding) {
        if (finding.assetType() == AssetType.SKILL) {
            return assetTypeLabel(finding.assetType()) + ": " + finding.name();
        }
        return finding.harness().displayName()
            + " "
            + assetTypeLabel(finding.assetType())
            + ": "
            + finding.name()
            + " -> "
            + finding.path();
    }

    private String assetTypeLabel(AssetType assetType) {
        return switch (assetType) {
            case SKILL -> "Skill";
            case RULE -> "Rule";
            case GUIDANCE -> "Guidance file";
            case MCP -> "MCP";
            case CONFIG -> "Config file";
        };
    }

    private List<Finding> sorted(List<Finding> findings) {
        return findings.stream()
            .sorted(Comparator
                .comparing(Finding::harness)
                .thenComparing(Finding::scope)
                .thenComparing(Finding::assetType)
                .thenComparing(Finding::name)
                .thenComparing(Finding::path))
            .toList();
    }

    private int countProjectFindings(Path projectRoot) {
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        return (int) snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.PROJECT)
            .filter(finding -> belongsToProject(finding, normalizedProjectRoot))
            .count();
    }

    private List<Finding> projectFindings(Path projectRoot) {
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        return sorted(snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.PROJECT)
            .filter(finding -> belongsToProject(finding, normalizedProjectRoot))
            .toList());
    }

    private List<Finding> findingsForView(View currentView) {
        return switch (currentView) {
            case USER -> sorted(snapshot.findings().stream().filter(finding -> finding.scope() == Scope.USER).toList());
            case PROJECT_DETAIL -> selectedProject.map(this::projectFindings).orElse(List.of());
            case OVERVIEW, PROJECTS, FINDING_DETAIL, SKILL_FILE_DETAIL -> List.of();
        };
    }

    private boolean isFindingMenuView(View currentView) {
        return currentView == View.USER;
    }

    private List<ProjectDetection> projectDetections() {
        return config.projectScanRoots().stream()
            .map(projectRoot -> new ProjectDetection(projectRoot, countProjectFindings(projectRoot)))
            .filter(project -> project.detections() > 0)
            .toList();
    }

    private int totalProjectDetections(List<ProjectDetection> projectDetections) {
        return projectDetections.stream()
            .mapToInt(ProjectDetection::detections)
            .sum();
    }

    private boolean belongsToProject(Finding finding, Path normalizedProjectRoot) {
        return assignedProjectRoot(finding)
            .map(normalizedProjectRoot::equals)
            .orElse(false);
    }

    private Optional<Path> assignedProjectRoot(Finding finding) {
        if (finding.scope() != Scope.PROJECT) {
            return Optional.empty();
        }
        try {
            Path findingPath = Path.of(finding.path()).toAbsolutePath().normalize();
            return config.projectScanRoots().stream()
                .map(path -> path.toAbsolutePath().normalize())
                .filter(findingPath::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private List<String> preview(Finding finding) {
        Path path = Path.of(finding.path());
        return preview(path);
    }

    private List<String> preview(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var children = Files.list(path)) {
                    List<String> names = children
                        .limit(PREVIEW_LINES)
                        .map(child -> "- " + child.getFileName())
                        .toList();
                    return names.isEmpty() ? List.of("(empty directory)") : names;
                }
            }
            if (!Files.isRegularFile(path)) {
                return List.of("(not a regular file)");
            }
            try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
                List<String> preview = lines
                    .limit(PREVIEW_LINES)
                    .toList();
                return preview.isEmpty() ? List.of("(empty file)") : preview;
            }
        } catch (IOException | RuntimeException e) {
            return List.of("(preview unavailable: " + e.getMessage() + ")");
        }
    }

    private Path deleteTarget(Finding finding) {
        if (isSkillFinding(finding)) {
            return skillRoot(finding);
        }
        Path path = Path.of(finding.path()).toAbsolutePath().normalize();
        return path;
    }

    private boolean isSelectedSkill() {
        return selectedFinding.map(this::isSkillFinding).orElse(false);
    }

    private boolean isSkillFinding(Finding finding) {
        return finding.assetType() == AssetType.SKILL;
    }

    private Path skillRoot(Finding finding) {
        Path path = Path.of(finding.path()).toAbsolutePath().normalize();
        if (finding.assetType() == AssetType.SKILL
            && "directory-child".equals(finding.metadata().get("source"))
            && path.getFileName() != null
            && "SKILL.md".equals(path.getFileName().toString())
            && path.getParent() != null) {
            return path.getParent();
        }
        return path;
    }

    private List<Path> skillFiles(Finding finding) {
        Path root = skillRoot(finding);
        if (!Files.isDirectory(root)) {
            return Files.isRegularFile(root) ? List.of(root) : List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths
                .filter(Files::isRegularFile)
                .sorted(Comparator
                    .comparing((Path path) -> !path.getFileName().toString().equals("SKILL.md"))
                    .thenComparing(path -> root.relativize(path).toString()))
                .toList();
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    private void scanSelectedSkill() {
        if (selectedFinding.isEmpty()) {
            return;
        }
        deleteConfirmationPending = false;
        Path scanTarget = skillRoot(selectedFinding.get());
        long runId = ++scannerRunId;
        showActionSection("Skill Scanner Output", scannerInitialMessages(scanTarget));
        Thread scannerThread = new Thread(() -> runSkillScanner(runId, scanTarget), "skill-scanner-runner");
        scannerThread.setDaemon(true);
        scannerThread.start();
    }

    private List<String> scannerInitialMessages(Path scanTarget) {
        return List.of(
            "Command: skill-scanner scan " + scanTarget + " --use-behavioral --policy strict",
            "Status: starting..."
        );
    }

    private void runSkillScanner(long runId, Path scanTarget) {
        try {
            if (!isSkillScannerInstalled()) {
                updateScannerMessages(runId, List.of(
                    "skill-scanner is not installed.",
                    "Visit: " + SKILL_SCANNER_REPOSITORY
                ));
                return;
            }
            updateScannerStatus(runId, "Status: running...");
            runSkillScannerProcess(runId, scanTarget);
        } catch (IOException e) {
            updateScannerStatus(runId, "Skill scanner failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateScannerStatus(runId, "Skill scanner interrupted.");
        }
    }

    private boolean isSkillScannerInstalled() throws InterruptedException {
        try {
            Process process = new ProcessBuilder("skill-scanner", "--help")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
            }
            return completed;
        } catch (IOException e) {
            return false;
        }
    }

    private void runSkillScannerProcess(long runId, Path scanTarget) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("skill-scanner", "scan", scanTarget.toString(), "--use-behavioral", "--policy", "strict")
            .redirectErrorStream(true)
            .start();
        Thread outputReader = new Thread(() -> readOutput(runId, process), "skill-scanner-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();

        boolean completed = process.waitFor(SKILL_SCANNER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroy();
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
        outputReader.join(1_000);
        if (completed) {
            updateScannerStatus(runId, "Exit code: " + process.exitValue());
        } else {
            updateScannerStatus(runId, "Timed out after " + SKILL_SCANNER_TIMEOUT.toSeconds() + "s");
        }
    }

    private void readOutput(long runId, Process process) {
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sanitizeScannerLine(line).ifPresent(sanitized -> appendScannerOutput(runId, sanitized));
            }
        } catch (IOException e) {
            appendScannerOutput(runId, "(scanner output unavailable: " + e.getMessage() + ")");
        }
    }

    private Optional<String> sanitizeScannerLine(String line) {
        String sanitized = line
            .replaceAll("\\u001B\\][^\\u0007]*(\\u0007|\\u001B\\\\)", "")
            .replaceAll("\\u001B\\[[0-?]*[ -/]*[@-~]", "")
            .replaceAll("[\\p{Cntrl}&&[^\\t]]", "");
        return sanitized.isBlank() ? Optional.empty() : Optional.of(sanitized);
    }

    private void updateScannerMessages(long runId, List<String> messages) {
        runner().runOnRenderThread(() -> {
            if (runId == scannerRunId) {
                showActionSection("Skill Scanner Output", messages);
            }
        });
    }

    private void updateScannerStatus(long runId, String status) {
        runner().runOnRenderThread(() -> {
            if (runId == scannerRunId && actionTitle.isPresent()) {
                List<String> messages = new ArrayList<>(actionMessages);
                if (messages.size() < 2) {
                    messages.add(status);
                } else {
                    messages.set(1, status);
                }
                actionMessages = List.copyOf(messages);
            }
        });
    }

    private void appendScannerOutput(long runId, String line) {
        runner().runOnRenderThread(() -> {
            if (runId == scannerRunId && actionTitle.isPresent()) {
                List<String> messages = new ArrayList<>(actionMessages);
                messages.add(line);
                actionMessages = List.copyOf(trimScannerMessages(messages));
            }
        });
    }

    private List<String> trimScannerMessages(List<String> messages) {
        int headerLines = Math.min(2, messages.size());
        int outputLines = messages.size() - headerLines;
        if (outputLines <= SCANNER_OUTPUT_LINES) {
            return messages;
        }
        List<String> trimmed = new ArrayList<>(messages.subList(0, headerLines));
        trimmed.add("... output truncated to last " + SCANNER_OUTPUT_LINES + " lines");
        trimmed.addAll(messages.subList(messages.size() - SCANNER_OUTPUT_LINES, messages.size()));
        return trimmed;
    }

    private void deleteSelectedFinding() {
        if (selectedFinding.isEmpty()) {
            return;
        }
        Finding finding = selectedFinding.get();
        Path target = deleteTarget(finding);
        try {
            deleteRecursively(target);
            deleteConfirmationPending = false;
            selectedFinding = Optional.empty();
            refreshAfterDelete(finding);
            show(findingReturnView);
            showActionMessage("Deleted: " + target);
        } catch (IOException | RuntimeException e) {
            showActionMessage("Delete failed: " + e.getMessage());
            deleteConfirmationPending = false;
        }
    }

    private void refreshAfterDelete(Finding deletedFinding) {
        if (refreshSource != null) {
            refreshFromInterval();
            return;
        }
        snapshot = new Snapshot(
            snapshot.createdAt(),
            snapshot.findings().stream()
                .filter(finding -> !finding.key().equals(deletedFinding.key()))
                .toList()
        );
    }

    private void deleteRecursively(Path target) throws IOException {
        if (Files.notExists(target)) {
            return;
        }
        if (Files.isDirectory(target)) {
            try (var paths = Files.walk(target)) {
                List<Path> deleteOrder = paths
                    .sorted(Comparator.reverseOrder())
                    .toList();
                for (Path path : deleteOrder) {
                    Files.deleteIfExists(path);
                }
            }
            return;
        }
        Files.deleteIfExists(target);
    }

    private long count(Harness harness, Scope scope) {
        return snapshot.findings().stream()
            .filter(finding -> finding.harness() == harness)
            .filter(finding -> finding.scope() == scope)
            .count();
    }

    private long count(List<Finding> findings, AssetType assetType) {
        return findings.stream()
            .filter(finding -> finding.assetType() == assetType)
            .count();
    }

    private int maxPage() {
        int size = switch (view) {
            case USER -> (int) snapshot.findings().stream().filter(finding -> finding.scope() == Scope.USER).count();
            case PROJECTS -> projectDetections().size();
            case PROJECT_DETAIL -> selectedProject.map(this::projectFindings).map(List::size).orElse(0);
            case FINDING_DETAIL -> selectedFinding.filter(this::isSkillFinding).map(this::skillFiles).map(List::size).orElse(0);
            case SKILL_FILE_DETAIL -> 0;
            case OVERVIEW -> 0;
        };
        int pageSize = view == View.PROJECTS
            || view == View.PROJECT_DETAIL
            || view == View.FINDING_DETAIL
            || isFindingMenuView(view)
            ? PROJECT_PAGE_SIZE
            : PAGE_SIZE;
        return maxPage(size, pageSize);
    }

    private int maxPage(int size, int pageSize) {
        return Math.max(0, (size - 1) / pageSize);
    }

    private enum View {
        OVERVIEW,
        USER,
        PROJECTS,
        PROJECT_DETAIL,
        FINDING_DETAIL,
        SKILL_FILE_DETAIL
    }

    @FunctionalInterface
    public interface RefreshSource {
        RefreshResult refresh() throws Exception;
    }

    public record RefreshResult(Snapshot snapshot, List<ChangeEvent> changes) {
        public RefreshResult {
            changes = List.copyOf(changes);
        }
    }

    private record ProjectDetection(Path path, int detections) {
    }

}
