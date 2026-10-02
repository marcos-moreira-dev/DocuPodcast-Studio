package com.marcosmoreiradev.docupodcaststudio.architecture;

import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Enforces construction through the official GUI catalog, including generic controls. */
final class GuiComponentUsagePolicyTest {
    private static final String PRESENTATION_PACKAGE =
            "com.marcosmoreiradev.docupodcaststudio.presentation";
    private static final Path PRESENTATION = Path.of("src/main/java", PRESENTATION_PACKAGE.replace('.', '/'));
    private static final String JAVAFX_CONTROL = "javafx.scene.control.";
    private static final Set<String> PRODUCT_CONTROL_OWNERS = Set.of(
            "Button", "ToggleButton", "CheckBox", "RadioButton", "ComboBox", "ColorPicker",
            "TextField", "TextArea", "Spinner", "Slider", "ListView", "TreeView", "TableView",
            "TabPane", "Tab", "MenuBar", "ContextMenu", "ProgressBar", "ProgressIndicator",
            "ScrollPane", "SplitPane", "Accordion", "TitledPane", "Dialog");
    private static final Set<String> NATIVE_EXCEPTION_OWNERS = Set.of(
            "Alert", "ButtonType", "FileChooser", "DirectoryChooser");
    private static final Pattern RAW_CONSTRUCTOR = Pattern.compile(
            "\\bnew\\s+(?:javafx\\.(?:scene\\.control|stage)\\.)?(" +
                    Stream.concat(PRODUCT_CONTROL_OWNERS.stream(), NATIVE_EXCEPTION_OWNERS.stream())
                            .sorted(Comparator.comparingInt(String::length).reversed())
                            .collect(Collectors.joining("|")) +
                    ")(?:\\s*<[^;(){}]*>)?\\s*\\(");
    private static final Pattern OFFICIAL_FACTORY = Pattern.compile(
            "\\b(ActionButtonFactory|StudioFormControls|StudioCollectionControls|StudioNavigationControls|" +
                    "StudioFeedbackControls|StudioViewportControls|StudioAccordion|StudioDialogShell|" +
                    "NativeDialogResponse|NativeSourceChooser)\\.([A-Za-z0-9_]+)\\s*\\(");

    @Test
    void directJavaFxConstructorsExistOnlyInsideTheApprovedFactories() {
        List<String> violations = new ClassFileImporter().importPackages(PRESENTATION_PACKAGE).stream()
                .flatMap(type -> type.getConstructorCallsFromSelf().stream())
                .filter(this::isAuditedConstructor)
                // A custom component must invoke its JavaFX superclass constructor; this is
                // inheritance initialization, not product code constructing a peer control.
                .filter(call -> !call.getOriginOwner().isAssignableTo(call.getTargetOwner().getName()))
                .filter(call -> !isAllowedBoundary(call))
                .map(call -> call.getOriginOwner().getName() + ":" + call.getLineNumber()
                        + " constructs " + call.getTargetOwner().getName())
                .sorted()
                .toList();
        assertTrue(violations.isEmpty(), () -> "Direct JavaFX control constructors outside catalog boundaries:\n"
                + String.join("\n", violations));
    }

    @Test
    void sourceAuditHasZeroProductDebtAndWritesTheUsageReport() throws IOException {
        List<Usage> raw = new ArrayList<>();
        List<Usage> official = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(PRESENTATION)) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file);
                collect(raw, file, text, RAW_CONSTRUCTOR, true);
                collect(official, file, text, OFFICIAL_FACTORY, false);
            }
        }
        List<Usage> violations = raw.stream().filter(usage -> !isAllowedBoundary(usage)).toList();
        writeReport(raw, official, violations);
        assertTrue(violations.isEmpty(), () -> "GUI constructor debt remains:\n" + violations);
    }

    private boolean isAuditedConstructor(JavaConstructorCall call) {
        String owner = call.getTargetOwner().getName();
        String simple = call.getTargetOwner().getSimpleName();
        return (owner.startsWith(JAVAFX_CONTROL) && PRODUCT_CONTROL_OWNERS.contains(simple))
                || (owner.startsWith(JAVAFX_CONTROL) && Set.of("Alert", "ButtonType").contains(simple))
                || (owner.startsWith("javafx.stage.") && Set.of("FileChooser", "DirectoryChooser").contains(simple));
    }

    private boolean isAllowedBoundary(JavaConstructorCall call) {
        return allowedOrigin(call.getOriginOwner().getName(), call.getTargetOwner().getSimpleName());
    }

    private static boolean isAllowedBoundary(Usage usage) {
        if (usage.file().contains("/presentation/components/")) return true;
        return switch (usage.control()) {
            case "Alert", "ButtonType" -> usage.file().endsWith("/presentation/dialogs/NativeDialogResponse.java");
            case "FileChooser", "DirectoryChooser" -> usage.file().endsWith("/presentation/dialogs/NativeSourceChooser.java");
            default -> false;
        };
    }

    private static boolean allowedOrigin(String origin, String targetSimpleName) {
        if (origin.contains(".presentation.components.")) return true;
        return switch (targetSimpleName) {
            case "Alert", "ButtonType" -> origin.endsWith(".presentation.dialogs.NativeDialogResponse");
            case "FileChooser", "DirectoryChooser" -> origin.endsWith(".presentation.dialogs.NativeSourceChooser");
            default -> false;
        };
    }

    private static void collect(List<Usage> target, Path file, String text, Pattern pattern, boolean raw) {
        pattern.matcher(text).results().forEach(match -> target.add(new Usage(portable(file),
                1 + (int) text.substring(0, match.start()).chars().filter(character -> character == '\n').count(),
                match.group(1), raw ? "constructor" : match.group(2))));
    }

    private static void writeReport(List<Usage> raw, List<Usage> official, List<Usage> violations) throws IOException {
        Path report = Path.of("target/reports/gui-usage/gui-component-usage.md");
        Files.createDirectories(report.getParent());
        Map<String, Long> byFactory = official.stream().collect(Collectors.groupingBy(
                usage -> usage.control() + "." + usage.operation(), TreeMap::new, Collectors.counting()));
        Map<String, Long> bySurface = official.stream().collect(Collectors.groupingBy(
                usage -> surface(usage.file()), TreeMap::new, Collectors.counting()));
        long nativeExceptions = official.stream().filter(usage ->
                usage.control().equals("NativeDialogResponse") || usage.control().equals("NativeSourceChooser")).count();
        StringBuilder out = new StringBuilder("# GUI component usage audit\n\n")
                .append("The visual catalog is the only product-control construction path. ")
                .append("Native ButtonBar responses and operating-system choosers are explicit exceptions.\n\n")
                .append("- official factory calls: ").append(official.size()).append('\n')
                .append("- native constructor exceptions: ").append(nativeExceptions).append('\n')
                .append("- controls without construction contract: ").append(violations.size()).append("\n\n")
                .append("## By surface\n\n");
        bySurface.forEach((surface, count) -> out.append("- ").append(surface).append(": ").append(count).append('\n'));
        out.append("\n## By factory and variant entrypoint\n\n");
        byFactory.forEach((factory, count) -> out.append("- ").append(factory).append(": ").append(count).append('\n'));
        out.append("\n## Violations\n\n");
        if (violations.isEmpty()) out.append("None.\n");
        else violations.forEach(usage -> out.append("- ").append(usage).append('\n'));
        Files.writeString(report, out);
    }

    private static String surface(String file) {
        String marker = "/presentation/";
        int start = file.indexOf(marker);
        if (start < 0) return "presentation";
        String tail = file.substring(start + marker.length());
        int slash = tail.indexOf('/');
        return slash < 0 ? "presentation" : tail.substring(0, slash);
    }

    private static String portable(Path path) { return path.toString().replace('\\', '/'); }
    private record Usage(String file, int line, String control, String operation) { }
}
