package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppBrainMapSourceTest {
    @Test
    void appBrainMapDefinesDesktopBackendEquivalentAndAuditTargets() throws Exception {
        String brain = read("docs/productizacion/MAPA_CEREBRO_APP.md");
        String audit = read("docs/productizacion/AUDITORIA_CEREBRO_PRIORITARIA.md");
        String tanda = read("docs/84_TANDA_59B_CATALOGO_COMPONENTES_Y_MAPA_CEREBRO.md");

        assertTrue(brain.contains("equivalente funcional al backend"));
        assertTrue(brain.contains("domain"));
        assertTrue(brain.contains("application"));
        assertTrue(brain.contains("infrastructure"));
        assertTrue(brain.contains("presentation"));
        assertTrue(brain.contains("bootstrap"));
        assertTrue(brain.contains("Documento narrable del proyecto"));
        assertTrue(brain.contains("capas/proyecciones internas"));
        assertTrue(brain.contains("DocuPodcastShellViewModel"));
        assertTrue(brain.contains("DocumentNarrationCoordinator"));
        assertTrue(brain.contains("PlaybackWorkflowCoordinator"));
        assertTrue(brain.contains("SettingsWorkflowCoordinator"));

        assertTrue(audit.contains("Auditoría prioritaria del cerebro"));
        assertTrue(audit.contains("Abrir DOCX"));
        assertTrue(audit.contains("Escuchar documento"));
        assertTrue(audit.contains("Guardar/reabrir"));
        assertTrue(audit.contains("Video simple"));
        assertTrue(audit.contains("Ningún refactor del cerebro debe cambiar comportamiento visible sin declararlo"));

        assertTrue(tanda.contains("catálogo/contrato de componentes transversales"));
        assertTrue(tanda.contains("mapa del cerebro de la app"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
