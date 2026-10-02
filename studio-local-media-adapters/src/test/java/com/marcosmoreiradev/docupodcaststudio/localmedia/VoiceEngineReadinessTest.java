package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceEngineReadinessTest {
    @TempDir Path temporary;

    @Test
    void productionCatalogDeclaresEveryArtifactUsedByPiperAndXtts() throws Exception {
        materializeProductionVoiceLayout(temporary);

        MediaEnginePlatform platform = LocalMediaAdapters.create(
                new LocalMediaLayout(temporary, temporary));

        assertTrue(platform.voiceEngines().require(PiperVoiceEngine.ID)
                .inspectReadiness(null).ready());
        assertTrue(platform.voiceEngines().require(XttsVoiceEngine.ID)
                .inspectReadiness(null).ready());
        assertFalse(Files.exists(temporary.resolve("scripts/tts/setup-xtts.ps1")),
                "readiness must not depend on a legacy setup script");
    }

    @Test
    void piperRequiresScriptExecutableModelAndMetadata() throws Exception {
        Map<String, String> paths = piperConfiguration(temporary.resolve("piper"));
        materializeFiles(paths, "commandTemplate");
        PiperVoiceEngine engine = new PiperVoiceEngine(
                new EngineConfiguration(PiperVoiceEngine.ID, paths));

        assertTrue(engine.inspectReadiness(null).ready());

        Files.delete(Path.of(paths.get("metadata")));
        EngineReadiness unavailable = engine.inspectReadiness(null);
        assertFalse(unavailable.ready());
        assertTrue(String.join(" ", unavailable.recommendedActions())
                .contains("metadatos del modelo Piper"));
    }

    @Test
    void xttsRequiresAllSynthesisScriptsWrappersModelFilesAndSpeaker() throws Exception {
        Map<String, String> paths = xttsConfiguration(temporary.resolve("xtts"));
        materializeXtts(paths);
        XttsVoiceEngine engine = new XttsVoiceEngine(
                new EngineConfiguration(XttsVoiceEngine.ID, paths));

        assertTrue(engine.inspectReadiness(null).ready());

        for (String key : new String[]{"script", "batchScript", "python", "wrapper",
                "batchWrapper", "workerWrapper", "modelConfig", "modelCheckpoint",
                "vocabulary", "speakers", "dvae", "melStats", "defaultSpeaker"}) {
            Path required = Path.of(paths.get(key));
            Files.delete(required);
            assertFalse(engine.inspectReadiness(null).ready(),
                    () -> "XTTS was reported ready without " + key);
            touch(required);
        }
    }

    @Test
    void xttsRejectsPythonOutsideItsManagedRuntime() throws Exception {
        Path root = temporary.resolve("runtime");
        Map<String, String> paths = new LinkedHashMap<>(xttsConfiguration(root));
        Path externalPython = temporary.resolve("external/python.exe");
        paths.put("python", externalPython.toString());
        materializeXtts(paths);
        XttsVoiceEngine engine = new XttsVoiceEngine(
                new EngineConfiguration(XttsVoiceEngine.ID, paths));

        EngineReadiness readiness = engine.inspectReadiness(null);

        assertFalse(readiness.ready());
        assertTrue(readiness.summary().contains("no es portable"));
    }

    @Test
    void xttsDiagnosticsPreserveTheEffectiveDeviceReportedByTheWrapper() {
        String output = "device_requested=auto\n"
                + "device_backend=cuda device_name=NVIDIA\n"
                + "device=cuda:0\n";

        assertEquals("cuda:0",
                XttsVoiceEngine.effectiveDeviceFromOutput(output, "auto"));
        assertEquals("cpu",
                XttsVoiceEngine.effectiveDeviceFromOutput("device=cpu", "auto"));
        assertEquals("gpu:1",
                XttsVoiceEngine.effectiveDeviceFromOutput("", "gpu:1"));
    }

    private static Map<String, String> piperConfiguration(Path root) {
        return Map.of(
                "commandTemplate", "piper {textFile} {outputFile}",
                "script", root.resolve("piper.ps1").toString(),
                "executable", root.resolve("piper.exe").toString(),
                "model", root.resolve("voice.onnx").toString(),
                "metadata", root.resolve("voice.onnx.json").toString());
    }

    private static Map<String, String> xttsConfiguration(Path root) {
        Path model = root.resolve("model");
        return Map.ofEntries(
                Map.entry("commandTemplate", "xtts {textFile} {outputFile}"),
                Map.entry("batchCommandTemplate", "xtts-batch {manifestFile}"),
                Map.entry("runtimeRoot", root.toString()),
                Map.entry("script", root.resolve("xtts.ps1").toString()),
                Map.entry("batchScript", root.resolve("xtts-batch.ps1").toString()),
                Map.entry("python", root.resolve("venv/Scripts/python.exe").toString()),
                Map.entry("wrapper", root.resolve("synthesize.py").toString()),
                Map.entry("batchWrapper", root.resolve("synthesize_batch.py").toString()),
                Map.entry("workerWrapper", root.resolve("synthesize_worker.py").toString()),
                Map.entry("modelDirectory", model.toString()),
                Map.entry("modelConfig", model.resolve("config.json").toString()),
                Map.entry("modelCheckpoint", model.resolve("model.pth").toString()),
                Map.entry("vocabulary", model.resolve("vocab.json").toString()),
                Map.entry("speakers", model.resolve("speakers_xtts.pth").toString()),
                Map.entry("dvae", model.resolve("dvae.pth").toString()),
                Map.entry("melStats", model.resolve("mel_stats.pth").toString()),
                Map.entry("defaultSpeaker", model.resolve("speakers/default.wav").toString()));
    }

    private static void materializeProductionVoiceLayout(Path root) throws Exception {
        for (Path file : new Path[]{
                root.resolve("scripts/tts/piper-file-to-wav.ps1"),
                root.resolve("tools/piper/piper.exe"),
                root.resolve("models/tts/piper/voices/es_ES-default-medium.onnx"),
                root.resolve("models/tts/piper/voices/es_ES-default-medium.onnx.json"),
                root.resolve("scripts/tts/xtts-file-to-wav.ps1"),
                root.resolve("scripts/tts/xtts-batch-to-wav.ps1"),
                root.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe"),
                root.resolve("tools/xtts-wrapper/synthesize_xtts.py"),
                root.resolve("tools/xtts-wrapper/synthesize_xtts_batch.py"),
                root.resolve("tools/xtts-wrapper/synthesize_xtts_worker.py"),
                root.resolve("models/tts/xtts/config.json"),
                root.resolve("models/tts/xtts/model.pth"),
                root.resolve("models/tts/xtts/vocab.json"),
                root.resolve("models/tts/xtts/speakers_xtts.pth"),
                root.resolve("models/tts/xtts/dvae.pth"),
                root.resolve("models/tts/xtts/mel_stats.pth"),
                root.resolve("models/tts/xtts/speakers/voz-por-defecto.wav")}) {
            touch(file);
        }
    }

    private static void materializeXtts(Map<String, String> paths) throws Exception {
        Files.createDirectories(Path.of(paths.get("modelDirectory")));
        materializeFiles(paths, "commandTemplate", "batchCommandTemplate", "runtimeRoot",
                "modelDirectory");
    }

    private static void materializeFiles(Map<String, String> paths, String... excluded)
            throws Exception {
        java.util.Set<String> omitted = java.util.Set.of(excluded);
        for (Map.Entry<String, String> entry : paths.entrySet()) {
            if (!omitted.contains(entry.getKey())) touch(Path.of(entry.getValue()));
        }
    }

    private static void touch(Path file) throws Exception {
        Files.createDirectories(file.toAbsolutePath().normalize().getParent());
        Files.writeString(file, "test");
    }
}
