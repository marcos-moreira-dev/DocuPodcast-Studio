package com.marcosmoreiradev.docupodcaststudio.infrastructure.voice;

import com.marcosmoreiradev.docupodcaststudio.application.voice.MaterializedVoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceLibraryWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.CharacterProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.PerformanceStyle;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Writes voices/voice-library.json beside the project file. */
public final class VoiceLibraryWorkspaceFileRepository implements VoiceLibraryWorkspaceRepository {
    private static final String RELATIVE_PATH = "voices/voice-library.json";

    @Override
    public MaterializedVoiceLibrary materialize(VoiceLibrary voiceLibrary, Path projectFile) throws IOException {
        Objects.requireNonNull(voiceLibrary, "voiceLibrary");
        Objects.requireNonNull(projectFile, "projectFile");
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            throw new IOException("Project file must have a parent directory");
        }
        Path target = projectDirectory.resolve(RELATIVE_PATH).normalize();
        Files.createDirectories(target.getParent());
        Files.writeString(target, writeJson(voiceLibrary), StandardCharsets.UTF_8);
        return new MaterializedVoiceLibrary(RELATIVE_PATH, "Biblioteca de voces");
    }

    private static String writeJson(VoiceLibrary library) {
        StringBuilder out = new StringBuilder(4096);
        out.append("{\n");
        field(out, 1, "id", quote(library.id())); out.append(",\n");
        field(out, 1, "updatedAt", quote(library.updatedAt().toString())); out.append(",\n");
        field(out, 1, "notes", quote(library.notes())); out.append(",\n");
        out.append("    \"voices\": [");
        if (!library.voices().isEmpty()) out.append("\n");
        for (int i = 0; i < library.voices().size(); i++) {
            VoiceProfile voice = library.voices().get(i);
            out.append("        {\n");
            field(out, 3, "id", quote(voice.id())); out.append(",\n");
            field(out, 3, "displayName", quote(voice.displayName())); out.append(",\n");
            field(out, 3, "type", quote(voice.type().name())); out.append(",\n");
            field(out, 3, "engineId", quote(voice.engineId().value())); out.append(",\n");
            field(out, 3, "engineType", quote(VoiceProfile.engineTypeFor(voice.engineId().value()).name())); out.append(",\n");
            field(out, 3, "language", quote(voice.language())); out.append(",\n");
            field(out, 3, "sampleAssetId", quote(voice.sampleAssetId())); out.append(",\n");
            field(out, 3, "modelAssetId", quote(voice.modelAssetId())); out.append(",\n");
            field(out, 3, "qualityPreset", quote(voice.qualityPreset().name())); out.append(",\n");
            field(out, 3, "supportsStyleTransfer", Boolean.toString(voice.supportsStyleTransfer())); out.append(",\n");
            field(out, 3, "consentNote", quote(voice.consentNote())); out.append("\n");
            out.append("        }");
            if (i < library.voices().size() - 1) out.append(',');
            out.append("\n");
        }
        out.append("    ],\n");
        writeReferenceSampleSets(out, library.referenceSampleSets());
        out.append(",\n");
        out.append("    \"characters\": [");
        if (!library.characters().isEmpty()) out.append("\n");
        for (int i = 0; i < library.characters().size(); i++) {
            CharacterProfile character = library.characters().get(i);
            out.append("        {\n");
            field(out, 3, "id", quote(character.id())); out.append(",\n");
            field(out, 3, "displayName", quote(character.displayName())); out.append(",\n");
            field(out, 3, "defaultVoiceProfileId", quote(character.defaultVoiceProfileId())); out.append(",\n");
            field(out, 3, "defaultPerformanceStyleId", quote(character.defaultPerformanceStyleId())); out.append(",\n");
            field(out, 3, "description", quote(character.description())); out.append("\n");
            out.append("        }");
            if (i < library.characters().size() - 1) out.append(',');
            out.append("\n");
        }
        out.append("    ],\n");
        out.append("    \"styles\": [");
        if (!library.styles().isEmpty()) out.append("\n");
        for (int i = 0; i < library.styles().size(); i++) {
            PerformanceStyle style = library.styles().get(i);
            out.append("        {\n");
            field(out, 3, "id", quote(style.id())); out.append(",\n");
            field(out, 3, "displayName", quote(style.displayName())); out.append(",\n");
            field(out, 3, "description", quote(style.description())); out.append(",\n");
            field(out, 3, "requiresEngineSupport", Boolean.toString(style.requiresEngineSupport())); out.append("\n");
            out.append("        }");
            if (i < library.styles().size() - 1) out.append(',');
            out.append("\n");
        }
        out.append("    ]\n");
        out.append("}\n");
        return out.toString();
    }


    private static void writeReferenceSampleSets(StringBuilder out, java.util.List<VoiceReferenceSampleSet> sampleSets) {
        out.append("    \"referenceSampleSets\": [");
        if (!sampleSets.isEmpty()) out.append("\n");
        for (int i = 0; i < sampleSets.size(); i++) {
            VoiceReferenceSampleSet sampleSet = sampleSets.get(i);
            out.append("        {\n");
            field(out, 3, "voiceProfileId", quote(sampleSet.voiceProfileId())); out.append(",\n");
            out.append("            \"samples\": [");
            if (!sampleSet.samples().isEmpty()) out.append("\n");
            for (int j = 0; j < sampleSet.samples().size(); j++) {
                VoiceReferenceSample sample = sampleSet.samples().get(j);
                out.append("                {\n");
                field(out, 5, "id", quote(sample.id())); out.append(",\n");
                field(out, 5, "voiceProfileId", quote(sample.voiceProfileId())); out.append(",\n");
                field(out, 5, "tone", quote(sample.tone().name())); out.append(",\n");
                field(out, 5, "fileUri", quote(sample.fileUri())); out.append(",\n");
                field(out, 5, "origin", quote(sample.origin().name())); out.append(",\n");
                field(out, 5, "ownership", quote(sample.ownership().name())); out.append(",\n");
                field(out, 5, "durationMillis", Long.toString(sample.durationMillis())); out.append(",\n");
                field(out, 5, "createdAt", quote(sample.createdAt().toString())); out.append(",\n");
                field(out, 5, "notes", quote(sample.notes())); out.append("\n");
                out.append("                }");
                if (j < sampleSet.samples().size() - 1) out.append(',');
                out.append("\n");
            }
            out.append("            ]\n");
            out.append("        }");
            if (i < sampleSets.size() - 1) out.append(',');
            out.append("\n");
        }
        out.append("    ]");
    }

    private static void field(StringBuilder out, int level, String name, String value) {
        out.append("    ".repeat(level)).append(quote(name)).append(": ").append(value);
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(safe.length() + 16).append('"');
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.append('"').toString();
    }
}
