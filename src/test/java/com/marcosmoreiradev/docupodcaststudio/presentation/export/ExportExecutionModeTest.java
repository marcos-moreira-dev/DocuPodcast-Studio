package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportExecutionModeTest {
    @Test
    void onlyExplicitPreparationModeAuthorizesFullDocumentWork() {
        assertFalse(ExportExecutionMode.READY_ONLY.preparesFullDocument());
        assertTrue(ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT.preparesFullDocument());
    }
}
