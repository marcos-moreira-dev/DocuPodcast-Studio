package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Request to import a user-provided audio sample into the project voice library. */
public record VoiceSampleImportRequest(
        String targetVoiceProfileId,
        Path sourceAudioFile,
        String displayName,
        VoiceProfileType profileType,
        String consentNote,
        VoiceReferenceTone tone,
        com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin origin
) {
    public VoiceSampleImportRequest {
        targetVoiceProfileId = token(targetVoiceProfileId, "targetVoiceProfileId");
        sourceAudioFile = Objects.requireNonNull(sourceAudioFile, "sourceAudioFile");
        displayName = normalize(displayName);
        profileType = Objects.requireNonNullElse(profileType, VoiceProfileType.OWN);
        consentNote = normalize(consentNote);
        tone = Objects.requireNonNullElse(tone, VoiceReferenceTone.NEUTRAL);
        origin = Objects.requireNonNullElse(origin, com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin.IMPORTED_FILE);
    }

    public VoiceSampleImportRequest(String targetVoiceProfileId,
                                    Path sourceAudioFile,
                                    String displayName,
                                    VoiceProfileType profileType,
                                    String consentNote) {
        this(targetVoiceProfileId, sourceAudioFile, displayName, profileType, consentNote, VoiceReferenceTone.NEUTRAL, com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin.IMPORTED_FILE);
    }

    public static VoiceSampleImportRequest forOwnVoice(String targetVoiceProfileId, Path sourceAudioFile, String displayName) {
        return forOwnVoiceTone(targetVoiceProfileId, sourceAudioFile, displayName, VoiceReferenceTone.NEUTRAL);
    }

    public static VoiceSampleImportRequest forOwnVoiceTone(String targetVoiceProfileId,
                                                           Path sourceAudioFile,
                                                           String displayName,
                                                           VoiceReferenceTone tone) {
        return new VoiceSampleImportRequest(targetVoiceProfileId, sourceAudioFile, displayName,
                VoiceProfileType.OWN, "Voz propia registrada por el usuario.", tone, com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin.IMPORTED_FILE);
    }


    public static VoiceSampleImportRequest forRecordedVoiceTone(String targetVoiceProfileId,
                                                               Path sourceAudioFile,
                                                               String displayName,
                                                               VoiceReferenceTone tone) {
        return new VoiceSampleImportRequest(targetVoiceProfileId, sourceAudioFile, displayName,
                VoiceProfileType.OWN, "Voz propia grabada por el usuario.", tone, com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin.RECORDED_IN_APP);
    }

    public String normalizedExtension() {
        String fileName = sourceAudioFile.getFileName() == null ? "" : sourceAudioFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public boolean supportedAudioExtension() {
        return switch (normalizedExtension()) {
            case "wav", "mp3", "flac", "ogg", "m4a" -> true;
            default -> false;
        };
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
