package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Decides which completed WAV units remain reusable after document edits. */
public final class CompareAudioSourceFingerprintsUseCase {
    public AudioSourceInvalidationReport compare(List<AudioGenerationUnit> current,
                                                 List<AudioSegmentSnapshot> persisted) {
        Map<String, AudioSegmentSnapshot> byId = new LinkedHashMap<>();
        for (AudioSegmentSnapshot segment : persisted == null ? List.<AudioSegmentSnapshot>of() : persisted) {
            byId.put(segment.segmentId(), segment);
        }
        ArrayList<String> reusable = new ArrayList<>();
        ArrayList<String> stale = new ArrayList<>();
        ArrayList<String> missing = new ArrayList<>();
        for (AudioGenerationUnit unit : current == null ? List.<AudioGenerationUnit>of() : current) {
            AudioSegmentSnapshot previous = byId.get(unit.id());
            if (previous == null) missing.add(unit.id());
            else if (previous.reusableFor(unit.sourceFingerprint())) reusable.add(unit.id());
            else stale.add(unit.id());
        }
        return new AudioSourceInvalidationReport(reusable, stale, missing);
    }
}
