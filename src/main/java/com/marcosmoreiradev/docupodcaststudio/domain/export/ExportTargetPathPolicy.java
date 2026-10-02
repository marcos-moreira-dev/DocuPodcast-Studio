package com.marcosmoreiradev.docupodcaststudio.domain.export;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Normalizes user-selected export targets and appends the required extension when missing. */
public final class ExportTargetPathPolicy {
    private ExportTargetPathPolicy() {
    }

    public static Path ensureMarkdownExtension(Path target) {
        return ensureExtension(target, ".md");
    }

    public static Path ensureWavExtension(Path target) {
        return ensureExtension(target, ".wav");
    }

    public static Path ensureMp3Extension(Path target) {
        return ensureExtension(target, ".mp3");
    }

    public static Path ensureAacExtension(Path target) {
        return ensureExtension(target, ".aac");
    }

    public static Path ensureJsonExtension(Path target) {
        return ensureExtension(target, ".json");
    }

    public static Path ensureZipExtension(Path target) {
        return ensureExtension(target, ".zip");
    }

    public static Path ensureExtension(Path target, String extension) {
        Objects.requireNonNull(target, "target");
        String ext = Objects.requireNonNull(extension, "extension").trim();
        if (ext.isBlank()) {
            return target;
        }
        String text = target.toString();
        if (text.toLowerCase(Locale.ROOT).endsWith(ext.toLowerCase(Locale.ROOT))) {
            return target;
        }
        return Path.of(text + ext);
    }
}
