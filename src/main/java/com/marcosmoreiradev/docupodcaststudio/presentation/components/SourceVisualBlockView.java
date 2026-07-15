package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Reusable visual block for read-only images and tables detected in the source document.
 *
 * <p>Source visuals are document evidence only: they are not automatically added to the
 * storyboard and they only become video material when the user assigns a visual.</p>
 */
public final class SourceVisualBlockView extends VBox {
    private static final double MIN_RESPONSIVE_IMAGE_WIDTH = 160.0;

    public SourceVisualBlockView(String title, String detail, Node visual) {
        this(title, detail, visual, false, null);
    }

    private SourceVisualBlockView(String title, String detail, Node visual, boolean tableOnly) {
        this(title, detail, visual, tableOnly, null);
    }

    private SourceVisualBlockView(String title, String detail, Node visual, boolean tableOnly, Node headerAction) {
        getStyleClass().add(tableOnly ? AppStyles.UI_SOURCE_TABLE_BLOCK : AppStyles.UI_SOURCE_VISUAL_BLOCK);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(tableOnly ? 4 : 8);
        setMaxWidth(Double.MAX_VALUE);
        if (!tableOnly) {
            VBox header = new VBox(2);
            header.getStyleClass().add(AppStyles.UI_SOURCE_VISUAL_HEADER);
            Label titleLabel = new Label(title == null || title.isBlank() ? "Visual del documento" : title.strip());
            titleLabel.getStyleClass().add(AppStyles.UI_SOURCE_VISUAL_TITLE);
            titleLabel.setWrapText(true);
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            HBox titleRow = new HBox(8, titleLabel);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            if (headerAction != null) {
                titleRow.getChildren().add(headerAction);
            }
            Label detailLabel = new Label(detail == null || detail.isBlank()
                    ? "Bloque visual de la fuente original. No se asigna a la secuencia visual automáticamente."
                    : detail.strip());
            detailLabel.getStyleClass().add(AppStyles.UI_SOURCE_VISUAL_DETAIL);
            detailLabel.setWrapText(true);
            header.getChildren().addAll(titleRow, detailLabel);
            getChildren().add(header);
        }
        getChildren().add(visual);
        Tooltip.install(this, new Tooltip("Visual detectado en la fuente. El usuario decide si lo asocia a la secuencia visual."));
    }

    private SourceVisualBlockView(Node visual) {
        getStyleClass().add("ui-source-visual-plain-image");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setMaxWidth(Double.MAX_VALUE);
        getChildren().add(visual);
        Tooltip.install(this, new Tooltip("Imagen detectada en la fuente. El usuario decide si la asocia a la secuencia visual."));
    }

    public static SourceVisualBlockView sourceNotice(String title, String detail, String marker, String body) {
        Label mark = new Label(marker == null || marker.isBlank() ? "◇" : marker.strip());
        mark.getStyleClass().add(AppStyles.UI_SOURCE_VISUAL_NOTICE_MARK);
        Label text = new Label(body == null || body.isBlank()
                ? "Bloque visual detectado en la fuente documental."
                : body.strip());
        text.setWrapText(true);
        text.getStyleClass().add(AppStyles.UI_SOURCE_VISUAL_NOTICE_TEXT);
        HBox notice = new HBox(10, mark, text);
        notice.getStyleClass().add(AppStyles.UI_SOURCE_VISUAL_NOTICE);
        notice.setAlignment(Pos.CENTER_LEFT);
        return new SourceVisualBlockView(title, detail, notice);
    }

    public static Optional<SourceVisualBlockView> embeddedImage(String title, String detail, String base64) {
        return embeddedImage(title, detail, base64, () -> false, null, null);
    }

    public static Optional<SourceVisualBlockView> embeddedImage(
            String title,
            String detail,
            String base64,
            BooleanSupplier playbackRunning,
            Runnable pausePlayback,
            Runnable resumePlayback) {
        if (base64 == null || base64.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Image image = new Image(new ByteArrayInputStream(bytes));
            if (image.isError()) {
                return Optional.empty();
            }
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setManaged(true);
            view.getStyleClass().addAll("document-embedded-image-preview", "document-embedded-image-plain");
            ResponsiveSourceImageHost imageHost = new ResponsiveSourceImageHost(view, image);
            imageHost.getStyleClass().add("document-embedded-image-responsive-host");
            Button fullscreen = ActionButtonFactory.iconOnly(
                    AppIcon.FULLSCREEN,
                    "Pantalla completa (se pausa la reproduccion)",
                    () -> openFullscreenImage(view, image, title, playbackRunning, pausePlayback, resumePlayback),
                    AppStyles.UI_ACTION_BUTTON,
                    AppStyles.UI_ACTION_BUTTON_SECONDARY,
                    "document-embedded-image-fullscreen-button");
            Label fullscreenLabel = new Label("Pantalla completa (se pausa la reproduccion)");
            fullscreenLabel.getStyleClass().add("document-embedded-image-fullscreen-label");
            fullscreenLabel.setWrapText(true);
            fullscreenLabel.setMaxWidth(260);
            HBox fullscreenAction = new HBox(6, fullscreen, fullscreenLabel);
            fullscreenAction.setAlignment(Pos.CENTER_LEFT);
            fullscreenAction.getStyleClass().add("document-embedded-image-fullscreen-action");
            return Optional.of(new SourceVisualBlockView(title, detail, imageHost, false, fullscreenAction));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static void openFullscreenImage(
            Node ownerNode,
            Image image,
            String title,
            BooleanSupplier playbackRunning,
            Runnable pausePlayback,
            Runnable resumePlayback) {
        Scene scene = ownerNode == null ? null : ownerNode.getScene();
        AtomicBoolean pausedByFullscreen = new AtomicBoolean(false);
        ImageFullscreenViewer.show(
                image,
                scene == null ? null : scene.getWindow(),
                scene == null ? List.of() : scene.getStylesheets(),
                title == null || title.isBlank() ? "Imagen del documento" : title,
                "No se pudo mostrar la imagen del documento",
                null,
                "Presiona Escape para salir y reanudar la reproduccion de la narracion.",
                () -> {
                    if (pausePlayback != null && playbackRunning != null && playbackRunning.getAsBoolean()) {
                        pausePlayback.run();
                        pausedByFullscreen.set(true);
                    }
                },
                () -> {
                    if (resumePlayback != null && pausedByFullscreen.get()) {
                        resumePlayback.run();
                    }
                });
    }

    /**
     * Render a read-only DOCX table as a sober GridPane-backed grid instead of a raw Markdown preview.
     * The table remains a visual/non-narratable source block, not an editable spreadsheet.
     */
    public static SourceVisualBlockView tableGrid(String title, String detail, String markdown, String rows, String columns) {
        List<List<String>> parsed = parseMarkdownTable(markdown);
        if (parsed.isEmpty()) {
            return placeholder(title,
                    detail == null || detail.isBlank()
                            ? "Tabla detectada en la fuente. Filas: " + safeCount(rows) + " · columnas: " + safeCount(columns) + "."
                            : detail,
                    "Tabla");
        }
        return new SourceVisualBlockView(title, detail, new SourceTableGridView(parsed), true);
    }

    private static List<List<String>> parseMarkdownTable(String markdown) {
        List<List<String>> rows = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return rows;
        }
        for (String line : markdown.split("\\R")) {
            String trimmed = line == null ? "" : line.strip();
            if (!trimmed.startsWith("|") || !trimmed.contains("|") || trimmed.matches("[|\\s:.-]+")) {
                continue;
            }
            String[] parts = trimmed.split("\\|");
            List<String> cells = new ArrayList<>();
            for (String part : parts) {
                String cell = part == null ? "" : part.strip();
                if (!cell.isBlank()) {
                    cells.add(cell);
                }
            }
            if (!cells.isEmpty()) {
                rows.add(cells);
            }
        }
        return rows;
    }

    private static String safeCount(String value) {
        return value == null || value.isBlank() ? "?" : value.strip();
    }

    public static SourceVisualBlockView placeholder(String title, String detail, String marker) {
        Label markerLabel = new Label(marker == null || marker.isBlank() ? "Visual" : marker.strip());
        markerLabel.getStyleClass().add(AppStyles.UI_SOURCE_VISUAL_PLACEHOLDER);
        markerLabel.setWrapText(true);
        return new SourceVisualBlockView(title, detail, markerLabel);
    }

    private static final class ResponsiveSourceImageHost extends StackPane {
        private final ImageView view;
        private final Image image;

        private ResponsiveSourceImageHost(ImageView view, Image image) {
            this.view = view;
            this.image = image;
            getChildren().add(view);
            setMinWidth(0);
            setMaxWidth(Double.MAX_VALUE);
            setPrefWidth(Region.USE_COMPUTED_SIZE);
        }

        @Override
        protected void layoutChildren() {
            double width = Math.max(MIN_RESPONSIVE_IMAGE_WIDTH, getWidth() - snappedLeftInset() - snappedRightInset());
            view.setFitWidth(width);
            super.layoutChildren();
        }

        @Override
        protected double computePrefWidth(double height) {
            return MIN_RESPONSIVE_IMAGE_WIDTH;
        }

        @Override
        protected double computePrefHeight(double width) {
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return MIN_RESPONSIVE_IMAGE_WIDTH;
            }
            double targetWidth = width <= 0 ? computePrefWidth(-1) : width;
            return targetWidth * image.getHeight() / image.getWidth();
        }
    }
}
