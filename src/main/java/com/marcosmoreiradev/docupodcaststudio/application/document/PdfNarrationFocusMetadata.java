package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFocusRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Stable compact codec for carrying PDF focus geometry through script/audio manifests. */
public final class PdfNarrationFocusMetadata {
    public static final String KEY = "pdfNarrationFocus";

    private PdfNarrationFocusMetadata() { }

    public static String encode(PdfNarrationFocusRef focus) {
        String boxes = focus.boxes().stream().map(box -> String.format(Locale.ROOT,
                        "%.4f,%.4f,%.4f,%.4f", box.xMin(), box.yMin(), box.xMax(), box.yMax()))
                .collect(java.util.stream.Collectors.joining(";"));
        return focus.pageNumber() + "|" + String.join(",", focus.sourceRegionIds()) + "|"
                + boxes + "|" + focus.accessibleLabel().replace("|", " ");
    }

    public static Optional<PdfNarrationFocusRef> decode(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            String[] parts = value.split("\\|", 4);
            int page = Integer.parseInt(parts[0]);
            List<String> ids = parts[1].isBlank() ? List.of() : List.of(parts[1].split(","));
            ArrayList<PdfNarrationFocusRef.FocusBox> boxes = new ArrayList<>();
            for (String raw : parts[2].split(";")) {
                String[] coordinates = raw.split(",");
                if (coordinates.length != 4) continue;
                boxes.add(new PdfNarrationFocusRef.FocusBox(
                        Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
                        Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[3])));
            }
            return boxes.isEmpty() ? Optional.empty() : Optional.of(new PdfNarrationFocusRef(
                    page, ids, boxes, parts.length == 4 ? parts[3] : "Zona narrada"));
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }
}
