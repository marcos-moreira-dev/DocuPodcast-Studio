package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkInputProviderFactoryTest {
    @Test
    void lectureStudioOnlyProviderDoesNotReportJavaFxMouseFallback() {
        InkInputProvider provider = InkInputProviderFactory.createLectureStudioOnly();
        InkInputCapabilities capabilities = provider.capabilities();

        assertNotEquals("JavaFX mouse", capabilities.providerName());
        assertTrue(capabilities.providerName().startsWith("LectureStudio stylus"));
    }
}
