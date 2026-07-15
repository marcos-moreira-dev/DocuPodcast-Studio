package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StartupEnginePreflightT90ESourceTest {
    @Test
    void t90eAddsStartupPreflightUseCaseAndPublicScript() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/AppStartupEnginePreflightUseCase.java")));
        assertTrue(Files.isRegularFile(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/StartupEnginePreflightReport.java")));
        assertTrue(Files.isRegularFile(Path.of("scripts/23-preflight-arranque-motores.bat")));
        assertTrue(Files.isRegularFile(Path.of("scripts/tts/preflight-startup-engines.ps1")));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/AppStartupEnginePreflightUseCase.java"));
        assertTrue(useCase.contains("LOCAL_PYTHON"));
        assertTrue(useCase.contains("tools/xtts-wrapper/.venv/Scripts/python.exe"));
        assertTrue(useCase.contains("No instales Python global"));
    }

    @Test
    void t90eDocumentsFutureAppStartupAssistantWithoutVisualPromises() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("docs/productizacion/T90E_PREFLIGHT_ARRANQUE_MOTORES.md")));
        String doc = Files.readString(Path.of("docs/productizacion/T90E_PREFLIGHT_ARRANQUE_MOTORES.md"));
        assertTrue(doc.contains("preflight de arranque"));
        assertTrue(doc.contains("no descarga sin permiso"));
        assertTrue(doc.contains("T90E no rediseña la GUI"));
    }
}
