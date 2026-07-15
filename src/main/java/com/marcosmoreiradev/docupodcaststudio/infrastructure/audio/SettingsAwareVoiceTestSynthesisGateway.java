package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsRepository;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsMigrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisResult;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves persisted voice settings at the moment a short voice test is generated. */
public final class SettingsAwareVoiceTestSynthesisGateway implements VoiceTestSynthesisGateway {
    private final OperationalSettingsRepository settingsRepository;
    private final Path applicationRoot;
    private final ExternalProcessRunner processRunner;

    public SettingsAwareVoiceTestSynthesisGateway(OperationalSettingsRepository settingsRepository, Path applicationRoot) {
        this(settingsRepository, applicationRoot, new DefaultExternalProcessRunner());
    }

    public SettingsAwareVoiceTestSynthesisGateway(OperationalSettingsRepository settingsRepository,
                                                  Path applicationRoot,
                                                  ExternalProcessRunner processRunner) {
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.applicationRoot = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        this.processRunner = processRunner == null ? new DefaultExternalProcessRunner() : processRunner;
    }

    @Override
    public VoiceTestSynthesisResult synthesize(VoiceTestSynthesisRequest request) throws IOException {
        OperationalSettings settings = loadAndPersistRepairedSettings();
        String command = SettingsAwareAudioGenerationGateway.resolveTtsCommandTemplate(settings, applicationRoot);
        LocalTtsProcessConfiguration configuration = new LocalTtsProcessConfiguration(
                command,
                SettingsAwareAudioGenerationGateway.effectiveTtsDisplayName(settings, command),
                settings.tts().language(),
                settings.tts().voiceProfileId(),
                settings.tts().timeoutSeconds(),
                settings.tts().maxRetries(),
                SettingsAwareAudioGenerationGateway.effectiveTtsComputePolicy(settings, command, applicationRoot),
                SettingsAwareAudioGenerationGateway.effectiveTtsComputeDeviceId(settings, command, applicationRoot));
        if (!configuration.enabled()) {
            return VoiceTestSynthesisResult.blocked(
                    "No hay una voz real lista. Abre Configuración inicial y prepara el equipo.", true);
        }
        return new LocalProcessVoiceTestSynthesisGateway(configuration, processRunner).synthesize(request);
    }

    private OperationalSettings loadAndPersistRepairedSettings() throws IOException {
        OperationalSettings loaded = loadSettings();
        OperationalSettings repaired = OperationalSettingsMigrationPolicy.repair(loaded);
        try {
            settingsRepository.save(repaired);
        } catch (IOException ignored) {
            // Voice tests must still use the repaired in-memory settings even if persistence fails.
        }
        return repaired;
    }

    private OperationalSettings loadSettings() throws IOException {
        try {
            return settingsRepository.load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }
}
