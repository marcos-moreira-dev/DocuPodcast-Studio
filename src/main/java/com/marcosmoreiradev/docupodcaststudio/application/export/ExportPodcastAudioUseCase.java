package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;

import com.marcosmoreiradev.docupodcaststudio.application.errors.ExternalProcessFailedException;
import com.marcosmoreiradev.docupodcaststudio.application.errors.ExternalProcessTimeoutException;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Exports final user-facing audio.
 *
 * <p>DocuPodcast keeps its internal cache and resumable chunks as WAV. MP3/AAC are produced only
 * as final delivery formats through the local video/audio component so the working cache remains
 * lossless and debuggable.</p>
 */
public final class ExportPodcastAudioUseCase {
    private static final Duration COMPRESSION_TIMEOUT = Duration.ofMinutes(20);

    private final ExportPodcastWavUseCase wavExporter;
    private final EmbeddedFfmpegLocator locator;
    private final FfmpegRuntimeProbeUseCase probe;
    private final ExternalProcessRunner runner;

    public ExportPodcastAudioUseCase() {
        this(new ExportPodcastWavUseCase(), new EmbeddedFfmpegLocator(),
                new FfmpegRuntimeProbeUseCase(ExternalProcessRunner.unavailable("ExportPodcastAudioUseCase")),
                ExternalProcessRunner.unavailable("ExportPodcastAudioUseCase"));
    }

    public ExportPodcastAudioUseCase(ExportPodcastWavUseCase wavExporter) {
        this(wavExporter, new EmbeddedFfmpegLocator(),
                new FfmpegRuntimeProbeUseCase(ExternalProcessRunner.unavailable("ExportPodcastAudioUseCase")),
                ExternalProcessRunner.unavailable("ExportPodcastAudioUseCase"));
    }

    public ExportPodcastAudioUseCase(ExportPodcastWavUseCase wavExporter,
                                     EmbeddedFfmpegLocator locator,
                                     FfmpegRuntimeProbeUseCase probe) {
        this(wavExporter, locator, probe, ExternalProcessRunner.unavailable("ExportPodcastAudioUseCase"));
    }

    public ExportPodcastAudioUseCase(ExportPodcastWavUseCase wavExporter,
                                     EmbeddedFfmpegLocator locator,
                                     FfmpegRuntimeProbeUseCase probe,
                                     ExternalProcessRunner runner) {
        this.wavExporter = Objects.requireNonNull(wavExporter, "wavExporter");
        this.locator = Objects.requireNonNull(locator, "locator");
        this.probe = Objects.requireNonNull(probe, "probe");
        this.runner = runner == null ? ExternalProcessRunner.unavailable("ExportPodcastAudioUseCase") : runner;
    }

    public PodcastFinalAudioExportResult exportLatest(List<AudioJobSnapshot> jobs,
                                                       Path projectDirectory,
                                                       Path target,
                                                       Path applicationRoot,
                                                       Path configuredFfmpeg) throws IOException {
        AudioExportFormat format = AudioExportFormat.fromTarget(target);
        if (format == AudioExportFormat.WAV) {
            return PodcastFinalAudioExportResult.fromWav(wavExporter.exportLatest(jobs, projectDirectory, target));
        }
        Path normalizedTarget = format.normalizeTarget(target).toAbsolutePath().normalize();
        Path workDirectory = workDirectoryFor(normalizedTarget);
        Files.createDirectories(workDirectory);
        Path sourceWav = workDirectory.resolve(baseName(normalizedTarget) + ".source.wav");
        PodcastFinalWavExportResult wav = wavExporter.exportLatest(jobs, projectDirectory, sourceWav);
        return compress(wav, normalizedTarget, format, applicationRoot, configuredFfmpeg);
    }

    public PodcastFinalAudioExportResult exportPlaybackManifest(PlaybackManifest manifest,
                                                                 Path projectDirectory,
                                                                 Path target,
                                                                 Path applicationRoot,
                                                                 Path configuredFfmpeg) throws IOException {
        return exportPlaybackManifest(manifest, projectDirectory, target, applicationRoot, configuredFfmpeg, 1.0);
    }

    public PodcastFinalAudioExportResult exportPlaybackManifest(PlaybackManifest manifest,
                                                                 Path projectDirectory,
                                                                 Path target,
                                                                 Path applicationRoot,
                                                                 Path configuredFfmpeg,
                                                                 double playbackRate) throws IOException {
        AudioExportFormat format = AudioExportFormat.fromTarget(target);
        if (format == AudioExportFormat.WAV) {
            return PodcastFinalAudioExportResult.fromWav(
                    wavExporter.exportPlaybackManifest(manifest, projectDirectory, target, playbackRate));
        }
        Path normalizedTarget = format.normalizeTarget(target).toAbsolutePath().normalize();
        Path workDirectory = workDirectoryFor(normalizedTarget);
        Files.createDirectories(workDirectory);
        Path sourceWav = workDirectory.resolve(baseName(normalizedTarget) + ".source.wav");
        PodcastFinalWavExportResult wav = wavExporter.exportPlaybackManifest(manifest, projectDirectory, sourceWav, playbackRate);
        return compress(wav, normalizedTarget, format, applicationRoot, configuredFfmpeg);
    }

    private PodcastFinalAudioExportResult compress(PodcastFinalWavExportResult wav,
                                                   Path target,
                                                   AudioExportFormat format,
                                                   Path applicationRoot,
                                                   Path configuredFfmpeg) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        FfmpegToolDiscovery discovery = locator.locate(applicationRoot == null ? Path.of(".") : applicationRoot, configuredFfmpeg);
        FfmpegRuntimeReport runtime = probe.inspect(discovery);
        if (!runtime.ffmpegReady()) {
            throw new IOException("No se encontró Video local para comprimir audio. Prepara Video local en Configuración o exporta WAV.");
        }
        run(compressionCommand(runtime.ffmpegExecutable(), wav.targetFile(), target, format));
        if (!Files.isRegularFile(target) || Files.size(target) <= 0L) {
            throw new IOException("La compresión de audio terminó sin crear un archivo válido: " + target);
        }
        PodcastFinalAudioExportResult result = new PodcastFinalAudioExportResult(target, reportPath(target), wav.targetFile(), format,
                wav.jobId(), true, wav.segmentCount(), wav.durationSeconds(), Files.size(target), wav.sourceClipPaths());
        writeReport(result, runtime);
        return result;
    }

    private static List<String> compressionCommand(Path ffmpeg, Path sourceWav, Path target, AudioExportFormat format) {
        ArrayList<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        command.add("-i");
        command.add(sourceWav.toString());
        command.add("-vn");
        command.add("-codec:a");
        if (format == AudioExportFormat.MP3) {
            command.add("libmp3lame");
        } else if (format == AudioExportFormat.AAC) {
            command.add("aac");
        } else {
            command.add("copy");
        }
        if (format.compressed()) {
            command.add("-b:a");
            command.add("192k");
        }
        command.add(target.toString());
        return List.copyOf(command);
    }

    private void run(List<String> command) throws IOException {
        try {
            ExternalProcessResult result = runner.run(ExternalProcessRequest.of(command, "ffmpeg-final-audio", COMPRESSION_TIMEOUT)
                    .redirectingErrorStream());
            if (result.timedOut()) {
                throw new ExternalProcessTimeoutException("Compresión de audio detenida",
                        "La compresión de audio tardó demasiado y fue detenida.",
                        result.commandAudit(), result.combinedOutputTail(), "");
            }
            if (result.exitCode() != 0) {
                throw new ExternalProcessFailedException("Video local falló",
                        "Video local devolvió código " + result.exitCode() + " al comprimir audio.",
                        result.exitCode(), result.commandAudit(), result.combinedOutputTail(), "", null);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Compresión de audio interrumpida.", ex);
        }
    }

    private static Path workDirectoryFor(Path target) {
        Path parent = target.getParent() == null ? Path.of(".") : target.getParent();
        return parent.resolve(".docupodcast-audio-work").toAbsolutePath().normalize();
    }

    private static Path reportPath(Path target) {
        Path parent = target.getParent();
        return (parent == null ? Path.of(baseName(target) + ".audio-export-report.md")
                : parent.resolve(baseName(target) + ".audio-export-report.md")).toAbsolutePath().normalize();
    }

    private static String baseName(Path target) {
        String filename = target.getFileName() == null ? "podcast" : target.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot <= 0 ? filename : filename.substring(0, dot);
    }

    private static void writeReport(PodcastFinalAudioExportResult result, FfmpegRuntimeReport runtime) throws IOException {
        if (result.reportFile().getParent() != null) {
            Files.createDirectories(result.reportFile().getParent());
        }
        StringBuilder out = new StringBuilder();
        out.append("# Exportación de audio final\n\n");
        out.append("- Formato: ").append(result.format().displayName()).append("\n");
        out.append("- Job fuente: ").append(result.jobId()).append("\n");
        out.append("- Modo: ").append(result.modeLabel()).append("\n");
        out.append("- Segmentos/fuentes: ").append(result.segmentCount()).append("\n");
        out.append("- Duración estimada: ").append(String.format(Locale.ROOT, "%.2f", result.durationSeconds())).append(" segundos\n");
        out.append("- Tamaño final: ").append(result.sizeBytes()).append(" bytes\n");
        out.append("- Archivo final: `").append(result.targetFile()).append("`\n");
        out.append("- WAV de trabajo: `").append(result.sourceWavFile()).append("`\n");
        out.append("- Herramienta local: `").append(runtime.ffmpegExecutable()).append("`\n\n");
        out.append("## Fuentes\n\n");
        for (String source : result.sourceClipPaths()) {
            out.append("- `").append(source).append("`\n");
        }
        Files.writeString(result.reportFile(), out.toString(), StandardCharsets.UTF_8);
    }
}
