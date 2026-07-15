package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class SettingsAwareGatewayRepairPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void voiceTestGatewayPersistsRepairedLegacySettingsBeforePreflight() throws Exception {
        InMemorySettingsRepository repository = new InMemorySettingsRepository(legacySettings());
        SettingsAwareVoiceTestSynthesisGateway gateway = new SettingsAwareVoiceTestSynthesisGateway(repository, tempDir);

        gateway.synthesize(new VoiceTestSynthesisRequest(
                "voice-test",
                "hola",
                "es",
                "VOC-NARRATOR",
                null,
                tempDir.resolve("voice-test.txt"),
                tempDir.resolve("voice-test.wav"),
                tempDir,
                AudioEngineDescriptor.mock()));

        assertRepaired(repository.saved);
    }

    @Test
    void audioGatewayPersistsRepairedLegacySettingsBeforeResolvingDelegate() {
        InMemorySettingsRepository repository = new InMemorySettingsRepository(legacySettings());
        SettingsAwareAudioGenerationGateway gateway = new SettingsAwareAudioGenerationGateway(
                repository,
                tempDir,
                new InMemoryAudioJobQueue(),
                new AudioJobRepositoryStub(),
                new AudioProcessDiagnosticsRepositoryStub());

        gateway.engineDescriptor();

        assertRepaired(repository.saved);
    }

    private static void assertRepaired(OperationalSettings saved) {
        assertNotNull(saved);
        assertEquals("xtts", saved.tts().engineMode());
        assertEquals("", saved.tts().commandTemplate());
        assertEquals("models/tts/xtts", saved.storage().modelsDirectory().replace('\\', '/'));
    }

    private static OperationalSettings legacySettings() {
        return new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings(
                        "external",
                        "powershell -File C:/old/scripts/tts/Voz IA avanzada-file-to-wav.ps1 -ModelDir \"C:/old/recursos locales IA avanzada/model.pth\"",
                        "Voz IA avanzada",
                        "es",
                        "VOC-NARRATOR",
                        180,
                        1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings("C:/old/recursos locales IA avanzada/model.pth", "exports"),
                OperationalSettings.DiagnosticSettings.defaults());
    }

    private static final class InMemorySettingsRepository implements OperationalSettingsRepository {
        private final OperationalSettings loaded;
        private OperationalSettings saved;

        private InMemorySettingsRepository(OperationalSettings loaded) {
            this.loaded = loaded;
        }

        @Override
        public OperationalSettings load() {
            return loaded;
        }

        @Override
        public void save(OperationalSettings settings) {
            saved = settings;
        }
    }

    private static final class AudioJobRepositoryStub implements com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository {
        @Override
        public void save(Path projectDirectory, com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot snapshot) {
        }

        @Override
        public java.util.Optional<com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot> load(Path projectDirectory, String jobId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.List<com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot> list(Path projectDirectory) {
            return java.util.List.of();
        }

        @Override
        public void deleteAll(Path projectDirectory) {
        }
    }

    private static final class AudioProcessDiagnosticsRepositoryStub implements com.marcosmoreiradev.docupodcaststudio.application.audio.AudioProcessDiagnosticsRepository {
        @Override
        public void append(Path projectDirectory, String jobId, com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent event) {
        }

        @Override
        public java.util.List<com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent> list(Path projectDirectory, String jobId) {
            return java.util.List.of();
        }
    }
}
