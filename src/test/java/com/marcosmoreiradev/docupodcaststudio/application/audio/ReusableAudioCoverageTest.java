package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSourceFingerprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReusableAudioCoverageTest {
    @TempDir Path root;

    @Test
    void unitesExactAudioAcrossJobsAndReportsMissingStaleAndMissingFile() throws Exception {
        AudioGenerationUnit readyA = unit("U-A", "alpha");
        AudioGenerationUnit readyB = unit("U-B", "beta");
        AudioGenerationUnit stale = unit("U-C", "current");
        AudioGenerationUnit missingFile = unit("U-D", "delta");
        AudioGenerationUnit missing = unit("U-E", "epsilon");
        writeWav("jobs/J-1/audio/U-A.wav");
        writeWav("jobs/J-2/audio/U-B.wav");
        AudioJobSnapshot first = job("J-1", List.of(
                completed(readyA, "jobs/J-1/audio/U-A.wav"),
                completed(unit("U-C", "old"), "jobs/J-1/audio/U-C.wav")));
        AudioJobSnapshot second = job("J-2", List.of(
                completed(readyB, "jobs/J-2/audio/U-B.wav"),
                completed(missingFile, "jobs/J-2/audio/U-D.wav")));

        ReusableAudioCoverage.Report report = new ReusableAudioCoverage().resolve(
                List.of(readyA, readyB, stale, missingFile, missing),
                List.of(first, second), root);

        assertEquals(ReusableAudioCoverage.State.READY, report.entry("U-A").orElseThrow().state());
        assertEquals(ReusableAudioCoverage.State.READY, report.entry("U-B").orElseThrow().state());
        assertEquals(ReusableAudioCoverage.State.STALE, report.entry("U-C").orElseThrow().state());
        assertEquals(ReusableAudioCoverage.State.MISSING_FILE, report.entry("U-D").orElseThrow().state());
        assertEquals(ReusableAudioCoverage.State.MISSING, report.entry("U-E").orElseThrow().state());
        assertEquals(2, report.readyAudio().size());
    }

    @Test
    void explicitManualAudioHasPrecedenceEvenWhenItsLegacyFingerprintIsUntraceable() throws Exception {
        AudioGenerationUnit current = unit("U-1", "new text");
        writeWav("jobs/MANUAL/audio/U-1-manual.wav");
        AudioSegmentSnapshot manual = new AudioSegmentSnapshot("U-1", "Manual",
                AudioSegmentStatus.COMPLETED, "jobs/MANUAL/audio/U-1-manual.wav",
                1.0, 1, "", AudioSourceFingerprint.untraceable());

        ReusableAudioCoverage.Report report = new ReusableAudioCoverage().resolve(
                List.of(current), List.of(job("MANUAL", List.of(manual))), root);

        assertTrue(report.complete());
        assertTrue(report.entry("U-1").orElseThrow().manual());
    }

    @Test
    void cancelledPersistedUnitWithoutWavIsInvalidRatherThanStale() {
        AudioGenerationUnit current = unit("U-1", "texto");
        AudioSegmentSnapshot cancelled = new AudioSegmentSnapshot(
                current.id(), current.title(), AudioSegmentStatus.CANCELLED,
                "", 0.0, 1, "", current.sourceFingerprint());

        ReusableAudioCoverage.Report report = new ReusableAudioCoverage().resolve(
                List.of(current), List.of(job("CANCELLED", List.of(cancelled))), root);

        assertEquals(ReusableAudioCoverage.State.INVALID,
                report.entry("U-1").orElseThrow().state());
    }

    @Test
    void exportReuses27AndRequestsOnlyThreeMissingAndTwoStale() throws Exception {
        java.util.ArrayList<AudioGenerationUnit> current = new java.util.ArrayList<>();
        java.util.ArrayList<AudioSegmentSnapshot> persisted = new java.util.ArrayList<>();
        for (int index = 1; index <= 32; index++) {
            AudioGenerationUnit unit = unit("U-%02d".formatted(index), "texto-" + index);
            current.add(unit);
            if (index <= 27) {
                String path = "jobs/OLD/audio/" + unit.id() + ".wav";
                writeWav(path);
                persisted.add(completed(unit, path));
            } else if (index >= 31) {
                persisted.add(completed(unit(unit.id(), "texto-antiguo"),
                        "jobs/OLD/audio/" + unit.id() + ".wav"));
            }
        }
        AudioJobSnapshot oldJob = job("OLD", persisted);

        ReusableAudioCoverage.Report first = new ReusableAudioCoverage().resolve(
                current, List.of(oldJob), root);

        assertEquals(27, first.readyAudio().size());
        assertEquals(5, first.missingOrStale().size());
        assertEquals(3, first.entries().stream()
                .filter(entry -> entry.state() == ReusableAudioCoverage.State.MISSING).count());
        assertEquals(2, first.entries().stream()
                .filter(entry -> entry.state() == ReusableAudioCoverage.State.STALE).count());

        java.util.ArrayList<AudioSegmentSnapshot> generatedGaps = new java.util.ArrayList<>();
        for (ReusableAudioCoverage.Entry gap : first.missingOrStale()) {
            String path = "jobs/GAPS/audio/" + gap.unit().id() + ".wav";
            writeWav(path);
            generatedGaps.add(completed(gap.unit(), path));
        }
        List<AudioJobSnapshot> afterPartialJob = List.of(
                oldJob, job("GAPS", generatedGaps));
        ReusableAudioCoverage.Report afterFirstExport =
                new ReusableAudioCoverage().resolve(current, afterPartialJob, root);
        ReusableAudioCoverage.Report secondExport =
                new ReusableAudioCoverage().resolve(current, afterPartialJob, root);

        assertEquals(32, afterFirstExport.readyAudio().size());
        assertEquals(0, afterFirstExport.missingOrStale().size(),
                "la primera exportación deja cobertura completa");
        assertEquals(0, secondExport.missingOrStale().size(),
                "la segunda exportación implica cero llamadas TTS");
    }

    @Test
    void combinedExportCoverageIsIdempotentAcrossNewExportJobs() throws Exception {
        AudioGenerationUnit first = unit("U-1", "alpha");
        AudioGenerationUnit second = unit("U-2", "beta");
        writeWav("jobs/J-1/audio/U-1.wav");
        writeWav("jobs/J-2/audio/U-2.wav");
        List<AudioJobSnapshot> jobs = List.of(
                job("J-1", List.of(completed(first, "jobs/J-1/audio/U-1.wav"))),
                job("J-2", List.of(completed(second, "jobs/J-2/audio/U-2.wav"))));

        AudioCoverageSnapshotAssembler.Result export1 =
                new AudioCoverageSnapshotAssembler().assemble(
                        List.of(first, second), jobs, root);
        AudioCoverageSnapshotAssembler.Result export2 =
                new AudioCoverageSnapshotAssembler().assemble(
                        List.of(first, second), jobs, root);

        assertTrue(export1.readyForPreflight());
        assertTrue(export2.readyForPreflight());
        assertEquals(0, export1.coverage().missingOrStale().size());
        assertEquals(0, export2.coverage().missingOrStale().size());
        assertEquals(2, export2.exportJobs().getFirst().segments().size());
        assertEquals(2, export1.composedSegments());
        assertEquals(0, export2.composedSegments());
        assertEquals(2, export2.reusedSegments());
        assertTrue(Files.isRegularFile(root.resolve(export2.manifestRelativePath())));
        assertEquals(export1.acousticRevision(), export2.acousticRevision());
        assertNotEquals(export1.correlationId(), export2.correlationId());
    }

    @Test
    void passiveInspectionNeverMaterializesMissingCompositions() throws Exception {
        AudioGenerationUnit first = unit("U-1", "alpha");
        writeWav("jobs/J-1/audio/U-1.wav");
        List<AudioJobSnapshot> jobs = List.of(job("J-1", List.of(
                completed(first, "jobs/J-1/audio/U-1.wav"))));
        AudioCoverageSnapshotAssembler assembler = new AudioCoverageSnapshotAssembler();
        Path compositionRoot = root.resolve("jobs/segment-audio-cache");

        AudioCoverageSnapshotAssembler.PassiveInspection before = assembler.inspect(
                List.of(first), jobs, root);

        assertTrue(before.coverage().complete());
        assertFalse(before.compositionsReady());
        assertEquals(List.of("U-1"), before.missingCompositionSegmentIds());
        assertFalse(Files.exists(compositionRoot),
                "readiness must not create composition directories or files");

        assertTrue(assembler.assemble(List.of(first), jobs, root).readyForPreflight());
        AudioCoverageSnapshotAssembler.PassiveInspection after = assembler.inspect(
                List.of(first), jobs, root);
        assertTrue(after.readyForPreflight());
    }

    @Test
    void changingExactlyOneSpokenInputMakesExactlyOneUnitStale() throws Exception {
        AudioGenerationUnit first = unit("U-1", "alpha");
        AudioGenerationUnit second = unit("U-2", "beta");
        writeWav("jobs/OLD/audio/U-1.wav");
        writeWav("jobs/OLD/audio/U-2.wav");
        AudioJobSnapshot old = job("OLD", List.of(
                completed(first, "jobs/OLD/audio/U-1.wav"),
                completed(second, "jobs/OLD/audio/U-2.wav")));

        ReusableAudioCoverage.Report report = new ReusableAudioCoverage().resolve(
                List.of(first, unit("U-2", "beta corregido")), List.of(old), root);

        assertEquals(1, report.readyAudio().size());
        assertEquals(1, report.entries().stream().filter(entry ->
                entry.state() == ReusableAudioCoverage.State.STALE).count());
        assertEquals("U-2", report.missingOrStale().getFirst().unit().id());
    }

    @Test
    void multipleTtsChunksBecomeOneManifestedSegmentAssetWithoutRegeneration()
            throws Exception {
        AudioGenerationUnit first = new AudioGenerationUnit("SEG-1-U001", "uno",
                "primera parte", "SEG-1", "VOC", "STY", List.of(),
                AudioSourceFingerprint.generic("segmento completo", "VOC|STY", "prep"));
        AudioGenerationUnit second = new AudioGenerationUnit("SEG-1-U002", "dos",
                "segunda parte", "SEG-1", "VOC", "STY", List.of(),
                first.sourceFingerprint());
        writeRealWav("jobs/J/audio/SEG-1-U001.wav", (byte) 1);
        writeRealWav("jobs/J/audio/SEG-1-U002.wav", (byte) 2);
        List<AudioJobSnapshot> jobs = List.of(job("J", List.of(
                completed(first, "jobs/J/audio/SEG-1-U001.wav"),
                completed(second, "jobs/J/audio/SEG-1-U002.wav"))));

        AudioCoverageSnapshotAssembler.Result firstExport =
                new AudioCoverageSnapshotAssembler().assemble(
                        List.of(first, second), jobs, root);
        AudioCoverageSnapshotAssembler.Result secondExport =
                new AudioCoverageSnapshotAssembler().assemble(
                        List.of(first, second), jobs, root);

        assertTrue(firstExport.readyForPreflight());
        assertEquals(1, firstExport.exportJobs().getFirst().segments().size());
        assertEquals("SEG-1", firstExport.exportJobs().getFirst()
                .segments().getFirst().segmentId());
        assertEquals(1, firstExport.composedSegments());
        assertEquals(0, secondExport.composedSegments());
        assertEquals(1, secondExport.reusedSegments());
        AudioSegmentSnapshot resolved = secondExport.exportJobs().getFirst()
                .segments().getFirst();
        assertEquals(first.sourceFingerprint().voiceConfigurationSha256(),
                resolved.sourceFingerprint().voiceConfigurationSha256(),
                "composition must preserve the authorized effective voice identity");
        String manifest = Files.readString(root.resolve(secondExport.manifestRelativePath()));
        assertTrue(manifest.contains("\"version\": 2"));
        assertTrue(manifest.contains(resolved.sourceFingerprint().voiceConfigurationSha256()));
        assertTrue(manifest.contains("\"fileSha256\""));
    }

    @Test
    void oneEffectiveVoiceChangeInvalidatesOnlyItsUnit() throws Exception {
        AudioGenerationUnit first = unit("U-1", "alpha");
        AudioGenerationUnit second = unit("U-2", "beta");
        writeWav("jobs/OLD/audio/U-1.wav");
        writeWav("jobs/OLD/audio/U-2.wav");
        AudioJobSnapshot old = job("OLD", List.of(
                completed(first, "jobs/OLD/audio/U-1.wav"),
                completed(second, "jobs/OLD/audio/U-2.wav")));
        AudioGenerationUnit changedVoice = second.withSourceFingerprint(
                second.sourceFingerprint().withVoiceConfiguration("VOC-OTHER|STY"));

        ReusableAudioCoverage.Report report = new ReusableAudioCoverage().resolve(
                List.of(first, changedVoice), List.of(old), root);

        assertEquals(1, report.readyAudio().size());
        assertEquals(1, report.missingOrStale().size());
        assertEquals("U-2", report.missingOrStale().getFirst().unit().id());
        assertEquals(ReusableAudioCoverage.State.STALE,
                report.missingOrStale().getFirst().state());
        assertEquals("VOICE_CONFIGURATION_CHANGED",
                report.missingOrStale().getFirst().mismatchReason());
        assertNotEquals(second.sourceFingerprint().voiceConfigurationSha256(),
                changedVoice.sourceFingerprint().voiceConfigurationSha256());
    }

    private AudioGenerationUnit unit(String id, String text) {
        return new AudioGenerationUnit(id, id, text, id, "VOC", "STY",
                List.of(), AudioSourceFingerprint.generic(text, "VOC|STY", "prep"));
    }

    private AudioSegmentSnapshot completed(AudioGenerationUnit unit, String path) {
        return new AudioSegmentSnapshot(unit.id(), unit.title(), AudioSegmentStatus.COMPLETED,
                path, 1.0, 1, "", unit.sourceFingerprint());
    }

    private AudioJobSnapshot job(String id, List<AudioSegmentSnapshot> segments) {
        Instant now = Instant.now();
        return new AudioJobSnapshot(id, "Doc", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, segments.size(), segments.size(),
                0, 1.0, "", "", 0, "", "jobs/" + id,
                "", "", segments, now, now);
    }

    private void writeWav(String relative) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[64]);
    }

    private void writeRealWav(String relative, byte sample) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        byte[] pcm = new byte[1600];
        java.util.Arrays.fill(pcm, sample);
        var format = new javax.sound.sampled.AudioFormat(16000, 16, 1, true, false);
        try (var stream = new javax.sound.sampled.AudioInputStream(
                new java.io.ByteArrayInputStream(pcm), format,
                pcm.length / format.getFrameSize())) {
            javax.sound.sampled.AudioSystem.write(stream,
                    javax.sound.sampled.AudioFileFormat.Type.WAVE, file.toFile());
        }
    }
}
