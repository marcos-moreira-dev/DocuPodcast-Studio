package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class XttsBatchDocumentGenerationSourceTest {
    @Test
    void documentGenerationUsesOneXttsProcessForPendingSegmentsWhenManagedRuntimeIsAvailable() throws Exception {
        String gateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));
        String script = Files.readString(Path.of("scripts/tts/xtts-batch-to-wav.ps1"));
        String wrapper = Files.readString(Path.of("tools/xtts-wrapper/synthesize_xtts_batch.py"));

        assertTrue(gateway.contains("runXttsBatchJobIfAvailable"));
        assertTrue(gateway.contains("xtts-batch-to-wav.ps1"));
        assertTrue(gateway.contains("synthesize_xtts_batch.py"));
        assertTrue(gateway.contains("DOCUPODCAST_XTTS_BATCH: segment_done="));
        assertTrue(script.contains("DOCUPODCAST_XTTS_FORCE_BATCH"));
        assertTrue(wrapper.contains("segments_total"));
        assertTrue(wrapper.contains("tts = TTS(model_path=str(model_root)"));
        assertTrue(wrapper.contains("tts.to(device)"));
        assertTrue(wrapper.contains("for item in items"));
    }
}
