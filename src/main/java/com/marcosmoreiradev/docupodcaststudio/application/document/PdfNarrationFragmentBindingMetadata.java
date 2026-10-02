package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFragmentBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Versioned metadata codec for persisted unit-to-source-fragment grounding. */
public final class PdfNarrationFragmentBindingMetadata {
    public static final String KEY = "pdfNarrationFragments";
    private static final String VERSION = "1";

    private PdfNarrationFragmentBindingMetadata() { }

    public static String encode(List<PdfNarrationFragmentBinding> bindings) {
        String payload = (bindings == null ? List.<PdfNarrationFragmentBinding>of() : bindings)
                .stream().map(PdfNarrationFragmentBindingMetadata::entry)
                .collect(java.util.stream.Collectors.joining("\n"));
        return VERSION + "|" + encoded(payload);
    }

    public static List<PdfNarrationFragmentBinding> decode(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            String[] header = value.split("\\|", 2);
            if (header.length != 2 || !VERSION.equals(header[0])) return List.of();
            String payload = decoded(header[1]);
            if (payload.isBlank()) return List.of();
            ArrayList<PdfNarrationFragmentBinding> result = new ArrayList<>();
            for (String line : payload.split("\\n")) {
                parseEntry(line).ifPresent(result::add);
            }
            return List.copyOf(result);
        } catch (RuntimeException invalid) {
            return List.of();
        }
    }

    public static Optional<PdfNarrationFragmentBinding> resolve(String value, String unitId) {
        String requested = unitId == null ? "" : unitId.strip();
        List<PdfNarrationFragmentBinding> bindings = decode(value);
        Optional<PdfNarrationFragmentBinding> exact = bindings.stream()
                .filter(binding -> binding.unitId().equals(requested)).findFirst();
        if (exact.isPresent()) return exact;
        String parent = parentSegmentId(requested);
        return bindings.stream().filter(binding -> binding.unitId().equals(parent)).findFirst();
    }

    private static String entry(PdfNarrationFragmentBinding binding) {
        String lines = binding.sourceLineIndices().stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        String words = binding.sourceWordIndices().stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        String boxes = binding.playbackBboxes().stream().map(box -> String.format(Locale.ROOT,
                        "%.4f,%.4f,%.4f,%.4f,%.4f,%.4f", box.xMin(), box.yMin(),
                        box.xMax(), box.yMax(), box.pageWidth(), box.pageHeight()))
                .collect(java.util.stream.Collectors.joining(";"));
        return String.join("\t", binding.unitId(), binding.segmentId(),
                binding.sourceBlockId(), binding.regionId(), Integer.toString(binding.pageNumber()),
                Integer.toString(binding.sourceTextStart()), Integer.toString(binding.sourceTextEnd()),
                lines, words, boxes, binding.geometryAuthority().name(), encoded(binding.sourceText()));
    }

    private static Optional<PdfNarrationFragmentBinding> parseEntry(String value) {
        try {
            String[] parts = value.split("\\t", -1);
            if (parts.length != 12) return Optional.empty();
            int page = Integer.parseInt(parts[4]);
            List<PdfPageGeometry> boxes = new ArrayList<>();
            if (!parts[9].isBlank()) {
                for (String raw : parts[9].split(";")) {
                    String[] c = raw.split(",");
                    if (c.length != 6) return Optional.empty();
                    boxes.add(new PdfPageGeometry(Double.parseDouble(c[0]), Double.parseDouble(c[1]),
                            Double.parseDouble(c[2]), Double.parseDouble(c[3]),
                            Double.parseDouble(c[4]), Double.parseDouble(c[5]),
                            PdfPageGeometry.CANONICAL_SPACE));
                }
            }
            return Optional.of(new PdfNarrationFragmentBinding(parts[0], parts[1], parts[2],
                    parts[3], page, Integer.parseInt(parts[5]), Integer.parseInt(parts[6]),
                    indices(parts[7]), indices(parts[8]), boxes, decoded(parts[11]),
                    PdfNarrationFragmentBinding.GeometryAuthority.valueOf(parts[10])));
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
    }

    private static List<Integer> indices(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",")).map(Integer::parseInt).toList();
    }

    private static String parentSegmentId(String unitId) {
        int marker = unitId.lastIndexOf("-U");
        return marker > 0 ? unitId.substring(0, marker) : unitId;
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decoded(String value) {
        return value == null || value.isBlank() ? "" : new String(
                Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
