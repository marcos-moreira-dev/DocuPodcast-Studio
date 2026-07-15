package com.marcosmoreiradev.docupodcaststudio.application.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioEngineDescriptorTest {
    @Test
    void mockDescriptorIsNotRealTts() {
        AudioEngineDescriptor descriptor = AudioEngineDescriptor.mock();

        assertFalse(descriptor.realTts());
        assertTrue(descriptor.statusLabel().contains("mock"));
    }

    @Test
    void processDescriptorCanRepresentConfiguredRealTts() {
        AudioEngineDescriptor descriptor = AudioEngineDescriptor.process("XTTS local", true, "tts --text {textFile}", "ok");

        assertTrue(descriptor.realTts());
        assertTrue(descriptor.configured());
        assertTrue(descriptor.statusLabel().contains("TTS real"));
    }
}
