package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Operational help for the Word-first document workspace. */
public final class DocumentHelpPanel extends ScrollPane {
    public DocumentHelpPanel() {
        getStyleClass().add("document-side-scroll");
        VBox content = new VBox(8);
        content.getStyleClass().add("document-side-content");
        setFitToWidth(true);
        setContent(content);
        add(content, "Ayuda operativa", "document-side-title");
        add(content, "Qué hacer aquí", "document-side-subtitle");
        add(content, "• Abre un Word/DOCX con tus notas.\n• Revisa títulos, subtítulos, párrafos, listas, tablas e imágenes detectadas.\n• Usa Estructura para navegar y Propiedades para inspeccionar cada bloque.\n• Usa Diagnóstico para ver advertencias antes de preparar la lectura.", "document-side-muted");
        add(content, "Límites actuales", "document-side-subtitle");
        add(content, "Esta tanda no interpreta el contenido visual de imágenes ni reemplaza un editor Word. Las tablas se resumen como avisos revisables. El siguiente paso será el perfil de lectura para ajustar títulos/subtítulos.", "document-side-muted");
        add(content, "Word-first", "document-side-subtitle");
        add(content, "DOCX es la entrada prioritaria porque tus notas viven en Word. Markdown seguirá siendo útil como puente humano/IA, pero no reemplaza este flujo principal.", "document-side-muted");
    }

    private static void add(VBox content, String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        content.getChildren().add(label);
    }
}
