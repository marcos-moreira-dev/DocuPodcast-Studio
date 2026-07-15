package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrainV1FreezeSourceTest {
    @Test
    void brainV1FreezeHasExecutableMatrixAndDocumentsBoundaries() throws Exception {
        String matrix = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/brain/BrainV1CapabilityMatrix.java");
        String mainDoc = read("docs/productizacion/BRAIN_V1_FREEZE_T80.md");
        String roadmap = read("docs/productizacion/ROADMAP_POST_T80_CEREBRO_CONGELADO.md");
        String tanda = read("docs/112_TANDA_80_CONGELACION_CEREBRO_V1.md");

        assertTrue(matrix.contains("document-intake"));
        assertTrue(matrix.contains("listen-document"));
        assertTrue(matrix.contains("media-input"));
        assertTrue(matrix.contains("compute-device"));
        assertTrue(matrix.contains("V2_DEFERRED"));

        assertTrue(mainDoc.contains("Documento narrable"));
        assertTrue(mainDoc.contains("Escuchar documento"));
        assertTrue(mainDoc.contains("Configuración = ajustes operativos avanzados"));
        assertTrue(mainDoc.contains("OCR local por Tesseract"));
        assertTrue(mainDoc.contains("no cabina de avión"));
        assertTrue(mainDoc.contains("MP3/WAV/video→audio"));
        assertTrue(mainDoc.contains("CPU/GPU"));
        assertTrue(mainDoc.contains("smoke automático"));

        assertTrue(roadmap.contains("Lectura 15"));
        assertTrue(roadmap.contains("T81"));
        assertTrue(roadmap.contains("T82"));
        assertTrue(tanda.contains("BrainV1CapabilityMatrix"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
