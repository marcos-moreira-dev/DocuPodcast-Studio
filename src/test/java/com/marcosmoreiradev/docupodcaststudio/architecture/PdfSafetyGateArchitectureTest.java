package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrails that keep generated PDF treatments pending until explicit review. */
final class PdfSafetyGateArchitectureTest {

    @Test
    void automaticPdfProducersCannotApproveGeneratedTreatments() throws Exception {
        String panel = source("presentation/document/DocumentContextDetailsPanel.java");
        String coordinator = source(
                "presentation/shell/workflow/PdfNarratablePreparationCoordinator.java");

        assertFalse(panel.contains("shouldApproveAutomatically"));
        assertEquals(1, occurrences(panel, ".reviewOrUpsert("),
                "the panel may approve only through its explicit review action");
        assertTrue(panel.indexOf(".reviewOrUpsert(")
                        > panel.indexOf("private void reviewPdfTreatment("),
                "the remaining approval call must belong to the explicit review method");
        assertFalse(coordinator.contains(".reviewOrUpsert("),
                "the automatic listening batch must persist drafts only");
    }

    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio").resolve(relative), StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(token, index)) >= 0;
             index += token.length()) {
            count++;
        }
        return count;
    }
}
