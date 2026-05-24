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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TamboInitAnalysisApp extends ToolkitApp {

    private static final int PAGE_SIZE = 12;
    private static final int PROJECT_PAGE_SIZE = 9;
    private static final int PREVIEW_LINES = 12;

    private final AuditConfig config;
    private Snapshot snapshot;
    private List<ChangeEvent> changes;
    private Optional<Path> reportPath;
    private final RefreshSource refreshSource;
    private Optional<String> refreshError = Optional.empty();
    private View view = View.OVERVIEW;
    private int page;
    private Optional<Path> selectedProject = Optional.empty();
    private Optional<Finding> selectedFinding = Optional.empty();
    private Optional<Path> selectedSkillFile = Optional.empty();
    private View findingReturnView = View.OVERVIEW;
    private boolean deleteConfirmationPending;
    private Optional<String> actionMessage = Optional.empty();
    private Thread.UncaughtExceptionHandler previousUncaughtExceptionHandler;

    public TamboInitAnalysisApp(AuditConfig config, Snapshot snapshot) {
        this(config, snapshot, List.of(), Optional.empty());
    }

    public TamboInitAnalysisApp(AuditConfig config, Snapshot snapshot, List<ChangeEvent> changes) {
        this(config, snapshot, changes, Optional.empty());
    }

    public TamboInitAnalysisApp(
        AuditConfig config,
        Snapshot snapshot,
        List<ChangeEvent> changes,
        Optional<Path> reportPath
    ) {
        this(config, snapshot, changes, reportPath, null);
    }

    public TamboInitAnalysisApp(
        AuditConfig config,
        Snapshot snapshot,
        List<ChangeEvent> changes,
        Optional<Path> reportPath,
        RefreshSource refreshSource
    ) {
        this.config = config;
        this.snapshot = snapshot;
        this.changes = List.copyOf(changes);
        this.reportPath = reportPath;
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
        if (view == View.OVERVIEW && key.isCharIgnoreCase('q')) {
            return EventResult.HANDLED;
        }
        if ((key.isQuit() || key.isCharIgnoreCase('q')) && view != View.OVERVIEW) {
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
            actionMessage = Optional.of("Delete requested. Press Y to confirm, or B/Esc to cancel.");
            return EventResult.HANDLED;
        }
        if ((view == View.FINDING_DETAIL || view == View.SKILL_FILE_DETAIL) && key.isCharIgnoreCase('y') && deleteConfirmationPending) {
            deleteSelectedFinding();
            return EventResult.HANDLED;
        }
        if (view == View.FINDING_DETAIL && isSelectedSkill() && key.character() >= '1' && key.character() <= '9') {
            return showSkillFileDetail(key.character() - '1');
        }
        if (view == View.PROJECTS && key.character() >= '1' && key.character() <= '9') {
            return showProjectDetail(key.character() - '1');
        }
        if (view == View.PROJECT_DETAIL && key.character() >= '1' && key.character() <= '9') {
            return showFindingDetail(key.character() - '1');
        }
        if (isHarnessView(view) && key.character() >= '1' && key.character() <= '9') {
            return showHarnessFindingDetail(key.character() - '1');
        }
        if (key.isChar('1') || key.isCharIgnoreCase('c')) {
            show(View.CURSOR);
            return EventResult.HANDLED;
        }
        if (key.isChar('2') || key.isCharIgnoreCase('a')) {
            show(View.CLAUDE);
            return EventResult.HANDLED;
        }
        if (key.isChar('3') || key.isCharIgnoreCase('x')) {
            show(View.CODEX);
            return EventResult.HANDLED;
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

    private void preloadTuiKeyClasses() {
        KeyEvent.ofChar(' ');
        KeyModifiers.NONE.toString();
    }

    private void refreshFromInterval() {
        try {
            RefreshResult result = refreshSource.refresh();
            snapshot = result.snapshot();
            changes = List.copyOf(result.changes());
            reportPath = result.reportPath();
            refreshError = Optional.empty();
        } catch (Exception e) {
            refreshError = Optional.of("Refresh failed: " + e.getMessage());
        }
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
        actionMessage = Optional.empty();
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

    private EventResult showHarnessFindingDetail(int pageOffset) {
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
            case CURSOR -> harnessView(Harness.CURSOR);
            case CLAUDE -> harnessView(Harness.CLAUDE);
            case CODEX -> harnessView(Harness.CODEX);
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
        elements.add(text("Scan update: " + config.interval().toSeconds() + "s"));
        refreshError.ifPresent(error -> elements.add(text(error)));
        reportPath.ifPresent(path -> elements.add(text("Report: " + path)));
        elements.add(spacer());
        elements.add(text("User harness discovery").bold());
        for (Harness harness : Harness.values()) {
            elements.add(text(harness.displayName() + ": " + userHarnessSummary(harness)));
        }
        elements.add(spacer());
        elements.add(text("Project folders: " + config.projectsDirectories()));
        elements.add(text("Projects discovered: " + config.projectScanRoots().size() + " -> " + projectDetectionCount() + " detections"));
        elements.add(spacer());
        elements.add(text("Navigation: 1 Cursor, 2 Claude, 3 Codex, U user, P projects, Esc quit").dim());
        return elements.toArray(Element[]::new);
    }

    private Element[] harnessView(Harness harness) {
        List<Finding> findings = userFindingsForHarness(harness);
        List<Element> elements = header(harness.displayName() + " Discovery");
        Path userRoot = config.userRoots().get(harness);
        elements.add(text("User path: " + userRoot));
        elements.add(text("User detections: " + count(harness, Scope.USER)));
        elements.add(spacer());
        addCounts(elements, findings);
        addPagedFindingMenu(elements, findings);
        return elements.toArray(Element[]::new);
    }

    private Element[] userView() {
        List<Finding> findings = sorted(snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.USER)
            .toList());
        List<Element> elements = header("User Level Discovery");
        for (Harness harness : Harness.values()) {
            elements.add(text(harness.displayName() + " path: " + config.userRoots().get(harness)));
            elements.add(text("  detections: " + count(harness, Scope.USER)));
        }
        addPagedFindings(elements, findings);
        return elements.toArray(Element[]::new);
    }

    private Element[] projectsView() {
        List<Element> elements = header("Project Discovery");
        elements.add(text("Project folders: " + config.projectsDirectories()));
        List<ProjectDetection> projectDetections = projectDetections();
        elements.add(text("Projects discovered: " + config.projectScanRoots().size() + " -> " + projectDetections.size() + " with detections"));
        elements.add(spacer());
        addPagedProjects(elements, projectDetections);
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
        elements.add(text("B/Esc back, D delete, Y confirm delete, H overview, Q quit").dim());
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
        elements.add(text("B/Esc back, 1-9 open file, N/PageDown next, R/PageUp previous, D delete whole skill, Q quit").dim());
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
        elements.add(text("B/Esc back, D delete whole skill, Y confirm delete, H overview, Q quit").dim());
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
        elements.add(text("B/Esc back, H overview, N/PageDown next, R/PageUp previous, Q quit").dim());
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
        elements.add(text("MCPs: " + counts.get(AssetType.MCP)));
        elements.add(text("Config files: " + counts.get(AssetType.CONFIG)));
    }

    private void addPagedFindings(List<Element> elements, List<Finding> findings) {
        List<String> lines = sorted(findings).stream()
            .map(this::formatFinding)
            .toList();
        addPagedLines(elements, lines);
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

    private void addPagedLines(List<Element> elements, List<String> lines) {
        elements.add(spacer());
        elements.add(text("Details (" + lines.size() + ")").bold());
        if (lines.isEmpty()) {
            elements.add(text("- none"));
            return;
        }
        int maxPage = maxPage(lines.size());
        page = Math.min(page, maxPage);
        int start = page * PAGE_SIZE;
        int end = Math.min(lines.size(), start + PAGE_SIZE);
        elements.add(text("Page " + (page + 1) + "/" + (maxPage + 1)));
        for (String line : lines.subList(start, end)) {
            elements.add(text("- " + line));
        }
    }

    private void addFooterMessage(List<Element> elements) {
        actionMessage.ifPresent(message -> {
            elements.add(spacer());
            elements.add(text(message).dim());
        });
    }

    private String formatFinding(Finding finding) {
        return finding.harness().displayName()
            + " "
            + finding.scope().name().toLowerCase(Locale.ROOT)
            + " "
            + finding.assetType().name().toLowerCase(Locale.ROOT)
            + " "
            + finding.name()
            + " -> "
            + finding.path();
    }

    private List<Finding> userFindingsForHarness(Harness harness) {
        return snapshot.findings().stream()
            .filter(finding -> finding.harness() == harness)
            .filter(finding -> finding.scope() == Scope.USER)
            .toList();
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
            case CURSOR -> userFindingsForHarness(Harness.CURSOR);
            case CLAUDE -> userFindingsForHarness(Harness.CLAUDE);
            case CODEX -> userFindingsForHarness(Harness.CODEX);
            case USER -> sorted(snapshot.findings().stream().filter(finding -> finding.scope() == Scope.USER).toList());
            case PROJECT_DETAIL -> selectedProject.map(this::projectFindings).orElse(List.of());
            case OVERVIEW, PROJECTS, FINDING_DETAIL, SKILL_FILE_DETAIL -> List.of();
        };
    }

    private boolean isHarnessView(View currentView) {
        return currentView == View.CURSOR || currentView == View.CLAUDE || currentView == View.CODEX;
    }

    private List<ProjectDetection> projectDetections() {
        return config.projectScanRoots().stream()
            .map(projectRoot -> new ProjectDetection(projectRoot, countProjectFindings(projectRoot)))
            .filter(project -> project.detections() > 0)
            .toList();
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

    private long projectDetectionCount() {
        return snapshot.findings().stream()
            .filter(finding -> finding.scope() == Scope.PROJECT)
            .count();
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
            actionMessage = Optional.of("Deleted: " + target);
        } catch (IOException | RuntimeException e) {
            actionMessage = Optional.of("Delete failed: " + e.getMessage());
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
            case CURSOR -> userFindingsForHarness(Harness.CURSOR).size();
            case CLAUDE -> userFindingsForHarness(Harness.CLAUDE).size();
            case CODEX -> userFindingsForHarness(Harness.CODEX).size();
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
            || isHarnessView(view)
            ? PROJECT_PAGE_SIZE
            : PAGE_SIZE;
        return maxPage(size, pageSize);
    }

    private int maxPage(int size) {
        return maxPage(size, PAGE_SIZE);
    }

    private int maxPage(int size, int pageSize) {
        return Math.max(0, (size - 1) / pageSize);
    }

    private enum View {
        OVERVIEW,
        CURSOR,
        CLAUDE,
        CODEX,
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

    public record RefreshResult(Snapshot snapshot, List<ChangeEvent> changes, Optional<Path> reportPath) {
        public RefreshResult {
            changes = List.copyOf(changes);
            reportPath = reportPath == null ? Optional.empty() : reportPath;
        }
    }

    private record ProjectDetection(Path path, int detections) {
    }
}
