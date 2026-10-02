package com.marcosmoreiradev.docupodcaststudio.media.api;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

final class PdfVlmRuntimeProfileTest {
    @Test
    void defaultsPreserveMeasuredFourBProfile() {
        PdfVlmRuntimeProfile profile = PdfVlmRuntimeProfile.from(new Properties(), Map.of());
        assertEquals(PdfVlmRuntimeProfile.MODEL_4B_Q8, profile.model());
        assertEquals(8192, profile.contextTokens());
        assertEquals("q8_0", profile.kvCacheType());
        assertTrue(profile.flashAttention());
    }

    @Test
    void eightBProfileIsExplicitAndDoesNotChangeQuantization() {
        Properties properties = new Properties();
        properties.setProperty("docupodcast.pdfVlm.model", PdfVlmRuntimeProfile.MODEL_8B_Q8);
        PdfVlmRuntimeProfile profile = PdfVlmRuntimeProfile.from(properties, Map.of());
        assertEquals(PdfVlmRuntimeProfile.MODEL_8B_Q8, profile.model());
        assertEquals(8192, profile.contextTokens());
        assertEquals("q8_0", profile.kvCacheType());
        assertTrue(profile.modelHostMib() > PdfVlmRuntimeProfile.defaults().modelHostMib());
        assertFalse(profile.model().contains("q4"));
    }

    @Test
    void environmentCanSelectAllRuntimeControlsWithoutDomainChanges() {
        PdfVlmRuntimeProfile profile = PdfVlmRuntimeProfile.from(new Properties(), Map.of(
                "PDF_VLM_MODEL", PdfVlmRuntimeProfile.MODEL_8B_Q8,
                "PDF_VLM_CONTEXT", "8192",
                "PDF_VLM_NUM_PREDICT", "1800",
                "PDF_VLM_BATCH", "512",
                "PDF_VLM_KV_CACHE", "q8_0",
                "PDF_VLM_FLASH_ATTENTION", "true"));
        assertEquals(1800, profile.maxOutputTokens());
        assertEquals("8192", profile.requestOptions().get("contextWindowTokens"));
        assertEquals(PdfVlmRuntimeProfile.MODEL_8B_Q8,
                profile.requestOptions().get("model"));
    }
}
