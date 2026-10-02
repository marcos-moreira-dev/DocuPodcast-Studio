package com.marcosmoreiradev.docupodcaststudio.application.voice;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EffectiveVoiceAssignmentResolverTest {
    private final EffectiveVoiceAssignmentResolver resolver = new EffectiveVoiceAssignmentResolver();

    @Test
    void resolvesFragmentThenCharacterThenGlobalThenEngineDefault() {
        assertResolution("VOC-FRAGMENT", EffectiveVoiceAssignmentResolver.Source.FRAGMENT,
                Optional.of("VOC-FRAGMENT"), Optional.of("VOC-CHARACTER"), "VOC-GLOBAL");
        assertResolution("VOC-CHARACTER", EffectiveVoiceAssignmentResolver.Source.CHARACTER,
                Optional.empty(), Optional.of("VOC-CHARACTER"), "VOC-GLOBAL");
        assertResolution("VOC-GLOBAL", EffectiveVoiceAssignmentResolver.Source.GLOBAL,
                Optional.empty(), Optional.empty(), "VOC-GLOBAL");
        assertResolution("VOC-ENGINE", EffectiveVoiceAssignmentResolver.Source.ENGINE_DEFAULT,
                Optional.empty(), Optional.empty(), "VOC-NARRATOR", "VOC-ENGINE");
    }

    @Test
    void clearingFragmentVoiceRevealsCharacterVoiceWithoutChangingOtherLayers() {
        var withFragment = resolver.resolve(Optional.of("VOC-FRAGMENT"),
                Optional.of("VOC-CHARACTER"), "VOC-GLOBAL", "VOC-ENGINE");
        var cleared = resolver.resolve(Optional.empty(),
                Optional.of("VOC-CHARACTER"), "VOC-GLOBAL", "VOC-ENGINE");

        assertEquals("VOC-FRAGMENT", withFragment.voiceProfileId());
        assertEquals("VOC-CHARACTER", cleared.voiceProfileId());
        assertEquals(EffectiveVoiceAssignmentResolver.Source.CHARACTER, cleared.source());
    }

    private void assertResolution(String voiceId, EffectiveVoiceAssignmentResolver.Source source,
                                  Optional<String> fragment, Optional<String> character,
                                  String global) {
        assertResolution(voiceId, source, fragment, character, global, "VOC-ENGINE");
    }

    private void assertResolution(String voiceId, EffectiveVoiceAssignmentResolver.Source source,
                                  Optional<String> fragment, Optional<String> character,
                                  String global, String engineDefault) {
        var result = resolver.resolve(fragment, character, global, engineDefault);
        assertEquals(voiceId, result.voiceProfileId());
        assertEquals(source, result.source());
    }
}
