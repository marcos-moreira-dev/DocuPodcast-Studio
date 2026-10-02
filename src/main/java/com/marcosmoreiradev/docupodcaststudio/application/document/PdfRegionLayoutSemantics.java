package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfContentRoute;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.regex.Pattern;

/** Deterministic layout metadata; it never infers global order from completion order. */
public final class PdfRegionLayoutSemantics {
    private static final Pattern MIXED_MATH = Pattern.compile(
            "(?iu)(\\\\(?:frac|lim|sum|int|sqrt|sin|cos|tan)|[=<>≤≥∑∫√^_]|[⁰¹²³⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉])");

    private PdfRegionLayoutSemantics() { }

    public static PdfContentRoute route(PdfRegionType type, String visibleText) {
        PdfRegionType safe = type == null ? PdfRegionType.UNKNOWN : type;
        return switch (safe) {
            case IMAGE -> PdfContentRoute.VLM_VISUAL;
            case TABLE, CODE, MATH -> PdfContentRoute.VLM_STRUCTURED;
            case PARAGRAPH, LIST, SIDEBAR, TITLE, HEADING, SUBHEADING, CAPTION ->
                    MIXED_MATH.matcher(visibleText == null ? "" : visibleText).find()
                            ? PdfContentRoute.VLM_MIXED : PdfContentRoute.OCR_SAFE;
            default -> PdfContentRoute.VLM_MIXED;
        };
    }
}
