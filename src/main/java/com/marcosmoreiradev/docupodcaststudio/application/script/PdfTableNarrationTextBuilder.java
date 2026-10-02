package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Deterministic, separator-free narration for small canonical PDF tables. */
final class PdfTableNarrationTextBuilder {
    private static final int MAX_SPOKEN_ROWS = 24;
    private final PdfMathSpeechNormalizer math = new PdfMathSpeechNormalizer();

    String build(PdfRegion region) {
        List<List<String>> rows = Arrays.stream(region.effectiveText().split("\\R"))
                .map(String::strip).filter(line -> !line.isBlank())
                .map(PdfTableNarrationTextBuilder::cells).toList();
        if (rows.size() < 2 || rows.getFirst().size() < 2) {
            return "Tabla: " + math.normalizeSpeech(region, region.effectiveText())
                    .replace(';', '.').replace('|', '.');
        }
        List<String> headers = rows.getFirst();
        ArrayList<String> spoken = new ArrayList<>();
        spoken.add("Tabla con " + (rows.size() - 1) + " filas de datos.");
        int limit = Math.min(rows.size(), MAX_SPOKEN_ROWS + 1);
        for (int rowIndex = 1; rowIndex < limit; rowIndex++) {
            List<String> row = rows.get(rowIndex);
            ArrayList<String> fields = new ArrayList<>();
            for (int column = 0; column < Math.min(headers.size(), row.size()); column++) {
                fields.add(math.normalizeSpeech(region, headers.get(column)) + ": "
                        + math.normalizeSpeech(region, row.get(column)));
            }
            spoken.add(String.join(". ", fields) + ".");
        }
        if (rows.size() > limit) {
            spoken.add("Continúa con " + (rows.size() - limit) + " filas adicionales.");
        }
        return String.join(" ", spoken).replace(";", ".").replace("|", ".");
    }

    private static List<String> cells(String row) {
        return Arrays.stream(row.split("\\s*[;|]\\s*", -1))
                .map(String::strip).toList();
    }
}
