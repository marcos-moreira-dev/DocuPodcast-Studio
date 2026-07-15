package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadmapStudyPdfPostT100Test {
    @Test
    void roadmapKeepsCurrentStudyPdfContinuityAndDmsGuardrail() throws Exception {
        Path roadmapPath = Path.of("docs/productizacion/ROADMAP_ESTUDIO_PDF_POST_T100.md");
        Path memoryPath = Path.of("00_MEMORIA_PROYECTO/151_TANDA_101_ROADMAP_ESTUDIO_PDF_CONTINUIDAD.md");
        assertTrue(Files.exists(roadmapPath));
        assertTrue(Files.exists(memoryPath));

        String roadmap = Files.readString(roadmapPath);
        for (int tanda = 102; tanda <= 118; tanda++) {
            assertTrue(roadmap.contains("T" + tanda), "Falta T" + tanda + " en el roadmap rector.");
        }
        assertTrue(roadmap.contains("Domain Model Studio/UENS"));
        assertTrue(roadmap.contains("numeracion historica de otra linea de trabajo"));
        assertTrue(roadmap.contains("docs/00_ONBOARDING_PROYECTO.md"));
        assertTrue(roadmap.contains("docs/03_REFERENCIAS_DMS_FRACTAL.md"));
        assertTrue(roadmap.contains("00_MEMORIA_PROYECTO/03_REFERENCIAS_DMS_RESUMEN.md"));
        assertTrue(Files.exists(Path.of("docs/00_ONBOARDING_PROYECTO.md")));
        assertTrue(Files.exists(Path.of("docs/03_REFERENCIAS_DMS_FRACTAL.md")));
        assertTrue(Files.exists(Path.of("00_MEMORIA_PROYECTO/03_REFERENCIAS_DMS_RESUMEN.md")));
    }
}
