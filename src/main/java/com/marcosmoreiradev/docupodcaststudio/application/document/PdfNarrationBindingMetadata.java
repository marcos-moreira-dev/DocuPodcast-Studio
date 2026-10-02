package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfObjectNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** Versioned compact codec that carries PDF narration provenance through audio. */
public final class PdfNarrationBindingMetadata {
    public static final String KEY = "pdfNarrationBinding";
    private static final String VERSION = "1";

    private PdfNarrationBindingMetadata() {
    }

    public static String encode(PdfNarrationBinding binding) {
        return String.join("|",
                VERSION,
                Integer.toString(binding.pageNumber()),
                binding.sourceLayer().name(),
                binding.policy().name(),
                Long.toString(binding.sourceRevision()),
                encoded(String.join("\n", binding.sourceRegionIds())),
                encoded(binding.interpretationId()),
                encoded(binding.sourceFingerprint()));
    }

    public static Optional<PdfNarrationBinding> decode(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            String[] parts = value.split("\\|", -1);
            if (parts.length != 8 || !VERSION.equals(parts[0])) {
                return Optional.empty();
            }
            String regionValue = decoded(parts[5]);
            List<String> regionIds = regionValue.isBlank()
                    ? List.of() : List.of(regionValue.split("\\n"));
            return Optional.of(new PdfNarrationBinding(
                    Integer.parseInt(parts[1]),
                    regionIds,
                    PdfSemanticTextLayer.valueOf(parts[2]),
                    PdfObjectNarrationPolicy.valueOf(parts[3]),
                    decoded(parts[6]),
                    Long.parseLong(parts[4]),
                    decoded(parts[7])));
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decoded(String value) {
        if (value.isBlank()) return "";
        return new String(Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8);
    }
}
