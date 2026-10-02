package com.marcosmoreiradev.docupodcaststudio.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfigurationSchema;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class EngineRegistryContractTest {
    @Test
    void aNewEngineIsRegisteredAndSelectedWithoutProviderConditionals() {
        EngineRegistry<MediaEngine> registry = new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS);
        MediaEngine fake = fake("future-voice");

        registry.register(fake);

        assertSame(fake, registry.require(new EngineId("future-voice")));
        assertEquals(List.of("future-voice"), registry.descriptors().stream()
                .map(descriptor -> descriptor.id().value()).toList());
    }

    @Test
    void duplicateIdsFailAtCompositionTime() {
        EngineRegistry<MediaEngine> registry = new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS);
        registry.register(fake("same"));
        assertThrows(IllegalArgumentException.class, () -> registry.register(fake("same")));
    }

    private static MediaEngine fake(String id) {
        return new MediaEngine() {
            private final EngineDescriptor descriptor = new EngineDescriptor(new EngineId(id),
                    CapabilityId.VOICE_SYNTHESIS, "Fake", "1", "test", Set.of(), true);
            @Override public EngineDescriptor descriptor() { return descriptor; }
            @Override public EngineConfigurationSchema configurationSchema() {
                return new EngineConfigurationSchema(descriptor.id(), List.of());
            }
            @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
                return EngineReadiness.ready(descriptor.id(), "ready");
            }
        };
    }
}
