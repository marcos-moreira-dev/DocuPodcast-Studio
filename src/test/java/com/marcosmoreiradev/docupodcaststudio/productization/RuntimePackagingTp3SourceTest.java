package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimePackagingTp3SourceTest {
    @Test
    void packagingContractCoversToolsModelsScriptsAndDemoResources() throws Exception {
        assertContains("docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md", "tools/");
        assertContains("docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md", "models/");
        assertContains("docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md", "scripts/tts");
        assertContains("docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md", "FFmpeg");
        assertContains("docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md", "Piper");
        assertContains("docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md", "Coqui/XTTS");
        assertContains("docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md", "no usa PATH global");
        assertTrue(Files.exists(Path.of("scripts/29-verificar-runtime-layout.bat")));
        assertContains("scripts/29-verificar-runtime-layout.bat", "tools\\ffmpeg\\bin");
        assertContains("scripts/29-verificar-runtime-layout.bat", "models\\tts\\piper\\voices");
    }

    @Test
    void runtimeBundleManifestIsApplicationCodeNotOnlyDocumentation() throws Exception {
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildRuntimeBundleManifestUseCase.java", "ffmpeg.exe");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildRuntimeBundleManifestUseCase.java", "piper.exe");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildRuntimeBundleManifestUseCase.java", "tools/xtts-wrapper");
        assertContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/RuntimeBundleManifest.java", "missingRequiredItems");
    }

    private static void assertContains(String path, String expected) throws Exception {
        String text = Files.readString(Path.of(path));
        assertTrue(text.contains(expected), path + " debe contener: " + expected);
    }
}
