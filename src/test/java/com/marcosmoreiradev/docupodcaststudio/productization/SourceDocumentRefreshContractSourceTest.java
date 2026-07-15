package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceDocumentRefreshContractSourceTest {
    @Test
    void readOnlySourceDocumentsCanBeRefreshedByComparisonWithoutOverwritingSource() throws Exception {
        String contract = read("docs/productizacion/CONTRATO_REFRESCAR_DOCUMENTO_FUENTE_V1.md");
        String readOnly = read("docs/productizacion/CONTRATO_DOCUMENTOS_SOLO_LECTURA_V1.md");
        String brain = read("docs/productizacion/MAPA_CEREBRO_APP.md");
        String roadmap = read("docs/productizacion/ROADMAP_POST_T60B_REFRESCO_FUENTE.md");

        assertTrue(contract.contains("Refrescar contenido"));
        assertTrue(contract.contains("solo lectura"));
        assertTrue(contract.contains("SourceDocumentChangeReport"));
        assertTrue(contract.contains("audio asociado debe marcarse como **obsoleto**"));
        assertTrue(contract.contains("capa huérfana") || contract.contains("capas huérfanas"));
        assertTrue(contract.contains("no se debe borrar automáticamente trabajo del usuario"));
        assertTrue(contract.contains("no edita ni sobrescribe el documento fuente"));

        assertTrue(readOnly.contains("Refrescar contenido"));
        assertTrue(readOnly.contains("contenido externo cambió"));
        assertTrue(readOnly.contains("audio obsoleto"));

        assertTrue(brain.contains("Refresco de documento fuente"));
        assertTrue(brain.contains("SourceDocumentRefreshCoordinator"));
        assertTrue(brain.contains("DerivedArtifactStalenessPolicy"));

        assertTrue(roadmap.contains("snapshot"));
        assertTrue(roadmap.contains("detector"));
        assertTrue(roadmap.contains("reporte"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
