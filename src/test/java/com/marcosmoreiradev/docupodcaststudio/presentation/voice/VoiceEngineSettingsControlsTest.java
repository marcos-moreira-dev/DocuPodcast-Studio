package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.VoiceEngineOperationalState;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeEnvironmentReport;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.TtsEngineModes;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioControlContract;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceEngineSettingsControlsTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void exposesStyledKeyboardReachableChoicesInProductOrder() throws Exception {
        fx(() -> {
            FakeBackend backend = new FakeBackend(OperationalSettings.defaults(), true);
            VoiceEngineSettingsControls controls = new VoiceEngineSettingsControls(backend, ignored -> { });
            VBox section = controls.selectionSection();
            controls.refresh();
            new Scene(section, 420, 480);

            ComboBox<VoiceEngineSettingsControls.EngineModeChoice> engines = controls.engineModeSelector();
            assertEquals(List.of("Voz local simple", "Voz IA avanzada",
                            "Qwen3-TTS local · 1.7B Q8", "Modo de prueba"),
                    engines.getItems().stream().map(VoiceEngineSettingsControls.EngineModeChoice::label).toList());
            assertEquals(List.of(TtsEngineModes.LOCAL_SIMPLE, TtsEngineModes.ADVANCED_AI,
                            "qwen3-tts-local", TtsEngineModes.TEST),
                    engines.getItems().stream().map(VoiceEngineSettingsControls.EngineModeChoice::id).toList());
            assertEquals("Voz local simple", engines.getValue().label());
            assertTrue(engines.isFocusTraversable());
            assertTrue(controls.computeDeviceSelector().isFocusTraversable());
            assertTrue(StudioControlContract.descriptor(engines).isPresent());
            assertTrue(StudioControlContract.descriptor(controls.computeDeviceSelector()).isPresent());
            assertNotNull(section.lookup("#" + VoiceEngineSettingsControls.STATUS_ID));
            return null;
        });
    }

    @Test
    void unavailableDiagnosticModeRemainsVisibleButCannotReplaceActiveEngine() throws Exception {
        fx(() -> {
            FakeBackend backend = new FakeBackend(OperationalSettings.defaults(), false);
            List<List<String>> summaries = new ArrayList<>();
            VoiceEngineSettingsControls controls = new VoiceEngineSettingsControls(backend, summaries::add);
            controls.refresh();

            VoiceEngineSettingsControls.EngineModeChoice diagnostic =
                    controls.engineModeSelector().getItems().get(3);
            assertFalse(diagnostic.available());
            controls.engineModeSelector().setValue(diagnostic);

            assertEquals("Voz local simple", controls.engineModeSelector().getValue().label());
            assertEquals(0, backend.saveCount);
            assertTrue(summaries.getLast().getFirst().contains("no está disponible"));
            return null;
        });
    }

    @Test
    void engineAndGpuSelectionsPersistAndPreserveUnrelatedModernSettings() throws Exception {
        fx(() -> {
            OperationalSettings initial = OperationalSettings.defaults();
            FakeBackend backend = new FakeBackend(initial, true);
            VoiceEngineSettingsControls controls = new VoiceEngineSettingsControls(backend, ignored -> { });
            controls.refresh();

            controls.engineModeSelector().getSelectionModel().select(2);
            assertEquals("qwen3-tts-local", backend.selectedEngineId);
            assertEquals("qwen3-tts-local", backend.current.mediaEngines().voiceEngineId());

            VoiceEngineSettingsControls.ComputeDeviceChoice gpu = controls.computeDeviceSelector()
                    .getItems().stream().filter(choice -> "cuda:0".equals(choice.id())).findFirst().orElseThrow();
            controls.computeDeviceSelector().setValue(gpu);

            assertEquals(ComputeDevicePolicy.SPECIFIC_DEVICE, backend.current.compute().policy());
            assertEquals("cuda:0", backend.current.compute().selectedDeviceId());
            assertTrue(backend.current.compute().allowGpuForTts());
            assertEquals(initial.compute().allowGpuForVideo(), backend.current.compute().allowGpuForVideo());
            assertEquals(initial.compute().allowGpuForContentAnalysis(),
                    backend.current.compute().allowGpuForContentAnalysis());
            assertEquals(initial.compute().allowRamOffloadForContentAnalysis(),
                    backend.current.compute().allowRamOffloadForContentAnalysis());
            assertSame(initial.imageSuperResolution(), backend.current.imageSuperResolution());
            assertEquals("qwen3-tts-local", backend.current.mediaEngines().voiceEngineId());
            assertEquals(2, backend.voiceTestResetCount);
            assertTrue(controls.operationalStatus().getText().contains("Qwen3-TTS"));
            assertTrue(controls.operationalStatus().getText().contains("NVIDIA RTX"));
            return null;
        });
    }

    private static final class FakeBackend implements VoiceEngineSettingsControls.Backend {
        private OperationalSettings current;
        private final boolean diagnosticReady;
        private int saveCount;
        private int voiceTestResetCount;
        private String selectedEngineId = "";

        private FakeBackend(OperationalSettings current, boolean diagnosticReady) {
            this.current = current;
            this.diagnosticReady = diagnosticReady;
        }

        @Override public OperationalSettings load() { return current; }

        @Override
        public void save(OperationalSettings settings) {
            current = settings;
            saveCount++;
        }

        @Override
        public ComputeEnvironmentReport inspectCompute(OperationalSettings settings) {
            return new ComputeEnvironmentReport(settings.compute().policy(),
                    settings.compute().selectedDeviceId(),
                    List.of(ComputeDeviceDescriptor.cpu("CPU local"),
                            ComputeDeviceDescriptor.gpu("cuda:0", "NVIDIA RTX", "NVIDIA")),
                    List.of(), List.of());
        }

        @Override
        public List<VoiceEngineOperationalState> operationalStates() {
            return List.of(state(TtsEngineModes.LOCAL_SIMPLE, "Voz local simple", true),
                    state(TtsEngineModes.ADVANCED_AI, "Voz IA avanzada", true),
                    state("qwen3-tts-local", "Qwen3-TTS local · 1.7B Q8", true),
                    state(TtsEngineModes.TEST, "Modo de prueba", diagnosticReady));
        }

        @Override
        public OperationalSettings selectEngine(OperationalSettings source, String engineId) {
            selectedEngineId = engineId;
            OperationalSettings.MediaEngineSelectionSettings media = source.mediaEngines();
            OperationalSettings.MediaEngineSelectionSettings selected =
                    new OperationalSettings.MediaEngineSelectionSettings(engineId,
                            media.imageEngineId(), media.videoGenerationEngineId(), media.videoRenderEngineId());
            OperationalSettings.TtsEngineSettings previous = source.tts();
            OperationalSettings.TtsEngineSettings tts = new OperationalSettings.TtsEngineSettings(
                    engineId, previous.commandTemplate(), engineId, previous.language(),
                    previous.voiceProfileId(), previous.timeoutSeconds(), previous.maxRetries(),
                    previous.xttsDownloadBaseUrl(), previous.piperRuntimeZipUrl(),
                    previous.piperDefaultVoiceUrl(), previous.piperDefaultVoiceMetadataUrl());
            return new OperationalSettings(source.readingDocument(), source.playbackBuffer(), tts,
                    source.video(), source.imageGeneration(), source.imageSuperResolution(), selected,
                    source.frameGeneration(), source.compute(), source.ocr(), source.storage(),
                    source.diagnostics());
        }

        @Override
        public void resetVoiceTest(String message) {
            voiceTestResetCount++;
        }

        private static VoiceEngineOperationalState state(String id, String name, boolean ready) {
            return new VoiceEngineOperationalState(id, name, true, ready, true,
                    TtsEngineModes.ADVANCED_AI.equals(id) || "qwen3-tts-local".equals(id),
                    TtsEngineModes.ADVANCED_AI.equals(id) || "qwen3-tts-local".equals(id),
                    ready ? "Listo" : "No disponible",
                    ready ? name + " listo para sintetizar." : name + " no está registrado.",
                    List.of(), List.of());
        }
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { value.set(action.call()); }
            catch (Throwable throwable) { failure.set(throwable); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() instanceof Exception exception) throw exception;
        if (failure.get() != null) throw new AssertionError(failure.get());
        return value.get();
    }
}
