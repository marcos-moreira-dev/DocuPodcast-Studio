package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

/** Idempotently stores an admitted PDF content ROI inside the project media tree. */
public final class MaterializePdfDocumentContentAssetUseCase {
    public static final int DPI = 288;
    public static final double MARGIN_POINTS = 12.0;
    public static final double VIDEO_CONTEXT_PADDING_POINTS = 18.0;
    public static final long MAX_PIXEL_COUNT = 32_000_000L;
    private static final String DIRECTORY = "media/images/document-study/pdf-roi";

    private final CapturePdfVisualRegionUseCase capture;

    public MaterializePdfDocumentContentAssetUseCase(CapturePdfVisualRegionUseCase capture) {
        this.capture = Objects.requireNonNull(capture, "capture");
    }

    public Result materialize(DocumentContentItem content, Path sourcePdf,
                              Path projectDirectory) throws IOException {
        return materialize(content, List.of(content), sourcePdf, projectDirectory);
    }

    public Result materialize(DocumentContentItem content,
                              List<DocumentContentItem> pageContent,
                              Path sourcePdf, Path projectDirectory) throws IOException {
        Objects.requireNonNull(content, "content");
        PdfContentAnchor anchor = content.pdfAnchor().orElseThrow(() ->
                new IllegalArgumentException("PDF content anchor is required"));
        Path root = Objects.requireNonNull(projectDirectory, "projectDirectory")
                .toAbsolutePath().normalize();
        PdfContentAnchor captureAnchor = new PdfContentAnchor(anchor.pageNumber(),
                anchor.pageWidthPoints(), anchor.pageHeightPoints(),
                sourceCaptureBounds(content, anchor), anchor.sourceRegionIds(),
                anchor.sourceRegionBounds(), anchor.sourceRegionTexts(),
                anchor.unreliableSourceRegionIds(), anchor.revision(),
                anchor.visualFingerprint());
        CropDecision crop = captureAnchor.unreliableSourceRegionIds().isEmpty()
                ? siblingAwareCrop(captureAnchor, pageContent, VIDEO_CONTEXT_PADDING_POINTS)
                : fullPageFallback(captureAnchor);
        String effectiveFingerprint = fingerprint(anchor.visualFingerprint()
                + "|video-crop-v1|" + crop.finalCrop());
        String shortFingerprint = effectiveFingerprint.substring(
                0, Math.min(24, effectiveFingerprint.length()));
        String fileName = token(content.contentId()).toLowerCase(Locale.ROOT)
                + "-" + shortFingerprint + ".png";
        Path target = root.resolve(DIRECTORY).resolve(fileName).normalize();
        if (!target.startsWith(root.resolve(DIRECTORY).normalize())) {
            throw new IOException("Ruta de asset PDF fuera del proyecto.");
        }
        boolean reused = Files.isRegularFile(target);
        if (!reused) {
            var roi = crop.finalCrop();
            capture.capture(new PdfRegionCaptureRequest(sourcePdf,
                    new PdfViewportSelection(anchor.pageNumber(), roi.xMin(), roi.yMin(),
                            roi.xMax(), roi.yMax(), anchor.pageWidthPoints(),
                            anchor.pageHeightPoints(), anchor.pageWidthPoints(),
                            anchor.pageHeightPoints()),
                    target, 0.0, DPI, MAX_PIXEL_COUNT, Color.WHITE, true));
        }
        ProjectAssetReference asset = DocumentSourceVisualAssetReference.create(
                content, root, target, effectiveFingerprint);
        return new Result(target, asset, effectiveFingerprint, reused, crop);
    }

    public record Result(Path path, ProjectAssetReference asset,
                         String visualFingerprint, boolean reused,
                         CropDecision cropDecision) {
        public String projectRelativePath() {
            return asset.relativePath();
        }
    }

    /** Video-only crop policy. It never mutates the canonical PdfRegion/anchor. */
    static CropDecision siblingAwareCrop(PdfContentAnchor owner,
                                         List<DocumentContentItem> pageContent,
                                         double desiredPadding) {
        DocumentContentRectangle source = owner.roi();
        double nearestTop = 0.0;
        double nearestBottom = owner.pageHeightPoints();
        double nearestLeft = 0.0;
        double nearestRight = owner.pageWidthPoints();
        List<DocumentContentItem> siblings = pageContent == null ? List.of() : pageContent;
        for (DocumentContentItem candidate : siblings) {
            PdfContentAnchor sibling = candidate == null ? null : candidate.pdfAnchor().orElse(null);
            if (sibling == null || sibling == owner || sibling.pageNumber() != owner.pageNumber()
                    || sibling.sourceRegionIds().equals(owner.sourceRegionIds())) continue;
            DocumentContentRectangle box = sibling.roi();
            boolean horizontalOverlap = box.xMax() > source.xMin() && box.xMin() < source.xMax();
            boolean verticalOverlap = box.yMax() > source.yMin() && box.yMin() < source.yMax();
            if (horizontalOverlap && box.yMax() <= source.yMin()) {
                nearestTop = Math.max(nearestTop, box.yMax());
            }
            if (horizontalOverlap && box.yMin() >= source.yMax()) {
                nearestBottom = Math.min(nearestBottom, box.yMin());
            }
            if (verticalOverlap && box.xMax() <= source.xMin()) {
                nearestLeft = Math.max(nearestLeft, box.xMax());
            }
            if (verticalOverlap && box.xMin() >= source.xMax()) {
                nearestRight = Math.min(nearestRight, box.xMin());
            }
        }
        double padding = Math.max(0.0, desiredPadding);
        double topPadding = nearestTop > 0.0
                ? boundedPadding(source.yMin() - nearestTop, padding) : padding;
        double bottomPadding = nearestBottom < owner.pageHeightPoints()
                ? boundedPadding(nearestBottom - source.yMax(), padding) : padding;
        double leftPadding = nearestLeft > 0.0
                ? boundedPadding(source.xMin() - nearestLeft, padding) : padding;
        double rightPadding = nearestRight < owner.pageWidthPoints()
                ? boundedPadding(nearestRight - source.xMax(), padding) : padding;
        DocumentContentRectangle result = new DocumentContentRectangle(
                Math.max(0.0, source.xMin() - leftPadding),
                Math.max(0.0, source.yMin() - topPadding),
                Math.min(owner.pageWidthPoints(), source.xMax() + rightPadding),
                Math.min(owner.pageHeightPoints(), source.yMax() + bottomPadding));
        return new CropDecision(source, padding, nearestTop, nearestBottom,
                nearestLeft, nearestRight, result, "specialized-bbox+sibling-limits");
    }

    /**
     * An explicitly unreliable canonical bbox must never produce a precise-looking
     * but semantically wrong thumbnail. Preview and export therefore share the
     * same conservative, identity-preserving full-page fallback.
     */
    static CropDecision fullPageFallback(PdfContentAnchor owner) {
        DocumentContentRectangle page = new DocumentContentRectangle(0.0, 0.0,
                owner.pageWidthPoints(), owner.pageHeightPoints());
        return new CropDecision(owner.roi(), 0.0, 0.0, owner.pageHeightPoints(),
                0.0, owner.pageWidthPoints(), page,
                "page-fallback:unreliable-source-geometry");
    }

    private static double boundedPadding(double siblingGap, double desiredPadding) {
        return Math.min(desiredPadding, Math.max(0.0, siblingGap) / 2.0);
    }

    public record CropDecision(DocumentContentRectangle sourceBounds,
                               double desiredPaddingPoints,
                               double nearestSiblingTop,
                               double nearestSiblingBottom,
                               double nearestSiblingLeft,
                               double nearestSiblingRight,
                               DocumentContentRectangle finalCrop,
                               String cropSource) { }

    /**
     * Figures frequently own labels/captions that extend beyond the model's
     * tight object box. Preserve their semantic vertical unit while allowing a
     * page-safe horizontal context. Canonical PDF geometry remains untouched.
     */
    static DocumentContentRectangle sourceCaptureBounds(
            DocumentContentItem content, PdfContentAnchor anchor) {
        DocumentContentRectangle semantic = anchor.roi();
        if (content.kind() == DocumentContentKind.PROSE
                && containsVisualMath(anchor.sourceRegionTexts())) {
            // Mixed text+math regions are sometimes bounded by the text baseline
            // while the displayed formula lives immediately below it. Preserve
            // the complete semantic unit; neighbouring text is acceptable.
            return new DocumentContentRectangle(semantic.xMin(),
                    Math.max(0.0, semantic.yMin() - VIDEO_CONTEXT_PADDING_POINTS),
                    semantic.xMax(),
                    Math.min(anchor.pageHeightPoints(),
                            semantic.yMax() + VIDEO_CONTEXT_PADDING_POINTS));
        }
        if (content.kind() != DocumentContentKind.IMAGE) return semantic;
        double pageMargin = Math.min(36.0, anchor.pageWidthPoints() * 0.06);
        return new DocumentContentRectangle(
                Math.min(semantic.xMin(), pageMargin), semantic.yMin(),
                Math.max(semantic.xMax(), anchor.pageWidthPoints() - pageMargin),
                Math.min(anchor.pageHeightPoints(), semantic.yMax() + 12.0));
    }

    private static boolean containsVisualMath(Map<String, String> sourceTexts) {
        if (sourceTexts == null || sourceTexts.isEmpty()) return false;
        return sourceTexts.values().stream().filter(Objects::nonNull).anyMatch(text ->
                text.indexOf('\\') >= 0 || text.indexOf('≈') >= 0
                        || text.indexOf('≤') >= 0 || text.indexOf('≥') >= 0
                        || text.indexOf('=') >= 0 || text.indexOf('<') >= 0
                        || text.indexOf('>') >= 0);
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String token(String value) {
        String normalized = value == null ? "content" : value.strip();
        String result = normalized.replaceAll("[^A-Za-z0-9_-]", "-")
                .replaceAll("-+", "-");
        return result.isBlank() ? "content" : result;
    }
}
