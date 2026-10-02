package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Source-level final checklist for the prudent migration described by the root Word document. */
public final class RunMigrationClosureChecklistUseCase {
    public ArchitectureGuardrailReport run(Path repositoryRoot) throws IOException {
        Path root = repositoryRoot == null ? Path.of("").toAbsolutePath().normalize() : repositoryRoot.toAbsolutePath().normalize();
        String viewModel = read(root, "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String registry = read(root, "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String ribbon = read(root, "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String projectMode = read(root, "src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/ProjectMode.java");
        String fragmentId = read(root, "src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/fragment/FragmentId.java");
        String exportCenter = read(root, "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterCoordinator.java");

        int lines = viewModel.isBlank() ? 0 : viewModel.split("\\R", -1).length;
        boolean storyboardHidden = registry.contains("hidden(AppCommandId.OPEN_STORYBOARD")
                && !ribbon.contains("cmd(AppCommandId.OPEN_STORYBOARD");
        boolean audioJobsHidden = registry.contains("hidden(AppCommandId.OPEN_AUDIO_JOBS")
                && !ribbon.contains("cmd(AppCommandId.OPEN_AUDIO_JOBS");
        boolean modes = projectMode.contains("DOCUMENTARY_STUDIO")
                && projectMode.contains("NARRATIVE_VIDEO")
                && projectMode.contains("THEATRE_PRODUCTION");
        boolean noBanned = bannedTermsAbsent(root);
        ArrayList<String> issues = new ArrayList<>();
        if (lines > 2600) {
            issues.add("DocuPodcastShellViewModel supera 2600 lineas: " + lines);
        }
        if (!storyboardHidden) {
            issues.add("OPEN_STORYBOARD reaparece como comando visible o ribbon.");
        }
        if (!audioJobsHidden) {
            issues.add("OPEN_AUDIO_JOBS reaparece como comando visible o ribbon.");
        }
        if (!modes) {
            issues.add("No se detectan los tres modos oficiales de proyecto.");
        }
        if (fragmentId.isBlank()) {
            issues.add("No se detecta FragmentId como ancla canonica.");
        }
        if (exportCenter.isBlank()) {
            issues.add("No se detecta Centro de exportaciones.");
        }
        if (!noBanned) {
            issues.add("Se detecto alcance excluido en codigo principal.");
        }
        return new ArchitectureGuardrailReport(
                lines,
                lines <= 2600,
                storyboardHidden,
                audioJobsHidden,
                modes,
                !fragmentId.isBlank(),
                !exportCenter.isBlank(),
                noBanned,
                checklistItems(),
                issues);
    }

    private static List<String> checklistItems() {
        return List.of(
                "Abrir, guardar y cerrar proyecto existente.",
                "Crear Estudio documental, Video narrativo y Produccion teatral.",
                "Importar fuente, preparar lectura y reproducir seleccion.",
                "Revisar voces, audio por fragmento y referencias no disponibles.",
                "Revisar imagen principal, puente y visuales teatrales por FragmentId.",
                "Abrir Centro de exportaciones y revisar readiness/jobs relacionados.",
                "Validar soporte avanzado sin exponer workspaces internos.");
    }

    private static boolean bannedTermsAbsent(Path root) throws IOException {
        Path main = root.resolve("src/main/java").normalize();
        if (!Files.isDirectory(main)) {
            return true;
        }
        try (var stream = Files.walk(main)) {
            for (Path file : stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                String lower = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                if (lower.contains("youtube")
                        || lower.contains("whisper")
                        || lower.contains("stt/")
                        || lower.contains("blender fuerte")
                        || lower.contains("timeline manual avanzado")) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String read(Path root, String relative) throws IOException {
        Path file = root.resolve(relative).normalize();
        return Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
    }
}
