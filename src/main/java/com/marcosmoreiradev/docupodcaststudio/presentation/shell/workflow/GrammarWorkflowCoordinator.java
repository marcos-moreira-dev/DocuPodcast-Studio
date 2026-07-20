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
                ReadableDocument document = viewModel.importAndClassifySourceDocument(sourceFile);
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
                ReadableDocument document = viewModel.importAndClassifySourceDocument(sourceFile);
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
            viewModel.attachImportedDocument(bundle.document());
            applyTheatreGrammarPlan(bundle.plan());
            viewModel.buildNarrationScriptFromDocument();
            promptSaveProjectForImportedSourceIfNeeded.accept(sourceFile);
            GrammarImportReport report = materializeTheatre(sourceFile, bundle.plan(), bundle.report());
            viewModel.showTheatreScriptWorkspace(theatreGrammarImportMessage(bundle.plan(), report));
        } catch (IOException | RuntimeException ex) {
            showError("No se pudo importar la gramatica teatral", ex);
        }
    }

    private void completeNarrativeImport(Path sourceFile, NarrativeVideoGrammarImportBundle bundle,
                                         Consumer<Path> promptSaveProjectForImportedSourceIfNeeded) {
        try {
            viewModel.attachImportedDocument(bundle.document());
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

    private void applyTheatreGrammarPlan(ImportPlan plan) {
        for (ProfilePlan character : plan.characters()) {
            viewModel.saveTheatreCharacterDescription("", character.name(), character.notes());
        }
        for (ProfilePlan object : plan.objects()) {
            viewModel.saveTheatreObjectDescription("", object.name(), object.notes());
        }
        for (ActPlan act : plan.acts()) {
            String actId = ensureTheatreAct(act.name(), act.notes());
            for (ScenePlan scene : act.scenes()) {
                ensureTheatreScene(actId, scene.name(), scene.notes());
            }
        }
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
                : " Links visuales detectados: " + plan.mediaLinks().size() + " (quedan como referencias en esta importacion).";
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
