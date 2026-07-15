package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

/** Visual zigzag map for theatre storyboard frames. */
final class TheatreVisualSequenceMap extends Pane {
    private static final double WIDTH = 1020;
    private static final double LEFT = 26;
    private static final double TOP = 22;
    private static final double CARD_WIDTH = 236;
    private static final double CARD_HEIGHT = 204;
    private static final double ROW_HEIGHT = 248;
    private static final double BOTTOM = 28;
    private static final int COLUMNS = 3;

    TheatreVisualSequenceMap(List<Item> items) {
        List<Item> safeItems = items == null ? List.of() : List.copyOf(items);
        getStyleClass().add("theatre-visual-sequence-map");
        setPrefSize(WIDTH, TheatreZigzagLayout.heightFor(safeItems.size(), TOP, ROW_HEIGHT, BOTTOM, COLUMNS));
        setMinSize(WIDTH, getPrefHeight());
        render(safeItems);
    }

    private void render(List<Item> items) {
        getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("Sin intervenciones visuales para esta escena.");
            empty.getStyleClass().add("document-media-empty-note");
            empty.relocate(18, 28);
            getChildren().add(empty);
            return;
        }

        List<TheatreZigzagLayout.StagePoint> points = TheatreZigzagLayout.pointsFor(
                items.size(),
                WIDTH,
                LEFT,
                TOP,
                CARD_WIDTH,
                CARD_HEIGHT,
                ROW_HEIGHT,
                COLUMNS);
        ArrayList<Node> lines = new ArrayList<>();
        for (int index = 0; index < points.size() - 1; index++) {
            TheatreZigzagLayout.StagePoint from = points.get(index);
            TheatreZigzagLayout.StagePoint to = points.get(index + 1);
            Line line = new Line(from.centerX(), from.centerY(), to.centerX(), to.centerY());
            line.getStyleClass().add("theatre-visual-sequence-line");
            lines.add(line);
        }
        getChildren().addAll(lines);
        for (int index = 0; index < points.size(); index++) {
            TheatreZigzagLayout.StagePoint point = points.get(index);
            Node card = card(items.get(index));
            card.relocate(point.x(), point.y());
            getChildren().add(card);
        }
    }

    private Node card(Item item) {
        StackPane preview = new StackPane();
        preview.setMinSize(210, 118);
        preview.setPrefSize(210, 118);
        preview.getStyleClass().add("document-media-thumbnail-empty");
        if (item.imageUri() == null || item.imageUri().isBlank()) {
            Label empty = new Label("Boceto pendiente");
            empty.setTextFill(Color.BLACK);
            preview.getChildren().add(empty);
        } else {
            ImageView imageView = new ImageView(new Image(item.imageUri(), 210, 118, true, true, true));
            imageView.setFitWidth(210);
            imageView.setFitHeight(118);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            preview.getChildren().add(imageView);
        }

        Button fullscreen = smallButton(AppIcon.FULLSCREEN, "Ver imagen en pantalla completa.", item.onFullscreen());
        fullscreen.setDisable(item.imageUri() == null || item.imageUri().isBlank());
        Button toggle = smallButton(AppIcon.REFRESH, "Alternar imagen oficial/generada y frame dibujado.", item.onToggleVariant());
        toggle.setDisable(!item.canToggle());
        Button export = smallButton(AppIcon.SAVE, "Exportar frame activo.", item.onExportFrame());
        export.setDisable(!item.canExport());
        HBox actions = new HBox(4, fullscreen, toggle, export);
        actions.setAlignment(Pos.TOP_RIGHT);
        StackPane.setAlignment(actions, Pos.TOP_RIGHT);
        StackPane.setMargin(actions, new Insets(5));
        preview.getChildren().add(actions);

        Label title = new Label(item.title());
        title.getStyleClass().add("document-media-frame-title");
        title.setWrapText(true);
        Label variant = new Label(item.variantLabel());
        variant.getStyleClass().add("document-media-counter-label");
        Label text = new Label(item.preview());
        text.getStyleClass().add("document-media-preview");
        text.setWrapText(true);

        VBox box = new VBox(6, preview, title, variant, text);
        box.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        box.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        box.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        box.setPadding(new Insets(9));
        box.getStyleClass().add("document-media-frame-card");
        return box;
    }

    private static Button smallButton(AppIcon icon, String tooltip, Runnable action) {
        Button button = ActionButtonFactory.iconOnly(icon, tooltip, action, "document-media-thumbnail-corner-action");
        button.setMinSize(26, 24);
        button.setPrefSize(26, 24);
        button.setMaxSize(26, 24);
        return button;
    }

    record Item(
            String segmentId,
            String title,
            String preview,
            String imageUri,
            String variantLabel,
            boolean canToggle,
            boolean canExport,
            Runnable onFullscreen,
            Runnable onToggleVariant,
            Runnable onExportFrame
    ) {
        Item {
            segmentId = safe(segmentId);
            title = safe(title).isBlank() ? "Intervencion" : safe(title);
            preview = safe(preview);
            imageUri = safe(imageUri);
            variantLabel = safe(variantLabel).isBlank() ? "Boceto pendiente" : safe(variantLabel);
            onFullscreen = onFullscreen == null ? () -> { } : onFullscreen;
            onToggleVariant = onToggleVariant == null ? () -> { } : onToggleVariant;
            onExportFrame = onExportFrame == null ? () -> { } : onExportFrame;
        }

        private static String safe(String value) {
            return value == null ? "" : value.strip();
        }
    }
}
