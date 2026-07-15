package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class HandoffVisualReferencesSourceTest {
    @Test
    void visualReferencesAreVersionedInsideRepository() throws Exception {
        assertExists("docs/referencias/capturas-chat/INDICE_REFERENCIAS_VISUALES.md");
        assertExists("docs/referencias/capturas-chat/contact_sheet_referencias_visuales.png");
        assertExists("docs/referencias/capturas-chat/app-docupodcast/01_docupodcast_inicio_pre_t81.png");
        assertExists("docs/referencias/capturas-chat/app-docupodcast/08_docupodcast_documento_seleccion_metadata_pre_t81.png");
        assertExists("docs/referencias/capturas-chat/referencias-externas/01_xournal_lienzo_limpio_miniaturas.png");
        assertExists("docs/referencias/capturas-chat/referencias-externas/02_wps_toolbar_iconos_documento_pdf.png");
        assertExists("docs/referencias/capturas-chat/referencias-externas/04_blender_layout_general.png");
        assertExists("docs/referencias/capturas-chat/retroalimentacion-t81/02_barra_flotante_lectura_comprimida.png");
    }

    @Test
    void visualIndexExplainsDesignDecisions() throws Exception {
        String text = Files.readString(Path.of("docs/referencias/capturas-chat/INDICE_REFERENCIAS_VISUALES.md"));
        assertTrue(text.contains("Documento"));
        assertTrue(text.contains("Xournal++"));
        assertTrue(text.contains("WPS Office") || text.contains("WPS"));
        assertTrue(text.contains("Blender"));
        assertTrue(text.contains("Configuración solo desde el menú Configuración"));
        assertTrue(text.contains("rail derecho"));
        assertTrue(text.contains("inspector izquierdo"));
    }

    private static void assertExists(String path) {
        assertTrue(Files.exists(Path.of(path)), path + " debe existir en el repositorio.");
    }
}
