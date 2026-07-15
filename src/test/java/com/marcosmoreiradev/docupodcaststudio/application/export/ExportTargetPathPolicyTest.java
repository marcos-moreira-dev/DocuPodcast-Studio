package com.marcosmoreiradev.docupodcaststudio.application.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ExportTargetPathPolicyTest {
    @Test
    void appendsRequiredExtensionWhenMissing() {
        assertEquals(Path.of("guion.md"), ExportTargetPathPolicy.ensureMarkdownExtension(Path.of("guion")));
        assertEquals(Path.of("podcast.wav"), ExportTargetPathPolicy.ensureWavExtension(Path.of("podcast")));
    }

    @Test
    void keepsExistingExtensionCaseInsensitive() {
        assertEquals(Path.of("guion.MD"), ExportTargetPathPolicy.ensureMarkdownExtension(Path.of("guion.MD")));
    }
}
