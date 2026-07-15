package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceDocumentsReadOnlyContractSourceTest {
    @Test
    void sourceDocumentsAreDeclaredReadOnlyForCurrentVersion() throws Exception {
        String contract = read("docs/productizacion/CONTRATO_DOCUMENTOS_SOLO_LECTURA_V1.md");
        String roadmap = read("docs/productizacion/ROADMAP_POST_T59C_DOCUMENTOS_CEREBRO.md");
        String tanda = read("docs/85_TANDA_59C_CONTRATO_DOCUMENTOS_SOLO_LECTURA.md");
        String brain = read("docs/productizacion/MAPA_CEREBRO_APP.md");

        assertTrue(contract.contains("todo documento fuente abierto por el usuario debe tratarse como **solo lectura**"));
        assertTrue(contract.contains("Word/DOCX"));
        assertTrue(contract.contains("PDF"));
        assertTrue(contract.contains("Markdown/MD"));
        assertTrue(contract.contains("TXT"));
        assertTrue(contract.contains("Documento fuente = evidencia/origen inmutable"));
        assertTrue(contract.contains("Proyecto DocuPodcast = lugar donde viven Documento narrable, proyecciones de narración, capas, audio, storyboard, transcripciones y metadatos"));
        assertTrue(contract.contains("Abrir Markdown como documento no equivale a editar el archivo Markdown original"));
        assertTrue(contract.contains("No debe existir un flujo inverso automático"));

        assertTrue(roadmap.contains("Word/PDF/Markdown/TXT fuente = solo lectura en V1"));
        assertTrue(roadmap.contains("ningún flujo trate el documento fuente como editable"));

        assertTrue(tanda.contains("Word/DOCX, PDF, Markdown/MD y TXT"));
        assertTrue(tanda.contains("fuente inmutable"));

        assertTrue(brain.contains("Documentos fuente solo lectura"));
        assertTrue(brain.contains("Word/DOCX/PDF/Markdown/TXT fuente solo lectura"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
