package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class InkInputProviderFactoryTest {
    @Test
    void defaultProviderKeepsMouseBaselineAndNativePressureProvider() {
        InkInputProvider provider = InkInputProviderFactory.createDefault();

        assertTrue(provider instanceof MouseFirstInkInputProvider);
    }

    @Test
    void nativeOnlyProviderDoesNotExposeJavaFxMouseAsItsProvider() {
        InkInputProvider provider = InkInputProviderFactory.createNativeOnly();

        assertTrue(!(provider instanceof MouseFirstInkInputProvider));
        assertTrue(provider instanceof WindowsPointerInkInputProvider
                || provider instanceof LectureStudioStylusInputProvider
                || provider instanceof UnavailableInkInputProvider);
    }

    @Test
    void legacyLectureStudioEntryPointUsesTheNativeOnlyPipeline() {
        InkInputProvider provider = InkInputProviderFactory.createLectureStudioOnly();

        assertTrue(provider instanceof WindowsPointerInkInputProvider
                || provider instanceof LectureStudioStylusInputProvider
                || provider instanceof UnavailableInkInputProvider);
    }
}
