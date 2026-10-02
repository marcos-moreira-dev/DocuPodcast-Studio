package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentRectangle;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;
import com.marcosmoreiradev.docupodcaststudio.application.video.NarratedFrameBinding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrationFragmentAuditWriterTest {
    @Test
    void detectsSubstantiallyRepeatedSourceRangeSeparatelyFromVisualQuality() {
        assertTrue(NarrationFragmentAuditWriter.duplicate(
                binding("S1", 0, 80, "La palabra decisiva es radianes en esta explicación"),
                binding("S2", 4, 78, "palabra decisiva es radianes en esta explicación")));
    }

    @Test
    void doesNotDeduplicateLegitimateConsecutiveFragmentsInSameRegion() {
        assertFalse(NarrationFragmentAuditWriter.duplicate(
                binding("S1", 0, 40, "Primera oración"),
                binding("S2", 41, 90, "Segunda oración")));
    }

    private static NarratedFrameBinding binding(String segment, int start, int end, String text) {
        var box = new DocumentContentRectangle(10, 10, 100, 30);
        return new NarratedFrameBinding("F-" + segment, segment, "R1", List.of("R1"),
                "R1", "PARAGRAPH", 1, 0, DocumentPresentationMode.SOURCE_CAPTURE,
                NarratedFrameBinding.VisualSource.PDF_REGION_CROP, box, box,
                "frame.png", "audio.wav", 1000, "", "", start, end, text,
                List.of(), List.of(), List.of(box), true,
                NarratedFrameBinding.Quality.CORRECT, "ok");
    }
}
