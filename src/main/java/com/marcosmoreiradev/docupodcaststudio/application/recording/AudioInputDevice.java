package com.marcosmoreiradev.docupodcaststudio.application.recording;

/** User-selectable microphone/input line exposed by the local recording gateway. */
public record AudioInputDevice(String id, String displayName, boolean defaultDevice) {
    public static final String DEFAULT_ID = "default";

    public AudioInputDevice {
        id = normalize(id).isBlank() ? DEFAULT_ID : normalize(id);
        displayName = normalize(displayName).isBlank() ? "Microfono predeterminado" : normalize(displayName);
    }

    public static AudioInputDevice systemDefault() {
        return new AudioInputDevice(DEFAULT_ID, "Microfono predeterminado", true);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
