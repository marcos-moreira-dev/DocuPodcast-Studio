package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SettingsAwareAudioGenerationGatewayComputePolicyTest {
    @TempDir
    Path tempDir;

    @Test
    void autoAdvancedVoiceStaysCpuWhenCudaSmokeIsPending() {
        OperationalSettings settings = settings(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.AUTO, "", true, true, VideoEncoderPolicy.AUTO));

        assertEquals(ComputeDevicePolicy.CPU_ONLY,
                SettingsAwareAudioGenerationGateway.effectiveTtsComputePolicy(settings, "xtts-file-to-wav", tempDir));
        assertEquals("cpu",
                SettingsAwareAudioGenerationGateway.effectiveTtsComputeDeviceId(settings, "xtts-file-to-wav", tempDir));
    }

    @Test
    void specificAdvancedVoiceDeviceIsManualEvenWhenCudaSmokeIsPending() {
        OperationalSettings settings = settings(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.SPECIFIC_DEVICE, "gpu-intel-0", true, true, VideoEncoderPolicy.AUTO));

        assertEquals(ComputeDevicePolicy.SPECIFIC_DEVICE,
                SettingsAwareAudioGenerationGateway.effectiveTtsComputePolicy(settings, "xtts-file-to-wav", tempDir));
        assertEquals("gpu-intel-0",
                SettingsAwareAudioGenerationGateway.effectiveTtsComputeDeviceId(settings, "xtts-file-to-wav", tempDir));
    }

    @Test
    void explicitCpuAdvancedVoiceIsNotOverriddenByCudaSmoke() {
        OperationalSettings settings = settings(new OperationalSettings.ComputeSettings(
                ComputeDevicePolicy.SPECIFIC_DEVICE, "cpu", true, true, VideoEncoderPolicy.AUTO));

        assertEquals(ComputeDevicePolicy.CPU_ONLY,
                SettingsAwareAudioGenerationGateway.effectiveTtsComputePolicy(settings, "xtts-file-to-wav", tempDir));
        assertEquals("cpu",
                SettingsAwareAudioGenerationGateway.effectiveTtsComputeDeviceId(settings, "xtts-file-to-wav", tempDir));
    }

    private static OperationalSettings settings(OperationalSettings.ComputeSettings compute) {
        OperationalSettings defaults = OperationalSettings.defaults();
        return new OperationalSettings(
                defaults.readingDocument(),
                defaults.playbackBuffer(),
                new OperationalSettings.TtsEngineSettings("xtts", "", "Voz IA avanzada", "es", "VOC-NARRATOR", 900, 1),
                defaults.video(),
                compute,
                defaults.storage(),
                defaults.diagnostics());
    }
}
