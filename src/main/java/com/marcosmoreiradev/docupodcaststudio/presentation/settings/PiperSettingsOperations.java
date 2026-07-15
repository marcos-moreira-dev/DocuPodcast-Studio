package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadPiperPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportPiperVoiceFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectPiperSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperRuntimeDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperVoiceImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.SelectPiperAsEngineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Coordinates Voz local simple/Piper settings operations without owning the Settings dialog layout. */
final class PiperSettingsOperations {
    private final FxBackgroundTaskRunner backgroundTaskRunner;
    private final SettingsOperationProgressCoordinator operationProgress;
    private final ConfirmationPrompt confirmationPrompt;
    private final Runnable refresh;

    PiperSettingsOperations(FxBackgroundTaskRunner backgroundTaskRunner,
                            SettingsOperationProgressCoordinator operationProgress,
                            ConfirmationPrompt confirmationPrompt,
                            Runnable refresh) {
        this.backgroundTaskRunner = backgroundTaskRunner;
        this.operationProgress = operationProgress;
        this.confirmationPrompt = confirmationPrompt;
        this.refresh = refresh;
    }

    void verify(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        InspectPiperSetupReadinessUseCase inspector = services == null
                ? new InspectPiperSetupReadinessUseCase()
                : services.inspectPiperSetupReadiness();
        PiperSetupReadinessReport report = inspector.inspect(form.toSettings(), applicationRoot);
        if (report.ready()) {
            status.setText(SettingsDialog.friendlyEngineText(report.userMessage()) + " Voz local lista.");
        } else {
            status.setText("Voz local simple requiere preparación: " + summarizeMissing(report) + ".");
        }
    }

    void confirmAndPrepare(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (!confirmationPrompt.confirm(status, "Preparar Voz local simple", "Descargar voz local liviana",
                "La app descargará y verificará una voz local liviana dentro del programa.\n\n"
                        + "Es más pequeña que Voz IA avanzada y sirve como opción rápida para lectura.")) {
            status.setText("Preparación cancelada. No se modificó la configuración.");
            return;
        }
        runPreparation(form, services, applicationRoot, status, status);
    }

    void runPreparation(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot,
                        Label status, Node anchor) {
        OperationProgress progress = operationProgress.show(anchor,
                "Preparando Voz local simple",
                "Descargando voz local liviana",
                "La app descargará recursos locales de voz dentro del programa y verificará que puedan usarse.");
        progress.update("Iniciando preparación de Voz local simple...");
        status.setText("Preparando Voz local simple dentro del programa...");
        backgroundTaskRunner.start("preparando-voz-local-simple", () -> {
            try {
                DownloadPiperPortableRuntimeUseCase downloader = services == null
                        ? new DownloadPiperPortableRuntimeUseCase()
                        : services.downloadPiperPortableRuntime();
                PiperRuntimeDownloadReport report = downloader.download(form.toSettings(), applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> finishPreparation(form, services, report, status, progress));
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo preparar Voz local simple: "
                            + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    private void finishPreparation(SettingsFormModel form, SettingsApplicationServices services,
                                   PiperRuntimeDownloadReport report, Label status, OperationProgress progress) {
        String message;
        if (report.success()) {
            form.ttsVoiceProfileId.setText("voz-local-simple");
            message = selectAndSave(form, services);
            progress.finish(message);
        } else {
            message = SettingsDialog.friendlyEngineText(report.userMessage())
                    + " Pendiente: "
                    + (report.failedFiles().isEmpty()
                    ? "reintentar preparación"
                    : String.join(" · ", report.failedFiles()))
                    + ".";
            progress.fail(message);
        }
        status.setText(message);
        refresh.run();
    }

    void selectIfReady(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        InspectPiperSetupReadinessUseCase inspector = services == null
                ? new InspectPiperSetupReadinessUseCase()
                : services.inspectPiperSetupReadiness();
        PiperSetupReadinessReport report = inspector.inspect(form.toSettings(), applicationRoot);
        if (!report.canBeSelectedAsEngine()) {
            status.setText("Voz local simple aún requiere preparación: " + summarizeMissing(report) + ".");
            return;
        }
        String message = selectAndSave(form, services);
        status.setText(message);
        refresh.run();
    }

    String selectAndSave(SettingsFormModel form, SettingsApplicationServices services) {
        SelectPiperAsEngineUseCase selector = services == null
                ? new SelectPiperAsEngineUseCase()
                : services.selectPiperAsEngine();
        OperationalSettings selected = selector.select(form.toSettings());
        form.load(selected);
        if (services != null) {
            try {
                services.saveOperationalSettings().save(selected);
                return "Voz local simple lista y seleccionada como motor liviano.";
            } catch (IOException ex) {
                return "Voz local simple lista. No se pudo guardar automáticamente; pulsa Guardar cambios.";
            }
        }
        return "Voz local simple lista y seleccionada en pantalla.";
    }

    void importVoiceFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona la carpeta de Voz local simple");
        java.io.File selected = chooser.showDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected == null) {
            status.setText("Importación cancelada. Selecciona una carpeta válida de Voz local simple.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Importando Voz local simple",
                "Copiando y verificando voz local liviana",
                "La app copiará la voz seleccionada dentro del programa y comprobará que pueda usarse.");
        progress.update("Validando carpeta seleccionada de Voz local simple...");
        status.setText("Importando Voz local simple dentro del programa...");
        backgroundTaskRunner.start("importando-voz-local-simple", () -> {
            try {
                ImportPiperVoiceFolderUseCase importer = services == null
                        ? new ImportPiperVoiceFolderUseCase()
                        : services.importPiperVoiceFolder();
                PiperVoiceImportReport report = importer.importFrom(selected.toPath(), form.toSettings(),
                        applicationRoot, message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> finishImport(form, services, applicationRoot, status, progress, report));
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo importar Voz local simple: "
                            + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    private void finishImport(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot,
                              Label status, OperationProgress progress, PiperVoiceImportReport report) {
        if (report.success() && !report.primaryVoiceFileName().isBlank()) {
            form.ttsVoiceProfileId.setText(report.primaryVoiceFileName());
        }
        InspectPiperSetupReadinessUseCase inspector = services == null
                ? new InspectPiperSetupReadinessUseCase()
                : services.inspectPiperSetupReadiness();
        PiperSetupReadinessReport readiness = inspector.inspect(form.toSettings(), applicationRoot);
        String message;
        if (report.success() && readiness.canBeSelectedAsEngine()) {
            message = selectAndSave(form, services);
            progress.finish(message);
        } else if (report.success()) {
            message = "Importación completada. Aún falta preparar: " + summarizeMissingDetailed(readiness) + ".";
            progress.finish(message);
        } else {
            message = SettingsDialog.friendlyEngineText(report.userMessage());
            progress.fail(message);
        }
        status.setText(message);
        refresh.run();
    }

    static String summarizeMissing(PiperSetupReadinessReport report) {
        if (report == null || report.missingRequirements().isEmpty()) {
            return "sin componentes pendientes";
        }
        int count = report.missingRequirements().size();
        return count == 1 ? "1 componente pendiente" : count + " componentes pendientes";
    }

    static String summarizeMissingDetailed(PiperSetupReadinessReport report) {
        if (report == null || report.missingRequirements().isEmpty()) {
            return "sin componentes pendientes";
        }
        return report.missingRequirements().stream()
                .limit(4)
                .map(SettingsDialog::friendlyEngineText)
                .collect(Collectors.joining(" · "));
    }

    @FunctionalInterface
    interface ConfirmationPrompt {
        boolean confirm(Node anchor, String title, String header, String message);
    }
}
