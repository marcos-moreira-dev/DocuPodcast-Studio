package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfSemanticPageAnalysis;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfSemanticPageResponseParser;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfSemanticProtocolException;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Strict parser for PAGE / BEGIN / SOURCE / SPEECH / END / DONE V1. */
public final class BlockPdfSemanticPageResponseParser
        implements PdfSemanticPageResponseParser {
    public static final String VERSION = "V1";
    private static final double REPAIR_TOLERANCE = 2.0;
    private static final Map<String, PdfRegionType> SEMANTIC_TYPE_ALIASES =
            Map.ofEntries(
                    Map.entry("HEADLINE", PdfRegionType.HEADING),
                    Map.entry("SECTION_TITLE", PdfRegionType.HEADING),
                    Map.entry("SUBTITLE", PdfRegionType.SUBHEADING),
                    Map.entry("SUBSECTION_TITLE", PdfRegionType.SUBHEADING),
                    Map.entry("PROSE", PdfRegionType.PARAGRAPH),
                    Map.entry("TEXT", PdfRegionType.PARAGRAPH),
                    Map.entry("LIST_ITEM", PdfRegionType.LIST),
                    Map.entry("NOTE", PdfRegionType.SIDEBAR),
                    Map.entry("CALLOUT", PdfRegionType.SIDEBAR),
                    Map.entry("BOX", PdfRegionType.SIDEBAR),
                    Map.entry("DEFINITION", PdfRegionType.SIDEBAR),
                    Map.entry("EQUATION", PdfRegionType.MATH),
                    Map.entry("FORMULA", PdfRegionType.MATH),
                    Map.entry("FIGURE", PdfRegionType.IMAGE),
                    Map.entry("GRAPH", PdfRegionType.IMAGE),
                    Map.entry("DIAGRAM", PdfRegionType.IMAGE));

    @Override
    public PdfSemanticPageAnalysis parse(String protocolOutput)
            throws PdfSemanticProtocolException {
        String normalized = normalize(protocolOutput);
        if (normalized.isBlank()) {
            throw incomplete("La lectura semantica no devolvio contenido.");
        }
        String[] lines = normalized.split("\n", -1);
        int last = lastNonBlank(lines);
        if (last < 0 || !"DONE".equals(lines[last])) {
            throw incomplete("La lectura semantica termino sin DONE.");
        }
        if (last != lines.length - 1 && hasNonBlankAfter(lines, last)) {
            throw invalid("Existe contenido inesperado despues de DONE.");
        }

        Header header = header(lines[0]);
        ArrayList<PdfSemanticPageAnalysis.Element> elements = new ArrayList<>();
        ArrayList<String> pageUncertainties = new ArrayList<>();
        int index = 1;
        while (index < last) {
            if (lines[index].isBlank()) {
                index++;
                continue;
            }
            Begin begin = begin(lines[index], index + 1);
            index++;
            if (index >= last || !"SOURCE".equals(lines[index])) {
                throw invalid("La region " + (elements.size() + 1)
                        + " no comienza su payload con SOURCE.");
            }
            index++;
            ArrayList<String> source = new ArrayList<>();
            ArrayList<String> speech = new ArrayList<>();
            boolean readingSpeech = false;
            boolean ended = false;
            while (index < last) {
                String line = lines[index];
                if (isStructuralEnd(lines, index, last)) {
                    ended = true;
                    index++;
                    break;
                }
                if (!readingSpeech && speechApplicable(begin.type())
                        && "SPEECH".equals(line)
                        && hasStructuralEndAhead(lines, index + 1, last)) {
                    readingSpeech = true;
                    index++;
                    continue;
                }
                (readingSpeech ? speech : source).add(line);
                index++;
            }
            if (!ended) {
                throw incomplete("La ultima region semantica no termino con END.");
            }
            String sourceText = payload(source);
            String narrationText = payload(speech);
            PdfRegionType effectiveType = effectiveType(begin, sourceText);
            validatePayload(effectiveType, sourceText, narrationText);
            LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
            attributes.put("semanticProtocolVersion", VERSION);
            attributes.put("semanticResponseIndex",
                    Integer.toString(elements.size()));
            attributes.put("bboxValidation", begin.geometry().name());
            attributes.put("modelConfidence", "not-reported");
            if (!begin.rawType().equals(effectiveType.name())) {
                attributes.put("semanticTypeAlias", begin.rawType());
            }
            ArrayList<String> uncertainties = new ArrayList<>();
            if (begin.geometry() == Geometry.REPAIRED) {
                uncertainties.add("bbox-repaired-near-canonical-boundary");
                pageUncertainties.add("Se reparo un bbox a menos de "
                        + number(REPAIR_TOLERANCE) + " unidades del limite 0..1000.");
            }
            elements.add(new PdfSemanticPageAnalysis.Element(
                    "", elements.size(), effectiveType, begin.box(),
                    sourceText, narrationText, begin.narratability(),
                    0.0, uncertainties, attributes));
        }
        return new PdfSemanticPageAnalysis(0, header.language(), header.role(),
                elements, 0.0, pageUncertainties);
    }

    private static Header header(String line)
            throws PdfSemanticProtocolException {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 4 || !"PAGE".equals(parts[0])
                || !VERSION.equals(parts[1])) {
            throw invalid("La respuesta debe comenzar con PAGE|V1|idioma|rol.");
        }
        String language = parts[2].strip();
        if (language.isBlank() || language.chars().anyMatch(Character::isWhitespace)) {
            throw invalid("PAGE contiene un idioma invalido.");
        }
        return new Header(language, pageRole(parts[3]));
    }

    private static Begin begin(String line, int lineNumber)
            throws PdfSemanticProtocolException {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 8 || !"BEGIN".equals(parts[0])) {
            throw invalid("Linea " + lineNumber
                    + ": se esperaba BEGIN con siete campos.");
        }
        String rawType = token(parts[1]);
        PdfRegionType type = regionType(rawType);
        double xMin = number(parts[2], "xMin", lineNumber);
        double yMin = number(parts[3], "yMin", lineNumber);
        double xMax = number(parts[4], "xMax", lineNumber);
        double yMax = number(parts[5], "yMax", lineNumber);
        if (!(xMax > xMin) || !(yMax > yMin)) {
            throw invalid("Linea " + lineNumber
                    + ": bbox vacio o invertido.");
        }
        Geometry geometry;
        if (inside(xMin, yMin, xMax, yMax)) {
            geometry = Geometry.VALID;
        } else if (nearBoundary(xMin, yMin, xMax, yMax)) {
            geometry = Geometry.REPAIRED;
            xMin = clamp(xMin);
            yMin = clamp(yMin);
            xMax = clamp(xMax);
            yMax = clamp(yMax);
            if (!(xMax > xMin) || !(yMax > yMin)) {
                throw invalid("Linea " + lineNumber
                        + ": bbox no reparable.");
            }
        } else {
            throw invalid("Linea " + lineNumber
                    + ": bbox fuera del rango 0..1000.");
        }
        double reportedConfidence = number(parts[7], "confianza", lineNumber);
        if (reportedConfidence < 0.0 || reportedConfidence > 100.0) {
            throw invalid("Linea " + lineNumber
                    + ": confianza fuera del rango 0..100.");
        }
        return new Begin(rawType, type,
                new PdfSemanticPageAnalysis.NormalizedBox(
                        xMin, yMin, xMax, yMax),
                narratability(parts[6]), geometry);
    }

    private static void validatePayload(PdfRegionType type, String source,
                                        String speech)
            throws PdfSemanticProtocolException {
        if (type == PdfRegionType.IMAGE) {
            if (source.isBlank() && speech.isBlank()) {
                throw invalid("Una imagen requiere SOURCE o SPEECH.");
            }
            return;
        }
        if (source.isBlank()) {
            throw invalid("La region " + type + " requiere SOURCE.");
        }
    }

    private static boolean isStructuralEnd(String[] lines, int index,
                                           int last) {
        if (!"END".equals(lines[index])) return false;
        int next = nextNonBlank(lines, index + 1, last);
        return next >= 0 && ("DONE".equals(lines[next])
                || looksLikeBegin(lines[next]));
    }

    private static boolean hasStructuralEndAhead(String[] lines, int start,
                                                 int last) {
        for (int index = start; index < last; index++) {
            if (isStructuralEnd(lines, index, last)) return true;
        }
        return false;
    }

    private static boolean looksLikeBegin(String value) {
        return value != null && value.startsWith("BEGIN|")
                && value.split("\\|", -1).length == 8;
    }

    private static boolean speechApplicable(PdfRegionType type) {
        return type == PdfRegionType.MATH || type == PdfRegionType.IMAGE;
    }

    private static PdfRegionType effectiveType(Begin begin, String source) {
        if (!"FIGURE".equals(begin.rawType())) return begin.type();
        String normalized = source == null ? "" : source.strip().toLowerCase(Locale.ROOT);
        double height = begin.box().yMax() - begin.box().yMin();
        if (height <= 90.0 && (normalized.startsWith("figura ")
                || normalized.startsWith("figure ")
                || normalized.startsWith("fig. "))) {
            return PdfRegionType.CAPTION;
        }
        return begin.type();
    }

    private static PdfRegionType regionType(String value)
            throws PdfSemanticProtocolException {
        String normalized = token(value);
        PdfRegionType alias = SEMANTIC_TYPE_ALIASES.get(normalized);
        if (alias != null) return alias;
        try {
            return PdfRegionType.valueOf(normalized);
        } catch (IllegalArgumentException unsupported) {
            throw invalid("Tipo semantico no reconocido: " + value);
        }
    }

    static Map<String, PdfRegionType> semanticTypeAliases() {
        return SEMANTIC_TYPE_ALIASES;
    }

    private static PdfNarratability narratability(String value)
            throws PdfSemanticProtocolException {
        return switch (token(value)) {
            case "N", "NARRATABLE" -> PdfNarratability.NARRATABLE;
            case "X", "NON_NARRATABLE" -> PdfNarratability.NON_NARRATABLE;
            case "U", "UNCERTAIN" -> PdfNarratability.UNCERTAIN;
            default -> throw invalid("Narratabilidad no reconocida: " + value);
        };
    }

    private static PdfPageRole pageRole(String value)
            throws PdfSemanticProtocolException {
        try {
            return PdfPageRole.valueOf(token(value));
        } catch (IllegalArgumentException unsupported) {
            throw invalid("Rol de pagina no reconocido: " + value);
        }
    }

    private static double number(String value, String field, int line)
            throws PdfSemanticProtocolException {
        try {
            double parsed = Double.parseDouble(value.strip());
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException invalid) {
            throw new PdfSemanticProtocolException(
                    PdfSemanticProtocolException.Kind.INVALID,
                    "Linea " + line + ": " + field + " no es finito.", invalid);
        }
    }

    private static boolean inside(double xMin, double yMin,
                                  double xMax, double yMax) {
        return xMin >= 0.0 && yMin >= 0.0
                && xMax <= 1000.0 && yMax <= 1000.0;
    }

    private static boolean nearBoundary(double xMin, double yMin,
                                        double xMax, double yMax) {
        return xMin >= -REPAIR_TOLERANCE && yMin >= -REPAIR_TOLERANCE
                && xMax <= 1000.0 + REPAIR_TOLERANCE
                && yMax <= 1000.0 + REPAIR_TOLERANCE;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1000.0, value));
    }

    private static String payload(List<String> lines) {
        int first = 0;
        int last = lines.size();
        while (first < last && lines.get(first).isBlank()) first++;
        while (last > first && lines.get(last - 1).isBlank()) last--;
        return String.join("\n", lines.subList(first, last));
    }

    private static String normalize(String value) {
        String safe = value == null ? "" : value;
        if (safe.startsWith("\uFEFF")) safe = safe.substring(1);
        return safe.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String token(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }

    private static int lastNonBlank(String[] lines) {
        for (int index = lines.length - 1; index >= 0; index--) {
            if (!lines[index].isBlank()) return index;
        }
        return -1;
    }

    private static int nextNonBlank(String[] lines, int start, int limit) {
        for (int index = start; index <= limit; index++) {
            if (!lines[index].isBlank()) return index;
        }
        return -1;
    }

    private static boolean hasNonBlankAfter(String[] lines, int index) {
        for (int current = index + 1; current < lines.length; current++) {
            if (!lines[current].isBlank()) return true;
        }
        return false;
    }

    private static PdfSemanticProtocolException incomplete(String message) {
        return new PdfSemanticProtocolException(
                PdfSemanticProtocolException.Kind.INCOMPLETE, message);
    }

    private static PdfSemanticProtocolException invalid(String message) {
        return new PdfSemanticProtocolException(
                PdfSemanticProtocolException.Kind.INVALID, message);
    }

    private static String number(double value) {
        return value == Math.rint(value)
                ? Long.toString((long) value) : Double.toString(value);
    }

    private enum Geometry { VALID, REPAIRED }

    private record Header(String language, PdfPageRole role) { }

    private record Begin(String rawType,
                         PdfRegionType type,
                         PdfSemanticPageAnalysis.NormalizedBox box,
                         PdfNarratability narratability,
                         Geometry geometry) { }
}
