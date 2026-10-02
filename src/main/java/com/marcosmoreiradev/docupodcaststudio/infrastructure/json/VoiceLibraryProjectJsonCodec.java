package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.*;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.*;
import com.marcosmoreiradev.docupodcaststudio.domain.study.*;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonReadSupport.*;
import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonWriteSupport.*;

final class VoiceLibraryProjectJsonCodec implements ProjectJsonSectionCodec {
    static VoiceLibrary readVoiceLibrary(Map<String, Object> map) throws IOException {
        if (map.isEmpty()) {
            return VoiceLibrary.defaults();
        }
        String id = stringOrDefault(map.get("id"), VoiceLibrary.defaults().id());
        String notes = stringOrDefault(map.get("notes"), "");
        Instant updatedAt = map.get("updatedAt") == null ? Instant.now() : instant(map.get("updatedAt"), "voiceLibrary.updatedAt");
        List<VoiceProfile> voices = readVoiceProfiles(map.get("voices"));
        List<CharacterProfile> characters = readCharacters(map.get("characters"));
        List<PerformanceStyle> styles = readStyles(map.get("styles"));
        List<VoiceReferenceSampleSet> referenceSampleSets = readReferenceSampleSets(map.get("referenceSampleSets"));
        VoiceLibrary parsed = new VoiceLibrary(id, voices.isEmpty() ? VoiceLibrary.defaults().voices() : voices,
                characters.isEmpty() ? VoiceLibrary.defaults().characters() : characters,
                styles.isEmpty() ? VoiceLibrary.defaults().styles() : styles,
                referenceSampleSets, updatedAt, notes);
        return withOfficialAdvancedPresets(parsed);
    }

    static VoiceLibrary withOfficialAdvancedPresets(VoiceLibrary library) {
        VoiceLibrary updated = library;
        if (updated.voiceById("VOC-OWN-PLACEHOLDER").isPresent()) {
            updated = updated.withoutVoice("VOC-OWN-PLACEHOLDER");
        }
        for (VoiceProfile preset : OfficialAdvancedVoicePresetCatalog.profiles()) {
            if (updated.voiceById(preset.id()).isEmpty()) {
                updated = updated.withVoice(preset);
            }
        }
        for (VoiceReferenceSampleSet sampleSet : OfficialAdvancedVoicePresetCatalog.sampleSets()) {
            if (updated.referenceSampleSetByVoiceId(sampleSet.voiceProfileId()).isEmpty()) {
                updated = updated.withReferenceSampleSet(sampleSet);
            }
        }
        return updated;
    }

    @SuppressWarnings("unchecked")
    static List<VoiceProfile> readVoiceProfiles(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.voices must be an array");
        }
        ArrayList<VoiceProfile> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.voices entries must be objects");
            }
            Map<String, Object> voice = (Map<String, Object>) raw;
            result.add(new VoiceProfile(
                    string(voice.get("id"), "voice.id"),
                    string(voice.get("displayName"), "voice.displayName"),
                    enumValue(VoiceProfileType.class, stringOrDefault(voice.get("type"), VoiceProfileType.UNKNOWN.name()), "voice.type"),
                    resolveVoiceEngineType(voice),
                    stringOrDefault(voice.get("language"), "es"),
                    stringOrDefault(voice.get("sampleAssetId"), ""),
                    stringOrDefault(voice.get("modelAssetId"), ""),
                    enumValue(VoiceQualityPreset.class, stringOrDefault(voice.get("qualityPreset"), VoiceQualityPreset.BALANCED.name()), "voice.qualityPreset"),
                    booleanOrDefault(voice.get("supportsStyleTransfer"), false),
                    stringOrDefault(voice.get("consentNote"), ""),
                    voiceEngineMetadata(voice)
            ));
        }
        return List.copyOf(result);
    }

    static VoiceEngineType resolveVoiceEngineType(Map<String, Object> voice) throws IOException {
        String engineId = stringOrDefault(voice.get("engineId"), "");
        if (!engineId.isBlank()) return VoiceProfile.engineTypeFor(engineId);
        return enumValue(VoiceEngineType.class,
                stringOrDefault(voice.get("engineType"), VoiceEngineType.UNKNOWN.name()), "voice.engineType");
    }

    static Map<String, String> voiceEngineMetadata(Map<String, Object> voice) throws IOException {
        String engineId = stringOrDefault(voice.get("engineId"), "");
        return engineId.isBlank() ? Map.of() : Map.of("engineId", engineId);
    }

    @SuppressWarnings("unchecked")
    static List<VoiceReferenceSampleSet> readReferenceSampleSets(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.referenceSampleSets must be an array");
        }
        ArrayList<VoiceReferenceSampleSet> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.referenceSampleSets entries must be objects");
            }
            Map<String, Object> set = (Map<String, Object>) raw;
            String voiceProfileId = string(set.get("voiceProfileId"), "referenceSampleSet.voiceProfileId");
            result.add(new VoiceReferenceSampleSet(voiceProfileId, readReferenceSamples(set.get("samples"), voiceProfileId)));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<VoiceReferenceSample> readReferenceSamples(Object value, String fallbackVoiceProfileId) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("referenceSampleSet.samples must be an array");
        }
        ArrayList<VoiceReferenceSample> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("referenceSampleSet.samples entries must be objects");
            }
            Map<String, Object> sample = (Map<String, Object>) raw;
            result.add(new VoiceReferenceSample(
                    string(sample.get("id"), "referenceSample.id"),
                    stringOrDefault(sample.get("voiceProfileId"), fallbackVoiceProfileId),
                    enumValue(VoiceReferenceTone.class, stringOrDefault(sample.get("tone"), VoiceReferenceTone.NEUTRAL.name()), "referenceSample.tone"),
                    string(sample.get("fileUri"), "referenceSample.fileUri"),
                    enumValue(VoiceSampleOrigin.class, stringOrDefault(sample.get("origin"), VoiceSampleOrigin.IMPORTED_FILE.name()), "referenceSample.origin"),
                    enumValue(VoiceFileOwnership.class, stringOrDefault(sample.get("ownership"), VoiceFileOwnership.EXTERNAL_REFERENCE.name()), "referenceSample.ownership"),
                    longOrDefault(sample.get("durationMillis"), 0),
                    sample.get("createdAt") == null ? Instant.now() : instant(sample.get("createdAt"), "referenceSample.createdAt"),
                    stringOrDefault(sample.get("notes"), ""),
                    stringOrDefault(sample.get("referenceTranscript"), "")
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<CharacterProfile> readCharacters(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.characters must be an array");
        }
        ArrayList<CharacterProfile> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.characters entries must be objects");
            }
            Map<String, Object> character = (Map<String, Object>) raw;
            result.add(new CharacterProfile(
                    string(character.get("id"), "character.id"),
                    string(character.get("displayName"), "character.displayName"),
                    stringOrDefault(character.get("defaultVoiceProfileId"), "VOC-NARRATOR"),
                    stringOrDefault(character.get("defaultPerformanceStyleId"), "STY-NEUTRAL"),
                    stringOrDefault(character.get("description"), ""),
                    Map.of()
            ));
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    static List<PerformanceStyle> readStyles(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("voiceLibrary.styles must be an array");
        }
        ArrayList<PerformanceStyle> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException("voiceLibrary.styles entries must be objects");
            }
            Map<String, Object> style = (Map<String, Object>) raw;
            result.add(new PerformanceStyle(
                    string(style.get("id"), "style.id"),
                    string(style.get("displayName"), "style.displayName"),
                    stringOrDefault(style.get("description"), ""),
                    booleanOrDefault(style.get("requiresEngineSupport"), false),
                    Map.of()
            ));
        }
        return List.copyOf(result);
    }

    static void writeVoiceLibrary(StringBuilder out, VoiceLibrary library) {
        indent(out, 1).append("\"voiceLibrary\": {\n");
        field(out, 2, "id", quote(library.id())); out.append(",\n");
        field(out, 2, "updatedAt", quote(library.updatedAt().toString())); out.append(",\n");
        field(out, 2, "notes", quote(library.notes())); out.append(",\n");
        indent(out, 2).append("\"voices\": [");
        if (!library.voices().isEmpty()) { out.append("\n"); }
        for (int i = 0; i < library.voices().size(); i++) {
            VoiceProfile voice = library.voices().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(voice.id())); out.append(",\n");
            field(out, 4, "displayName", quote(voice.displayName())); out.append(",\n");
            field(out, 4, "type", quote(voice.type().name())); out.append(",\n");
            field(out, 4, "engineId", quote(voice.engineId().value())); out.append(",\n");
            field(out, 4, "engineType", quote(VoiceProfile.engineTypeFor(voice.engineId().value()).name())); out.append(",\n");
            field(out, 4, "language", quote(voice.language())); out.append(",\n");
            field(out, 4, "sampleAssetId", quote(voice.sampleAssetId())); out.append(",\n");
            field(out, 4, "modelAssetId", quote(voice.modelAssetId())); out.append(",\n");
            field(out, 4, "qualityPreset", quote(voice.qualityPreset().name())); out.append(",\n");
            field(out, 4, "supportsStyleTransfer", Boolean.toString(voice.supportsStyleTransfer())); out.append(",\n");
            field(out, 4, "consentNote", quote(voice.consentNote())); out.append("\n");
            indent(out, 3).append("}");
            if (i < library.voices().size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, 2).append("],\n");
        writeReferenceSampleSets(out, library.referenceSampleSets(), 2);
        out.append(",\n");
        indent(out, 2).append("\"characters\": [");
        if (!library.characters().isEmpty()) { out.append("\n"); }
        for (int i = 0; i < library.characters().size(); i++) {
            CharacterProfile character = library.characters().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(character.id())); out.append(",\n");
            field(out, 4, "displayName", quote(character.displayName())); out.append(",\n");
            field(out, 4, "defaultVoiceProfileId", quote(character.defaultVoiceProfileId())); out.append(",\n");
            field(out, 4, "defaultPerformanceStyleId", quote(character.defaultPerformanceStyleId())); out.append(",\n");
            field(out, 4, "description", quote(character.description())); out.append("\n");
            indent(out, 3).append("}");
            if (i < library.characters().size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, 2).append("],\n");
        indent(out, 2).append("\"styles\": [");
        if (!library.styles().isEmpty()) { out.append("\n"); }
        for (int i = 0; i < library.styles().size(); i++) {
            PerformanceStyle style = library.styles().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(style.id())); out.append(",\n");
            field(out, 4, "displayName", quote(style.displayName())); out.append(",\n");
            field(out, 4, "description", quote(style.description())); out.append(",\n");
            field(out, 4, "requiresEngineSupport", Boolean.toString(style.requiresEngineSupport())); out.append("\n");
            indent(out, 3).append("}");
            if (i < library.styles().size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, 2).append("]\n");
        indent(out, 1).append("}");
    }

    static void writeReferenceSampleSets(StringBuilder out, List<VoiceReferenceSampleSet> sampleSets, int level) {
        indent(out, level).append("\"referenceSampleSets\": [");
        if (!sampleSets.isEmpty()) { out.append("\n"); }
        for (int i = 0; i < sampleSets.size(); i++) {
            VoiceReferenceSampleSet sampleSet = sampleSets.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "voiceProfileId", quote(sampleSet.voiceProfileId())); out.append(",\n");
            indent(out, level + 2).append("\"samples\": [");
            if (!sampleSet.samples().isEmpty()) { out.append("\n"); }
            for (int j = 0; j < sampleSet.samples().size(); j++) {
                VoiceReferenceSample sample = sampleSet.samples().get(j);
                indent(out, level + 3).append("{\n");
                field(out, level + 4, "id", quote(sample.id())); out.append(",\n");
                field(out, level + 4, "voiceProfileId", quote(sample.voiceProfileId())); out.append(",\n");
                field(out, level + 4, "tone", quote(sample.tone().name())); out.append(",\n");
                field(out, level + 4, "fileUri", quote(sample.fileUri())); out.append(",\n");
                field(out, level + 4, "origin", quote(sample.origin().name())); out.append(",\n");
                field(out, level + 4, "ownership", quote(sample.ownership().name())); out.append(",\n");
                field(out, level + 4, "durationMillis", Long.toString(sample.durationMillis())); out.append(",\n");
                field(out, level + 4, "createdAt", quote(sample.createdAt().toString())); out.append(",\n");
                field(out, level + 4, "notes", quote(sample.notes())); out.append(",\n");
                field(out, level + 4, "referenceTranscript", quote(sample.referenceTranscript())); out.append("\n");
                indent(out, level + 3).append("}");
                if (j < sampleSet.samples().size() - 1) { out.append(","); }
                out.append("\n");
            }
            indent(out, level + 2).append("]\n");
            indent(out, level + 1).append("}");
            if (i < sampleSets.size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, level).append("]");
    }

    @Override public String sectionName() { return "voiceLibrary"; }
}
