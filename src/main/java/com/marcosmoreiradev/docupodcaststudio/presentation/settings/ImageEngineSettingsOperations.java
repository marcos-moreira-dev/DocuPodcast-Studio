package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImagePackageDownloadPreflight;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.LocalTheatreImagePackageDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.LocalTheatreImagePackageImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.LocalTheatreImagePreparationReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxComponentImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxComponentImportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/** Coordinates local theatre image engine settings operations. */
final class ImageEngineSettingsOperations {
    private final FxBackgroundTaskRunner backgroundTaskRunner;
    private final SettingsOperationProgressCoordinator operationProgress;
    private final VideoLocalSettingsOperations.ConfirmationPrompt confirmationPrompt;
    private final Runnable refresh;

    ImageEngineSettingsOperations(FxBackgroundTaskRunner backgroundTaskRunner,
                                  SettingsOperationProgressCoordinator operationProgress,
                                  VideoLocalSettingsOperations.ConfirmationPrompt confirmationPrompt,
                                  Runnable refresh) {
        this.backgroundTaskRunner = backgroundTaskRunner;
        this.operationProgress = operationProgress;
        this.confirmationPrompt = confirmationPrompt;
        this.refresh = refresh;
    }

    void verify(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (services == null) {
            status.setText("No hay servicios de configuracion conectados para Imagen IA teatral.");
            return;
        }
        ImageEngineReadinessReport report = services.inspectLocalTheatreImageEngine()
                .inspect(form.toSettings(), applicationRoot);
        status.setText(SettingsDialog.friendlyEngineText(report.statusLabel() + " Siguiente accion: " + report.nextAction()));
    }

    void confirmAndPrepare(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (!confirmationPrompt.confirm(status, "Preparar Imagen IA teatral", "Crear carpetas locales",
                "DocuPodcast creara tools/image, models/image y carpetas de salida dentro del programa.\n\n"
                        + "Esto no instala un runtime ejecutable, workflow, adaptadores ni modelos pesados. El runtime compatible y el paquete de imagen requieren confirmacion separada.")) {
            status.setText("Preparacion cancelada. No se modifico Imagen IA teatral.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Preparando Imagen IA teatral",
                "Creando carpetas locales",
                "Se preparan solo carpetas. No se marcara el motor como listo sin runtime, workflow y PNG real de prueba.");
        backgroundTaskRunner.start("preparando-imagen-ia-teatral", () -> {
            LocalTheatreImagePreparationReport report = services.prepareLocalTheatreImageRuntime()
                    .prepare(form.toSettings(), applicationRoot,
                            message -> progress.update(SettingsDialog.friendlyProgress(message)));
            Platform.runLater(() -> {
                String message = SettingsDialog.friendlyEngineText(report.userMessage());
                status.setText(message);
                if (report.success()) {
                    progress.finish(message);
                } else {
                    progress.fail(message);
                }
                refresh.run();
            });
        });
    }

    void importRuntimeFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (services == null) {
            status.setText("No hay servicios de configuracion conectados para importar runtime de Imagen IA teatral.");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona runtime local de Imagen IA teatral");
        File selected = chooser.showDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected == null) {
            status.setText("Importacion cancelada. Selecciona una carpeta con ComfyUI portable o runtime compatible.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Importando runtime de Imagen IA teatral",
                "Validando y copiando runtime",
                "La carpeta debe incluir un lanzador real o ComfyUI/main.py con Python local.");
        backgroundTaskRunner.start("importando-runtime-imagen-ia-teatral", () -> {
            LocalTheatreImagePreparationReport report = services.importLocalTheatreImageRuntime()
                    .importFrom(selected.toPath(), form.toSettings(), applicationRoot,
                            message -> progress.update(SettingsDialog.friendlyProgress(message)));
            Platform.runLater(() -> {
                String message = SettingsDialog.friendlyEngineText(report.userMessage());
                status.setText(message);
                if (report.success()) {
                    progress.finish(message);
                } else {
                    progress.fail(message);
                }
                refresh.run();
            });
        });
    }

    void confirmAndDownloadPackage(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        confirmAndDownloadPackage(form, services, applicationRoot, status, false);
    }

    void confirmAndDownloadPackage(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot,
                                   Label status, boolean forceReinstall) {
        ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(form.toSettings().imageGeneration().preset());
        ImagePackageDownloadPreflight preflight = services.downloadLocalTheatreImagePackage()
                .preflight(form.toSettings(), applicationRoot);
        if (!preflight.downloadable()) {
            status.setText(SettingsDialog.friendlyEngineText(preflight.userMessage()));
            return;
        }
        String warning = profile.highEnd()
                ? "Este perfil esta pensado para una computadora potente y puede rondar " + humanBytes(preflight.expectedBytes()) + ". Nunca es descarga default."
                : "El paquete ocupa alrededor de " + humanBytes(preflight.expectedBytes()) + " y puede tardar bastante.";
        if (preflight.requiresAuthentication()) {
            warning += "\n\nEl proveedor puede exigir cuenta, token o licencia. Si falla, importa el paquete local.";
        }
        if (!confirmationPrompt.confirm(status, "Descargar paquete de Imagen IA", "Descarga pesada con confirmacion especifica",
                "Perfil seleccionado: " + profile.displayName() + ".\n\n"
                        + profile.description() + "\n\n"
                        + preflight.userMessage() + "\n\n"
                        + warning)) {
            status.setText("Descarga cancelada. El paquete pesado no se descargo.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Descargando Imagen IA teatral",
                (forceReinstall ? "Reinstalando " : "Descargando ") + profile.displayName(),
                "La descarga se ejecuta solo porque aceptaste esta accion especifica.");
        backgroundTaskRunner.start("descargando-imagen-ia-teatral", () -> {
            LocalTheatreImagePackageDownloadReport report = services.downloadLocalTheatreImagePackage()
                    .download(form.toSettings(), applicationRoot,
                            message -> progress.update(SettingsDialog.friendlyProgress(message)),
                            forceReinstall);
            Platform.runLater(() -> finishDownload(status, progress, report));
        });
    }

    private void finishDownload(Label status, OperationProgress progress, LocalTheatreImagePackageDownloadReport report) {
        String message = SettingsDialog.friendlyEngineText(report.userMessage());
        status.setText(message);
        if (report.success()) {
            progress.finish(message);
        } else {
            progress.fail(message);
        }
        refresh.run();
    }

    void importPackageFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona paquete local de Imagen IA teatral");
        File selected = chooser.showDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected == null) {
            status.setText("Importacion cancelada. Selecciona una carpeta valida de paquete de imagen.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Importando Imagen IA teatral",
                "Copiando paquete a models/image",
                "Se copiara el paquete seleccionado dentro del programa.");
        backgroundTaskRunner.start("importando-imagen-ia-teatral", () -> {
            LocalTheatreImagePackageImportReport report = services.importLocalTheatreImagePackage()
                    .importFrom(selected.toPath(), form.toSettings(), applicationRoot,
                            message -> progress.update(SettingsDialog.friendlyProgress(message)));
            Platform.runLater(() -> finishImport(status, progress, report));
        });
    }

    void importFluxFile(SettingsApplicationServices services, Path applicationRoot, Label status) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecciona componente FLUX");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Safetensors", "*.safetensors"));
        File selected = chooser.showOpenDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected != null) {
            importFlux(selected.toPath(), services, applicationRoot, status);
        }
    }

    void importFluxFolder(SettingsApplicationServices services, Path applicationRoot, Label status) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona text_encoder, text_encoder_2 o carpeta de componentes FLUX");
        File selected = chooser.showDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected != null) {
            importFlux(selected.toPath(), services, applicationRoot, status);
        }
    }

    private void importFlux(Path source, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (services == null) {
            status.setText("No hay servicios conectados para importar componentes FLUX.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Importando componentes FLUX",
                "Validando componentes",
                "Los shards T5 se consolidan por streaming en un proceso Python independiente; el original no se modifica.");
        backgroundTaskRunner.start("importando-componentes-flux", () -> {
            FluxComponentImportReport report = services.importFluxComponents().importFrom(
                    source, applicationRoot, message -> progress.update(SettingsDialog.friendlyProgress(message)));
            Platform.runLater(() -> {
                status.setText(report.userMessage());
                if (report.success()) {
                    progress.finish(report.userMessage());
                } else {
                    progress.fail(report.userMessage());
                }
                refresh.run();
            });
        });
    }

    private void finishImport(Label status, OperationProgress progress, LocalTheatreImagePackageImportReport report) {
        String message = SettingsDialog.friendlyEngineText(report.userMessage());
        status.setText(message);
        if (report.success()) {
            progress.finish(message);
        } else {
            progress.fail(message);
        }
        refresh.run();
    }

    void startEngine(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        OperationProgress progress = operationProgress.show(status,
                "Iniciando Imagen IA teatral",
                "Arrancando motor local",
                "DocuPodcast usara el runtime local preparado en tools/image.");
        backgroundTaskRunner.start("iniciando-imagen-ia-teatral", () -> {
            ImageEngineSmokeReport report = services.startLocalTheatreImageEngine().start(form.toSettings(), applicationRoot);
            Platform.runLater(() -> finishSmoke(status, progress, report));
        });
    }

    void stopEngine(SettingsFormModel form, SettingsApplicationServices services, Label status) {
        ImageEngineSmokeReport report = services.stopLocalTheatreImageEngine().stop(form.toSettings());
        status.setText(SettingsDialog.friendlyEngineText(report.userMessage()));
        refresh.run();
    }

    void runSmoke(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (services == null) {
            status.setText("No hay servicios de configuracion conectados para probar Imagen IA teatral.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Probando Imagen IA teatral",
                "Verificando runtime, paquete y motor",
                "La prueba detecta o inicia el motor local y debe generar un PNG real.");
        backgroundTaskRunner.start("probando-imagen-ia-teatral", () -> {
            ImageEngineSmokeReport report = services.runLocalTheatreImageSmoke().runDetailed(form.toSettings(), applicationRoot);
            Platform.runLater(() -> finishSmoke(status, progress, report));
        });
    }

    private void finishSmoke(Label status, OperationProgress progress, ImageEngineSmokeReport report) {
        String message = SettingsDialog.friendlyEngineText(report.userMessage());
        status.setText(message);
        if (report.success()) {
            progress.finish(saveSettingsIfPossible(message));
        } else {
            progress.fail(message);
        }
        refresh.run();
    }

    private String saveSettingsIfPossible(String message) {
        try {
            // Settings are saved by the dialog action bar; this path keeps smoke idempotent.
            return message;
        } catch (RuntimeException ex) {
            return message;
        }
    }

    private static String humanBytes(long bytes) {
        if (bytes <= 0) {
            return "tamano definido por el paquete";
        }
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        if (gb >= 1.0) {
            return String.format(java.util.Locale.ROOT, "%.1f GB", gb);
        }
        double mb = bytes / 1024.0 / 1024.0;
        return String.format(java.util.Locale.ROOT, "%.0f MB", mb);
    }
}
