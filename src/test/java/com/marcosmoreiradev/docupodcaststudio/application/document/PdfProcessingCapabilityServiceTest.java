package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class PdfProcessingCapabilityServiceTest {
    @Test
    void limitedProfileSerializesQwenAndTtsWithoutChangingProjectData() {
        PdfProcessingCapabilityProfile profile =
                PdfProcessingCapabilityService.profile(Set.of(
                        ContentAnalysisOperation.IMAGE_DESCRIPTION,
                        ContentAnalysisOperation.MATH_SPEECH), true,
                        8L * 1024 * 1024 * 1024);

        assertEquals(PdfProcessingCapabilityProfile.HardwareClass.LIMITED,
                profile.hardwareClass());
        assertTrue(profile.ocrAvailable());
        assertTrue(profile.visualDescriptionAvailable());
        assertTrue(profile.mathSpeechAvailable());
        assertFalse(profile.mathRecognitionAvailable());
        assertTrue(profile.serializeGenerativeAndTts());
        assertEquals(1, profile.maxParallelOcrPages());
    }

    @Test
    void standardProfileAllowsSafePrefetchAndKeepsCapabilitiesIndependent() {
        PdfProcessingCapabilityProfile profile =
                PdfProcessingCapabilityService.profile(Set.of(
                        ContentAnalysisOperation.TABLE_STRUCTURE_RECOGNITION,
                        ContentAnalysisOperation.MATH_RECOGNITION), false,
                        32L * 1024 * 1024 * 1024);

        assertEquals(PdfProcessingCapabilityProfile.HardwareClass.STANDARD,
                profile.hardwareClass());
        assertFalse(profile.ocrAvailable());
        assertTrue(profile.tableStructureAvailable());
        assertTrue(profile.mathRecognitionAvailable());
        assertFalse(profile.mathSpeechAvailable());
        assertFalse(profile.serializeGenerativeAndTts());
        assertEquals(3, profile.prefetchPages());
    }
}
