package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.application.process.AiModelResourceGuard.AiModelResourceKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AiModelResourceGuardTest {
    @Test
    void blocksSecondAiModelUntilCurrentLeaseIsReleased() {
        AiModelResourceGuard guard = new AiModelResourceGuard();

        AiModelResourceGuard.Lease audio = guard.acquire(AiModelResourceKind.AUDIO, "chunks largos");

        AiModelResourceBusyException error = assertThrows(AiModelResourceBusyException.class,
                () -> guard.acquire(AiModelResourceKind.IMAGE, "intervencion 3"));

        assertTrue(error.getMessage().contains("No se puede cargar mas de un modelo de IA a la vez"));
        assertTrue(error.getMessage().contains("Generacion de audio"));
        assertEquals(AiModelResourceKind.AUDIO, guard.activeLease().orElseThrow().kind());

        audio.close();
        assertFalse(guard.activeLease().isPresent());

        try (AiModelResourceGuard.Lease image = guard.acquire(AiModelResourceKind.IMAGE, "intervencion 3")) {
            assertEquals(AiModelResourceKind.IMAGE, image.kind());
        }
    }
}
