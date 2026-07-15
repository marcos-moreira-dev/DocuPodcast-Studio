package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Project-level library of voices, characters, performance styles and reference samples. */
public record VoiceLibrary(
        String id,
        List<VoiceProfile> voices,
        List<CharacterProfile> characters,
        List<PerformanceStyle> styles,
        List<VoiceReferenceSampleSet> referenceSampleSets,
        Instant updatedAt,
        String notes
) {
    public VoiceLibrary {
        id = tokenOrDefault(id, "VOICE-LIBRARY-001");
        voices = voices == null ? List.of() : List.copyOf(voices);
        characters = characters == null ? List.of() : List.copyOf(characters);
        styles = styles == null ? List.of() : List.copyOf(styles);
        referenceSampleSets = referenceSampleSets == null ? List.of() : List.copyOf(referenceSampleSets);
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        notes = notes == null ? "" : notes.strip();
        validateUnique("voice", voices.stream().map(VoiceProfile::id).toList());
        validateUnique("character", characters.stream().map(CharacterProfile::id).toList());
        validateUnique("style", styles.stream().map(PerformanceStyle::id).toList());
        validateUnique("reference sample set", referenceSampleSets.stream().map(VoiceReferenceSampleSet::voiceProfileId).toList());
        validateReferences(voices, characters, styles);
        validateSampleSetReferences(voices, referenceSampleSets);
    }

    public VoiceLibrary(String id,
                        List<VoiceProfile> voices,
                        List<CharacterProfile> characters,
                        List<PerformanceStyle> styles,
                        Instant updatedAt,
                        String notes) {
        this(id, voices, characters, styles, List.of(), updatedAt, notes);
    }

    public static VoiceLibrary defaults() {
        return new VoiceLibrary(
                "VOICE-LIBRARY-001",
                defaultVoiceProfiles(),
                List.of(CharacterProfile.narrator()),
                List.of(
                        PerformanceStyle.neutral(),
                        PerformanceStyle.warm(),
                        PerformanceStyle.happy(),
                        PerformanceStyle.cheerful(),
                        PerformanceStyle.sad(),
                        PerformanceStyle.calm(),
                        PerformanceStyle.serious(),
                        PerformanceStyle.dramatic()),
                OfficialAdvancedVoicePresetCatalog.sampleSets(),
                Instant.now(),
                "Biblioteca inicial: voz simple prediseñada, voces avanzadas prediseñadas oficiales y estilos base."
        );
    }

    public Optional<VoiceProfile> voiceById(String voiceId) {
        String target = normalize(voiceId);
        return voices.stream().filter(voice -> voice.id().equals(target)).findFirst();
    }

    public Optional<CharacterProfile> characterById(String characterId) {
        String target = normalize(characterId);
        return characters.stream().filter(character -> character.id().equals(target)).findFirst();
    }

    public Optional<PerformanceStyle> styleById(String styleId) {
        String target = normalize(styleId);
        return styles.stream().filter(style -> style.id().equals(target)).findFirst();
    }

    public Optional<VoiceReferenceSampleSet> referenceSampleSetByVoiceId(String voiceProfileId) {
        String target = normalize(voiceProfileId);
        return referenceSampleSets.stream()
                .filter(set -> set.voiceProfileId().equals(target))
                .findFirst();
    }

    public VoiceLibrary withVoice(VoiceProfile voice) {
        LinkedHashMap<String, VoiceProfile> updated = new LinkedHashMap<>();
        voices.forEach(existing -> updated.put(existing.id(), existing));
        updated.put(voice.id(), voice);
        return new VoiceLibrary(id, List.copyOf(updated.values()), characters, styles, referenceSampleSets, Instant.now(), notes);
    }

    public VoiceLibrary withoutVoice(String voiceProfileId) {
        String target = normalize(voiceProfileId);
        List<VoiceProfile> updatedVoices = voices.stream()
                .filter(voice -> !voice.id().equals(target))
                .toList();
        List<VoiceReferenceSampleSet> updatedSamples = referenceSampleSets.stream()
                .filter(set -> !set.voiceProfileId().equals(target))
                .toList();
        return new VoiceLibrary(id, updatedVoices, characters, styles, updatedSamples, Instant.now(), notes);
    }

    public VoiceLibrary withCharacter(CharacterProfile character) {
        LinkedHashMap<String, CharacterProfile> updated = new LinkedHashMap<>();
        characters.forEach(existing -> updated.put(existing.id(), existing));
        updated.put(character.id(), character);
        return new VoiceLibrary(id, voices, List.copyOf(updated.values()), styles, referenceSampleSets, Instant.now(), notes);
    }

    public VoiceLibrary withStyle(PerformanceStyle style) {
        LinkedHashMap<String, PerformanceStyle> updated = new LinkedHashMap<>();
        styles.forEach(existing -> updated.put(existing.id(), existing));
        updated.put(style.id(), style);
        return new VoiceLibrary(id, voices, characters, List.copyOf(updated.values()), referenceSampleSets, Instant.now(), notes);
    }

    public VoiceLibrary withReferenceSampleSet(VoiceReferenceSampleSet sampleSet) {
        LinkedHashMap<String, VoiceReferenceSampleSet> updated = new LinkedHashMap<>();
        referenceSampleSets.forEach(existing -> updated.put(existing.voiceProfileId(), existing));
        updated.put(sampleSet.voiceProfileId(), sampleSet);
        return new VoiceLibrary(id, voices, characters, styles, List.copyOf(updated.values()), Instant.now(), notes);
    }

    public VoiceLibrary withoutReferenceSampleSet(String voiceProfileId) {
        String target = normalize(voiceProfileId);
        List<VoiceReferenceSampleSet> updated = referenceSampleSets.stream()
                .filter(set -> !set.voiceProfileId().equals(target))
                .toList();
        return new VoiceLibrary(id, voices, characters, styles, updated, Instant.now(), notes);
    }

    public VoiceLibrary withReferenceSample(VoiceReferenceSample sample) {
        VoiceReferenceSampleSet current = referenceSampleSetByVoiceId(sample.voiceProfileId())
                .orElseGet(() -> new VoiceReferenceSampleSet(sample.voiceProfileId(), List.of()));
        return withReferenceSampleSet(current.withSample(sample));
    }

    public boolean supportsSegmentVoice(String characterId, String voiceId, String styleId) {
        return characterById(characterId).isPresent() && voiceById(voiceId).isPresent() && styleById(styleId).isPresent();
    }

    private static void validateReferences(List<VoiceProfile> voices, List<CharacterProfile> characters, List<PerformanceStyle> styles) {
        LinkedHashSet<String> voiceIds = new LinkedHashSet<>();
        voices.forEach(voice -> voiceIds.add(voice.id()));
        LinkedHashSet<String> styleIds = new LinkedHashSet<>();
        styles.forEach(style -> styleIds.add(style.id()));
        for (CharacterProfile character : characters) {
            if (!voiceIds.contains(character.defaultVoiceProfileId())) {
                throw new IllegalArgumentException("Character " + character.id() + " references missing voice " + character.defaultVoiceProfileId());
            }
            if (!styleIds.contains(character.defaultPerformanceStyleId())) {
                throw new IllegalArgumentException("Character " + character.id() + " references missing style " + character.defaultPerformanceStyleId());
            }
        }
    }

    private static void validateSampleSetReferences(List<VoiceProfile> voices, List<VoiceReferenceSampleSet> referenceSampleSets) {
        LinkedHashSet<String> voiceIds = new LinkedHashSet<>();
        voices.forEach(voice -> voiceIds.add(voice.id()));
        for (VoiceReferenceSampleSet sampleSet : referenceSampleSets) {
            if (!voiceIds.contains(sampleSet.voiceProfileId())) {
                throw new IllegalArgumentException("Reference sample set references missing voice " + sampleSet.voiceProfileId());
            }
        }
    }

    private static List<VoiceProfile> defaultVoiceProfiles() {
        ArrayList<VoiceProfile> profiles = new ArrayList<>();
        profiles.add(VoiceProfile.predefinedNarrator());
        profiles.addAll(OfficialAdvancedVoicePresetCatalog.profiles());
        return List.copyOf(profiles);
    }

    private static void validateUnique(String label, List<String> ids) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String id : ids) {
            if (!unique.add(id)) {
                throw new IllegalArgumentException("Duplicate " + label + " id: " + id);
            }
        }
    }

    private static String tokenOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("id must not contain whitespace");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
