package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelSetupProgressListener;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageProfile;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadDecision;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadPreflight;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provider-neutral facade for dependency preparation actions exposed by the
 * settings workspace.
 */
public final class DependencyPreparationService {
    private final SettingsApplicationServices services;

    DependencyPreparationService(SettingsApplicationServices services) {
        this.services = Objects.requireNonNull(services, "settings services");
    }

    public String prepareRecommended(OperationalSettings settings,
                                     Path applicationRoot,
                                     ModelSetupProgressListener progress) {
        String voice = downloadLocalVoice(settings, applicationRoot, progress);
        String video = downloadVideoTools(settings, applicationRoot, progress);
        return voice + " " + video;
    }

    public String downloadLocalVoice(OperationalSettings settings,
                                     Path applicationRoot,
                                     ModelSetupProgressListener progress) {
        return downloadLocalVoice(settings, applicationRoot, progress, ManagedDownloadDecision.USE_EXISTING);
    }

    public String downloadLocalVoice(OperationalSettings settings,
                                     Path applicationRoot,
                                     ModelSetupProgressListener progress,
                                     ManagedDownloadDecision decision) {
        ManagedDownloadPreflight preflight = preflightLocalVoice(settings, applicationRoot);
        if (reuseOrCancel(preflight, decision)) return reuseMessage(preflight);
        return services.downloadPiperPortableRuntime()
                .download(settings, applicationRoot, progress,
                        decision == ManagedDownloadDecision.REDOWNLOAD)
                .userMessage();
    }

    public String downloadVideoTools(OperationalSettings settings,
                                     Path applicationRoot,
                                     ModelSetupProgressListener progress) {
        return downloadVideoTools(settings, applicationRoot, progress, ManagedDownloadDecision.USE_EXISTING);
    }

    public String downloadVideoTools(OperationalSettings settings,
                                     Path applicationRoot,
                                     ModelSetupProgressListener progress,
                                     ManagedDownloadDecision decision) {
        ManagedDownloadPreflight preflight = preflightVideoTools(settings, applicationRoot);
        if (reuseOrCancel(preflight, decision)) return reuseMessage(preflight);
        return services.downloadFfmpegPortableRuntime()
                .download(settings, applicationRoot, progress,
                        decision == ManagedDownloadDecision.REDOWNLOAD)
                .userMessage();
    }

    public String downloadVisualModel(OperationalSettings settings,
                                      Path applicationRoot,
                                      ModelSetupProgressListener progress) {
        return downloadVisualModel(settings, applicationRoot, progress, ManagedDownloadDecision.USE_EXISTING);
    }

    public String downloadVisualModel(OperationalSettings settings,
                                      Path applicationRoot,
                                      ModelSetupProgressListener progress,
                                      ManagedDownloadDecision decision) {
        ManagedDownloadPreflight preflight = preflightVisualModel(settings, applicationRoot);
        if (reuseOrCancel(preflight, decision)) return reuseMessage(preflight);
        return services.downloadLocalTheatreImagePackage()
                .download(settings, applicationRoot, progress,
                        decision == ManagedDownloadDecision.REDOWNLOAD)
                .userMessage();
    }

    public ManagedDownloadPreflight preflightLocalVoice(
            OperationalSettings settings, Path applicationRoot) {
        Path root = normalizedRoot(applicationRoot);
        var report = services.inspectPiperSetupReadiness().inspect(settings, root);
        Path location = report.piperExecutable();
        Path partial = RuntimeArtifactPaths.fromRoot(root).piperRoot()
                .resolve("piper-runtime.download.zip");
        ManagedDownloadState state = report.ready() ? ManagedDownloadState.VALID
                : Files.isRegularFile(partial) ? ManagedDownloadState.PARTIAL
                : anyRegular(report.piperExecutable(), report.voiceModel(), report.voiceMetadata())
                ? ManagedDownloadState.INVALID : ManagedDownloadState.MISSING;
        return new ManagedDownloadPreflight(
                "Piper y voz española",
                state,
                location,
                0L,
                combinedSize(report.piperExecutable(), report.voiceModel(), report.voiceMetadata()),
                "",
                "",
                "MIT / modelos según proveedor",
                effective(settings).tts().piperRuntimeZipUrl(),
                report.userMessage());
    }

    public ManagedDownloadPreflight preflightVideoTools(
            OperationalSettings settings, Path applicationRoot) {
        Path root = normalizedRoot(applicationRoot);
        var discovery = new EmbeddedFfmpegLocator().locate(root, null);
        var report = services.inspectFfmpegRuntime().inspect(discovery);
        Path partial = RuntimeArtifactPaths.fromRoot(root).ffmpegRoot()
                .resolve("downloads/video-local-runtime.zip.download");
        ManagedDownloadState state = report.readyForFinalVideo() ? ManagedDownloadState.VALID
                : Files.isRegularFile(partial) ? ManagedDownloadState.PARTIAL
                : anyRegular(report.ffmpegExecutable(), report.ffprobeExecutable())
                ? ManagedDownloadState.INVALID : ManagedDownloadState.MISSING;
        return new ManagedDownloadPreflight(
                "FFmpeg y FFprobe",
                state,
                RuntimeArtifactPaths.fromRoot(root).ffmpegBinDirectory(),
                0L,
                combinedSize(report.ffmpegExecutable(), report.ffprobeExecutable()),
                "",
                "",
                "GPL/LGPL según compilación",
                effective(settings).video().ffmpegDownloadUrl(),
                state == ManagedDownloadState.VALID
                        ? "FFmpeg y FFprobe están instalados y verificados."
                        : String.join(" ", report.warnings()));
    }

    public ManagedDownloadPreflight preflightVisualModel(
            OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = effective(settings);
        Path root = normalizedRoot(applicationRoot);
        ImageModelPackageProfile profile =
                ImageModelPackageProfile.fromPreset(current.imageGeneration().preset());
        Path model = root.resolve("models/image").resolve(profile.checkpointName()).normalize();
        Path partial = model.resolveSibling(model.getFileName() + ".part");
        var readiness = services.inspectLocalTheatreImageSetupReadiness().inspect(current, root);
        ManagedDownloadState state = readiness.modelUsable() ? ManagedDownloadState.VALID
                : Files.isRegularFile(partial) ? ManagedDownloadState.PARTIAL
                : Files.isRegularFile(model) ? ManagedDownloadState.INVALID : ManagedDownloadState.MISSING;
        return new ManagedDownloadPreflight(
                profile.displayName(),
                state,
                model,
                profile.approximateBytes(),
                size(model),
                "",
                "",
                profile.accessPolicy().name(),
                profile.downloadUrl(),
                readiness.userMessage());
    }

    public String importLocalOcr(Path sourceFolder,
                                 Path applicationRoot,
                                 ModelSetupProgressListener progress) {
        return services.importTesseractRuntimeFolder()
                .importFrom(sourceFolder, applicationRoot, progress)
                .userMessage();
    }

    public String configureImportedVisualModel(ImageModelPackageProfile profile, Path root) throws IOException {
        if (profile != ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT
                && profile != ImageModelPackageProfile.HIGH_QUALITY_FLUX) throw new IOException("Modelo no admitido en esta importación.");
        var bundle = profile == ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT
                ? com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxModelBundle.inspectKontext(root)
                : com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxModelBundle.inspect(root);
        if (!bundle.ready()) return "Importación parcial; faltan: " + String.join(", ", bundle.missingComponents());
        for (Path file : java.util.List.of(bundle.model(), bundle.vae(), bundle.clipL(), bundle.t5())) {
            com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxComponentImportUseCase.validateTensorContainer(file);
        }
        OperationalSettings current = services.loadOperationalSettings().load();
        var image = current.imageGeneration();
        var configured = new com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings(
                image.engineMode(), image.baseUrl(), image.devicePolicy(), profile.presetId(), profile.checkpointName(),
                image.adaptersDirectory(), image.timeoutSeconds(), image.lowVram(), image.memoryProfile(), image.maxAttempts());
        var report = services.saveOperationalSettings().save(new OperationalSettings(
                current.readingDocument(), current.playbackBuffer(), current.tts(), current.video(), configured,
                current.imageSuperResolution(), current.mediaEngines(), current.frameGeneration(), current.compute(),
                current.ocr(), current.storage(), current.diagnostics()));
        if (!report.errors().isEmpty()) throw new IOException(String.join(" ", report.errors()));
        return "Archivos verificados estructuralmente y perfil configurado. Falta comprobar el runtime y realizar la prueba del motor.";
    }

    private static boolean reuseOrCancel(
            ManagedDownloadPreflight preflight, ManagedDownloadDecision decision) {
        ManagedDownloadDecision current = decision == null ? ManagedDownloadDecision.CANCEL : decision;
        if (current == ManagedDownloadDecision.CANCEL) {
            throw new IllegalStateException("La preparación fue cancelada.");
        }
        if (preflight.valid() && current == ManagedDownloadDecision.USE_EXISTING) return true;
        if ((preflight.state() == ManagedDownloadState.INVALID
                || preflight.state() == ManagedDownloadState.PARTIAL)
                && current != ManagedDownloadDecision.REDOWNLOAD) {
            throw new IllegalStateException("El recurso local no se reemplazará sin la decisión explícita "
                    + "de reparar o volver a descargar.");
        }
        return false;
    }

    private static String reuseMessage(ManagedDownloadPreflight preflight) {
        return preflight.resourceId() + " ya está instalado y validado; se reutilizó sin conexión.";
    }

    private static OperationalSettings effective(OperationalSettings settings) {
        return settings == null ? OperationalSettings.defaults() : settings;
    }

    private static Path normalizedRoot(Path root) {
        return root == null ? Path.of(".").toAbsolutePath().normalize()
                : root.toAbsolutePath().normalize();
    }

    private static boolean anyRegular(Path... paths) {
        if (paths == null) return false;
        for (Path path : paths) if (path != null && Files.isRegularFile(path)) return true;
        return false;
    }

    private static long combinedSize(Path... paths) {
        long total = 0L;
        if (paths == null) return total;
        for (Path path : paths) total += size(path);
        return total;
    }

    private static long size(Path path) {
        try {
            return path != null && Files.isRegularFile(path) ? Files.size(path) : 0L;
        } catch (IOException ignored) {
            return 0L;
        }
    }
}
