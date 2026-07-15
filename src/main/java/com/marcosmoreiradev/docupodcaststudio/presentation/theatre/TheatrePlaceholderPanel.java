package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Empty-state panels for theatre modules whose data layer is introduced incrementally. */
final class TheatrePlaceholderPanel {
    private TheatrePlaceholderPanel() {
    }

    static VBox characters() {
        return panel("Personajes",
                "Detectará líneas con formato PERSONAJE: texto.",
                "Luego podrás fusionar alias y asignar fotos frontal, lateral, trasera o referencia libre.");
    }

    static VBox textualMap() {
        return panel("Mapa textual",
                "Intervencion 1, Intervencion 2... son identificadores de fragmentos en secuencia.",
                "No representan personajes por sí solos; sirven para ordenar acciones del guion.");
    }

    static VBox spatialMap() {
        return panel("Mapa espacial",
                "Aquí vivirá la vista superior del escenario.",
                "Cada intervencion podrá tener coordenadas por escenario y exportarse como mapa lateral.");
    }

    static VBox actions() {
        return panel("Acciones",
                "Registra cambios de posición, flechas y desplazamientos entre fragmentos.",
                "La línea de acciones seguirá la secuencia narrativa, no segundos de reproducción.");
    }

    static VBox objects() {
        return panel("Objetos",
                "Organiza utilería, escenografía y referencias visuales de escena.",
                "Estos datos se usarán después para revisar continuidad y exportar la obra.");
    }

    private static VBox panel(String title, String primary, String secondary) {
        Label heading = new Label(title);
        heading.getStyleClass().add("side-panel-heading");
        Label first = new Label(primary);
        Label second = new Label(secondary);
        first.setWrapText(true);
        second.setWrapText(true);
        VBox box = new VBox(10, heading, first, second);
        box.setPadding(new Insets(14));
        box.getStyleClass().add("theatre-placeholder-panel");
        return box;
    }
}
