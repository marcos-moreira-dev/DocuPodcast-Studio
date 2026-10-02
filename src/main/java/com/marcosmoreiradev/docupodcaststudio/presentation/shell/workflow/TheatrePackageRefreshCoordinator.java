package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.PreparedTheatreRefresh;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.RefreshTheatrePackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreImportStateRepository;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreRefreshDiagnostic;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreRefreshPreflightReport;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreRefreshProgress;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreRefreshResult;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeSourceChooser;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDialogResponse;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.TheatreRefreshProgressDialog;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Bridges the pure theatre package workflow to the shared JavaFX command/progress/dialog infrastructure. */
public final class TheatrePackageRefreshCoordinator {
    private static final ButtonType APPLY = NativeDialogResponse.button(
            "Aplicar cambios", ButtonBar.ButtonData.OK_DONE);

    private final DocuPodcastShellViewModel viewModel;
    private final RefreshTheatrePackageUseCase useCase;
    private final TheatreImportStateRepository stateRepository;
    private final FxBackgroundTaskRunner tasks;
    private final ExceptionAlertPresenter alerts;
    private final Supplier<Window> owner;

    public TheatrePackageRefreshCoordinator(DocuPodcastShellViewModel viewModel,
                                             RefreshTheatrePackageUseCase useCase,
                                             TheatreImportStateRepository stateRepository,
                                             FxBackgroundTaskRunner tasks,
                                             ExceptionAlertPresenter alerts,
                                             Supplier<Window> owner) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.useCase = Objects.requireNonNull(useCase, "useCase");
        this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository");
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public void start() {
        try {
            if (viewModel.currentProjectModeProperty().get() != ProjectMode.THEATRE_PRODUCTION) return;
            Path projectFile = viewModel.currentProjectFile()
                    .orElseThrow(() -> new IllegalStateException("Guarda el proyecto antes de vincular una carpeta teatral."));
            if (viewModel.hasUnsavedChanges()) {
                throw new IllegalStateException("Guarda los cambios pendientes antes de refrescar la obra.");
            }
            Path sourceRoot = linkedSource(projectFile).orElseGet(this::chooseSource);
            if (sourceRoot == null) return;
            if (!viewModel.beginTheatreRefresh()) return;
            if (isOfficialV2(sourceRoot)) {
                startOfficialImport(viewModel.currentProject().orElseThrow(), projectFile, sourceRoot);
                return;
            }
            startPrepare(viewModel.currentProject().orElseThrow(), projectFile, sourceRoot);
        } catch (Exception failure) {
            alerts.showFailure("No se pudo iniciar la actualización de la obra", failure, owner.get());
        }
    }

    private void startOfficialImport(com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject project,
                                     Path projectFile, Path source) {
        TheatreRefreshProgressDialog dialog = new TheatreRefreshProgressDialog(owner.get(), false);
        Task<com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.ImportOfficialTheatrePackageUseCase.Result> task = new Task<>() {
            @Override protected com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.ImportOfficialTheatrePackageUseCase.Result call() throws Exception {
                updateMessage("Validando gramática, manifiesto y grafo teatral…"); updateProgress(-1, 1);
                Path root = projectFile.toAbsolutePath().normalize().getParent();
                return new com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.ImportOfficialTheatrePackageUseCase()
                        .execute(project, source, root, null);
            }
        };
        dialog.bind(task);
        task.setOnSucceeded(event -> {
            dialog.close();
            if (viewModel.currentProject().orElse(null) != project || !viewModel.currentProjectFile().filter(projectFile::equals).isPresent()) {
                viewModel.finishTheatreRefresh("La importación no se aplicó porque cambió el proyecto activo.");
                return;
            }
            var result = task.getValue();
            viewModel.applyTheatrePackageImport(result);
            viewModel.finishTheatreRefresh("Paquete theatre-v2 importado; guarda el proyecto para confirmar.");
            alerts.show(UserNotification.success("Paquete teatral importado", "Parlamentos y configuración incorporados. Guarda el proyecto para confirmar."
                    + (result.warnings().isEmpty() ? "" : "\n" + String.join("\n", result.warnings()))), owner.get());
        });
        task.setOnFailed(event -> fail(dialog, "No se pudo importar el paquete theatre-v2", task.getException()));
        task.setOnCancelled(event -> cancelled(dialog)); dialog.show(); tasks.start("theatre-v2-import", task);
    }

    private void startPrepare(com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject project,
                              Path projectFile, Path sourceRoot) {
        TheatreRefreshProgressDialog progressDialog = new TheatreRefreshProgressDialog(owner.get());
        Task<PreparedTheatreRefresh> task = new Task<>() {
            @Override protected PreparedTheatreRefresh call() throws Exception {
                return useCase.prepare(project, projectFile, sourceRoot,
                        this::publish, this::isCancelled);
            }
            private void publish(TheatreRefreshProgress value) {
                updateMessage(value.message());
                javafx.application.Platform.runLater(() -> viewModel.updateStatusMessage(value.message()));
                if (value.fraction() < 0) updateProgress(-1, 1);
                else updateProgress(value.completed(), Math.max(1, value.total()));
            }
        };
        progressDialog.bind(task);
        task.setOnSucceeded(event -> {
            progressDialog.close();
            handlePrepared(task.getValue());
        });
        task.setOnFailed(event -> fail(progressDialog, "No se pudo analizar la carpeta teatral", task.getException()));
        task.setOnCancelled(event -> cancelled(progressDialog));
        progressDialog.show();
        tasks.start("theatre-package-preflight", task);
    }

    private void handlePrepared(PreparedTheatreRefresh prepared) {
        TheatreRefreshPreflightReport report = prepared.preflight();
        if (!report.canCommit()) {
            viewModel.finishTheatreRefresh("La obra no se modificó: el preflight encontró errores.");
            StudioMessageDialog.create(owner.get(), Alert.AlertType.ERROR, "Actualizar obra teatral",
                    "No se pueden aplicar los cambios", summary(report), details(report), ButtonType.CLOSE).show();
            return;
        }
        if (report.plan().isNoOp() && prepared.previousState() != null) {
            viewModel.finishTheatreRefresh("La obra ya está actualizada.");
            alerts.show(UserNotification.success("Obra al día",
                    "No se encontraron cambios efectivos en la carpeta vinculada."), owner.get());
            return;
        }
        Alert confirmation = StudioMessageDialog.create(owner.get(), Alert.AlertType.CONFIRMATION,
                "Actualizar obra teatral", "Revisa el plan antes de aplicarlo",
                summary(report) + "\n\nLos archivos ausentes se conservarán; no se borrará trabajo materializado.",
                details(report), APPLY, ButtonType.CANCEL);
        Optional<ButtonType> decision = confirmation.showAndWait();
        if (decision.isEmpty() || decision.get() != APPLY) {
            viewModel.finishTheatreRefresh("Actualización cancelada antes de modificar el proyecto.");
            return;
        }
        startCommit(prepared);
    }

    private void startCommit(PreparedTheatreRefresh prepared) {
        TheatreRefreshProgressDialog progressDialog = new TheatreRefreshProgressDialog(owner.get(), false);
        Task<TheatreRefreshResult> task = new Task<>() {
            @Override protected TheatreRefreshResult call() throws Exception {
                return useCase.commit(prepared, this::publish, this::isCancelled);
            }
            private void publish(TheatreRefreshProgress value) {
                updateMessage(value.message());
                javafx.application.Platform.runLater(() -> viewModel.updateStatusMessage(value.message()));
                if (value.fraction() < 0) updateProgress(-1, 1);
                else updateProgress(value.completed(), Math.max(1, value.total()));
            }
        };
        progressDialog.bind(task);
        task.setOnSucceeded(event -> {
            progressDialog.close();
            TheatreRefreshResult result = task.getValue();
            viewModel.applyPersistedTheatreRefresh(result.project(), result.message());
            alerts.show(UserNotification.success("Obra actualizada", result.message()), owner.get());
        });
        task.setOnFailed(event -> fail(progressDialog, "No se pudo actualizar la obra", task.getException()));
        task.setOnCancelled(event -> cancelled(progressDialog));
        progressDialog.show();
        tasks.start("theatre-package-commit", task);
    }

    private void fail(TheatreRefreshProgressDialog dialog, String headline, Throwable failure) {
        dialog.close();
        viewModel.finishTheatreRefresh("La obra no se modificó.");
        alerts.showFailure(headline, failure, owner.get());
    }

    private void cancelled(TheatreRefreshProgressDialog dialog) {
        dialog.close();
        viewModel.finishTheatreRefresh("Actualización cancelada de forma segura.");
    }

    private Optional<Path> linkedSource(Path projectFile) throws Exception {
        Path projectRoot = projectFile.toAbsolutePath().normalize().getParent();
        if (projectRoot == null) return Optional.empty();
        return stateRepository.open(projectRoot).flatMap(state -> {
            try {
                Path candidate = Path.of(state.sourceRoot()).toAbsolutePath().normalize();
                return Files.isDirectory(candidate) ? Optional.of(candidate) : Optional.empty();
            } catch (InvalidPathException ignored) {
                return Optional.empty();
            }
        });
    }

    private Path chooseSource() {
        ButtonType folder = NativeDialogResponse.button("Carpeta", ButtonBar.ButtonData.LEFT);
        ButtonType zip = NativeDialogResponse.button("ZIP theatre-v2", ButtonBar.ButtonData.RIGHT);
        var decision = StudioMessageDialog.create(owner.get(), Alert.AlertType.CONFIRMATION,
                "Importar obra teatral", "Selecciona el formato de entrada",
                "Carpeta y ZIP theatre-v2 usan el mismo pipeline determinístico.", "", folder, zip, ButtonType.CANCEL).showAndWait();
        if (decision.isEmpty() || decision.get() == ButtonType.CANCEL) return null;
        if (decision.get() == zip) {
            FileChooser chooser = new FileChooser(); chooser.setTitle("Importar paquete teatral ZIP");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Paquete teatral ZIP", "*.zip"));
            var selected = chooser.showOpenDialog(owner.get());
            return selected == null ? null : selected.toPath().toAbsolutePath().normalize();
        }
        DirectoryChooser chooser = NativeSourceChooser.directoryChooser(); chooser.setTitle("Vincular carpeta de obra teatral");
        var selected = chooser.showDialog(owner.get()); return selected == null ? null : selected.toPath().toAbsolutePath().normalize();
    }

    private static boolean isOfficialV2(Path source) {
        if (source.getFileName().toString().toLowerCase().endsWith(".zip")) return true;
        Path manifest = source.resolve("docupodcast-theatre.json");
        try { return Files.isRegularFile(manifest) && Files.readString(manifest).matches("(?s).*\\\"schemaVersion\\\"\\s*:\\s*2.*"); }
        catch (Exception ignored) { return false; }
    }

    private static String summary(TheatreRefreshPreflightReport report) {
        return "Nuevos: " + report.newAssets()
                + " · Modificados: " + report.modifiedAssets()
                + " · Renombrados: " + report.plan().count(TheatrePackageDeltaStatus.RENAMED)
                + " · Sin cambios: " + report.unchangedAssets()
                + " · Ausentes conservados: " + report.retainedMissingAssets()
                + " · Advertencias: " + report.warnings()
                + " · Errores: " + report.errors() + ".";
    }

    private static String details(TheatreRefreshPreflightReport report) {
        StringBuilder result = new StringBuilder();
        report.plan().deltas().forEach(delta -> result.append(delta.status()).append(" · ")
                .append(delta.logicalId()).append(" · ").append(delta.reason()).append('\n'));
        for (TheatreRefreshDiagnostic diagnostic : report.diagnostics()) {
            result.append(diagnostic.severity()).append(" · ").append(diagnostic.code()).append(" · ")
                    .append(diagnostic.logicalId()).append(" · ").append(diagnostic.message()).append('\n');
        }
        return result.toString().strip();
    }
}
