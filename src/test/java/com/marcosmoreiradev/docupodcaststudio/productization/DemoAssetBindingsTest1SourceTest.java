package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DEMO-ASSET-BINDINGS-TEST1 protects the theatre demo visual package and MD bindings. */
final class DemoAssetBindingsTest1SourceTest {
    @Test
    void theatreDemoHasPlayBodyBindingsAndAssets() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/examples/ClasspathExampleProjectCatalog.java"));
        String manifest = Files.readString(Path.of("src/main/resources/examples/aviadores-comicos/PROYECTO_DEMO.md"));
        Path assetsDir = Path.of("src/main/resources/examples/aviadores-comicos/assets");
        Path fragmentDir = assetsDir.resolve("fragmentos");

        assertTrue(Files.exists(assetsDir.resolve("imagen_01_presentacion_personajes.png")));
        for (int i = 1; i <= 24; i++) {
            String prefix = "fragmento_" + String.format(java.util.Locale.ROOT, "%02d", i) + "_";
            try (var stream = Files.list(fragmentDir)) {
                assertTrue(stream.anyMatch(path -> path.getFileName().toString().startsWith(prefix)),
                        "Missing asset for " + prefix);
            }
        }

        assertTrue(catalog.contains("/examples/aviadores-comicos/PROYECTO_DEMO.md"));
        assertTrue(catalog.contains("\"teatro.md\""));
        assertEquals(21, countOccurrences(manifest, "imagen=fragmentos/"));
        assertTrue(manifest.contains("texto_inicio=5 | texto_fin=10"));
        assertTrue(manifest.contains("texto_inicio=12 | texto_fin=20"));
        assertTrue(manifest.contains("texto_inicio=22 | texto_fin=27"));
        assertTrue(manifest.contains("mapa_espacial=mapas/mapa-espacial.png"));
        assertTrue(Files.exists(assetsDir.resolve(Path.of("mapas", "mapa-espacial.png"))));
        assertTrue(manifest.contains("imagen=fragmentos/fragmento_03_revision_capitan.png"));
        assertTrue(manifest.contains("imagen=fragmentos/fragmento_24_cierre_tornillo_dorado.png"));
        assertTrue(manifest.contains("- Objeto: Mapa de ruta | escena=En el aire | imagen=utileria/obj_mapa_01.png"));
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
