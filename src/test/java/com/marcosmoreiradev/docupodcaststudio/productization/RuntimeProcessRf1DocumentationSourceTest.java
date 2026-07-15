package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the handoff documentation for runtime path and external process standards. */
final class RuntimeProcessRf1DocumentationSourceTest {
    @Test
    void productizationDocDescribesRuntimePathsRunnerAndExceptions() throws Exception {
        String doc = Files.readString(Path.of("docs/productizacion/RUNTIME_PATHS_EXTERNAL_PROCESS_RF1.md"));
        assertTrue(doc.contains("RuntimeArtifactPaths"));
        assertTrue(doc.contains("ExternalProcessRequest"));
        assertTrue(doc.contains("DefaultExternalProcessRunner"));
        assertTrue(doc.contains("ExternalProcessTimeoutException"));
        assertTrue(doc.contains("RUNTIME-ARCH-RC1"));
    }
}
