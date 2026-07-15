package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.document.DownloadTesseractPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ImportTesseractRuntimeFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractLanguageDiscovery;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeLocator;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.nio.file.Path;

/** Coordinates OCR/Tesseract settings operations without touching PDF presentation code. */
final class OcrSettingsOperations {
    private final FxBackgroundTaskRunner backgroundTaskRunner;
    private final SettingsOperationProgressCoordinator operationProgress;
    private final Runnable refresh;

    OcrSettingsOperations(FxBackgroundTaskRunner backgroundTaskRunner,
                          SettingsOperationProgressCoordinator operationProgress,
                          Runnable refresh) {
        this.backgroundTaskRunner = backgroundTaskRunner;
        this.operationProgress = operationProgress;
        this.refresh = refresh;
    }

    void verify(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        status.setText(statusText(form, applicationRoot));
    }

    String statusText(SettingsFormModel form, Path applicationRoot) {
        OperationalSettings settings = form.toSettings();
        TesseractRuntimeLocator locator = new TesseractRuntimeLocator();
        TesseractToolDiscovery discovery = locator.locate(settings, applicationRoot);
        if (!discovery.ready()) {
            String downloadHint = settings.ocr().tesseractRuntimeZipUrl().isBlank()
                    ? "No hay URL de descarga configurada; usa Importar carpeta Tesseract portable."
                    : "Hay URL configurada; puedes usar Descargar o importar carpeta portable.";
            return "OCR no configurado. " + downloadHint;
        }
        TesseractLanguageDiscovery languages = locator.inspectLanguages(discovery, settings.ocr().languages());
        if (!languages.ready()) {
            return "OCR encontro Tesseract (" + discovery.source() + "), pero faltan idiomas: "
                    + languages.missingLabel() + ". Importa carpeta con tessdata/spa.traineddata y eng.traineddata.";
        }
        return "OCR listo. Fuente: " + discovery.source() + " -> " + discovery.command();
    }

    void importRuntimeFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona la carpeta que contiene tesseract.exe y tessdata");
        java.io.File selected = chooser.showDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected == null) {
            status.setText("Importacion OCR cancelada. Selecciona una carpeta Tesseract portable.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Importando OCR local",
                "Copiando Tesseract dentro del programa",
                "La app buscara tesseract.exe y los idiomas spa/eng en la carpeta seleccionada.");
        progress.update("Buscando Tesseract OCR en la carpeta seleccionada...");
        status.setText("Importando OCR local dentro del programa...");
        backgroundTaskRunner.start("importando-tesseract-ocr", () -> {
            try {
                TesseractRuntimeImportReport report = new ImportTesseractRuntimeFolderUseCase()
                        .importFrom(selected.toPath(), applicationRoot,
                                message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> finishImport(form, services, status, progress, report));
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo importar OCR local: "
                            + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    void downloadRuntime(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (form.toSettings().ocr().tesseractRuntimeZipUrl().isBlank()) {
            status.setText("No hay URL OCR configurada. Importa una carpeta Tesseract portable.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Descargando OCR local",
                "Descargando Tesseract configurado",
                "La app usara la URL OCR configurada en settings o variables de entorno.");
        progress.update("Iniciando descarga OCR local...");
        status.setText("Descargando OCR local dentro del programa...");
        backgroundTaskRunner.start("descargando-tesseract-ocr", () -> {
            try {
                TesseractRuntimeImportReport report = new DownloadTesseractPortableRuntimeUseCase()
                        .download(form.toSettings(), applicationRoot,
                                message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> finishImport(form, services, status, progress, report));
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo descargar OCR local: "
                            + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    private void finishImport(SettingsFormModel form, SettingsApplicationServices services, Label status,
                              OperationProgress progress, TesseractRuntimeImportReport report) {
        if (report.executable() != null) {
            OperationalSettings current = form.toSettings();
            form.applyOcrSettings(new OperationalSettings.OcrSettings(
                    current.ocr().engineMode(),
                    report.executable().toString(),
                    current.ocr().languages(),
                    current.ocr().dpi(),
                    current.ocr().timeoutSeconds(),
                    current.ocr().cacheEnabled(),
                    current.ocr().tesseractRuntimeZipUrl()));
        }
        String message = SettingsDialog.friendlyEngineText(report.userMessage());
        if (report.success()) {
            message = saveSettingsIfPossible(form, services, message + " OCR quedo guardado para reintentar paginas PDF pendientes.");
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
            return message + " No se pudo guardar automaticamente; pulsa Guardar cambios.";
        }
    }
}
