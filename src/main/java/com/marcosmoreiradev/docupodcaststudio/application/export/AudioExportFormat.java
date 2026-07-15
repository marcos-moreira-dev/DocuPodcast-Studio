package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** User-facing final audio formats. Internal chunk/cache audio stays WAV. */
public enum AudioExportFormat {
    WAV("WAV", ".wav", false),
    MP3("MP3", ".mp3", true),
    AAC("AAC", ".aac", true);

    private final String displayName;
    private final String extension;
    private final boolean compressed;

    AudioExportFormat(String displayName, String extension, boolean compressed) {
        this.displayName = displayName;
        this.extension = extension;
        this.compressed = compressed;
    }

    public String displayName() {
        return displayName;
    }

    public String extension() {
        return extension;
    }

    public boolean compressed() {
        return compressed;
    }

    public Path normalizeTarget(Path target) {
        return ExportTargetPathPolicy.ensureExtension(target, extension);
    }

    public static AudioExportFormat fromTarget(Path target) {
        Objects.requireNonNull(target, "target");
        String text = target.toString().toLowerCase(Locale.ROOT);
        if (text.endsWith(MP3.extension)) {
            return MP3;
        }
        if (text.endsWith(AAC.extension)) {
            return AAC;
        }
        return WAV;
    }
}
