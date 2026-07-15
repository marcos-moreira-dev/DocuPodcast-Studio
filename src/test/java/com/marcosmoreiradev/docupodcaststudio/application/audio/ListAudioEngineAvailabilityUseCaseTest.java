package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ListAudioEngineAvailabilityUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void documentCatalogAlwaysKeepsOperativeFallbacksAndHidesBrokenAdvancedFromUsableSet() {
        ListAudioEngineAvailabilityUseCase useCase = new ListAudioEngineAvailabilityUseCase(
                new FixedSettingsRepository(OperationalSettings.defaults()), tempDir);

        List<AudioEngineAvailability> engines = useCase.list();

        assertTrue(engines.stream().anyMatch(engine -> engine.displayName().equals("Modo de prueba") && engine.usableInDocument()));
        assertTrue(engines.stream().anyMatch(engine -> engine.displayName().equals("Audio del computador") && engine.usableInDocument()));
        assertFalse(engines.stream().anyMatch(engine -> engine.displayName().equals("Voz IA avanzada") && engine.usableInDocument()));
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
