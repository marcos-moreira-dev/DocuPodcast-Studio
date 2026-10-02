package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPageRegion;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfTextLayerOrigin;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextHighlight;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTarget;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfVisualTextTargetKind;

import static org.junit.jupiter.api.Assertions.*;

final class PdfVisualDocumentViewStyleTest {

    @Test
    void narrationFocusIsTransparentEvenBeforeCssIsApplied() {
        Rectangle rectangle = new Rectangle();

        PdfVisualDocumentView.configureNarrationFocusRectangle(rectangle);

        Color fill = (Color) rectangle.getFill();
        assertTrue(fill.getOpacity() < 0.1);
        assertNotEquals(Color.BLACK, fill);
        assertEquals(4.0, rectangle.getStrokeWidth());
        assertTrue(rectangle.isMouseTransparent());
    }

    @Test
    void playbackHighlightUsesTwentyTwoPercentFillAndRemainsNonInteractive() {
        Rectangle rectangle = new Rectangle();

        PdfVisualDocumentView.configurePlaybackHighlightRectangle(
                rectangle, "pdf-visual-text-highlight");

        Color fill = (Color) rectangle.getFill();
        assertEquals(0.22, fill.getOpacity(), 0.001);
        assertEquals(1.0, rectangle.getOpacity());
        assertEquals(Color.TRANSPARENT, rectangle.getStroke());
        assertTrue(rectangle.isMouseTransparent());
        assertTrue(rectangle.getStyleClass().contains("pdf-visual-text-highlight"));
    }

    @Test
    void stylesheetKeepsPlaybackHoverAndFixedSelectionVisuallyDistinct() throws Exception {
        String css = Files.readString(Path.of(
                "src/main/resources/css/document/pdf-visual-viewer.css"),
                StandardCharsets.UTF_8);

        assertTrue(css.contains(".pdf-visual-text-highlight {\n    -fx-fill: rgba(125, 211, 252, 0.22);"));
        assertTrue(css.contains(".pdf-visual-text-hover {\n    -fx-fill: transparent;"));
        assertTrue(css.contains(".pdf-visual-text-selected {\n    -fx-fill: transparent;"));
        assertTrue(css.contains("-fx-stroke: #6d28d9;"));
    }

    @Test
    void textPaddingDefaultsToTwelveHorizontalAndTwentyFiveVerticalPixels() {
        System.clearProperty(PdfVisualDocumentView.TEXT_PADDING_HORIZONTAL_PROPERTY);
        System.clearProperty(PdfVisualDocumentView.TEXT_PADDING_VERTICAL_PROPERTY);
        assertEquals(12.0, PdfVisualDocumentView.textPaddingHorizontalPixels());
        assertEquals(25.0, PdfVisualDocumentView.textPaddingVerticalPixels());
        try {
            System.setProperty(PdfVisualDocumentView.TEXT_PADDING_HORIZONTAL_PROPERTY, "18.5");
            System.setProperty(PdfVisualDocumentView.TEXT_PADDING_VERTICAL_PROPERTY, "100");
            assertEquals(18.5, PdfVisualDocumentView.textPaddingHorizontalPixels());
            assertEquals(40.0, PdfVisualDocumentView.textPaddingVerticalPixels());
        } finally {
            System.clearProperty(PdfVisualDocumentView.TEXT_PADDING_HORIZONTAL_PROPERTY);
            System.clearProperty(PdfVisualDocumentView.TEXT_PADDING_VERTICAL_PROPERTY);
        }
    }

    @Test
    void paddingIsAsymmetricAndClippedToTheVisiblePage() {
        var tight = new PdfVisualDocumentView.VisualBox(100, 80, 820, 140);
        var page = new PdfVisualDocumentView.VisualBox(0, 0, 825, 500);

        var visible = PdfVisualDocumentView.paddedAndClipped(tight, page, 12, 25);

        assertEquals(88.0, visible.xMin());
        assertEquals(825.0, visible.xMax(), "right padding is clipped by the page");
        assertEquals(55.0, visible.yMin());
        assertEquals(165.0, visible.yMax());
    }

    @Test
    void playbackFixedAndHoverShareTheTargetTextEnvelope() {
        PdfPageRegion semantic = new PdfPageRegion(1, 40, 60, 580, 300, 612, 792);
        PdfPageRegion tight = new PdfPageRegion(1, 72, 90, 410, 180, 612, 792);
        PdfVisualTextTarget target = new PdfVisualTextTarget(
                "P", "P", 0, 20, 1, semantic, List.of(tight), List.of("P"),
                "Párrafo", PdfTextLayerOrigin.OCR_LOCAL, PdfVisualTextTargetKind.BLOCK);

        PdfPageRegion playback = target.highlight().visibleRegion();
        PdfPageRegion fixed = PdfVisualDocumentView.textOverlayRegion(target);
        PdfPageRegion hover = PdfVisualDocumentView.textOverlayRegion(target);

        assertEquals(playback, fixed);
        assertEquals(playback, hover);
        assertEquals(semantic, target.region(), "semantic bbox remains canonical");
    }

    @Test
    void zoomChangesProjectionScaleButNotCanonicalBoundsOrPixelPadding() {
        PdfPageRegion canonical = new PdfPageRegion(1, 72, 100, 420, 142, 612, 792);
        for (double zoom : List.of(0.75, 1.0, 1.25, 1.5)) {
            double width = 612 * zoom;
            double height = 792 * zoom;
            var viewport = new com.marcosmoreiradev.docupodcaststudio.application.document
                    .PdfPageCoordinateTransform(612, 792, 0, width, height)
                    .toViewport(canonical, 0, 0, width, height);
            var visible = PdfVisualDocumentView.paddedAndClipped(
                    new PdfVisualDocumentView.VisualBox(viewport.xMin(), viewport.yMin(),
                            viewport.xMax(), viewport.yMax()),
                    new PdfVisualDocumentView.VisualBox(0, 0, width, height), 12, 25);
            assertEquals(viewport.xMax() + 12, visible.xMax(), 0.001);
            assertEquals(viewport.yMax() + 25, visible.yMax(), 0.001);
            assertEquals(420.0, canonical.xMaxPoints(), 0.001);
        }
    }

    @Test
    void activePlaybackSuppressesNarrationFocusAndOverlappingHover() {
        PdfPageRegion box = new PdfPageRegion(1, 100, 100, 400, 300, 612, 792);
        PdfVisualTextHighlight active = new PdfVisualTextHighlight(
                1, List.of(box), "Imagen", PdfTextLayerOrigin.NATIVE_BBOX);
        PdfVisualTextTarget sameTarget = new PdfVisualTextTarget(
                "IMAGE", "IMAGE", 0, 6, 1, box, "Imagen",
                PdfTextLayerOrigin.NATIVE_BBOX,
                PdfVisualTextTargetKind.SEMANTIC_COMPONENT);

        assertFalse(PdfVisualDocumentView.shouldShowNarrationFocus(active, 1));
        assertTrue(PdfVisualDocumentView.shouldShowNarrationFocus(active, 2));
        assertFalse(PdfVisualDocumentView.shouldShowHover(active, sameTarget));
    }
}
