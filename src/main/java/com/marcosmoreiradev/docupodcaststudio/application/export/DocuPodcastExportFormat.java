package com.marcosmoreiradev.docupodcaststudio.application.export;

/** Export formats supported by DocuPodcast artifacts. */
public enum DocuPodcastExportFormat {
    MARKDOWN("Markdown", ".md"),
    WAV("WAV", ".wav"),
    MP3("MP3", ".mp3"),
    AAC("AAC", ".aac"),
    MP4("MP4", ".mp4"),
    JSON("JSON", ".json"),
    ZIP("ZIP", ".zip"),
    DIRECTORY("Carpeta", ""),
    VIDEO_PACKAGE("Paquete de video simple", "");

    private final String displayName;
    private final String extension;

    DocuPodcastExportFormat(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }

    public String displayName() {
        return displayName;
    }

    public String extension() {
        return extension;
    }
    public static DocuPodcastExportFormat fromAudio(AudioExportFormat format) {
        if (format == null) {
            return WAV;
        }
        return switch (format) {
            case WAV -> WAV;
            case MP3 -> MP3;
            case AAC -> AAC;
        };
    }

}
