package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadPiperPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.LocalTheatreImagePackageDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperRuntimeDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Coordinates the first-use setup prompt without keeping long operations in SettingsDialog. */
final class InitialSetupSettingsOperations {
    private final FxBackgroundTaskRunner backgroundTaskRunner;
    private final SettingsOperationProgressCoordinator operationProgress;
    private final PiperSettingsOperations.ConfirmationPrompt confirmationPrompt;
    private final Runnable refresh;
    private final PiperSettingsOperations piperOperations;

    InitialSetupSettingsOperations(FxBackgroundTaskRunner backgroundTaskRunner,
                                   SettingsOperationProgressCoordinator operationProgress,
                                   PiperSettingsOperations.ConfirmationPrompt confirmationPrompt,
                                   Runnable refresh,
                                   PiperSettingsOperations piperOperations) {
        this.backgroundTaskRunner = backgroundTaskRunner;
        this.operationProgress = operationProgress;
        this.confirmationPrompt = confirmationPrompt;
        this.refresh = refresh;
        this.piperOperations = piperOperations;
    }

    void prompt(Node anchor, SettingsFormModel form, SettingsApplicationServices services) {
        if (services == null || form == null) {
            return;
        }
        Path applicationRoot = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        OperationalSettings current = form.toSettings();
        PiperSetupReadinessReport piper = services.inspectPiperSetupReadiness().inspect(current, applicationRoot);
        boolean piperMissing = !piper.ready();
        boolean imageMissing = !services.inspectLocalTheatreImageSetupReadiness()
                .inspect(settingsWithTest4gb(current), applicationRoot)
                .ready();
        if (!piperMissing && !imageMissing) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Configuracion inicial");
        alert.setHeaderText("Preparar este equipo");
        VBox body = new VBox(12);
        body.setMinWidth(660);
        Label lead = wrapped("Prepara primero la voz rapida. Imagen IA teatral Prueba 4GB es opcional y solo se descarga si la marcas.");
        CheckBox piperChoice = new CheckBox("Voz local simple para que escuches documentos cuanto antes");
        piperChoice.setSelected(piperMissing);
        piperChoice.setVisible(piperMissing);
        piperChoice.setManaged(piperMissing);
        CheckBox imageChoice = new CheckBox("Imagen IA teatral Prueba 4GB (opcional, " + humanBytes(ImageModelPackageProfile.TEST_4GB_SD15.approximateBytes()) + ")");
        imageChoice.setSelected(false);
        imageChoice.setVisible(imageMissing);
        imageChoice.setManaged(imageMissing);
        Label note = wrapped("Voz IA avanzada puede tardar mucho mas y se prepara despues desde esta misma ventana. El paquete de imagen es pesado y pedira confirmacion propia.");
        body.getChildren().addAll(lead, piperChoice, imageChoice, note);
        alert.getDialogPane().setContent(body);
        alert.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        DialogStyler.apply(alert, ownerOf(anchor));
        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        boolean preparePiper = piperMissing && piperChoice.isSelected();
        boolean prepareImage = imageMissing && imageChoice.isSelected();
        if (prepareImage && !confirmationPrompt.confirm(anchor,
                "Descargar Imagen IA teatral",
                "Confirmar descarga Prueba 4GB",
                "Descargaras el paquete Prueba 4GB de Imagen IA teatral dentro del programa.\n\n"
                        + "Tamano aproximado: " + humanBytes(ImageModelPackageProfile.TEST_4GB_SD15.approximateBytes()) + ". "
                        + "La descarga puede tardar y no bloquea el uso normal si decides omitirla.")) {
            prepareImage = false;
        }
        if (!preparePiper && !prepareImage) {
            return;
        }
        runSelected(anchor, form, services, applicationRoot, preparePiper, prepareImage);
    }

    void run(Node anchor, SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        if (form == null) {
            return;
        }
        Path root = applicationRoot == null
                ? RuntimePathResolver.defaultResolver().resolve().applicationRoot()
                : applicationRoot;
        runSelected(anchor, form, services, root, true, false);
    }

    private void runSelected(Node anchor, SettingsFormModel form, SettingsApplicationServices services,
                             Path applicationRoot, boolean preparePiper, boolean prepareImage) {
        if (services == null) {
            return;
        }
        OperationProgress progress = operationProgress.show(anchor,
                "Configuracion inicial",
                "Preparando este equipo",
                "DocuPodcast prepara solo los elementos seleccionados. La imagen IA se descarga unicamente porque aceptaste esa accion especifica.");
        progress.update("Iniciando configuracion inicial...");
        backgroundTaskRunner.start("configuracion-inicial-voz-local-simple", () -> {
            InitialSetupResult result = execute(form, services, applicationRoot, preparePiper, prepareImage, progress);
            Platform.runLater(() -> finish(form, services, result, progress, anchor));
        });
    }

    private InitialSetupResult execute(SettingsFormModel form, SettingsApplicationServices services,
                                       Path applicationRoot, boolean preparePiper, boolean prepareImage,
                                       OperationProgress progress) {
        PiperRuntimeDownloadReport piperReport = null;
        LocalTheatreImagePackageDownloadReport imageReport = null;
        RuntimeException failure = null;
        try {
            OperationalSettings settings = form.toSettings();
            if (preparePiper) {
                progress.update("Descargando Voz local simple...");
                DownloadPiperPortableRuntimeUseCase downloader = services.downloadPiperPortableRuntime();
                piperReport = downloader.download(settings, applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
            }
            if (prepareImage) {
                progress.update("Descargando Imagen IA teatral Prueba 4GB...");
                imageReport = services.downloadLocalTheatreImagePackage()
                        .download(settingsWithTest4gb(settings), applicationRoot,
                                message -> progress.update(SettingsDialog.friendlyProgress(message)));
            }
        } catch (RuntimeException ex) {
            failure = ex;
        }
        return new InitialSetupResult(preparePiper, prepareImage, piperReport, imageReport, failure);
    }

    private void finish(SettingsFormModel form, SettingsApplicationServices services, InitialSetupResult result,
                        OperationProgress progress, Node anchor) {
        if (result.failure() != null) {
            String message = "No se pudo completar Configuracion inicial: "
                    + SettingsDialog.friendlyEngineText(result.failure().getMessage());
            progress.fail(message);
            refresh.run();
            return;
        }

        List<String> messages = new ArrayList<>();
        boolean failed = false;
        if (result.preparePiper()) {
            PiperRuntimeDownloadReport report = result.piperReport();
            if (report != null && report.success()) {
                form.ttsVoiceProfileId.setText("voz-local-simple");
                messages.add(piperOperations.selectAndSave(form, services));
            } else {
                failed = true;
                messages.add("Voz local simple pendiente: "
                        + SettingsDialog.friendlyEngineText(report == null ? "sin reporte" : report.userMessage()));
            }
        }
        if (result.prepareImage()) {
            LocalTheatreImagePackageDownloadReport report = result.imageReport();
            if (report != null && report.success()) {
                form.imagePreset.setValue(ImageModelPackageProfile.TEST_4GB_SD15.presetId());
                form.imageModelName.setText(ImageModelPackageProfile.TEST_4GB_SD15.checkpointName());
                form.imageLowVram.setSelected(true);
                saveSettings(form, services);
                messages.add("Imagen IA teatral Prueba 4GB lista.");
            } else {
                failed = true;
                messages.add("Imagen IA teatral pendiente: "
                        + SettingsDialog.friendlyEngineText(report == null ? "sin reporte" : report.userMessage()));
            }
        }

        String message = "Configuracion inicial completada. " + String.join(" ", messages);
        if (failed) {
            progress.fail(message);
        } else {
            progress.finish(message);
        }
        if (anchor instanceof Label label) {
            label.setText(message);
        }
        refresh.run();
    }

    private void saveSettings(SettingsFormModel form, SettingsApplicationServices services) {
        if (services == null) {
            return;
        }
        try {
            services.saveOperationalSettings().save(form.toSettings());
        } catch (IOException ignored) {
            // The progress message remains useful; the user can still press Guardar cambios.
        }
    }

    private static OperationalSettings settingsWithTest4gb(OperationalSettings current) {
        OperationalSettings settings = current == null ? OperationalSettings.defaults() : current;
        ImageGenerationSettings image = new ImageGenerationSettings(
                settings.imageGeneration().engineMode(),
                settings.imageGeneration().baseUrl(),
                settings.imageGeneration().devicePolicy(),
                ImageModelPackageProfile.TEST_4GB_SD15.presetId(),
                ImageModelPackageProfile.TEST_4GB_SD15.checkpointName(),
                settings.imageGeneration().adaptersDirectory(),
                settings.imageGeneration().timeoutSeconds(),
                true);
        return new OperationalSettings(
                settings.readingDocument(),
                settings.playbackBuffer(),
                settings.tts(),
                settings.video(),
                image,
                settings.frameGeneration(),
                settings.compute(),
                settings.ocr(),
                settings.storage(),
                settings.diagnostics());
    }

    private static Label wrapped(String text) {
        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setMinWidth(Region.USE_PREF_SIZE);
        label.setPrefWidth(640);
        return label;
    }

    private static Window ownerOf(Node anchor) {
        return anchor != null && anchor.getScene() != null ? anchor.getScene().getWindow() : null;
    }

    private static String humanBytes(long bytes) {
        if (bytes <= 0) {
            return "tamano por verificar";
        }
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        return gb >= 1.0 ? String.format(java.util.Locale.ROOT, "%.2f GB", gb)
                : String.format(java.util.Locale.ROOT, "%.0f MB", bytes / 1024.0 / 1024.0);
    }

    private record InitialSetupResult(
            boolean preparePiper,
            boolean prepareImage,
            PiperRuntimeDownloadReport piperReport,
            LocalTheatreImagePackageDownloadReport imageReport,
            RuntimeException failure
    ) {
    }
}
