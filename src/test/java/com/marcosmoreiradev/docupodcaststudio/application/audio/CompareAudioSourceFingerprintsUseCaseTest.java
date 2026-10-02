package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSourceFingerprint;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionRevisionRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CompareAudioSourceFingerprintsUseCaseTest {
    @Test
    void invalidatesOnlyTheChangedRegionUnit() {
        AudioSourceFingerprint first = fingerprint("R-1", 1, "uno", "voz-a");
        AudioSourceFingerprint second = fingerprint("R-2", 1, "dos", "voz-b");
        List<AudioSegmentSnapshot> persisted = List.of(
                snapshot("A", first), snapshot("B", second));
        List<AudioGenerationUnit> current = List.of(
                unit("A", first),
                unit("B", fingerprint("R-2", 2, "dos corregido", "voz-b")));

        AudioSourceInvalidationReport report =
                new CompareAudioSourceFingerprintsUseCase().compare(current, persisted);

        assertEquals(List.of("A"), report.reusableSegmentIds());
        assertEquals(List.of("B"), report.staleSegmentIds());
    }

    private static AudioSourceFingerprint fingerprint(String id, long revision, String text, String voice) {
        return AudioSourceFingerprint.pdf(List.of(new PdfRegionRevisionRef(id, 1, revision)),
                text, voice, "normalización-v1");
    }

    private static AudioSegmentSnapshot snapshot(String id, AudioSourceFingerprint fingerprint) {
        return new AudioSegmentSnapshot(id, id, AudioSegmentStatus.COMPLETED,
                id + ".wav", 1.0, 1, "", fingerprint);
    }

    private static AudioGenerationUnit unit(String id, AudioSourceFingerprint fingerprint) {
        return new AudioGenerationUnit(id, id, "texto", id,
                "voz", "neutral", List.of(), fingerprint);
    }
}
