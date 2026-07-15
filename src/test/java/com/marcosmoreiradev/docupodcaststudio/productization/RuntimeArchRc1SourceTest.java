package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RC1: runtime architecture contracts remain documented and discoverable. */
final class RuntimeArchRc1SourceTest {
    @Test
    void runtimeArchitectureHasCentralContracts() throws Exception {
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/RuntimeArtifactPaths.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/ExternalProcessRunner.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/ExternalProcessFailedException.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/ModelArtifactContract.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/download/ManagedDownloadService.java")));
        assertTrue(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettingsMigrationPolicy.java")));
    }

    @Test
    void runtimeArchitectureDocumentationExists() throws Exception {
        String doc = Files.readString(Path.of("docs/productizacion/RUNTIME_ARCH_RC1_CONSOLIDACION.md"));
        assertTrue(doc.contains("RuntimeArtifactPaths"));
        assertTrue(doc.contains("ExternalProcessRunner"));
        assertTrue(doc.contains("ModelArtifactContract"));
        assertTrue(doc.contains("ManagedDownloadService"));
        assertTrue(doc.contains("OperationalSettingsMigrationPolicy"));
        assertTrue(doc.contains("ASCII-safe"));
    }

    @Test
    void xttsModelPathDuplicationIsExplicitlyForbidden() throws Exception {
        String doc = Files.readString(Path.of("docs/productizacion/RUNTIME_ARCH_RC1_CONSOLIDACION.md"));
        assertTrue(doc.contains("model.pth/model.pth"));
        assertFalse(Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"))
                .contains("Voz local simple/Piper"));
    }
}
