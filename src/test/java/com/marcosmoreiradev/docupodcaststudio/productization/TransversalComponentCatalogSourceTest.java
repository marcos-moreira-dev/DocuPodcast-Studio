package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransversalComponentCatalogSourceTest {
    @Test
    void catalogDefinesComponentsAsSemanticContractNotJustPrettyButtons() throws Exception {
        String catalog = read("docs/productizacion/CATALOGO_COMPONENTES_TRANSVERSALES.md");
        String roadmap = read("docs/productizacion/ROADMAP_POST_T59B_CEREBRO_COMPONENTES.md");

        assertTrue(catalog.contains("Componente transversal = contrato semántico + API de uso + estilo común + regla de gobernanza"));
        assertTrue(catalog.contains("No basta con crear una clase JavaFX bonita"));
        assertTrue(catalog.contains("ActionButtonFactory"));
        assertTrue(catalog.contains("ActionBar"));
        assertTrue(catalog.contains("TransportControls"));
        assertTrue(catalog.contains("RailActionRow"));
        assertTrue(catalog.contains("MetricBadge"));
        assertTrue(catalog.contains("DiagnosticCard"));
        assertTrue(catalog.contains("Regla anti-botonera"));
        assertTrue(catalog.contains("Esta tanda no pretende aplicar rediseño visual completo"));

        assertTrue(roadmap.contains("No se debe confundir catálogo transversal con aplicación visual masiva"));
        assertTrue(roadmap.contains("Primero gobernanza, luego cerebro, luego cara"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
