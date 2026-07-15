package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrail: advanced voice wrapper must not turn a selected model.pth into model.pth/model.pth. */
final class XttsModelPathHfSourceTest {
    @Test
    void xttsWrapperNormalizesMistakenModelPthPathBeforeLoadingModel() throws Exception {
        String wrapper = Files.readString(Path.of("tools/xtts-wrapper/synthesize_xtts.py"));

        assertTrue(wrapper.contains("def normalize_model_root"));
        assertTrue(wrapper.contains("model_root.name.lower() == \"model.pth\""));
        assertTrue(wrapper.contains("return model_root.parent"));
        assertTrue(wrapper.contains("requested = normalize_model_root(Path(raw_model_dir))"));
        assertFalse(wrapper.contains("model_root = Path(model_dir)\n    model_path = model_root / \"model.pth\""));
    }

    @Test
    void powershellBridgeAlsoNormalizesModelPthPathBeforeCallingWrapper() throws Exception {
        String script = Files.readString(Path.of("scripts/tts/xtts-file-to-wav.ps1"));

        assertTrue(script.contains("model-dir-apunta-a-model-pth"));
        assertTrue(script.contains("Split-Path -Parent $received"));
        assertTrue(script.contains("evitar model.pth/model.pth"));
    }
    @Test
    void legacyScriptPrefersPortableModelWhenLegacyFolderIsIncomplete() throws Exception {
        String script = Files.readString(Path.of("scripts/tts/Voz IA avanzada-file-to-wav.ps1"));

        assertTrue(script.contains("Test-XttsModelFolder"));
        assertTrue(script.contains("models\\tts\\xtts"));
        assertTrue(script.contains("model-dir-legacy-o-incompleto"));
    }

}
