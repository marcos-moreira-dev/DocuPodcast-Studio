package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF6: remaining large orchestrators and historical documentation cleanup have one active contract. */
final class PresentationOrchestrationRf6LegacyAuditSourceTest {
    @Test
    void currentAuditListsRemainingLargeOrchestratorsAndOrder() throws IOException {
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");

        assertTrue(audit.contains("DocuPodcastShellViewModel"));
        assertTrue(audit.contains("SettingsDialog"));
        assertTrue(audit.contains("VoiceLibraryWorkspaceView"));
        assertTrue(audit.contains("LocalTtsProcessAudioGenerationGateway"));
        assertTrue(audit.contains("DocuPodcastShellView"));
        assertTrue(audit.contains("Orden recomendado"));
        assertTrue(audit.contains("SettingsDialog`: mantenerlo estable"));
        assertTrue(audit.contains("VoiceLibraryWorkspaceView`: continuar separando la microaplicación `Voces`"));
    }

    @Test
    void historicalDocumentationRemainsArchiveOnly() throws IOException {
        String readme = read("DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md");
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");
        String debt = read("DOCUMENTACION_ACTUAL/05_DEUDA_TECNICA_Y_REFACTOR.md");

        assertTrue(readme.contains("10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md"));
        assertTrue(audit.contains("No borrar documentación histórica"));
        assertTrue(audit.contains("docs/"));
        assertTrue(audit.contains("DOCUMENTACION/"));
        assertTrue(audit.contains("DOCUMENTACION_ESTRATEGICA/"));
        assertTrue(audit.contains("00_MEMORIA_PROYECTO/"));
        assertTrue(debt.contains("10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md"));
    }

    @Test
    void embeddedDependencyAssistantUsesSharedBackgroundRunner() throws IOException {
        String runner = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/FxBackgroundTaskRunner.java");
        String assistant = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EmbeddedDependencySetupAssistant.java");
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");

        assertTrue(runner.contains("public Thread start(String threadName, Runnable runnable)"));
        assertTrue(assistant.contains("FxBackgroundTaskRunner"));
        assertTrue(assistant.contains("backgroundTaskRunner.start(\"docupodcast-embedded-dependency-setup\""));
        assertTrue(assistant.contains("backgroundTaskRunner.start(\"docupodcast-ffmpeg-embedded-setup\""));
        assertFalse(assistant.contains("Thread worker = new Thread"));
        assertTrue(settings.contains("FxBackgroundTaskRunner"));
        assertFalse(settings.contains("Thread worker = new Thread"),
                "SettingsDialog ya no debe conservar workers manuales tras RF7.");
    }

    @Test
    void rf6IsRecordedInCurrentMarkdownLog() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");

        assertTrue(bitacora.contains("Paso RF6-01: orquestadores restantes y legacy documental"));
        assertTrue(bitacora.contains("EmbeddedDependencySetupAssistant"));
        assertTrue(bitacora.contains("FxBackgroundTaskRunner"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF6"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
