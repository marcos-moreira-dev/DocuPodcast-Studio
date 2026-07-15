package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeEnvironmentReport;
import com.marcosmoreiradev.docupodcaststudio.application.compute.XttsCudaPreparationReport;
import com.marcosmoreiradev.docupodcaststudio.application.compute.XttsCudaSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.document.DownloadTesseractPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractLanguageDiscovery;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractRuntimeLocator;
import com.marcosmoreiradev.docupodcaststudio.application.document.TesseractToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperRuntimeDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PiperSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsModelDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsRuntimePreparationReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Guided installer for app-embedded voice, CUDA and video dependencies. */
public final class EmbeddedDependencySetupAssistant {
    private static final AtomicBoolean STARTUP_PROMPT_SHOWN = new AtomicBoolean(false);
    private static final ButtonType INSTALL_ALL = new ButtonType("Instalar todo recomendado", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType CHOOSE_TOOLS = new ButtonType("Elegir herramientas", ButtonBar.ButtonData.OTHER);
    private static final ButtonType NOT_NOW = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
    private final FxBackgroundTaskRunner backgroundTaskRunner = new FxBackgroundTaskRunner();

    public void runStartupPreflight(Window owner, SettingsApplicationServices services) {
        if (services == null) {
            return;
        }
        OperationalSettings settings = loadSettings(services);
        if (!settings.diagnostics().preflightOnStartup()) {
            return;
        }
        DependencySnapshot snapshot = inspect(services, settings);
        if (snapshot.ready() || !STARTUP_PROMPT_SHOWN.compareAndSet(false, true)) {
            return;
        }
        Optional<ButtonType> choice = dependencyPrompt(owner, snapshot,
                "Dependencias locales pendientes",
                "DocuPodcast encontro herramientas locales pendientes.",
                "Puedes instalar todo dentro de la carpeta del programa. No se usara Python global ni FFmpeg desde PATH; OCR requiere Tesseract portable o una URL configurada.");
        if (choice.isPresent() && choice.get() == INSTALL_ALL) {
            runRecommendedSetup(owner, services, false, null);
        } else if (choice.isPresent() && choice.get() == CHOOSE_TOOLS) {
            new SettingsDialog().showVoiceEngines(owner, services);
        }
    }

    public void showRecommendedSetup(Node anchor, SettingsApplicationServices services) {
        showRecommendedSetup(ownerOf(anchor), services);
    }

    public void showRecommendedSetup(Window owner, SettingsApplicationServices services) {
        if (services == null) {
            return;
        }
        DependencySnapshot snapshot = inspect(services, loadSettings(services));
        Optional<ButtonType> choice = dependencyPrompt(owner, snapshot,
                "Preparar dependencias recomendadas",
                "Instalar stack local embebido",
                "Se prepararan Voz IA avanzada, CUDA si aplica, Voz local simple, Video local/FFmpeg y OCR PDF local dentro del programa. Imagen IA teatral se informa aparte por tamano.");
        if (choice.isPresent() && choice.get() == INSTALL_ALL) {
            runRecommendedSetup(owner, services, false, null);
        } else if (choice.isPresent() && choice.get() == CHOOSE_TOOLS) {
            new SettingsDialog().showVoiceEngines(owner, services);
        }
    }

    public boolean offerVideoLocalSetupIfMissing(Window owner, SettingsApplicationServices services, Runnable onSuccess) {
        if (services == null || embeddedVideoReady(services)) {
            return false;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Preparar Video local");
        confirm.setHeaderText("Video local no esta preparado. Quieres instalarlo ahora?");
        confirm.getDialogPane().setContent(wrappedLabel(
                "Faltan ffmpeg.exe o ffprobe.exe embebidos, o el binario no confirma libx264. "
                        + "DocuPodcast puede descargar y verificar FFmpeg dentro de tools/ffmpeg/bin, "
                        + "guardar esa ruta y reintentar la exportacion al terminar."));
        confirm.getDialogPane().getButtonTypes().setAll(INSTALL_ALL, NOT_NOW);
        DialogStyler.apply(confirm, owner);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != INSTALL_ALL) {
            return true;
        }
        runVideoLocalSetup(owner, services, true, onSuccess == null ? () -> { } : onSuccess);
        return true;
    }

    public boolean embeddedVideoReady(SettingsApplicationServices services) {
        if (services == null) {
            return false;
        }
        Path root = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        FfmpegRuntimeReport report = embeddedFfmpegReport(root, services);
        return report.readyForFinalVideo();
    }

    private Optional<ButtonType> dependencyPrompt(Window owner, DependencySnapshot snapshot, String title, String header, String lead) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        StringBuilder body = new StringBuilder(lead == null ? "" : lead.strip());
        body.append("\n\nElementos pendientes:\n");
        List<String> missing = snapshot.missingLabels();
        if (missing.isEmpty()) {
            body.append("- Ninguno critico. Puedes reinstalar/verificar si deseas.\n");
        } else {
            missing.forEach(item -> body.append("- ").append(item).append('\n'));
        }
        body.append("\nInstalar todo recomendado incluye FFmpeg/FFprobe embebidos y OCR si hay URL configurada.");
        body.append("\nSi OCR no tiene URL, importa una carpeta Tesseract portable desde Configuracion.");
        body.append("\nImagen IA teatral no se descarga aqui: usa su tarjeta y acepta la confirmacion especifica del paquete pesado.");
        alert.getDialogPane().setContent(wrappedLabel(body.toString()));
        alert.getDialogPane().getButtonTypes().setAll(INSTALL_ALL, CHOOSE_TOOLS, NOT_NOW);
        DialogStyler.apply(alert, owner);
        return alert.showAndWait();
    }

    private DependencySnapshot inspect(SettingsApplicationServices services, OperationalSettings settings) {
        Path root = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        XttsSetupReadinessReport xtts = services.inspectXttsSetupReadiness().inspect(settings, root);
        PiperSetupReadinessReport piper = services.inspectPiperSetupReadiness().inspect(settings, root);
        FfmpegRuntimeReport ffmpeg = embeddedFfmpegReport(root, services);
        ComputeEnvironmentReport compute = services.inspectComputeEnvironment().inspect(settings);
        XttsCudaSmokeReport cuda = services.inspectXttsCudaSmoke().inspect(root);
        boolean cudaApplies = cudaApplies(settings, compute);
        boolean imageReady = services.inspectLocalTheatreImageSetupReadiness().inspect(settings, root).ready();
        boolean ocrReady = ocrReady(settings, root);
        return new DependencySnapshot(xtts.ready(), piper.ready(), ffmpeg.readyForFinalVideo(),
                !cudaApplies || cuda.gpuUsableForXtts(), cudaApplies, imageReady, ocrReady);
    }

    private void runRecommendedSetup(Window owner, SettingsApplicationServices services, boolean autoCloseOnSuccess, Runnable onSuccess) {
        SetupProgress progress = showProgress(owner,
                "Preparando dependencias locales",
                "Instalando stack embebido",
                "DocuPodcast preparara las herramientas locales dentro de la carpeta del programa.");
        backgroundTaskRunner.start("docupodcast-embedded-dependency-setup", () -> {
            SetupSummary summary = new SetupSummary();
            try {
                OperationalSettings settings = loadSettings(services);
                Path root = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
                prepareXtts(services, settings, root, progress, summary);
                prepareCudaIfNeeded(services, settings, root, progress, summary);
                preparePiper(services, settings, root, progress, summary);
                prepareFfmpeg(services, settings, root, progress, summary);
                prepareOcr(settings, root, progress, summary);
                Platform.runLater(() -> {
                    progress.finish(summary.message());
                    if (autoCloseOnSuccess && summary.blockingFailures() == 0) {
                        progress.close();
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    }
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> progress.fail("No se pudo completar la preparacion: " + friendly(ex.getMessage())));
            }
        });
    }

    private void runVideoLocalSetup(Window owner, SettingsApplicationServices services, boolean autoCloseOnSuccess, Runnable onSuccess) {
        SetupProgress progress = showProgress(owner,
                "Preparando Video local",
                "Instalando FFmpeg embebido",
                "DocuPodcast descargara FFmpeg/FFprobe y verificara libx264 antes de exportar MP4.");
        backgroundTaskRunner.start("docupodcast-ffmpeg-embedded-setup", () -> {
            SetupSummary summary = new SetupSummary();
            try {
                OperationalSettings settings = loadSettings(services);
                Path root = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
                prepareFfmpeg(services, settings, root, progress, summary);
                Platform.runLater(() -> {
                    progress.finish(summary.message());
                    if (autoCloseOnSuccess && summary.blockingFailures() == 0) {
                        progress.close();
                        onSuccess.run();
                    }
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> progress.fail("No se pudo preparar Video local: " + friendly(ex.getMessage())));
            }
        });
    }

    private void prepareXtts(SettingsApplicationServices services, OperationalSettings settings, Path root,
                             SetupProgress progress, SetupSummary summary) {
        XttsSetupReadinessReport readiness = services.inspectXttsSetupReadiness().inspect(settings, root);
        if (readiness.ready()) {
            summary.skipped("Voz IA avanzada", "ya estaba lista");
            return;
        }
        runStep(summary, "Voz IA avanzada", true, () -> {
            progress.update("Preparando Python local de Voz IA avanzada...");
            XttsRuntimePreparationReport preparation = services.prepareXttsPortableRuntime()
                    .prepare(settings, root, message -> progress.update(friendlyProgress(message)));
            XttsSetupReadinessReport currentReadiness = services.inspectXttsSetupReadiness().inspect(settings, root);
            if (!preparation.success() && !currentReadiness.ready()) {
                throw new IllegalStateException(preparation.userMessage() + preparationDetail(preparation));
            }
            if (!currentReadiness.ready()) {
                progress.update("Descargando recursos de Voz IA avanzada...");
                XttsModelDownloadReport download = services.downloadXttsOfficialModel()
                        .download(settings, root, message -> progress.update(friendlyProgress(message)));
                currentReadiness = services.inspectXttsSetupReadiness().inspect(settings, root);
                if (!download.success()) {
                    throw new IllegalStateException(download.userMessage() + " Reporte: " + download.diagnosticReport());
                }
            }
            if (!currentReadiness.ready()) {
                throw new IllegalStateException(currentReadiness.userMessage() + " Faltan: "
                        + String.join(", ", currentReadiness.missingRequirements())
                        + preparationDetail(preparation));
            }
        });
    }

    private void prepareCudaIfNeeded(SettingsApplicationServices services, OperationalSettings settings, Path root,
                                     SetupProgress progress, SetupSummary summary) {
        ComputeEnvironmentReport compute = services.inspectComputeEnvironment().inspect(settings);
        if (!cudaApplies(settings, compute)) {
            summary.skipped("CUDA Voz IA avanzada", "no aplica para el dispositivo actual");
            return;
        }
        XttsCudaSmokeReport smoke = services.inspectXttsCudaSmoke().inspect(root);
        if (smoke.gpuUsableForXtts()) {
            summary.skipped("CUDA Voz IA avanzada", "ya estaba confirmado");
            return;
        }
        runStep(summary, "CUDA Voz IA avanzada", false, () -> {
            progress.update("Preparando PyTorch CUDA dentro del Python local...");
            XttsCudaPreparationReport preparation = services.prepareXttsPytorchCuda()
                    .prepare(root, message -> progress.update(friendlyProgress(message)));
            if (!preparation.success()) {
                throw new IllegalStateException(preparation.userMessage() + " Log: " + preparation.logFile());
            }
            XttsCudaSmokeReport result = services.runXttsCudaSmoke()
                    .run(settings, compute, root, message -> progress.update(friendlyProgress(message)));
            if (!result.gpuUsableForXtts()) {
                throw new IllegalStateException(result.userMessage());
            }
        });
    }

    private void preparePiper(SettingsApplicationServices services, OperationalSettings settings, Path root,
                              SetupProgress progress, SetupSummary summary) {
        PiperSetupReadinessReport readiness = services.inspectPiperSetupReadiness().inspect(settings, root);
        if (readiness.ready()) {
            summary.skipped("Voz local simple", "ya estaba lista");
            return;
        }
        runStep(summary, "Voz local simple", false, () -> {
            progress.update("Descargando Voz local simple...");
            PiperRuntimeDownloadReport report = services.downloadPiperPortableRuntime()
                    .download(settings, root, message -> progress.update(friendlyProgress(message)));
            if (!report.success()) {
                throw new IllegalStateException(report.userMessage() + " Pendiente: " + String.join(", ", report.failedFiles()));
            }
            PiperSetupReadinessReport after = services.inspectPiperSetupReadiness().inspect(settings, root);
            if (!after.ready()) {
                throw new IllegalStateException(after.userMessage() + " Faltan: " + String.join(", ", after.missingRequirements()));
            }
        });
    }

    private void prepareOcr(OperationalSettings settings, Path root, SetupProgress progress, SetupSummary summary) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        if (ocrReady(current, root)) {
            summary.skipped("OCR PDF local", "ya estaba listo");
            return;
        }
        if (current.ocr().tesseractRuntimeZipUrl().isBlank()) {
            summary.failed("OCR PDF local", false,
                    "sin URL de descarga; importa una carpeta Tesseract portable en Configuracion.");
            return;
        }
        runStep(summary, "OCR PDF local", false, () -> {
            progress.update("Descargando Tesseract OCR configurado...");
            TesseractRuntimeImportReport report = new DownloadTesseractPortableRuntimeUseCase()
                    .download(current, root, message -> progress.update(friendlyProgress(message)));
            if (!report.success()) {
                throw new IllegalStateException(report.userMessage());
            }
            if (!ocrReady(current, root)) {
                throw new IllegalStateException("OCR se descargo, pero no valido ejecutable e idiomas spa/eng.");
            }
        });
    }

    private void prepareFfmpeg(SettingsApplicationServices services, OperationalSettings settings, Path root,
                               SetupProgress progress, SetupSummary summary) {
        FfmpegRuntimeReport existing = embeddedFfmpegReport(root, services);
        if (existing.readyForFinalVideo()) {
            saveEmbeddedFfmpegSettings(services, settings, existing.ffmpegExecutable());
            summary.skipped("Video local / FFmpeg", "ya estaba embebido");
            return;
        }
        runStep(summary, "Video local / FFmpeg", true, () -> {
            progress.update("Descargando FFmpeg y FFprobe embebidos...");
            FfmpegRuntimeDownloadReport report = services.downloadFfmpegPortableRuntime()
                    .download(settings, root, message -> progress.update(friendlyProgress(message)));
            Path diagnostics = writeFfmpegDiagnostic(root, report);
            if (!report.success()) {
                throw new IllegalStateException(report.userMessage() + " Diagnostico: " + diagnostics);
            }
            FfmpegRuntimeReport runtime = report.runtimeAfter();
            if (runtime == null || !runtime.readyForFinalVideo()) {
                String detail = runtime == null ? "sin reporte final" : String.join(", ", runtime.warnings());
                throw new IllegalStateException("FFmpeg embebido no quedo listo para MP4: " + detail
                        + ". Diagnostico: " + diagnostics);
            }
            saveEmbeddedFfmpegSettings(services, settings, report.ffmpegExecutable());
        });
    }

    private void saveEmbeddedFfmpegSettings(SettingsApplicationServices services, OperationalSettings current, Path ffmpeg) {
        if (services == null || ffmpeg == null) {
            return;
        }
        OperationalSettings.VideoRenderSettings video = new OperationalSettings.VideoRenderSettings(
                ffmpeg.toAbsolutePath().normalize().toString(),
                current.video().resolutionPreset(),
                true,
                current.video().silentVisualBlockSeconds(),
                current.video().ffmpegDownloadUrl());
        OperationalSettings repaired = new OperationalSettings(
                current.readingDocument(),
                current.playbackBuffer(),
                current.tts(),
                video,
                current.imageGeneration(),
                current.frameGeneration(),
                current.compute(),
                current.ocr(),
                current.storage(),
                current.diagnostics());
        try {
            services.saveOperationalSettings().save(repaired);
        } catch (IOException ignored) {
            // Setup remains valid; the summary/probe will expose the embedded runtime if settings save fails.
        }
    }

    private static void runStep(SetupSummary summary, String label, boolean blocking, StepAction action) {
        try {
            action.run();
            summary.completed(label);
        } catch (Exception ex) {
            summary.failed(label, blocking, friendly(ex.getMessage()));
        }
    }

    private static boolean cudaApplies(OperationalSettings settings, ComputeEnvironmentReport compute) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        if (!current.compute().allowGpuForTts() || current.compute().policy() == ComputeDevicePolicy.CPU_ONLY) {
            return false;
        }
        String selected = current.compute().selectedDeviceId().toLowerCase(Locale.ROOT);
        if (selected.contains("intel") || selected.contains("amd")) {
            return false;
        }
        if (selected.contains("nvidia")) {
            return true;
        }
        if (compute == null) {
            return false;
        }
        return compute.devices().stream().anyMatch(EmbeddedDependencySetupAssistant::nvidiaDevice);
    }

    private static boolean nvidiaDevice(ComputeDeviceDescriptor device) {
        if (device == null || !device.gpu()) {
            return false;
        }
        String text = (device.id() + " " + device.displayName() + " " + device.vendor()).toLowerCase(Locale.ROOT);
        return text.contains("nvidia");
    }

    private static FfmpegRuntimeReport embeddedFfmpegReport(Path root, SettingsApplicationServices services) {
        FfmpegToolDiscovery discovery = new EmbeddedFfmpegLocator().locate(root, null);
        return services.inspectFfmpegRuntime().inspect(discovery);
    }

    private static boolean ocrReady(OperationalSettings settings, Path root) {
        TesseractRuntimeLocator locator = new TesseractRuntimeLocator();
        TesseractToolDiscovery discovery = locator.locate(settings, root);
        TesseractLanguageDiscovery languages = locator.inspectLanguages(discovery,
                (settings == null ? OperationalSettings.defaults() : settings).ocr().languages());
        return discovery.ready() && languages.ready();
    }

    private static Path writeFfmpegDiagnostic(Path root, FfmpegRuntimeDownloadReport report) {
        Path diagnostics = root.resolve("target/docupodcast-engine-setup/ffmpeg-setup-diagnostics.txt").normalize();
        try {
            Files.createDirectories(diagnostics.getParent());
            ArrayList<String> lines = new ArrayList<>();
            lines.add("FFmpeg setup diagnostics");
            lines.add("checkedAt=" + Instant.now());
            lines.add("success=" + (report != null && report.success()));
            if (report != null) {
                lines.add("downloadFile=" + report.downloadFile());
                lines.add("extractedFolder=" + report.extractedFolder());
                lines.add("targetFolder=" + report.targetFolder());
                lines.add("ffmpegExecutable=" + report.ffmpegExecutable());
                lines.add("ffprobeExecutable=" + report.ffprobeExecutable());
                lines.add("downloadedFiles=" + report.downloadedFiles());
                lines.add("copiedFiles=" + report.copiedFiles());
                lines.add("failedItems=" + report.failedItems());
                lines.add("userMessage=" + report.userMessage());
                if (report.runtimeAfter() != null) {
                    lines.add("encoders=" + report.runtimeAfter().encoders());
                    lines.add("warnings=" + report.runtimeAfter().warnings());
                }
            }
            Files.write(diagnostics, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Best-effort diagnostics.
        }
        return diagnostics;
    }

    private static OperationalSettings loadSettings(SettingsApplicationServices services) {
        try {
            return services.loadOperationalSettings().load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }

    private static Label wrappedLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setMinWidth(620);
        label.setPrefWidth(720);
        label.setMaxWidth(780);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    private static Window ownerOf(Node anchor) {
        return anchor == null || anchor.getScene() == null ? null : anchor.getScene().getWindow();
    }

    private static SetupProgress showProgress(Window owner, String title, String header, String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setDisable(true);
        dialog.setOnCloseRequest(event -> {
            if (close.isDisabled()) {
                event.consume();
            }
        });
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 18, 24));
        Label explanation = wrappedLabel(message);
        ProgressBar progress = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        progress.setMaxWidth(Double.MAX_VALUE);
        TextArea detail = new TextArea("Iniciando preparacion...");
        detail.setEditable(false);
        detail.setWrapText(true);
        detail.setPrefRowCount(5);
        detail.setPrefHeight(140);
        detail.setFocusTraversable(false);
        Label hint = wrappedLabel("Las descargas se guardan dentro de la carpeta del programa.");
        body.getChildren().addAll(explanation, progress, detail, hint);
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().setMinWidth(840);
        dialog.getDialogPane().setPrefWidth(900);
        DialogStyler.apply(dialog, owner);
        dialog.show();
        return new SetupProgress(dialog, progress, detail, hint, close);
    }

    private static String friendlyProgress(String message) {
        return SettingsTechnicalMessageHumanizer.embeddedDependencyProgress(message);
    }

    private static String friendly(String text) {
        return SettingsTechnicalMessageHumanizer.engineText(text);
    }

    private static String preparationDetail(XttsRuntimePreparationReport report) {
        if (report == null) {
            return "";
        }
        return " Reporte: " + report.setupReport();
    }

    @FunctionalInterface
    private interface StepAction {
        void run() throws Exception;
    }

    private record DependencySnapshot(
            boolean xttsReady,
            boolean piperReady,
            boolean ffmpegReady,
            boolean cudaReadyOrSkipped,
            boolean cudaApplies,
            boolean imageReady,
            boolean ocrReady
    ) {
        boolean ready() {
            return xttsReady && piperReady && ffmpegReady && cudaReadyOrSkipped && imageReady && ocrReady;
        }

        List<String> missingLabels() {
            ArrayList<String> labels = new ArrayList<>();
            if (!xttsReady) {
                labels.add("Voz IA avanzada / Python local / modelo");
            }
            if (cudaApplies && !cudaReadyOrSkipped) {
                labels.add("CUDA para Voz IA avanzada");
            }
            if (!piperReady) {
                labels.add("Voz local simple / runtime");
            }
            if (!ffmpegReady) {
                labels.add("Video local / FFmpeg y FFprobe embebidos");
            }
            if (!ocrReady) {
                labels.add("OCR PDF local / Tesseract portable + idiomas spa/eng");
            }
            if (!imageReady) {
                labels.add("Imagen IA teatral local / paquete de modelos (requiere confirmacion aparte)");
            }
            return labels;
        }
    }

    private static final class SetupSummary {
        private final ArrayList<String> completed = new ArrayList<>();
        private final ArrayList<String> skipped = new ArrayList<>();
        private final ArrayList<String> failed = new ArrayList<>();
        private int blockingFailures;

        void completed(String label) {
            completed.add(label);
        }

        void skipped(String label, String reason) {
            skipped.add(label + " (" + reason + ")");
        }

        void failed(String label, boolean blocking, String message) {
            failed.add(label + ": " + message);
            if (blocking) {
                blockingFailures++;
            }
        }

        int blockingFailures() {
            return blockingFailures;
        }

        String message() {
            StringBuilder text = new StringBuilder("Preparacion finalizada.");
            if (!completed.isEmpty()) {
                text.append("\n\nListo:\n- ").append(String.join("\n- ", completed));
            }
            if (!skipped.isEmpty()) {
                text.append("\n\nOmitido:\n- ").append(String.join("\n- ", skipped));
            }
            if (!failed.isEmpty()) {
                text.append("\n\nPendiente o fallo:\n- ").append(String.join("\n- ", failed));
            }
            if (failed.isEmpty()) {
                text.append("\n\nTodas las dependencias recomendadas quedaron listas o ya estaban preparadas.");
            }
            return text.toString();
        }
    }

    private static final class SetupProgress {
        private final Dialog<Void> dialog;
        private final ProgressBar progressBar;
        private final TextArea detail;
        private final Label hint;
        private final Button closeButton;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private final long startedMillis = System.currentTimeMillis();

        SetupProgress(Dialog<Void> dialog, ProgressBar progressBar, TextArea detail, Label hint, Button closeButton) {
            this.dialog = Objects.requireNonNull(dialog, "dialog");
            this.progressBar = Objects.requireNonNull(progressBar, "progressBar");
            this.detail = Objects.requireNonNull(detail, "detail");
            this.hint = Objects.requireNonNull(hint, "hint");
            this.closeButton = Objects.requireNonNull(closeButton, "closeButton");
        }

        void update(String message) {
            Runnable action = () -> {
                detail.setText(message == null || message.isBlank() ? "Operacion en curso..." : message);
                hint.setText("Ultima actualizacion: " + LocalTime.now().withNano(0)
                        + ". Tiempo transcurrido: " + elapsedLabel(System.currentTimeMillis() - startedMillis) + ".");
            };
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        }

        void finish(String message) {
            finished.set(true);
            progressBar.setProgress(1.0);
            detail.setText(message == null || message.isBlank() ? "Operacion completada." : message);
            hint.setText("Completado. Puedes cerrar esta ventana.");
            closeButton.setDisable(false);
        }

        void fail(String message) {
            finished.set(true);
            progressBar.setProgress(0.0);
            detail.setText(message == null || message.isBlank() ? "La preparacion no se completo." : message);
            hint.setText("Error o interrupcion. Revisa el detalle y vuelve a intentar.");
            closeButton.setDisable(false);
        }

        void close() {
            if (Platform.isFxApplicationThread()) {
                dialog.close();
            } else {
                Platform.runLater(dialog::close);
            }
        }

        private static String elapsedLabel(long millis) {
            long seconds = Math.max(0L, millis / 1000L);
            long minutes = seconds / 60L;
            long remainingSeconds = seconds % 60L;
            return minutes + "m " + remainingSeconds + "s";
        }
    }
}
