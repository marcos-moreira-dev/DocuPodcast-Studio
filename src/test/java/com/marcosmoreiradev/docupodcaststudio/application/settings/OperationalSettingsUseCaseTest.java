package com.marcosmoreiradev.docupodcaststudio.application.settings;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OperationalSettingsUseCaseTest {
    @Test
    void defaultsKeepReaderSimpleAndExposeOperationalWarehouseValues() {
        OperationalSettings settings = OperationalSettings.defaults();

        assertEquals(18, settings.readingDocument().baseFontSize());
        assertEquals(5, settings.playbackBuffer().initialReadySegments());
        assertEquals(10, settings.playbackBuffer().lookaheadSegments());
        assertEquals("piper", settings.tts().engineMode());
        assertEquals("2K", settings.video().resolutionPreset());
        assertEquals("AUTO", settings.compute().policy().name());
        assertEquals("AUTO", settings.compute().videoEncoderPolicy().name());
        assertEquals("managed-local", settings.ocr().engineMode());
        assertEquals("spa+eng", settings.ocr().languages());
    }

    @Test
    void saveUseCasePersistsOnlyWhenValidationHasNoErrors() throws IOException {
        InMemoryRepository repository = new InMemoryRepository();
        ValidateOperationalSettingsUseCase validator = new ValidateOperationalSettingsUseCase();
        SaveOperationalSettingsUseCase save = new SaveOperationalSettingsUseCase(repository, validator);
        OperationalSettings custom = new OperationalSettings(
                new OperationalSettings.ReadingDocumentSettings(21, 1.5, true),
                new OperationalSettings.PlaybackBufferSettings(4, 12, true),
                new OperationalSettings.TtsEngineSettings("external", "piper --text {textFile} --output {outputFile}", "Piper local", "es", "VOC-NARRATOR", 120, 2),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());

        var report = save.save(custom);

        assertTrue(report.errors().isEmpty());
        assertTrue(repository.saved);
        assertEquals(21, repository.loaded.readingDocument().baseFontSize());
        assertEquals(12, repository.loaded.playbackBuffer().lookaheadSegments());
    }

    private static final class InMemoryRepository implements OperationalSettingsRepository {
        private OperationalSettings loaded = OperationalSettings.defaults();
        private boolean saved;

        @Override
        public OperationalSettings load() {
            return loaded;
        }

        @Override
        public void save(OperationalSettings settings) {
            loaded = settings;
            saved = true;
        }
    }
}
