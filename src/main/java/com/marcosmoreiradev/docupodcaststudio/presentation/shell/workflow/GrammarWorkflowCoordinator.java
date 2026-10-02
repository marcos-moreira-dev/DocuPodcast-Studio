package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.grammar.GrammarDiagnostic;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.GrammarImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ImportProjectGrammarMarkdownUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ProjectGrammarKind;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVideoImportPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ActPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ProfilePlan;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ScenePlan;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImportUseCase;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.DocumentImportProgressDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Presentation workflow for official Markdown grammar import/export. */
public final class GrammarWorkflowCoordinator {
    private final DocuPodcastShellViewModel viewModel;
    private final FxBackgroundTaskRunner backgroundTaskRunner;
    private final ExceptionAlertPresenter alertPresenter;
    private final Supplier<Window> ownerSupplier;

    public GrammarWorkflowCoordinator(
            DocuPodcastShellViewModel viewModel,
            FxBackgroundTaskRunner backgroundTaskRunner,
            ExceptionAlertPresenter alertPresenter,
            Supplier<Window> ownerSupplier) {
        this.viewModel = java.util.Objects.requireNonNull(viewModel, "viewModel");
        this.backgroundTaskRunner = java.util.Objects.requireNonNull(backgroundTaskRunner, "backgroundTaskRunner");
        this.alertPresenter = java.util.Objects.requireNonNull(alertPresenter, "alertPresenter");
        this.ownerSupplier = java.util.Objects.requireNonNull(ownerSupplier, "ownerSupplier");
    }

    public void exportTemplate(ProjectGrammarKind kind, Path target) {
        Path resolvedTarget = ensureMarkdownExtension(target);
        try {
            String markdown = viewModel.projectWorkspace().grammar().buildGrammarTemplate().templateFor(kind).markdown();
            Files.writeString(resolvedTarget, markdown, StandardCharsets.UTF_8);
            viewModel.updateStatusMessage("Plantilla " + kind.displayName().toLowerCase(java.util.Locale.ROOT)
                    + " exportada: " + resolvedTarget.getFileName() + ".");
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo exportar la plantilla", ex);
        }
    }

    public void importTheatreGrammar(Path sourceFile, Consumer<Path> promptSaveProjectForImportedSourceIfNeeded) {
        DocumentImportProgressDialog progress = new DocumentImportProgressDialog(owner(), sourceFile.getFileName().toString());
        Task<TheatreGrammarImportBundle> task = new Task<>() {
            @Override
            protected TheatreGrammarImportBundle call() throws Exception {
                updateMessage("Leyendo gramatica teatral Markdown...");
                ImportProjectGrammarMarkdownUseCase.TheatreParseResult parsed =
                        viewModel.projectWorkspace().grammar().importProjectGrammarMarkdown().parseTheatre(sourceFile);
                updateMessage("Importando obra como documento teatral...");
                ReadableDocument document = new com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarDocumentBuilder()
                        .build(parsed.plan(), sourceFile);
                return new TheatreGrammarImportBundle(document, parsed.plan(), parsed.report());
            }
        };
        progress.bind(task);
        task.setOnSucceeded(event -> {
            progress.close();
            PauseTransition closePulse = new PauseTransition(Duration.millis(80));
            closePulse.setOnFinished(closeEvent -> completeTheatreImport(sourceFile, task.getValue(), promptSaveProjectForImportedSourceIfNeeded));
            closePulse.play();
        });
        task.setOnFailed(event -> {
            progress.close();
            Throwable ex = task.getException();
            showError("No se pudo importar la gramatica teatral", ex instanceof Exception exception ? exception : new RuntimeException(ex));
        });
        progress.show();
        backgroundTaskRunner.start("docupodcast-theatre-grammar-import", task);
    }

    public void importNarrativeVideoGrammar(Path sourceFile, Consumer<Path> promptSaveProjectForImportedSourceIfNeeded) {
        DocumentImportProgressDialog progress = new DocumentImportProgressDialog(owner(), sourceFile.getFileName().toString());
        Task<NarrativeVideoGrammarImportBundle> task = new Task<>() {
            @Override
            protected NarrativeVideoGrammarImportBundle call() throws Exception {
                updateMessage("Leyendo gramatica narrativa Markdown...");
                ImportProjectGrammarMarkdownUseCase.NarrativeParseResult parsed =
                        viewModel.projectWorkspace().grammar().importProjectGrammarMarkdown().parseNarrative(sourceFile);
                updateMessage("Importando guion narrativo como documento...");
                ReadableDocument document = viewModel.importAndClassifyBlockSourceDocument(sourceFile);
                return new NarrativeVideoGrammarImportBundle(document, parsed.plan(), parsed.report());
            }
        };
        progress.bind(task);
        task.setOnSucceeded(event -> {
            progress.close();
            PauseTransition closePulse = new PauseTransition(Duration.millis(80));
            closePulse.setOnFinished(closeEvent -> completeNarrativeImport(sourceFile, task.getValue(), promptSaveProjectForImportedSourceIfNeeded));
            closePulse.play();
        });
        task.setOnFailed(event -> {
            progress.close();
            Throwable ex = task.getException();
            showError("No se pudo importar la gramatica narrativa", ex instanceof Exception exception ? exception : new RuntimeException(ex));
        });
        progress.show();
        backgroundTaskRunner.start("docupodcast-narrative-grammar-import", task);
    }

    private void completeTheatreImport(Path sourceFile, TheatreGrammarImportBundle bundle,
                                       Consumer<Path> promptSaveProjectForImportedSourceIfNeeded) {
        try {
            viewModel.attachImportedDocument(
                    new com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource(
                            bundle.document()));
            viewModel.buildNarrationScriptFromDocument();
            promptSaveProjectForImportedSourceIfNeeded.accept(sourceFile);
            if (viewModel.currentProjectFile().isEmpty()) {
                applyTheatreGrammarPlan(bundle.plan(), java.util.Map.of());
                viewModel.showTheatreScriptWorkspace("Importación pendiente: guarda y vuelve a importar la gramática para incorporar sus imágenes.");
                return;
            }
            importTheatreAssets(sourceFile, bundle);
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo importar la gramatica teatral", ex);
        }
    }

    private void importTheatreAssets(Path sourceFile, TheatreGrammarImportBundle bundle) {
        DocuPodcastProject original = viewModel.currentProject().orElseThrow();
        Path projectFile = viewModel.currentProjectFile().orElseThrow();
        var imageImporter = viewModel.generationWorkspace().storyboard().importImageAsset();
        DocumentImportProgressDialog progress = new DocumentImportProgressDialog(owner(), sourceFile.getFileName().toString());
        Task<TheatreAssetsBundle> task = new Task<>() {
            @Override protected TheatreAssetsBundle call() throws Exception {
                DocuPodcastProject[] project = {original};
                var assets = new com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarAssetImporter()
                        .importAssets(sourceFile, bundle.plan(), path -> {
                            updateMessage("Incorporando recurso teatral: " + path.getFileName());
                            var imported = imageImporter.importImage(project[0], projectFile, path);
                            project[0] = imported.project();
                            return imported.imageAsset().id();
                        });
                return new TheatreAssetsBundle(project[0], assets);
            }
        };
        progress.bind(task);
        task.setOnSucceeded(event -> {
            progress.close();
            if (viewModel.currentProject().orElse(null) != original
                    || !viewModel.currentProjectFile().filter(projectFile::equals).isPresent()) {
                showError("La obra cambió durante la importación", new IllegalStateException(
                        "Vuelve a importar la gramática en el proyecto que deseas configurar."));
                return;
            }
            try {
                var result = task.getValue();
                applyTheatreGrammarPlan(bundle.plan(), result.assets().assetIds(), result.project());
                viewModel.saveCurrentProject();
                GrammarImportReport report = materializeTheatre(sourceFile, bundle.plan(),
                        bundle.report().withAdditionalDiagnostics(result.assets().diagnostics())
                                .withAdditionalDiagnostics(unresolvedVoiceDiagnostics(bundle.plan())));
                viewModel.showTheatreScriptWorkspace(theatreGrammarImportMessage(bundle.plan(), report));
            } catch (IOException | RuntimeException ex) {
                showError("No se pudo completar la importación teatral", ex);
            }
        });
        task.setOnFailed(event -> {
            progress.close();
            showError("No se pudieron incorporar los recursos teatrales", task.getException());
        });
        progress.show();
        backgroundTaskRunner.start("docupodcast-theatre-assets-import", task);
    }

    private record TheatreAssetsBundle(DocuPodcastProject project,
            com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarAssetImporter.Result assets) { }

    private void completeNarrativeImport(Path sourceFile, NarrativeVideoGrammarImportBundle bundle,
                                         Consumer<Path> promptSaveProjectForImportedSourceIfNeeded) {
        try {
            viewModel.attachImportedDocument(
                    new com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource(
                            bundle.document()));
            viewModel.buildNarrationScriptFromDocument();
            promptSaveProjectForImportedSourceIfNeeded.accept(sourceFile);
            GrammarImportReport report = materializeNarrative(sourceFile, bundle.plan(), bundle.report());
            viewModel.showDocumentWorkspace(narrativeVideoGrammarImportMessage(bundle.plan(), report));
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo importar la gramatica narrativa", ex);
        }
    }

    private GrammarImportReport materializeNarrative(Path sourceFile, NarrativeVideoImportPlan plan,
                                                     GrammarImportReport report) throws IOException {
        Optional<DocuPodcastProject> project = viewModel.currentProject();
        if (project.isEmpty()) {
            return report.withAdditionalDiagnostics(List.of(GrammarDiagnostic.warning(
                    "SEMANTICS_NOT_MATERIALIZED", "No hay proyecto activo para escribir project-semantics.json.")));
        }
        return viewModel.projectWorkspace().grammar().importProjectGrammarMarkdown().materializeNarrative(
                project.get(), viewModel.currentProjectFile().orElse(null), sourceFile, plan,
                viewModel.currentScriptProperty().get(), report);
    }

    private GrammarImportReport materializeTheatre(Path sourceFile, ImportPlan plan, GrammarImportReport report) throws IOException {
        Optional<DocuPodcastProject> project = viewModel.currentProject();
        if (project.isEmpty()) {
            return report.withAdditionalDiagnostics(List.of(GrammarDiagnostic.warning(
                    "SEMANTICS_NOT_MATERIALIZED", "No hay proyecto activo para escribir project-semantics.json.")));
        }
        return viewModel.projectWorkspace().grammar().importProjectGrammarMarkdown().materializeTheatre(
                project.get(), viewModel.currentProjectFile().orElse(null), sourceFile, plan,
                viewModel.currentScriptProperty().get(), report);
    }

    private void applyTheatreGrammarPlan(ImportPlan plan, java.util.Map<String, String> assetIds) {
        DocuPodcastProject original = viewModel.currentProject()
                .orElseThrow(() -> new IllegalStateException("No hay proyecto activo."));
        applyTheatreGrammarPlan(plan, assetIds, original);
    }

    private void applyTheatreGrammarPlan(ImportPlan plan, java.util.Map<String, String> assetIds, DocuPodcastProject original) {
        TheatreImportUseCase.ImportResult imported = new TheatreImportUseCase().execute(
                plan, viewModel.currentScriptProperty().get(), original.voiceLibrary(), assetIds);
        DocuPodcastProject updated = original.withTheatre(imported.layer());
        for (var boundary : imported.sceneBoundariesStart().entrySet()) {
            updated = updated.withViewState("theatre.sceneBoundary." + boundary.getKey(),
                    boundary.getValue() + "|" + imported.sceneBoundariesEnd().getOrDefault(boundary.getKey(), ""));
        }
        for (var assignment : imported.emotionAssignments()) updated = updated.withNarrativeLayerAssignment(assignment);
        for (var assignment : imported.imageAssignments()) updated = updated.withNarrativeLayerAssignment(assignment);
        viewModel.applyTheatreGrammarImport(updated,
                "Gramática " + plan.grammarVersion() + " materializada en el proyecto teatral.");
    }

    private List<GrammarDiagnostic> unresolvedVoiceDiagnostics(ImportPlan plan) {
        var library = viewModel.currentProject().orElseThrow().voiceLibrary();
        return plan.characters().stream().filter(p -> !p.voz().isBlank())
                .filter(p -> library.voiceById(TheatreImportUseCase.resolveVoiceId(p.voz(), library)).isEmpty())
                .map(p -> GrammarDiagnostic.warning("THEATRE_VOICE_UNRESOLVED",
                        "Voz no disponible para " + p.name() + ": " + p.voz())).toList();
    }

    private String ensureTheatreAct(String displayName, String notes) {
        Optional<String> existing = viewModel.theatreActs().stream()
                .filter(act -> sameName(act.displayName(), displayName))
                .map(TheatreProjectLayer.TheatreAct::id)
                .findFirst();
        if (existing.isPresent()) {
            if (notes != null && !notes.isBlank()) {
                viewModel.updateTheatreAct(existing.get(), displayName, notes);
            }
            return existing.get();
        }
        viewModel.addTheatreAct(displayName, notes);
        return viewModel.theatreActs().stream()
                .filter(act -> sameName(act.displayName(), displayName))
                .reduce((first, second) -> second)
                .map(TheatreProjectLayer.TheatreAct::id)
                .orElse("ACT-ACTO-1");
    }

    private void ensureTheatreScene(String actId, String displayName, String notes) {
        Optional<String> existing = viewModel.theatreScenes().stream()
                .filter(scene -> sameName(scene.displayName(), displayName) && sameId(scene.actId(), actId))
                .map(TheatreProjectLayer.Scene::id)
                .findFirst();
        if (existing.isPresent()) {
            if (notes != null && !notes.isBlank()) {
                viewModel.updateTheatreScene(existing.get(), displayName, notes);
            }
            return;
        }
        viewModel.addTheatreScene(actId, displayName, notes);
    }

    private static String theatreGrammarImportMessage(ImportPlan plan, GrammarImportReport report) {
        int sceneCount = plan.acts().stream().mapToInt(act -> act.scenes().size()).sum();
        String links = plan.mediaLinks().isEmpty()
                ? ""
                : " Enlaces adicionales detectados: " + plan.mediaLinks().size() + ".";
        String interventions = plan.interventions().isEmpty()
                ? ""
                : " Intervenciones: " + plan.interventions().size() + ".";
        return "Obra importada desde gramatica: " + plan.acts().size() + " actos, " + sceneCount
                + " escenas, " + plan.characters().size() + " personajes, " + plan.objects().size() + " objetos."
                + links + interventions + " " + report.humanSummary();
    }

    private static String narrativeVideoGrammarImportMessage(NarrativeVideoImportPlan plan, GrammarImportReport report) {
        long visualPrompts = plan.fragments().stream().filter(fragment -> !fragment.visualPrompt().isBlank()).count();
        long bridgePrompts = plan.fragments().stream().filter(fragment -> !fragment.bridgePrompt().isBlank()).count();
        return "Guion narrativo importado: " + plan.fragmentCount() + " fragmentos, " + visualPrompts
                + " prompts visuales y " + bridgePrompts + " puentes opcionales. " + report.humanSummary();
    }

    private static boolean sameName(String left, String right) {
        return normalizeCompare(left).equals(normalizeCompare(right));
    }

    private static boolean sameId(String left, String right) {
        String normalizedLeft = left == null || left.isBlank() ? "ACT-ACTO-1" : left.strip();
        String normalizedRight = right == null || right.isBlank() ? "ACT-ACTO-1" : right.strip();
        return normalizedLeft.equals(normalizedRight);
    }

    private static String normalizeCompare(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static Path ensureMarkdownExtension(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        if (fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".md")) {
            return path;
        }
        return path.resolveSibling(fileName + ".md");
    }

    private Window owner() {
        return ownerSupplier.get();
    }

    private void showError(String header, Throwable error) {
        alertPresenter.showFailure(header, error, owner());
    }

    private record TheatreGrammarImportBundle(
            ReadableDocument document,
            ImportPlan plan,
            GrammarImportReport report) {
    }

    private record NarrativeVideoGrammarImportBundle(
            ReadableDocument document,
            NarrativeVideoImportPlan plan,
            GrammarImportReport report) {
    }
}
