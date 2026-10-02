package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ListAudioEngineAvailabilityUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void documentCatalogKeepsEverySourceVisibleInTheStableProductOrder() {
        ListAudioEngineAvailabilityUseCase useCase = new ListAudioEngineAvailabilityUseCase(
                new FixedSettingsRepository(OperationalSettings.defaults()), tempDir);

        List<AudioEngineAvailability> engines = useCase.list();

        assertEquals(List.of("piper", "xtts", "mock", "computer-audio"),
                engines.stream().map(AudioEngineAvailability::engineId).toList());
        assertEquals(List.of("Voz local simple", "Voz IA avanzada", "Modo de prueba", "Audio del computador"),
                engines.stream().map(AudioEngineAvailability::displayName).toList());
        assertTrue(engines.stream().anyMatch(engine -> engine.displayName().equals("Modo de prueba") && engine.usableInDocument()));
        assertTrue(engines.stream().anyMatch(engine -> engine.displayName().equals("Audio del computador") && engine.usableInDocument()));
        assertFalse(engines.stream().anyMatch(engine -> engine.displayName().equals("Voz IA avanzada") && engine.usableInDocument()));
        AudioEngineAvailability unavailableAdvanced = engines.stream()
                .filter(engine -> engine.engineId().equals("xtts"))
                .findFirst().orElseThrow();
        assertFalse(unavailableAdvanced.userMessage().isBlank(),
                "Un motor no operativo debe seguir visible y explicar por qué está deshabilitado.");
        assertFalse(unavailableAdvanced.recommendedAction().isBlank());
    }

    private record FixedSettingsRepository(OperationalSettings settings) implements OperationalSettingsRepository {
        @Override
        public OperationalSettings load() {
            return settings;
        }

        @Override
        public void save(OperationalSettings settings) throws IOException {
            // not needed for this read-only catalog test
        }
    }
}
