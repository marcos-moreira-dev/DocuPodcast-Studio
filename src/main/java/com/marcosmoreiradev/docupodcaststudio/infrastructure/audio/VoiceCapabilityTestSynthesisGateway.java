package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.application.voice.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Short voice test through the same registered capability as production jobs. */
public final class VoiceCapabilityTestSynthesisGateway implements VoiceTestSynthesisGateway {
    private final MediaCapabilityService media;
    private final LoadOperationalSettingsUseCase loadSettings;

    public VoiceCapabilityTestSynthesisGateway(MediaCapabilityService media, LoadOperationalSettingsUseCase loadSettings) {
        this.media = Objects.requireNonNull(media, "media capabilities");
        this.loadSettings = Objects.requireNonNull(loadSettings, "load settings");
    }

    @Override public VoiceTestSynthesisResult synthesize(VoiceTestSynthesisRequest request) throws IOException {
        OperationalSettings settings = loadSettings.load();
        EngineId selected = SelectedMediaEngines.from(settings).voice();
        VoiceSynthesisUnit unit = new VoiceSynthesisUnit(request.segmentId(), request.phrase(),
                request.voiceProfileId(), "", request.referenceSampleFile(), request.outputFile(), Map.of("purpose", "voice-test"));
        ExecutionContext context = new ExecutionContext("voice-test-" + request.segmentId(), CancellationToken.NONE,
                ProgressSink.NONE, new ExecutionPolicy(Duration.ofSeconds(settings.tts().timeoutSeconds()),
                settings.tts().maxRetries() + 1), ResourceLease.NONE);
        try {
            VoiceSynthesisBatchResult result = media.synthesize(selected,
                    new VoiceSynthesisBatchRequest(List.of(unit), request.language(), Map.of()), context);
            if (result.units().isEmpty() || !Files.isRegularFile(request.outputFile())) {
                return VoiceTestSynthesisResult.failed("El motor no produjo la prueba de voz.", "empty voice result");
            }
            return VoiceTestSynthesisResult.generated(Files.size(request.outputFile()), "Prueba de voz generada.",
                    result.diagnostics().toString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Prueba de voz cancelada.", ex);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return VoiceTestSynthesisResult.blocked(ex.getMessage(), true);
        }
    }
}
