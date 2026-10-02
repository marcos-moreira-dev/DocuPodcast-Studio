package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Exact, file-backed audio coverage assembled across every persisted job. */
public final class ReusableAudioCoverage {
    public enum State { READY, MISSING, STALE, MISSING_FILE, INVALID }

    public record Entry(AudioGenerationUnit unit, State state,
                        AudioSegmentSnapshot audio, boolean manual) {
        public Entry {
            unit = Objects.requireNonNull(unit, "unit");
            state = Objects.requireNonNull(state, "state");
        }

        public boolean ready() { return state == State.READY && audio != null; }

        public String mismatchReason() {
            if (ready()) return "";
            if (audio == null) return state.name();
            var expected = unit.sourceFingerprint();
            var resolved = audio.sourceFingerprint();
            ArrayList<String> differences = new ArrayList<>();
            if (!expected.textSha256().equals(resolved.textSha256())) differences.add("TEXT_CHANGED");
            if (!expected.voiceConfigurationSha256().equals(resolved.voiceConfigurationSha256())) {
                differences.add("VOICE_CONFIGURATION_CHANGED");
            }
            if (!expected.preprocessingSha256().equals(resolved.preprocessingSha256())) {
                differences.add("ACOUSTIC_PREPROCESSING_CHANGED");
            }
            return differences.isEmpty() ? state.name() : String.join(",", differences);
        }
    }

    public record Report(List<Entry> entries) {
        public Report {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        public List<AudioSegmentSnapshot> readyAudio() {
            return entries.stream().filter(Entry::ready).map(Entry::audio).toList();
        }

        public List<Entry> missingOrStale() {
            return entries.stream().filter(entry -> !entry.ready()).toList();
        }

        public boolean complete() {
            return !entries.isEmpty() && entries.stream().allMatch(Entry::ready);
        }

        public boolean completeFrom(String unitId) {
            boolean found = false;
            for (Entry entry : entries) {
                if (entry.unit().id().equals(unitId)) found = true;
                if (found && !entry.ready()) return false;
            }
            return found;
        }

        public Optional<Entry> entry(String unitId) {
            if (unitId == null || unitId.isBlank()) return Optional.empty();
            return entries.stream().filter(entry -> entry.unit().id().equals(unitId.strip())).findFirst();
        }
    }

    public Report resolve(List<AudioGenerationUnit> current,
                          List<AudioJobSnapshot> snapshots,
                          Path projectDirectory) {
        Path root = Objects.requireNonNull(projectDirectory, "projectDirectory")
                .toAbsolutePath().normalize();
        Map<String, List<AudioSegmentSnapshot>> byUnit = candidates(snapshots);
        ArrayList<Entry> result = new ArrayList<>();
        for (AudioGenerationUnit unit : current == null
                ? List.<AudioGenerationUnit>of() : current) {
            List<AudioSegmentSnapshot> candidates = candidatesFor(unit.id(), byUnit);
            AudioSegmentSnapshot manual = candidates.stream()
                    .filter(AudioSegmentSnapshot::completed)
                    .filter(ReusableAudioCoverage::manual)
                    .filter(segment -> validFile(root, segment)).findFirst().orElse(null);
            if (manual != null) {
                result.add(new Entry(unit, State.READY, manual, true));
                continue;
            }
            AudioSegmentSnapshot exact = candidates.stream()
                    .filter(segment -> segment.reusableFor(unit.sourceFingerprint()))
                    .findFirst().orElse(null);
            if (exact != null && validFile(root, exact)) {
                result.add(new Entry(unit, State.READY, exact, false));
            } else if (exact != null) {
                result.add(new Entry(unit, State.MISSING_FILE, exact, false));
            } else if (candidates.stream().anyMatch(AudioSegmentSnapshot::completed)) {
                result.add(new Entry(unit, State.STALE, candidates.getFirst(), false));
            } else if (!candidates.isEmpty()) {
                result.add(new Entry(unit, State.INVALID, candidates.getFirst(), false));
            } else {
                result.add(new Entry(unit, State.MISSING, null, false));
            }
        }
        return new Report(result);
    }

    private static Map<String, List<AudioSegmentSnapshot>> candidates(
            List<AudioJobSnapshot> snapshots) {
        LinkedHashMap<String, List<AudioSegmentSnapshot>> result = new LinkedHashMap<>();
        if (snapshots == null) return result;
        snapshots.stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(AudioJobSnapshot::updatedAt).reversed())
                .forEach(job -> job.segments().forEach(segment -> {
                    ArrayList<AudioSegmentSnapshot> values = new ArrayList<>(
                            result.getOrDefault(segment.segmentId(), List.of()));
                    values.add(segment);
                    result.put(segment.segmentId(), List.copyOf(values));
                }));
        return result;
    }

    private static List<AudioSegmentSnapshot> candidatesFor(
            String unitId, Map<String, List<AudioSegmentSnapshot>> byUnit) {
        List<AudioSegmentSnapshot> exact = byUnit.getOrDefault(unitId, List.of());
        if (!exact.isEmpty()) return exact;
        String prefix = unitId + "-";
        return byUnit.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .flatMap(entry -> entry.getValue().stream()).toList();
    }

    private static boolean manual(AudioSegmentSnapshot segment) {
        return segment.audioRelativePath().toLowerCase(java.util.Locale.ROOT)
                .endsWith("-manual.wav");
    }

    private static boolean validFile(Path root, AudioSegmentSnapshot segment) {
        if (segment == null || segment.audioRelativePath().isBlank()) return false;
        Path file = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        try {
            return file.startsWith(root) && Files.isRegularFile(file) && Files.size(file) > 44L;
        } catch (java.io.IOException ex) {
            return false;
        }
    }
}
