package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeReport;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inspects whether the user-facing final audio export can start for a concrete target file.
 *
 * <p>This check lives in application code so menu/ribbon/workflow surfaces can share one
 * product truth: WAV uses existing project audio, while MP3/AAC additionally require Video
 * local/FFmpeg because the app compresses a temporary WAV into the final delivery file.</p>
 */
public final class InspectFinalAudioExportReadinessUseCase {
    private final ExportPodcastWavUseCase wavExporter;
    private final EmbeddedFfmpegLocator ffmpegLocator;
    private final FfmpegRuntimeProbeUseCase ffmpegProbe;

    public InspectFinalAudioExportReadinessUseCase() {
        this(new ExportPodcastWavUseCase(), new EmbeddedFfmpegLocator(),
                new FfmpegRuntimeProbeUseCase(ExternalProcessRunner.unavailable("InspectFinalAudioExportReadinessUseCase")));
    }

    public InspectFinalAudioExportReadinessUseCase(ExportPodcastWavUseCase wavExporter,
                                                   EmbeddedFfmpegLocator ffmpegLocator,
                                                   FfmpegRuntimeProbeUseCase ffmpegProbe) {
        this.wavExporter = Objects.requireNonNull(wavExporter, "wavExporter");
        this.ffmpegLocator = Objects.requireNonNull(ffmpegLocator, "ffmpegLocator");
        this.ffmpegProbe = Objects.requireNonNull(ffmpegProbe, "ffmpegProbe");
    }

    public ExportReadinessItem inspect(List<AudioJobSnapshot> jobs,
                                       PlaybackManifest manifest,
                                       Path targetFile,
                                       Path applicationRoot,
                                       Path configuredFfmpeg) {
        AudioExportFormat format = targetFile == null ? AudioExportFormat.WAV : AudioExportFormat.fromTarget(targetFile);
        ArrayList<String> missing = new ArrayList<>();
        if (!hasExportableAudio(jobs, manifest)) {
            missing.add("Genera audio o prepara una cola de playback con WAVs antes de exportar audio final.");
        }
        if (format.compressed()) {
            FfmpegToolDiscovery discovery = ffmpegLocator.locate(applicationRoot == null ? Path.of(".") : applicationRoot, configuredFfmpeg);
            FfmpegRuntimeReport report = ffmpegProbe.inspect(discovery);
            if (!report.ffmpegReady()) {
                missing.add("Prepara Video local/FFmpeg para comprimir audio a " + format.displayName() + ".");
            }
        }
        if (!missing.isEmpty()) {
            return ExportReadinessItem.blocked(
                    ExportableArtifactKind.PODCAST_WAV,
                    DocuPodcastExportFormat.fromAudio(format),
                    targetHint(format),
                    missing,
                    List.of("La exportación final no sintetiza fragmentos faltantes ni instala FFmpeg durante el guardado."));
        }
        ArrayList<String> evidence = new ArrayList<>();
        evidence.add("Hay audio exportable para construir el archivo final.");
        if (format.compressed()) {
            evidence.add("Video local/FFmpeg está disponible para comprimir a " + format.displayName() + ".");
        } else {
            evidence.add("WAV final no requiere compresión ni Video local.");
        }
        return ExportReadinessItem.exportable(
                ExportableArtifactKind.PODCAST_WAV,
                DocuPodcastExportFormat.fromAudio(format),
                targetHint(format),
                evidence,
                List.of("El archivo final usa solo audio ya existente en el proyecto; no genera voces nuevas en esta acción."));
    }

    private boolean hasExportableAudio(List<AudioJobSnapshot> jobs, PlaybackManifest manifest) {
        if (manifest != null && !manifest.emptyManifest()
                && manifest.cues().stream().anyMatch(PlaybackCue::hasAudio)) {
            return true;
        }
        return wavExporter.canExport(jobs);
    }

    private static String targetHint(AudioExportFormat format) {
        return "audio-final" + format.extension();
    }
}
