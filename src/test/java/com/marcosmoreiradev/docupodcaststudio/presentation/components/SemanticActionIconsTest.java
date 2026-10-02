package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class SemanticActionIconsTest {
    @Test void resolvesCommonSpanishActionsWithoutReplacingProductIdentity() {
        Map<String, String> actions = Map.ofEntries(
                Map.entry("Cancelar", "x"),
                Map.entry("Guardar nota", "save"),
                Map.entry("Deshacer", "undo-2"),
                Map.entry("Rehacer", "redo-2"),
                Map.entry("Girar derecha", "rotate-cw"),
                Map.entry("Nueva voz", "plus"),
                Map.entry("Continuar último reanudable", "play"),
                Map.entry("Reprocesar lectura completa", "refresh-cw"),
                Map.entry("Refrescar contenido", "refresh-cw"),
                Map.entry("Pausar", "pause"),
                Map.entry("Reanudar", "play"),
                Map.entry("Buscar", "search"),
                Map.entry("Guardar contexto textual", "save"),
                Map.entry("Eliminar escena", "trash-2"),
                Map.entry("Dibujar/editar frame", "pencil"),
                Map.entry("Exportar paquetes IA", "download"),
                Map.entry("Grabar audio", "mic"),
                Map.entry("Copiar al baúl", "copy"),
                Map.entry("Añadir al lienzo", "plus"),
                Map.entry("Desagrupar", "layout-grid"),
                Map.entry("Agrupar", "layers"),
                Map.entry("Procesar lectura completa", "wand-sparkles"),
                Map.entry("Procesar intervalo", "wand-sparkles"),
                Map.entry("Detalles · procesamiento", "eye"),
                Map.entry("Pantalla completa", "maximize-2"),
                Map.entry("Lectura", "book-open"),
                Map.entry("Reproducción", "circle-play"),
                Map.entry("Motores y dependencias", "bot"),
                Map.entry("Procesamiento visual", "sparkles"),
                Map.entry("Rendimiento", "monitor-play"),
                Map.entry("Video final", "film"),
                Map.entry("Almacenamiento", "database"),
                Map.entry("Soporte", "badge-check"),
                Map.entry("Gestionar voces", "audio-lines"),
                Map.entry("Examinar…", "folder-open"),
                Map.entry("Comprobar instalación actual", "circle-check"));

        actions.forEach((label, icon) -> {
            assertEquals(icon, SemanticActionIcons.iconNameFor(label), label);
            assertNotNull(getClass().getResource("/icons/lucide/" + icon + ".svg"), icon);
        });
        assertNull(SemanticActionIcons.iconNameFor("Una intención exclusiva del producto"));
    }
}
