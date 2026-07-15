package com.marcosmoreiradev.docupodcaststudio.domain.recording;

/** Why the user recorded/imported an audio clip. */
public enum RecordingPurpose {
    HUMAN_VOICE_FOR_TEXT("Voz humana asociada a texto"),
    VOICE_SAMPLE_FOR_TTS("Muestra de voz para motor TTS"),
    NOTE_OR_REFERENCE("Nota o referencia");

    private final String displayName;

    RecordingPurpose(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
