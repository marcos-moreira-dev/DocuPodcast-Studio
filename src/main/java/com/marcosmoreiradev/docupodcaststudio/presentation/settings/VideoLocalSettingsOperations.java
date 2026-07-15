package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.video.DownloadFfmpegPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.application.video.ImportFfmpegRuntimeFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.nio.file.Path;

/** Coordinates Video local/FFmpeg settings operations without owning the Settings dialog layout. */
final class VideoLocalSettingsOperations {
    private final FxBackgroundTaskRunner backgroundTaskRunner;
    private final SettingsOperationProgressCoordinator operationProgress;
    private final ConfirmationPrompt confirmationPrompt;
    private final Runnable refresh;

    VideoLocalSettingsOperations(FxBackgroundTaskRunner backgroundTaskRunner,
                                 SettingsOperationProgressCoordinator operationProgress,
                                 ConfirmationPrompt confirmationPrompt,
                                 Runnable refresh) {
        this.backgroundTaskRunner = backgroundTaskRunner;
        this.operationProgress = operationProgress;
        this.confirmationPrompt = confirmationPrompt;
        this.refresh = refresh;
    }

    void verify(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (services == null) {
            status.setText("No hay servicios de configuración conectados para verificar video local.");
            return;
        }
        FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(applicationRoot, configuredFfmpegPath(form));
        FfmpegRuntimeReport report = services.inspectFfmpegRuntime().inspect(discovery);
        String encoders = report.encoders().isEmpty() ? "sin opciones confirmadas" : String.join(", ", report.encoders());
        if (report.readyForFinalVideo()) {
            status.setText("Video local listo. Opciones detectadas: " + encoders + ".");
        } else {
            status.setText("Video local requiere preparación. "
                    + SettingsDialog.friendlyEngineText(String.join(" · ", report.warnings())));
        }
    }

    void confirmAndPrepare(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (!confirmationPrompt.confirm(status, "Preparar video local", "Descargar y verificar video local",
                "DocuPodcast descargará el paquete de video configurado, copiará los componentes necesarios dentro del programa y verificará que sirvan para exportar video final.\n\n"
                        + "Esto puede demorar varios minutos según tu conexión. No modifica rutas globales del sistema.")) {
            status.setText("Preparación de video cancelada. No se modificó la configuración.");
            return;
        }
        downloadPortableRuntime(form, services, applicationRoot, status);
    }

    void downloadPortableRuntime(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        OperationProgress progress = operationProgress.show(status,
                "Preparando video local",
                "Descargando y verificando componentes de video",
                "La app descargará el paquete configurado y dejará los componentes dentro del programa, sin modificar rutas del sistema.");
        progress.update("Iniciando preparación de video local...");
        status.setText("Preparando video local dentro del programa...");
        backgroundTaskRunner.start("preparando-video-local", () -> {
            try {
                DownloadFfmpegPortableRuntimeUseCase downloader = services == null
                        ? new DownloadFfmpegPortableRuntimeUseCase()
                        : services.downloadFfmpegPortableRuntime();
                FfmpegRuntimeDownloadReport report = downloader.download(form.toSettings(), applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> finishDownload(form, services, status, progress, report));
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo preparar video local: "
                            + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    private void finishDownload(SettingsFormModel form, SettingsApplicationServices services, Label status,
                                OperationProgress progress, FfmpegRuntimeDownloadReport report) {
        if (report.ffmpegExecutable() != null) {
            form.ffmpegExecutable.setText(report.ffmpegExecutable().toString());
            form.preferEmbeddedFfmpeg.setSelected(true);
        }
        String message = SettingsDialog.friendlyEngineText(report.userMessage());
        if (!report.failedItems().isEmpty()) {
            message = message + " Detalle: "
                    + SettingsDialog.friendlyEngineText(String.join(" · ", report.failedItems()));
        }
        if (report.success()) {
            message = saveSettingsIfPossible(form, services, message);
        }
        status.setText(message);
        if (report.success()) {
            progress.finish(message);
        } else {
            progress.fail(message);
        }
        refresh.run();
    }

    void importRuntimeFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona la carpeta que contiene ffmpeg.exe y ffprobe.exe");
        java.io.File selected = chooser.showDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected == null) {
            status.setText("Importación cancelada. Selecciona una carpeta válida de video local.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Importando video local",
                "Copiando herramientas de video dentro del programa",
                "La app buscará los componentes necesarios dentro de la carpeta seleccionada y los copiará al programa.");
        progress.update("Buscando componentes de video dentro de la carpeta seleccionada...");
        status.setText("Importando video local dentro del programa...");
        backgroundTaskRunner.start("importando-video-local", () -> {
            try {
                ImportFfmpegRuntimeFolderUseCase importer = services == null
                        ? new ImportFfmpegRuntimeFolderUseCase()
                        : services.importFfmpegRuntimeFolder();
                FfmpegRuntimeImportReport report = importer.importFrom(selected.toPath(), applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> finishImport(form, services, status, progress, report));
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo importar video local: "
                            + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    private void finishImport(SettingsFormModel form, SettingsApplicationServices services, Label status,
                              OperationProgress progress, FfmpegRuntimeImportReport report) {
        if (report.success() && report.ffmpegExecutable() != null) {
            form.ffmpegExecutable.setText(report.ffmpegExecutable().toString());
            form.preferEmbeddedFfmpeg.setSelected(true);
        }
        String message = report.success()
                ? SettingsDialog.friendlyEngineText(report.userMessage()) + " Video local quedó guardado para próximos procesos."
                : SettingsDialog.friendlyEngineText(report.userMessage());
        if (report.success()) {
            message = saveSettingsIfPossible(form, services, message);
        }
        status.setText(message);
        if (report.success()) {
            progress.finish(message);
        } else {
            progress.fail(message);
        }
        refresh.run();
    }

    private static String saveSettingsIfPossible(SettingsFormModel form, SettingsApplicationServices services, String message) {
        if (services == null) {
            return message;
        }
        try {
            services.saveOperationalSettings().save(form.toSettings());
            return message;
        } catch (IOException ex) {
            return message + " No se pudo guardar automáticamente; pulsa Guardar cambios.";
        }
    }

    private static Path configuredFfmpegPath(SettingsFormModel form) {
        String raw = form == null ? "" : form.ffmpegExecutable.getText();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Path.of(raw.strip());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @FunctionalInterface
    interface ConfirmationPrompt {
        boolean confirm(Node anchor, String title, String header, String message);
    }
}
