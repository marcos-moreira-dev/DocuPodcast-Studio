package com.marcosmoreiradev.docupodcaststudio.infrastructure.resources;

import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceExportResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AiResourceExporterTest {
    @Test
    void exportsIndexAndOfficialDocumentResourcesWithoutScriptContract() throws Exception {
        OfficialAiResourceCatalog catalog = new OfficialAiResourceCatalog();
        Path target = Files.createTempDirectory("docupodcast-ai-resources-test");

        AiResourceExportResult result = new ClasspathAiResourceExporter(catalog).export(target);

        assertTrue(Files.exists(result.indexPath()));
        assertTrue(Files.exists(target.resolve("01_prompt_preparar_documento_lectura.md")));
        assertTrue(Files.exists(target.resolve("02_checklist_documento_lectura.md")));
        assertTrue(Files.exists(target.resolve("03_plantilla_notas_documento.md")));
        String index = Files.readString(result.indexPath());
        assertFalse(index.contains("docupodcast-script-v1"));
        assertFalse(index.contains("Importable por la aplicación: sí"));
        assertTrue(index.contains("Importable por la aplicación: no"));
        assertTrue(index.contains("Markdown se trata como documento fuente normal"));
        assertTrue(index.contains("Estado de importación"));
    }
}
