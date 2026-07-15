package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.nio.file.Path;
import java.util.List;

/** Result of exporting a deliverable final audio file in WAV, MP3 or AAC. */
public record PodcastFinalAudioExportResult(
        Path targetFile,
        Path reportFile,
        Path sourceWavFile,
        AudioExportFormat format,
        String jobId,
        boolean compressedFromWav,
        int segmentCount,
        double durationSeconds,
        long sizeBytes,
        List<String> sourceClipPaths
) {
    public PodcastFinalAudioExportResult {
        targetFile = targetFile == null ? Path.of("") : targetFile;
        reportFile = reportFile == null ? Path.of("") : reportFile;
        sourceWavFile = sourceWavFile == null ? Path.of("") : sourceWavFile;
        format = format == null ? AudioExportFormat.WAV : format;
        jobId = jobId == null ? "" : jobId.strip();
        segmentCount = Math.max(0, segmentCount);
        durationSeconds = Math.max(0.0, durationSeconds);
        sizeBytes = Math.max(0L, sizeBytes);
        sourceClipPaths = sourceClipPaths == null ? List.of() : List.copyOf(sourceClipPaths);
    }

    public String modeLabel() {
        if (format == AudioExportFormat.WAV) {
            return "WAV final exportado";
        }
        return format.displayName() + " final comprimido desde WAV de trabajo";
    }

    public String humanSummary() {
        return "Audio " + format.displayName() + " exportado: " + targetFile
                + " · " + modeLabel()
                + " · fuentes " + segmentCount
                + " · tamaño " + sizeBytes + " bytes";
    }

    public static PodcastFinalAudioExportResult fromWav(PodcastFinalWavExportResult wav) {
        return new PodcastFinalAudioExportResult(
                wav.targetFile(), wav.reportFile(), wav.targetFile(), AudioExportFormat.WAV, wav.jobId(),
                false, wav.segmentCount(), wav.durationSeconds(), wav.sizeBytes(), wav.sourceClipPaths());
    }
}
