package com.marcosmoreiradev.docupodcaststudio.presentation.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * UI projection of one audio generation job row.
 *
 * <p>The row deliberately combines active runtime DTOs and persisted snapshots into the
 * same shape so the Audio workspace can render a real queue instead of unrelated flat lists.</p>
 */
public record AudioJobRow(
        String jobId,
        String documentName,
        String stateLabel,
        String stageLabel,
        String segmentCounterLabel,
        String progressLabel,
        double progress,
        String currentSegmentLabel,
        String etaLabel,
        String message,
        String outputDirectory,
        String finalAudioPath,
        String manifestPath,
        boolean active,
        boolean running,
        boolean resumable,
        boolean completed,
        boolean failed,
        List<String> segmentLines
) {
    public AudioJobRow {
        jobId = normalize(jobId);
        documentName = blankTo(documentName, "Proyecto DocuPodcast");
        stateLabel = normalize(stateLabel);
        stageLabel = normalize(stageLabel);
        segmentCounterLabel = normalize(segmentCounterLabel);
        progressLabel = normalize(progressLabel);
        progress = clamp(progress);
        currentSegmentLabel = normalize(currentSegmentLabel);
        etaLabel = normalize(etaLabel);
        message = normalize(message);
        outputDirectory = normalize(outputDirectory);
        finalAudioPath = normalize(finalAudioPath);
        manifestPath = normalize(manifestPath);
        segmentLines = segmentLines == null ? List.of() : List.copyOf(segmentLines);
    }

    public static AudioJobRow active(AudioJobStatusDto status) {
        AudioJobStatusDto dto = status == null ? AudioJobStatusDto.idle() : status;
        String current = dto.currentSegmentId().isBlank()
                ? "Sin segmento activo"
                : dto.currentSegmentId() + (dto.currentSegmentTitle().isBlank() ? "" : " · " + dto.currentSegmentTitle());
        return new AudioJobRow(
                dto.jobId().isBlank() ? "AUDIO-IDLE" : dto.jobId(),
                dto.documentName(),
                dto.state().displayName(),
                dto.stage().displayName(),
                dto.segmentCounterLabel(),
                dto.progressPercentLabel(),
                dto.progress(),
                current,
                dto.etaLabel(),
                dto.message(),
                dto.outputDirectory(),
                dto.finalAudioPath(),
                dto.manifestPath(),
                !dto.jobId().isBlank(),
                dto.running(),
                false,
                dto.completed(),
                dto.failed(),
                List.of(current, dto.statusLine())
        );
    }

    public static AudioJobRow persisted(AudioJobSnapshot snapshot, Path projectDirectory, String activeJobId) {
        Objects.requireNonNull(snapshot, "snapshot");
        String output = projectDirectory == null ? snapshot.jobRelativeDirectory()
                : projectDirectory.toAbsolutePath().normalize().resolve(snapshot.jobRelativeDirectory()).normalize().toString();
        String current = snapshot.currentSegmentId().isBlank()
                ? "Sin segmento activo"
                : snapshot.currentSegmentId() + (snapshot.currentSegmentTitle().isBlank() ? "" : " · " + snapshot.currentSegmentTitle());
        List<String> segmentLines = snapshot.segments().stream()
                .map(AudioJobRow::segmentLine)
                .toList();
        return new AudioJobRow(
                snapshot.jobId(),
                snapshot.documentName(),
                snapshot.state().displayName(),
                snapshot.stage().displayName(),
                snapshot.completedSegments() + "/" + snapshot.totalSegments() + " segmentos",
                String.format(Locale.ROOT, "%.0f%%", snapshot.progress() * 100.0),
                snapshot.progress(),
                current,
                etaLabel(snapshot.estimatedRemainingSeconds(), snapshot.state().running()),
                snapshot.message(),
                output,
                snapshot.finalAudioPath(),
                snapshot.manifestPath(),
                snapshot.jobId().equals(activeJobId),
                snapshot.state().running(),
                snapshot.resumable(),
                snapshot.state().name().equals("COMPLETED"),
                snapshot.state().name().equals("FAILED"),
                segmentLines
        );
    }

    public String headline() {
        String activeMarker = active ? "ACTIVO · " : "";
        return activeMarker + jobId + " · " + stateLabel + " · " + segmentCounterLabel + " · " + progressLabel;
    }

    public String supportLine() {
        String recovery = resumable ? " · reanudable" : "";
        String finalAudio = finalAudioPath.isBlank() ? "" : " · final " + finalAudioPath;
        return stageLabel + " · " + currentSegmentLabel + " · " + etaLabel + recovery + finalAudio;
    }

    public String actionHint() {
        if (running) {
            return "Puede cancelarse desde la barra de acciones.";
        }
        if (resumable) {
            return "Puede reanudarse conservando segmentos completados.";
        }
        if (completed) {
            return "Listo para playback/exportación si el manifest está disponible.";
        }
        if (failed) {
            return "Revisa diagnóstico y reintenta desde la lectura preparada.";
        }
        return "Sin acción requerida.";
    }

    private static String segmentLine(AudioSegmentSnapshot segment) {
        String audio = segment.audioRelativePath().isBlank() ? "sin audio" : segment.audioRelativePath();
        String error = segment.errorMessage().isBlank() ? "" : " · " + segment.errorMessage();
        return segment.segmentId() + " · " + segment.status().displayName()
                + " · intentos " + segment.attempts()
                + " · " + audio + error;
    }

    private static String etaLabel(long seconds, boolean running) {
        if (!running || seconds <= 0) {
            return running ? "ETA calculándose" : "Sin ETA";
        }
        return durationLabel(seconds) + " restantes aprox.";
    }

    private static String durationLabel(long totalSeconds) {
        long value = Math.max(0L, totalSeconds);
        long hours = value / 3600L;
        long minutes = (value % 3600L) / 60L;
        long rest = value % 60L;
        if (hours > 0L) {
            return hours + " h " + minutes + " min " + rest + " s";
        }
        if (minutes > 0L) {
            return minutes + " min " + rest + " s";
        }
        return rest + " s";
    }

    private static String blankTo(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
