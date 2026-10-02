package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Conservative page-role classifier; UNKNOWN is preferred over an unsafe guess. */
public final class PdfPageRoleClassifier {
    public PdfPageRole classify(List<PdfRegion> regions) {
        List<PdfRegion> safe = regions == null ? List.of() : regions;
        String text = normalize(safe.stream().map(PdfRegion::effectiveText)
                .reduce("", (a, b) -> a + "\n" + b));
        if (text.matches("(?s).*(table of contents|contents|indice|contenido).*")
                && text.lines().filter(line -> line.matches(".*\\.{2,}\\s*\\d+.*")).count() >= 2) {
            return PdfPageRole.INDEX;
        }
        if (text.matches("(?s).*(bibliography|references|bibliografia|referencias).*")
                && text.lines().count() >= 5) return PdfPageRole.BIBLIOGRAPHY;
        if (text.matches("(?s).*(catalog|catalogo|isbn|edition|edicion).*")
                && text.lines().count() >= 4) return PdfPageRole.CATALOG;
        long visual = safe.stream().filter(region -> region.effectiveType() == PdfRegionType.IMAGE
                || region.effectiveType() == PdfRegionType.CAPTION).count();
        if (visual > 0 && visual >= Math.max(1, safe.size() / 2)) return PdfPageRole.VISUAL_REFERENCE;
        long prose = safe.stream().filter(region -> region.effectiveText().length() >= 40).count();
        return prose > 0 ? PdfPageRole.CONTENT : PdfPageRole.UNKNOWN;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
