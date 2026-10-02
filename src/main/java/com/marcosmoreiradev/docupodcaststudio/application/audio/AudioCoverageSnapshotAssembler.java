package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Builds the immutable, cross-job audio view consumed by export preflight/render. */
public final class AudioCoverageSnapshotAssembler {
    private final ReusableAudioCoverage coverage = new ReusableAudioCoverage();
    private final SegmentAudioCompositionStore compositions =
            new SegmentAudioCompositionStore();

    public Result assemble(List<AudioGenerationUnit> current,
                           List<AudioJobSnapshot> persisted,
                           Path projectDirectory) {
        return assemble(current, persisted, projectDirectory,
                "DOCEXP-" + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 16).toUpperCase(java.util.Locale.ROOT));
    }

    /** Reads current chunk and composition coverage without materializing derivatives. */
    public PassiveInspection inspect(List<AudioGenerationUnit> current,
                                     List<AudioJobSnapshot> persisted,
                                     Path projectDirectory) {
        List<AudioGenerationUnit> safeCurrent = current == null
                ? List.of() : List.copyOf(current);
        ReusableAudioCoverage.Report report = coverage.resolve(safeCurrent, persisted,
                Objects.requireNonNull(projectDirectory, "projectDirectory"));
        SegmentAudioCompositionStore.Inspection composition = report.complete()
                ? compositions.inspect(safeCurrent, report, projectDirectory)
                : new SegmentAudioCompositionStore.Inspection(
                safeCurrent.stream().map(unit -> unit.sourceSegmentId().isBlank()
                        ? unit.id() : unit.sourceSegmentId()).distinct().toList(), false);
        return new PassiveInspection(report, composition.missingSegmentIds(),
                composition.complete());
    }

    public Result assemble(List<AudioGenerationUnit> current,
                           List<AudioJobSnapshot> persisted,
                           Path projectDirectory,
                           String correlationId) {
        List<AudioGenerationUnit> safeCurrent = current == null ? List.of() : List.copyOf(current);
        String correlation = Objects.toString(correlationId, "").strip();
        if (correlation.isBlank()) {
            correlation = "DOCEXP-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 16).toUpperCase(java.util.Locale.ROOT);
        }
        String revision = acousticRevision(safeCurrent);
        ReusableAudioCoverage.Report report = coverage.resolve(safeCurrent, persisted,
                Objects.requireNonNull(projectDirectory, "projectDirectory"));
        if (!report.complete()) return new Result(report, List.of(), 0, 0,
                List.of(), "", correlation, revision);
        SegmentAudioCompositionStore.Result materialized = compositions.materialize(
                safeCurrent, report, projectDirectory);
        int segmentCount = (int) safeCurrent.stream().map(unit -> unit.sourceSegmentId().isBlank()
                ? unit.id() : unit.sourceSegmentId()).distinct().count();
        if (!materialized.complete(segmentCount)) {
            return new Result(report, List.of(), materialized.composed(),
                    materialized.reused(), materialized.failures(),
                    materialized.manifestRelativePath(), correlation, revision);
        }
        Instant now = Instant.now();
        AudioJobSnapshot combined = new AudioJobSnapshot(
                correlation, "Audio vigente para exportacion " + revision,
                AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                materialized.segmentAudio().size(), segmentCount, 0, 1.0,
                "", "", 0L, "Cobertura acustica reunida sin regenerar WAV.",
                "jobs/segment-audio-cache", "",
                materialized.manifestRelativePath(), materialized.segmentAudio(), now, now);
        return new Result(report, List.of(combined), materialized.composed(),
                materialized.reused(), materialized.failures(),
                materialized.manifestRelativePath(), correlation, revision);
    }

    /** Stable revision of only the acoustic inputs represented by the request. */
    public static String acousticRevision(List<AudioGenerationUnit> current) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (AudioGenerationUnit unit : current == null
                    ? List.<AudioGenerationUnit>of() : current) {
                update(digest, unit.sourceFingerprint().textSha256());
                update(digest, unit.sourceFingerprint().voiceConfigurationSha256());
                update(digest, unit.sourceFingerprint().preprocessingSha256());
            }
            return "NARR-" + HexFormat.of().formatHex(digest.digest())
                    .substring(0, 16).toUpperCase(java.util.Locale.ROOT);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(Objects.toString(value, "").getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) 0);
    }

    public record Result(ReusableAudioCoverage.Report coverage,
                         List<AudioJobSnapshot> exportJobs,
                         int composedSegments,
                         int reusedSegments,
                         List<String> compositionFailures,
                         String manifestRelativePath,
                         String correlationId,
                         String acousticRevision) {
        public Result {
            coverage = Objects.requireNonNull(coverage, "coverage");
            exportJobs = exportJobs == null ? List.of() : List.copyOf(exportJobs);
            composedSegments = Math.max(0, composedSegments);
            reusedSegments = Math.max(0, reusedSegments);
            compositionFailures = compositionFailures == null
                    ? List.of() : List.copyOf(compositionFailures);
            manifestRelativePath = manifestRelativePath == null
                    ? "" : manifestRelativePath;
            correlationId = correlationId == null ? "" : correlationId.strip();
            acousticRevision = acousticRevision == null ? "" : acousticRevision.strip();
        }

        public boolean readyForPreflight() {
            return coverage.complete() && exportJobs.size() == 1
                    && compositionFailures.isEmpty();
        }
    }

    public record PassiveInspection(
            ReusableAudioCoverage.Report coverage,
            List<String> missingCompositionSegmentIds,
            boolean compositionsReady) {
        public PassiveInspection {
            coverage = Objects.requireNonNull(coverage, "coverage");
            missingCompositionSegmentIds = missingCompositionSegmentIds == null
                    ? List.of() : List.copyOf(missingCompositionSegmentIds);
        }

        public boolean readyForPreflight() {
            return coverage.complete() && compositionsReady
                    && missingCompositionSegmentIds.isEmpty();
        }
    }
}
