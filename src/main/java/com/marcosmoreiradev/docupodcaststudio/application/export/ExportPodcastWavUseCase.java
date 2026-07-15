package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PunctuationPausePolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Exports a deliverable podcast WAV, copying an existing final or concatenating segment WAVs. */
public final class ExportPodcastWavUseCase {
    private final PcmWavConcatenator concatenator = new PcmWavConcatenator();
    private final PunctuationPausePolicy pausePolicy = new PunctuationPausePolicy();

    public PodcastFinalWavExportResult exportLatest(List<AudioJobSnapshot> jobs, Path projectDirectory, Path target) throws IOException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Objects.requireNonNull(target, "target");
        AudioJobSnapshot latest = latestExportableJob(jobs);
        if (latest == null) {
            throw new IllegalStateException("No hay audio final ni segmentos WAV completos disponibles para exportar.");
        }
        Path normalizedTarget = ExportTargetPathPolicy.ensureWavExtension(target).toAbsolutePath().normalize();
        Path projectRoot = projectDirectory.toAbsolutePath().normalize();
        if (!latest.finalAudioPath().isBlank()) {
            Path source = safeResolve(projectRoot, latest.finalAudioPath());
            if (!Files.exists(source) || !Files.isRegularFile(source)) {
                throw new IOException("No existe el audio final registrado: " + latest.finalAudioPath());
            }
            if (normalizedTarget.getParent() != null) {
                Files.createDirectories(normalizedTarget.getParent());
            }
            Files.copy(source, normalizedTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            PodcastFinalWavExportResult result = new PodcastFinalWavExportResult(normalizedTarget,
                    reportPath(normalizedTarget), latest.jobId(), false, latest.totalSegments(), durationFromSegments(latest),
                    Files.size(normalizedTarget), List.of(latest.finalAudioPath()));
            writeReport(result);
            return result;
        }
        List<AudioSegmentSnapshot> completed = completedSegmentsForFinal(latest);
        List<Path> clips = completed.stream()
                .map(AudioSegmentSnapshot::audioRelativePath)
                .map(relative -> safeResolveUnchecked(projectRoot, relative))
                .toList();
        PcmWavConcatenator.ConcatenationResult concatenation = concatenator.concatenate(clips, normalizedTarget);
        PodcastFinalWavExportResult result = new PodcastFinalWavExportResult(normalizedTarget, reportPath(normalizedTarget),
                latest.jobId(), true, concatenation.segmentCount(), concatenation.durationSeconds(), concatenation.sizeBytes(),
                completed.stream().map(AudioSegmentSnapshot::audioRelativePath).toList());
        writeReport(result);
        return result;
    }


    /**
     * Exports the audio material described by a playback manifest.
     *
     * <p>T92 uses this for manifests where user-selected audio clips are effective cues.
     * It expects project-relative WAV clips. Non-WAV or incompatible PCM files fail with a clear
     * concatenation error instead of silently producing a wrong export.</p>
     */
    public PodcastFinalWavExportResult exportPlaybackManifest(PlaybackManifest manifest, Path projectDirectory, Path target) throws IOException {
        return exportPlaybackManifest(manifest, projectDirectory, target, 1.0);
    }

    public PodcastFinalWavExportResult exportPlaybackManifest(PlaybackManifest manifest,
                                                              Path projectDirectory,
                                                              Path target,
                                                              double playbackRate) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Objects.requireNonNull(target, "target");
        if (manifest.emptyManifest()) {
            throw new IllegalStateException("No hay cues de playback con audio para exportar.");
        }
        List<PlaybackCue> audioCues = manifest.cues().stream()
                .filter(PlaybackCue::hasAudio)
                .toList();
        List<String> sourceRelativePaths = audioCues.stream()
                .map(PlaybackCue::audioRelativePath)
                .toList();
        if (sourceRelativePaths.isEmpty()) {
            throw new IllegalStateException("El manifest de playback no contiene archivos de audio exportables.");
        }
        Path normalizedTarget = ExportTargetPathPolicy.ensureWavExtension(target).toAbsolutePath().normalize();
        Path projectRoot = projectDirectory.toAbsolutePath().normalize();
        List<Path> clips = sourceRelativePaths.stream()
                .map(relative -> safeResolveUnchecked(projectRoot, relative))
                .toList();
        PcmWavConcatenator.ConcatenationResult concatenation = concatenator.concatenate(
                clips,
                silenceMillisAfterEachCue(audioCues, playbackRate),
                normalizedTarget);
        PodcastFinalWavExportResult result = new PodcastFinalWavExportResult(normalizedTarget, reportPath(normalizedTarget),
                manifest.sourceJobId(), true, concatenation.segmentCount(), concatenation.durationSeconds(), concatenation.sizeBytes(),
                sourceRelativePaths);
        writeReport(result);
        return result;
    }

    private List<Long> silenceMillisAfterEachCue(List<PlaybackCue> cues, double playbackRate) {
        if (cues == null || cues.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<Long> pauses = new java.util.ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            PlaybackCue next = i + 1 < cues.size() ? cues.get(i + 1) : null;
            pauses.add(pausePolicy.interCuePauseMillis(cues.get(i), next, playbackRate, 0L));
        }
        return List.copyOf(pauses);
    }

    public boolean canExport(List<AudioJobSnapshot> jobs) {
        return latestExportableJob(jobs) != null;
    }

    private AudioJobSnapshot latestExportableJob(List<AudioJobSnapshot> jobs) {
        return jobs == null ? null : jobs.stream()
                .filter(ExportPodcastWavUseCase::exportable)
                .max(Comparator.comparing(AudioJobSnapshot::updatedAt))
                .orElse(null);
    }

    private static boolean exportable(AudioJobSnapshot job) {
        if (job == null) {
            return false;
        }
        if (!job.finalAudioPath().isBlank()) {
            return true;
        }
        return !completedSegmentsForFinal(job).isEmpty();
    }

    private static List<AudioSegmentSnapshot> completedSegmentsForFinal(AudioJobSnapshot job) {
        if (job.state() != AudioJobState.COMPLETED || job.failedSegmentCount() > 0) {
            return List.of();
        }
        List<AudioSegmentSnapshot> completed = job.segments().stream()
                .filter(AudioSegmentSnapshot::completed)
                .filter(segment -> !segment.audioRelativePath().isBlank())
                .toList();
        if (completed.isEmpty()) {
            return List.of();
        }
        if (job.totalSegments() > 0 && completed.size() < job.totalSegments()) {
            return List.of();
        }
        return completed;
    }

    private static double durationFromSegments(AudioJobSnapshot job) {
        return job.segments().stream().mapToDouble(AudioSegmentSnapshot::durationSeconds).sum();
    }

    private static Path safeResolve(Path projectRoot, String relativePath) throws IOException {
        Path resolved = projectRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectRoot)) {
            throw new IOException("Ruta de audio fuera del proyecto: " + relativePath);
        }
        return resolved;
    }

    private static Path safeResolveUnchecked(Path projectRoot, String relativePath) {
        Path resolved = projectRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectRoot)) {
            throw new IllegalArgumentException("Ruta de audio fuera del proyecto: " + relativePath);
        }
        return resolved;
    }

    private static Path reportPath(Path wavTarget) {
        String filename = wavTarget.getFileName() == null ? "podcast.wav" : wavTarget.getFileName().toString();
        String base = filename.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")
                ? filename.substring(0, filename.length() - 4)
                : filename;
        Path parent = wavTarget.getParent();
        return parent == null ? Path.of(base + ".export-report.md") : parent.resolve(base + ".export-report.md");
    }

    private static void writeReport(PodcastFinalWavExportResult result) throws IOException {
        if (result.reportFile().getParent() != null) {
            Files.createDirectories(result.reportFile().getParent());
        }
        StringBuilder out = new StringBuilder();
        out.append("# Exportación de podcast WAV\n\n");
        out.append("- Job fuente: ").append(result.jobId()).append("\n");
        out.append("- Modo: ").append(result.modeLabel()).append("\n");
        out.append("- Segmentos: ").append(result.segmentCount()).append("\n");
        out.append("- Duración estimada: ").append(String.format(java.util.Locale.ROOT, "%.2f", result.durationSeconds())).append(" segundos\n");
        out.append("- Tamaño WAV: ").append(result.sizeBytes()).append(" bytes\n");
        out.append("- Archivo final: `").append(result.targetFile()).append("`\n\n");
        out.append("## Fuentes\n\n");
        for (String source : result.sourceClipPaths()) {
            out.append("- `").append(source).append("`\n");
        }
        Files.writeString(result.reportFile(), out.toString(), StandardCharsets.UTF_8);
    }
}
