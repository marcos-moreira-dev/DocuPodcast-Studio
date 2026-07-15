package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF5A starts the in-app/technical audit of local engine artifacts before final packaging. */
final class EngineArtifactAuditPf5ASourceTest {
    @Test
    void applicationDefinesLocalEngineArtifactAuditUseCase() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/AuditEngineArtifactsUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/EngineArtifactAuditReport.java"));
        assertTrue(useCase.contains("SHA-256"));
        assertTrue(useCase.contains("BuildEngineArtifactManifestUseCase"));
        assertTrue(useCase.contains("writeMarkdown"));
        assertTrue(report.contains("ENGINE_ARTIFACTS_AUDIT") || report.contains("auditoría local de artefactos"));
    }

    @Test
    void technicalAuditStaysOutOfNormalSettingsDialog() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/SettingsApplicationServices.java"));
        assertFalse(settings.contains("Inventario local de motores"));
        assertFalse(settings.contains("Auditar artefactos locales"));
        assertFalse(settings.contains("ENGINE_ARTIFACTS_AUDIT.md"));
        assertTrue(services.contains("AuditEngineArtifactsUseCase"));
    }

    @Test
    void technicalScriptAndReleaseCandidateIncludeEngineArtifactAudit() throws Exception {
        String script = Files.readString(Path.of("scripts/35-auditar-artefactos-motores.bat"));
        String rc = Files.readString(Path.of("scripts/16-release-candidate.bat"));
        assertTrue(script.contains("ENGINE_ARTIFACTS_AUDIT.md"));
        assertTrue(script.contains("certutil -hashfile"));
        assertTrue(script.contains("models\\tts\\xtts\\model.pth"));
        assertTrue(rc.contains("35-auditar-artefactos-motores.bat"));
    }
}
