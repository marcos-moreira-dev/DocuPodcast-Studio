package com.marcosmoreiradev.docupodcaststudio.application.batch;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

public final class DocumentVideoBatchPathPolicy {
    public static final String DESCRIPTOR_SUFFIX = ".docupodcast-batch.json";

    private DocumentVideoBatchPathPolicy() { }

    public static String safeName(String value) {
        String plain = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9._ -]", "")
                .strip().replaceAll("[ .]+$", "").replaceAll("\\s+", "-");
        return plain.isBlank() ? "documentos-a-video" : plain;
    }

    public static String stem(String filename) {
        int dot = filename.lastIndexOf('.');
        return safeName(dot > 0 ? filename.substring(0, dot) : filename);
    }

    public static Path projectRoot(Path destinationParent, String title) {
        return destinationParent.toAbsolutePath().normalize().resolve(safeName(title));
    }

    public static Path descriptor(Path projectRoot, String title) {
        return projectRoot.resolve(safeName(title).toLowerCase(Locale.ROOT) + DESCRIPTOR_SUFFIX);
    }
}
