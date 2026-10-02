package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;

import java.text.Normalizer;
import java.util.Locale;

/** Common monochrome action icons; product/module identity icons remain explicit. */
public final class SemanticActionIcons {
    private static final Object SYMBOL_DISPLAY = new Object();
    private SemanticActionIcons() { }

    public static void decorate(ButtonBase control, String label) {
        if (control == null || control.getGraphic() != null) return;
        String icon = iconNameFor(label);
        if (icon != null) {
            control.setGraphic(new LucideIconView(icon, 18));
            updateSymbolDisplay(control);
        }
    }

    /** Re-evaluates an icon when a control changes its user-facing action label. */
    public static void refresh(ButtonBase control, String label) {
        if (control == null) return;
        String icon = iconNameFor(label);
        control.setGraphic(icon == null ? null : new LucideIconView(icon, 18));
        updateSymbolDisplay(control);
    }

    /** A legacy glyph is an icon placeholder, not a second visible action label. */
    private static void updateSymbolDisplay(ButtonBase control) {
        String text = normalize(control.getText());
        boolean symbol = text.equals("x") || text.equals("✎")
                || text.equals("↑") || text.equals("↓");
        if (symbol && control.getGraphic() != null) {
            control.getProperties().putIfAbsent(SYMBOL_DISPLAY, control.getContentDisplay());
            control.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        } else {
            Object previous = control.getProperties().remove(SYMBOL_DISPLAY);
            if (previous instanceof ContentDisplay display) control.setContentDisplay(display);
        }
    }

    public static void decorate(MenuItem item) {
        if (item == null || item.getGraphic() != null) return;
        String icon = iconNameFor(item.getText());
        if (icon != null) item.setGraphic(new LucideIconView(icon, 17));
    }

    /** Monochrome semantic graphic for navigation rows, headings and list cells. */
    public static Node graphicFor(String label, double size) {
        String icon = iconNameFor(label);
        return icon == null ? null : new LucideIconView(icon, size);
    }

    static String iconNameFor(String label) {
        String text = normalize(label);
        if (text.isBlank()) return null;
        if (text.startsWith("guardar")) return "save";
        if (text.equals("↑")) return "arrow-up";
        if (text.equals("↓")) return "arrow-down";
        if (text.startsWith("deshacer")) return "undo-2";
        if (text.startsWith("rehacer")) return "redo-2";
        if (text.startsWith("girar derecha")) return "rotate-cw";
        if (text.startsWith("girar izquierda")) return "rotate-ccw";
        if (text.startsWith("reducir")) return "zoom-out";
        if (text.equals("ampliar")) return "zoom-in";
        if (text.startsWith("ajustar") || text.startsWith("reescalar")) return "maximize-2";
        if (text.startsWith("continuar") || text.startsWith("play")) return "play";
        if (text.startsWith("crear") || text.startsWith("nueva")) return "plus";
        if (text.startsWith("asociar")) return "image-up";
        if (text.equals("x") || has(text, "cancelar", "cerrar", "ocultar")) return "x";
        if (has(text, "reprocesar", "refrescar")) return "refresh-cw";
        if (has(text, "pausar")) return "pause";
        if (has(text, "reanudar")) return "play";
        if (has(text, "abrir proyecto")) return "folder-open";
        if (has(text, "deshabilitar")) return "eye-off";
        if (has(text, "eliminar", "borrar", "quitar", "limpiar", "dejar region")) return "trash-2";
        if (has(text, "buscar")) return "search";
        if (has(text, "recortar")) return "crop";
        if (has(text, "borrador")) return "eraser";
        if (text.equals("✎") || has(text, "editar", "definir", "dibujar", "nota")) return "pencil";
        if (has(text, "copiar")) return "copy";
        if (has(text, "pegar")) return "clipboard-paste";
        if (has(text, "desagrupar")) return "layout-grid";
        if (has(text, "agrupar")) return "layers";
        if (has(text, "subir")) return "arrow-up";
        if (has(text, "bajar")) return "arrow-down";
        if (has(text, "volver")) return "arrow-left";
        if (has(text, "anterior")) return "chevron-left";
        if (has(text, "siguiente", "posterior")) return "chevron-right";
        if (has(text, "inicio del", "fragmento inicial")) return "skip-back";
        if (has(text, "final del")) return "skip-forward";
        if (has(text, "pantalla completa")) return "maximize-2";
        if (has(text, "grabar")) return "mic";
        if (has(text, "detener")) return "circle-stop";
        if (has(text, "escuchar", "narrar")) return "volume-2";
        if (has(text, "comprobar", "verificar", "inspect_")) return "circle-check";
        if (has(text, "reproducir", "previsualizar", "probar", "iniciar")) return "play";
        if (has(text, "preparar", "procesar")) return "wand-sparkles";
        if (has(text, "generar", "renderizar")) return "wand-sparkles";
        if (has(text, "descargar", "exportar", "export_", "instalar")) return "download";
        if (has(text, "importar", "cargar")) return "upload";
        if (has(text, "reintentar", "reprocesar", "actualizar", "reemplazar", "repetir", "reparar")) return "refresh-cw";
        if (has(text, "restaurar", "predeterminada")) return "rotate-ccw";
        if (has(text, "abrir carpeta", "elegir carpeta", "examinar", "open_project_folder", "open_exports_folder")) return "folder-open";
        if (has(text, "configurar motor")) return "bot";
        if (has(text, "configuracion", "configurar")) return "settings";
        if (has(text, "gestionar voces", "biblioteca de voces")) return "audio-lines";
        if (has(text, "biblioteca")) return "book-open";
        if (has(text, "catalogo")) return "layout-grid";
        if (has(text, "trabajos")) return "briefcase";
        if (has(text, "marcador")) return "bookmark-plus";
        if (has(text, "seleccionar todos")) return "list";
        if (has(text, "nuevo", "agregar", "anadir", "ampliar")) return "plus";
        if (has(text, "guardar")) return "save";
        if (has(text, "aceptar", "aprobar", "aplicar", "asignar", "establecer", "habilitar")) return "check";
        if (has(text, "elegir", "seleccionar") && has(text, "imagen", "mascota")) return "image";
        if (has(text, "elegir", "seleccionar") && has(text, "audio", "pista", "voz")) return "audio-lines";
        if (has(text, "elegir", "seleccionar")) return "check";
        if (has(text, "inicio", "resumen", "vista general")) return "layout-grid";
        if (has(text, "lectura")) return "book-open";
        if (has(text, "reproduccion")) return "circle-play";
        if (has(text, "motor", "modelo", "dependencia", "inteligencia artificial", " ia")) return "bot";
        if (has(text, "procesamiento visual", "super resolucion")) return "sparkles";
        if (has(text, "rendimiento", "en ejecucion")) return "monitor-play";
        if (has(text, "en espera")) return "clock-3";
        if (has(text, "video")) return "film";
        if (has(text, "almacenamiento")) return "database";
        if (has(text, "soporte", "integridad", "readiness")) return "badge-check";
        if (has(text, "mapa")) return "map";
        if (has(text, "imagen", "mascota", "frame", "storyboard")) return "image";
        if (has(text, "audio", "pista", "voz")) return "audio-lines";
        if (has(text, "detalles") || text.matches("^(ver|mostrar)\\b.*")) return "eye";
        if (text.startsWith("ir ")) return "arrow-right";
        return null;
    }

    private static boolean has(String text, String... fragments) {
        for (String fragment : fragments) if (text.contains(fragment)) return true;
        return false;
    }

    private static String normalize(String value) {
        String text = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
