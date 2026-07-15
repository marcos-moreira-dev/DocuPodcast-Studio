package com.marcosmoreiradev.docupodcaststudio.infrastructure.export;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Sanitizes user/project names for export folders. */
public final class ExportFolderNamePolicy {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ExportFolderNamePolicy() {
    }

    public static String folderName(String title) {
        String base = title == null || title.isBlank() ? "proyecto" : title;
        String ascii = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (ascii.isBlank()) {
            ascii = "proyecto";
        }
        return ascii + "_export_" + STAMP.format(LocalDateTime.now());
    }
}
