package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreStageGeometry;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreSpatialRoleIcon;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.FloatingReadingControlBar;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TheatreFullscreenMapView {
    private static final double MAP_WIDTH = 760.0;
    private static final double MAP_HEIGHT = 476.0;
    private static final double MARKER_SIZE = 118.0;
    private static final double ARROW_TRIM_FACTOR = 0.56;
    private static final double SELF_LOOP_SCALE_FACTOR = 0.34;
    private static final double SELF_LOOP_RIGHT_OFFSET_FACTOR = 0.44;
    private static final double SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR = 0.10;

    private TheatreFullscreenMapView() {
    }

    public static void show(DocuPodcastShellViewModel viewModel,
                            Map<String, TheatreProjectLayer.TextActionPlacement> placements) {
        show(viewModel, placements, java.util.List.of(), null);
    }

    public static void show(DocuPodcastShellViewModel viewModel,
                            Map<String, TheatreProjectLayer.TextActionPlacement> placements,
                            Collection<String> stylesheets) {
        show(viewModel, placements, stylesheets, null);
    }

    public static void show(DocuPodcastShellViewModel viewModel,
                            Map<String, TheatreProjectLayer.TextActionPlacement> placements,
                            Collection<String> stylesheets,
                            Runnable primaryPlaybackAction) {
        if (viewModel == null || placements == null) {
            return;
        }
        Stage stage = new Stage();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("document-image-fullscreen-root");
        root.getStyleClass().add("theatre-fullscreen-root");

        HBox playbar = playbar(viewModel, primaryPlaybackAction);
        root.setTop(playbar);

        VBox companion = companionPane();
        MapCanvasPane mapPane = new MapCanvasPane();
        StackPane centerHost = new StackPane();
        centerHost.getStyleClass().add("theatre-fullscreen-center-host");
        root.setCenter(centerHost);

        Label bottomText = new Label("Selecciona una intervencion en el mapa de acciones.");
        bottomText.getStyleClass().add("theatre-fullscreen-bottom-text");
        bottomText.setPadding(new Insets(20, 34, 20, 34));
        bottomText.setWrapText(true);
        bottomText.setTextAlignment(TextAlignment.LEFT);
        bottomText.setMaxWidth(Double.MAX_VALUE);
        root.setBottom(bottomText);

        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().addAll(stylesheets == null ? java.util.List.of() : stylesheets);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                Label closeMsg = new Label("Cerrando mapa espacial...");
                closeMsg.getStyleClass().add("theatre-fullscreen-close-msg");
                closeMsg.setAlignment(Pos.CENTER);
                StackPane overlay = new StackPane(closeMsg);
                overlay.getStyleClass().add("theatre-fullscreen-overlay");
                overlay.setPrefSize(root.getWidth(), root.getHeight());
                root.getChildren().add(overlay);
                PauseTransition pause = new PauseTransition(Duration.seconds(1.0));
                pause.setOnFinished(e -> stage.close());
                pause.play();
            }
        });
        stage.setTitle("Mapa espacial y de acciones teatral");
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.fullScreenProperty().addListener((obs, wasFull, full) -> {
            if (Boolean.TRUE.equals(wasFull) && !Boolean.TRUE.equals(full)) {
                stage.close();
            }
        });
        stage.show();
        stage.setFullScreen(true);

        Runnable redraw = () -> {
            Map<String, TheatreProjectLayer.TextActionPlacement> visible = visiblePlacements(viewModel, placements);
            Optional<TheatreProjectLayer.TextActionPlacement> active = visible.values().stream().findFirst();
            Image background = loadBackgroundImage(viewModel, active.orElse(null)).orElse(null);
            rebuildCenter(centerHost, companion, mapPane, viewModel, visible);
            mapPane.redraw(viewModel, background, visible);
            Platform.runLater(() -> mapPane.redraw(viewModel, background, visible));
            String caption = caption(viewModel, active.orElse(null));
            bottomText.setText(caption);
            bottomText.setFont(Font.font("Georgia", FontWeight.BOLD, FontPosture.ITALIC, captionFontSize(caption)));
        };
        Platform.runLater(redraw);
        viewModel.activePlacementAliasProperty().addListener((obs, oldValue, newValue) -> redraw.run());
        viewModel.activeTextActionPlacementProperty().addListener((obs, oldValue, newValue) -> redraw.run());
        viewModel.focusedTheatreSceneIdProperty().addListener((obs, oldValue, newValue) -> redraw.run());
        viewModel.spatialFrameModeProperty().addListener((obs, oldValue, newValue) -> redraw.run());
    }

    private static HBox playbar(DocuPodcastShellViewModel viewModel, Runnable primaryPlaybackAction) {
        Label title = new Label("Mapa espacial y de acciones teatral");
        title.getStyleClass().add("theatre-fullscreen-playbar-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        FloatingReadingControlBar controls = new FloatingReadingControlBar(
                viewModel.documentPrimaryActionLabelProperty(),
                viewModel.documentPrimaryActionHintProperty(),
                primaryPlaybackAction == null ? viewModel::runDocumentPrimaryAction : primaryPlaybackAction,
                viewModel::pausePlayback,
                viewModel::resumePlayback,
                viewModel::stopPlayback,
                viewModel::playDocumentFromBeginning,
                viewModel::playPreviousFragment,
                viewModel::playNextFragment,
                viewModel.previousFragmentAvailableProperty(),
                viewModel.nextFragmentAvailableProperty(),
                Bindings.createBooleanBinding(() -> {
                    PlaybackCursor cursor = viewModel.playbackCursorProperty().get();
                    return cursor != null && !cursor.stoppedState();
                }, viewModel.playbackCursorProperty()),
                viewModel.playbackRateProperty(),
                () -> viewModel.setPlaybackRate(1.0),
                () -> viewModel.setPlaybackRate(1.5),
                () -> viewModel.setPlaybackRate(1.75),
                "Refrescar contenido",
                viewModel::refreshSourceDocument);
        controls.setVerticalLayout(false);
        controls.getStyleClass().add("theatre-fullscreen-reading-control");
        HBox bar = new HBox(14, title, spacer, controls);
        bar.getStyleClass().add("theatre-fullscreen-playbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 34, 14, 34));
        return bar;
    }

    private static VBox companionPane() {
        VBox companion = new VBox(12);
        companion.getStyleClass().add("theatre-fullscreen-companion");
        companion.setAlignment(Pos.TOP_CENTER);
        companion.setPadding(new Insets(10));
        return companion;
    }

    private static void rebuildCenter(StackPane centerHost,
                                      VBox companion,
                                      MapCanvasPane mapPane,
                                      DocuPodcastShellViewModel viewModel,
                                      Map<String, TheatreProjectLayer.TextActionPlacement> visiblePlacements) {
        clearCenterSizing(companion, mapPane);
        centerHost.getChildren().clear();
        String mode = TheatreStageGeometry.normalizeFrameMode(viewModel.spatialFrameModeProperty().get());
        if ("none".equals(mode)) {
            mapPane.getStyleClass().remove("theatre-fullscreen-map-split");
            if (!mapPane.getStyleClass().contains("theatre-fullscreen-map-only")) {
                mapPane.getStyleClass().add("theatre-fullscreen-map-only");
            }
            mapPane.setMinSize(0, 0);
            mapPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            centerHost.setPadding(Insets.EMPTY);
            StackPane.setMargin(mapPane, new Insets(22, 34, 16, 34));
            bindMapPaneToHost(centerHost, mapPane, 68, 38);
            centerHost.getChildren().add(mapPane);
            return;
        }

        rebuildCompanion(companion, viewModel, visiblePlacements);
        companion.setMaxHeight(Double.MAX_VALUE);
        mapPane.getStyleClass().remove("theatre-fullscreen-map-only");
        if (!mapPane.getStyleClass().contains("theatre-fullscreen-map-split")) {
            mapPane.getStyleClass().add("theatre-fullscreen-map-split");
        }

        HBox row = new HBox(12, companion, mapPane);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(12, 22, 8, 22));
        row.setFillHeight(true);
        HBox.setHgrow(mapPane, Priority.ALWAYS);
        if ("fragments".equals(mode)) {
            row.getStyleClass().add("theatre-fullscreen-fragments-layout");
            companion.setMinWidth(0);
            companion.setMaxWidth(Double.MAX_VALUE);
            mapPane.setMinWidth(0);
            mapPane.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(companion, Priority.ALWAYS);
            companion.prefWidthProperty().bind(Bindings.max(0, row.widthProperty().subtract(56).multiply(0.5)));
            mapPane.prefWidthProperty().bind(Bindings.max(0, row.widthProperty().subtract(56).multiply(0.5)));
            companion.prefHeightProperty().bind(Bindings.max(0, row.heightProperty().subtract(18)));
            mapPane.prefHeightProperty().bind(Bindings.max(0, row.heightProperty().subtract(18)));
        } else {
            row.getStyleClass().add("theatre-fullscreen-characters-layout");
            companion.setPrefWidth(520);
            companion.setMinWidth(420);
            companion.setMaxWidth(620);
            mapPane.setMinWidth(420);
            mapPane.setMaxWidth(Double.MAX_VALUE);
        }
        centerHost.setPadding(Insets.EMPTY);
        centerHost.getChildren().add(row);
    }

    private static void clearCenterSizing(VBox companion, MapCanvasPane mapPane) {
        companion.prefWidthProperty().unbind();
        companion.prefHeightProperty().unbind();
        mapPane.prefWidthProperty().unbind();
        mapPane.prefHeightProperty().unbind();
        StackPane.setMargin(mapPane, Insets.EMPTY);
        companion.setPrefWidth(Region.USE_COMPUTED_SIZE);
        companion.setPrefHeight(Region.USE_COMPUTED_SIZE);
        mapPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        mapPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
    }

    private static void bindMapPaneToHost(StackPane centerHost,
                                          MapCanvasPane mapPane,
                                          double horizontalMargins,
                                          double verticalMargins) {
        mapPane.prefWidthProperty().bind(Bindings.max(1, centerHost.widthProperty().subtract(horizontalMargins)));
        mapPane.prefHeightProperty().bind(Bindings.max(1, centerHost.heightProperty().subtract(verticalMargins)));
    }

    private static void rebuildCompanion(VBox companion,
                                         DocuPodcastShellViewModel viewModel,
                                         Map<String, TheatreProjectLayer.TextActionPlacement> visiblePlacements) {
        companion.getChildren().clear();
        String mode = TheatreStageGeometry.normalizeFrameMode(viewModel.spatialFrameModeProperty().get());
        String modeLabel = "fragments".equals(mode) ? "Fragmento visual"
                : "characters".equals(mode) ? "Personajes presentes" : "Sin acompanante";
        Label header = new Label(modeLabel);
        header.getStyleClass().add("theatre-fullscreen-companion-title");
        companion.getChildren().add(header);
        if ("fragments".equals(mode)) {
            Optional<String> activeVisual = activeFragmentVisualUri(viewModel, visiblePlacements);
            if (activeVisual.isPresent()) {
                StackPane imageSlot = new StackPane();
                imageSlot.getStyleClass().add("theatre-fullscreen-companion-image-slot");
                imageSlot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                VBox.setVgrow(imageSlot, Priority.ALWAYS);
                ImageView preview = new ImageView(new Image(activeVisual.get(), true));
                preview.setPreserveRatio(true);
                preview.setSmooth(true);
                preview.fitWidthProperty().bind(imageSlot.widthProperty().multiply(0.98));
                preview.fitHeightProperty().bind(imageSlot.heightProperty().multiply(0.98));
                imageSlot.getChildren().add(preview);
                companion.getChildren().add(imageSlot);
            } else {
                companion.getChildren().add(mutLabel("Sin fragmento visual"));
            }
        } else if ("characters".equals(mode)) {
            List<Participant> participants = participants(viewModel, visiblePlacements.values().stream().findFirst().orElse(null));
            if (participants.isEmpty()) {
                companion.getChildren().add(mutLabel("Sin personajes presentes"));
            } else {
                companion.getChildren().add(characterCompanionGrid(viewModel, participants));
            }
        } else {
            companion.getChildren().add(mutLabel("Sin acompanante"));
        }
    }

    private static TilePane characterCompanionGrid(DocuPodcastShellViewModel viewModel, List<Participant> participants) {
        int visibleCount = Math.min(4, participants.size());
        int columns = visibleCount <= 1 ? 1 : 2;
        double tileWidth = columns == 1 ? 360 : 210;
        double imageWidth = columns == 1 ? 330 : 190;
        double imageHeight = columns == 1 ? 470 : (visibleCount <= 2 ? 360 : 245);
        TilePane grid = new TilePane();
        grid.getStyleClass().add("theatre-fullscreen-character-grid");
        grid.setAlignment(Pos.CENTER);
        grid.setTileAlignment(Pos.CENTER);
        grid.setPrefColumns(columns);
        grid.setHgap(18);
        grid.setVgap(20);
        grid.setPrefTileWidth(tileWidth);
        grid.setMaxWidth((tileWidth * columns) + (columns - 1) * 18);
        VBox.setVgrow(grid, Priority.ALWAYS);
        for (int i = 0; i < visibleCount; i++) {
            grid.getChildren().add(characterCard(viewModel, participants.get(i), imageWidth, imageHeight));
        }
        if (participants.size() > visibleCount) {
            grid.getChildren().add(mutLabel("+" + (participants.size() - visibleCount) + " personajes"));
        }
        return grid;
    }

    private static VBox characterCard(DocuPodcastShellViewModel viewModel,
                                      Participant participant,
                                      double imageWidth,
                                      double imageHeight) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        characterImageUri(viewModel, participant).ifPresentOrElse(uri -> {
            ImageView image = new ImageView(new Image(uri, true));
            image.setFitWidth(imageWidth);
            image.setFitHeight(imageHeight);
            image.setPreserveRatio(true);
            image.setSmooth(true);
            card.getChildren().add(image);
        }, () -> card.getChildren().add(mutLabel("Sin imagen")));
        Label name = mutLabel(participant.name());
        name.getStyleClass().add("theatre-fullscreen-character-name");
        card.getChildren().add(name);
        return card;
    }

    private static Label mutLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("theatre-fullscreen-companion-muted");
        label.setWrapText(true);
        return label;
    }

    private static void drawMap(Canvas canvas, DocuPodcastShellViewModel viewModel, Image background,
                                Map<String, TheatreProjectLayer.TextActionPlacement> placements) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawBounds mapBounds = new DrawBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        if (background != null) {
            if (!background.isError() && background.getWidth() > 0) {
                mapBounds = imageFitBounds(background, 0, 0, canvas.getWidth(), canvas.getHeight());
                gc.drawImage(background, mapBounds.x(), mapBounds.y(), mapBounds.width(), mapBounds.height());
            } else {
                drawMissingMap(gc, canvas);
            }
        } else {
            drawMissingMap(gc, canvas);
        }
        DrawBounds overlayBounds = mapBounds;
        placements.values().stream().findFirst().ifPresent(placement -> {
            StagePoint origin = stagePoint(placement.origin(), overlayBounds);
            destinationLocations(viewModel, placement).forEach(destination ->
                    drawActionArrow(gc, origin, stagePoint(destination, overlayBounds), overlayBounds));
            drawParticipantGroups(gc, participants(viewModel, placement), overlayBounds,
                    selfLoopLocations(viewModel, placement, overlayBounds));
        });
    }

    private static void drawActionArrow(GraphicsContext gc, StagePoint origin, StagePoint dest, DrawBounds mapBounds) {
        boolean samePoint = Math.abs(origin.x() - dest.x()) < 0.5 && Math.abs(origin.y() - dest.y()) < 0.5;
        if (samePoint) {
            drawSelfLoop(gc, origin, mapBounds);
            return;
        }
        double trim = MARKER_SIZE * ARROW_TRIM_FACTOR;
        strokeArrow(gc, origin, dest, Color.web("rgba(0, 0, 0, 0.34)"), 10, 19, trim, trim);
        strokeArrow(gc, origin, dest, Color.WHITE, 8, 16, trim, trim);
        strokeArrow(gc, origin, dest, Color.web("#020617"), 4, 13, trim, trim);
    }

    private static void drawSelfLoop(GraphicsContext gc, StagePoint origin, DrawBounds mapBounds) {
        double size = MARKER_SIZE * SELF_LOOP_SCALE_FACTOR;
        double minX = mapBounds == null ? 4 : mapBounds.x() + 4;
        double minY = mapBounds == null ? 4 : mapBounds.y() + 4;
        double maxX = mapBounds == null ? MAP_WIDTH - size * 1.14 - 4 : mapBounds.x() + mapBounds.width() - size * 1.14 - 4;
        double maxY = mapBounds == null ? MAP_HEIGHT - size - 4 : mapBounds.y() + mapBounds.height() - size - 4;
        double x = clamp(origin.x() + MARKER_SIZE * SELF_LOOP_RIGHT_OFFSET_FACTOR, minX, maxX);
        double y = clamp(origin.y() + size * 0.06, minY, maxY);
        strokeSelfLoop(gc, x, y, size, Color.web("rgba(0, 0, 0, 0.34)"), 6, 10);
        strokeSelfLoop(gc, x, y, size, Color.WHITE, 4, 8);
        strokeSelfLoop(gc, x, y, size, Color.web("#020617"), 2.5, 6);
    }

    private static void strokeSelfLoop(GraphicsContext gc,
                                       double x,
                                       double y,
                                       double size,
                                       Color color,
                                       double lineWidth,
                                       double headLength) {
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        double startX = x + size * 0.10;
        double startY = y + size * 0.72;
        double c1x = x + size * 1.08;
        double c1y = y + size * 0.94;
        double c2x = x + size * 1.08;
        double c2y = y + size * 0.08;
        double ex = x + size * 0.12;
        double ey = y + size * 0.24;
        gc.beginPath();
        gc.moveTo(startX, startY);
        gc.bezierCurveTo(c1x, c1y, c2x, c2y, ex, ey);
        gc.stroke();
        double tangentAngle = Math.atan2(ey - c2y, ex - c2x);
        fillArrowHead(gc, ex, ey, tangentAngle, headLength);
    }

    private static void fillArrowHead(GraphicsContext gc,
                                      double tipX,
                                      double tipY,
                                      double angle,
                                      double headLength) {
        double spread = Math.toRadians(28);
        gc.fillPolygon(
                new double[]{
                        tipX,
                        tipX - headLength * Math.cos(angle - spread),
                        tipX - headLength * Math.cos(angle + spread)
                },
                new double[]{
                        tipY,
                        tipY - headLength * Math.sin(angle - spread),
                        tipY - headLength * Math.sin(angle + spread)
                },
                3);
    }

    private static void strokeArrow(GraphicsContext gc,
                                    StagePoint origin,
                                    StagePoint dest,
                                    Color color,
                                    double lineWidth,
                                    double headLength,
                                    double trimStart,
                                    double trimEnd) {
        double distance = Math.hypot(dest.x() - origin.x(), dest.y() - origin.y());
        if (distance <= trimStart + trimEnd + 1) {
            return;
        }
        double startX = origin.x() + (dest.x() - origin.x()) / distance * trimStart;
        double startY = origin.y() + (dest.y() - origin.y()) / distance * trimStart;
        double endX = dest.x() - (dest.x() - origin.x()) / distance * trimEnd;
        double endY = dest.y() - (dest.y() - origin.y()) / distance * trimEnd;
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.strokeLine(startX, startY, endX, endY);
        double angle = Math.atan2(endY - startY, endX - startX);
        double spread = Math.toRadians(24);
        double headWidth = Math.max(2, lineWidth * 0.78);
        gc.setLineWidth(headWidth);
        gc.strokeLine(endX, endY,
                endX - headLength * Math.cos(angle - spread),
                endY - headLength * Math.sin(angle - spread));
        gc.strokeLine(endX, endY,
                endX - headLength * Math.cos(angle + spread),
                endY - headLength * Math.sin(angle + spread));
    }

    private static void drawMissingMap(GraphicsContext gc, Canvas canvas) {
        gc.setFill(Color.web("rgba(15, 23, 42, 0.74)"));
        gc.fillRoundRect(0, 0, canvas.getWidth(), canvas.getHeight(), 12, 12);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 24));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Mapa espacial no configurado", canvas.getWidth() / 2.0, canvas.getHeight() / 2.0);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private static DrawBounds imageFitBounds(Image image, double x, double y, double width, double height) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || width <= 0 || height <= 0) {
            return new DrawBounds(x, y, Math.max(1, width), Math.max(1, height));
        }
        double scale = Math.min(width / image.getWidth(), height / image.getHeight());
        double drawW = Math.max(1, image.getWidth() * scale);
        double drawH = Math.max(1, image.getHeight() * scale);
        double drawX = x + (width - drawW) / 2.0;
        double drawY = y + (height - drawH) / 2.0;
        return new DrawBounds(drawX, drawY, drawW, drawH);
    }

    private static void drawMarker(GraphicsContext gc, StagePoint point, TheatreSpatialRoleIcon role) {
        Image icon = TheatreSpatialIconSet.image(role);
        double size = MARKER_SIZE;
        if (icon != null) {
            gc.drawImage(icon, point.x() - size / 2.0, point.y() - size / 2.0, size, size);
            return;
        }
        gc.setFill(Color.web("rgba(255, 255, 255, 0.90)"));
        gc.fillOval(point.x() - size / 2.0, point.y() - size / 2.0, size, size);
        gc.setStroke(Color.web("#020617"));
        gc.setLineWidth(2.5);
        gc.strokeOval(point.x() - size / 2.0, point.y() - size / 2.0, size, size);
    }

    private static Map<String, TheatreProjectLayer.TextActionPlacement> visiblePlacements(
            DocuPodcastShellViewModel viewModel,
            Map<String, TheatreProjectLayer.TextActionPlacement> placements) {
        if (placements == null || placements.isEmpty()) {
            return Map.of();
        }
        String activeAlias = viewModel.activePlacementAliasProperty().get();
        if (activeAlias == null || activeAlias.isBlank()) {
            return Map.of();
        }
        if (activeAlias != null && !activeAlias.isBlank()) {
            for (Map.Entry<String, TheatreProjectLayer.TextActionPlacement> entry : placements.entrySet()) {
                if (activeAlias.equals(entry.getValue().intervencionId())) {
                    return Map.of(entry.getKey(), entry.getValue());
                }
            }
        }
        return Map.of();
    }

    private static void drawParticipantGroups(GraphicsContext gc,
                                              List<Participant> participants,
                                              DrawBounds mapBounds,
                                              List<String> selfLoopLocations) {
        Map<String, MarkerGroup> byLocation = new LinkedHashMap<>();
        for (Participant participant : participants) {
            String location = participant.location() == null || participant.location().isBlank()
                    ? "centro" : participant.location();
            MarkerGroup group = byLocation.computeIfAbsent(location,
                    ignored -> new MarkerGroup(participant.role(), new ArrayList<>()));
            group.role = mergeRole(group.role, participant.role());
            if (containsIgnoreCase(selfLoopLocations, location)) {
                group.selfLoop = true;
            }
            if (!participant.name().isBlank() && !group.names.contains(participant.name())) {
                group.names.add(participant.name());
            }
            if (participant.speaking() && !participant.name().isBlank() && !group.speakers.contains(participant.name())) {
                group.speakers.add(participant.name());
            }
        }
        int index = 0;
        for (Map.Entry<String, MarkerGroup> entry : byLocation.entrySet()) {
            StagePoint pt = markerPointForSelfLoop(stagePoint(entry.getKey(), mapBounds), entry.getValue().selfLoop, mapBounds);
            drawMarker(gc, pt, entry.getValue().role);
            drawCharacterLabel(gc, pt, entry.getValue().names, entry.getValue().speakers, index++, mapBounds);
        }
    }

    private static List<String> selfLoopLocations(DocuPodcastShellViewModel viewModel,
                                                  TheatreProjectLayer.TextActionPlacement placement,
                                                  DrawBounds mapBounds) {
        ArrayList<String> result = new ArrayList<>();
        StagePoint origin = stagePoint(placement.origin(), mapBounds);
        destinationLocations(viewModel, placement).forEach(destination -> {
            if (sameStagePoint(origin, stagePoint(destination, mapBounds))) {
                addUnique(result, placement.origin());
            }
        });
        return List.copyOf(result);
    }

    private static boolean sameStagePoint(StagePoint first, StagePoint second) {
        return Math.abs(first.x() - second.x()) < 0.5 && Math.abs(first.y() - second.y()) < 0.5;
    }

    private static StagePoint markerPointForSelfLoop(StagePoint point, boolean selfLoop, DrawBounds mapBounds) {
        if (!selfLoop) {
            return point;
        }
        double half = MARKER_SIZE / 2.0;
        double minX = mapBounds.x() + half + 4;
        double maxX = mapBounds.x() + mapBounds.width() - half - 4;
        double x = clamp(point.x() - MARKER_SIZE * SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR, minX, maxX);
        return new StagePoint(x, point.y());
    }

    private static void drawCharacterLabel(GraphicsContext gc,
                                           StagePoint point,
                                           List<String> characters,
                                           List<String> speakers,
                                           int stackIndex,
                                           DrawBounds mapBounds) {
        double lineHeight = 17;
        double height = Math.max(22, 9 + Math.min(4, characters.size()) * lineHeight);
        double width = characterLabelWidth(characters, mapBounds.width());
        double minX = mapBounds.x() + 4;
        double maxX = mapBounds.x() + mapBounds.width() - width - 4;
        double minY = mapBounds.y() + 4;
        double maxY = mapBounds.y() + mapBounds.height() - height - 4;
        double x = Math.max(minX, Math.min(maxX, point.x() - width / 2 + (stackIndex % 2) * 8));
        double y = Math.max(minY, Math.min(maxY, point.y() + MARKER_SIZE / 2.0 + 8 + (stackIndex * 14)));
        gc.setFill(Color.web("rgba(15, 23, 42, 0.82)"));
        gc.fillRoundRect(x, y, width, height, 5, 5);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
        int shown = Math.min(4, characters.size());
        for (int i = 0; i < shown; i++) {
            String character = characters.get(i) == null ? "" : characters.get(i);
            double textX = x + 8;
            if (containsIgnoreCase(speakers, character)) {
                double cy = y + 10 + i * lineHeight;
                gc.setFill(Color.web("#F59E0B"));
                gc.fillOval(x + 8, cy - 4, 7, 7);
                gc.setFill(Color.WHITE);
                textX = x + 20;
            }
            gc.fillText(character, textX, y + 14 + i * lineHeight);
        }
        if (characters.size() > shown) {
            gc.fillText("+" + (characters.size() - shown), x + 8, y + 14 + shown * lineHeight);
        }
    }

    private static double characterLabelWidth(List<String> characters, double maxWidth) {
        int maxLength = 0;
        for (String character : characters) {
            maxLength = Math.max(maxLength, character == null ? 0 : character.length());
        }
        return Math.max(146, Math.min(Math.max(146, maxWidth - 8), 22 + maxLength * 7.2));
    }

    private static Optional<String> activeFragmentVisualUri(
            DocuPodcastShellViewModel viewModel,
            Map<String, TheatreProjectLayer.TextActionPlacement> visiblePlacements) {
        String alias = visiblePlacements.values().stream()
                .map(TheatreProjectLayer.TextActionPlacement::intervencionId)
                .findFirst()
                .orElse("");
        if (alias.isBlank()) {
            return Optional.empty();
        }
        return viewModel.theatreIntervencionesVisuales().stream()
                .filter(visual -> alias.equals(visual.intervencionId()))
                .findFirst()
                .flatMap(visual -> viewModel.projectImageAssetUri(visual.assetId()));
    }

    private static Optional<Image> loadBackgroundImage(DocuPodcastShellViewModel viewModel,
                                                       TheatreProjectLayer.TextActionPlacement activePlacement) {
        Optional<TheatreProjectLayer.Scene> scene = Optional.empty();
        if (activePlacement != null && activePlacement.sceneId() != null && !activePlacement.sceneId().isBlank()) {
            scene = viewModel.theatreScenes().stream()
                    .filter(candidate -> candidate.id().equals(activePlacement.sceneId()))
                    .findFirst();
        }
        if (scene.isEmpty()) {
            String focusedSceneId = viewModel.focusedTheatreSceneIdProperty().get();
            scene = viewModel.theatreScenes().stream()
                    .filter(candidate -> candidate.id().equals(focusedSceneId))
                    .findFirst();
        }
        return scene
                .filter(candidate -> candidate.spatialMapAssetId() != null && !candidate.spatialMapAssetId().isBlank())
                .flatMap(candidate -> viewModel.projectImageAssetUri(candidate.spatialMapAssetId()))
                .map(uri -> new Image(uri, false));
    }

    private static Optional<String> characterImageUri(DocuPodcastShellViewModel viewModel, Participant participant) {
        if (viewModel == null || participant == null || participant.characterId().isBlank()) {
            return Optional.empty();
        }
        return viewModel.theatreCharacterSceneImages(participant.characterId(), participant.sceneId()).stream()
                .findFirst()
                .flatMap(image -> viewModel.projectImageAssetUri(image.assetId()));
    }

    private static List<Participant> participants(DocuPodcastShellViewModel viewModel,
                                                  TheatreProjectLayer.TextActionPlacement placement) {
        if (placement == null) {
            return List.of();
        }
        LinkedHashMap<String, Participant> result = new LinkedHashMap<>();
        String speakerName = characterDisplayName(viewModel, placement.characterId());
        if (speakerName.isBlank()) {
            speakerName = firstNonSpecialCharacter(placement.characterLocations());
        }
        if (!speakerName.isBlank()) {
            result.put(speakerName, new Participant(placement.characterId(), speakerName,
                    placement.sceneId(), locationFor(placement, speakerName, placement.origin()),
                    TheatreSpatialRoleIcon.forSpeaker(speakerName), true));
        }
        for (String target : interactionTargets(placement.interactionTarget())) {
            if (isSelfTarget(target)) {
                continue;
            }
            if (TheatreSpatialRoleIcon.isAudience(target)) {
                result.putIfAbsent("PUBLICO", new Participant("", "PUBLICO", placement.sceneId(),
                        audienceLocation(), TheatreSpatialRoleIcon.AUDIENCE, false));
                continue;
            }
            if (isOffstageTarget(target)) {
                continue;
            }
            String targetId = characterIdForDisplay(viewModel, target);
            String targetName = characterDisplayName(viewModel, targetId);
            if (targetName.isBlank()) {
                targetName = target.strip();
            }
            if (isCharacterAbsent(placement, targetName)) {
                continue;
            }
            if (!targetName.equalsIgnoreCase(speakerName)) {
                result.put(targetName, new Participant(targetId, targetName,
                        placement.sceneId(), locationFor(placement, targetName, placement.destination()),
                        TheatreSpatialRoleIcon.forSpeaker(targetName), false));
            }
        }
        if (TheatreSpatialRoleIcon.isAudience(placement.destination())) {
            result.putIfAbsent("PUBLICO", new Participant("", "PUBLICO", placement.sceneId(),
                    audienceLocation(), TheatreSpatialRoleIcon.AUDIENCE, false));
        }
        if (placement.characterLocations() != null) {
            placement.characterLocations().forEach((character, location) -> {
                if (character != null && !character.isBlank() && !isSpecialTarget(character) && !isAbsentLocation(location)) {
                    String displayName = characterDisplayName(viewModel, characterIdForDisplay(viewModel, character));
                    if (displayName.isBlank()) {
                        displayName = character.strip();
                    }
                    result.putIfAbsent(displayName, new Participant(characterIdForDisplay(viewModel, displayName),
                            displayName, placement.sceneId(), location, TheatreSpatialRoleIcon.forSpeaker(displayName), false));
                }
            });
        }
        return List.copyOf(result.values());
    }

    private static String caption(DocuPodcastShellViewModel viewModel, TheatreProjectLayer.TextActionPlacement placement) {
        if (placement == null) {
            return "Selecciona una intervencion en el mapa de acciones.";
        }
        IntervencionCatalogo.IntervencionInfo catalogInfo = IntervencionCatalogo.intervenciones(
                        viewModel.currentDocumentProperty().get(),
                        viewModel.currentScriptProperty().get()).stream()
                .filter(item -> placement.intervencionId().equals(item.alias()))
                .findFirst()
                .orElse(null);
        String fullText = catalogInfo == null ? "" : catalogInfo.fullText();
        String preview = fullText.isBlank() && catalogInfo != null ? catalogInfo.preview() : fullText;
        String speaker = characterDisplayName(viewModel, placement.characterId());
        if (speaker.isBlank()) {
            speaker = cueLabel(preview);
        }
        String text = stripSpeaker(preview);
        return (speaker.isBlank() ? placement.intervencionId() : speaker) + ": " + text;
    }

    private static int captionFontSize(String text) {
        int length = text == null ? 0 : text.length();
        if (length > 260) {
            return 24;
        }
        if (length > 190) {
            return 27;
        }
        if (length > 130) {
            return 30;
        }
        return 34;
    }

    private static String characterDisplayName(DocuPodcastShellViewModel viewModel, String characterId) {
        if (viewModel == null || characterId == null || characterId.isBlank()) {
            return "";
        }
        return viewModel.theatreCharacterProfiles().stream()
                .filter(character -> character.id().equals(characterId))
                .map(TheatreProjectLayer.CharacterProfile::displayName)
                .findFirst()
                .orElse("");
    }

    private static String characterIdForDisplay(DocuPodcastShellViewModel viewModel, String displayName) {
        if (viewModel == null || displayName == null || displayName.isBlank()) {
            return "";
        }
        String normalized = displayName.strip();
        return viewModel.theatreCharacterProfiles().stream()
                .filter(character -> character.displayName().equalsIgnoreCase(normalized)
                        || character.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(normalized)))
                .map(TheatreProjectLayer.CharacterProfile::id)
                .findFirst()
                .orElse("");
    }

    private static String firstNonSpecialCharacter(Map<String, String> locations) {
        if (locations == null) {
            return "";
        }
        return locations.keySet().stream()
                .filter(name -> !isSpecialTarget(name))
                .findFirst()
                .orElse("");
    }

    private static String locationFor(TheatreProjectLayer.TextActionPlacement placement, String character, String fallback) {
        if (placement.characterLocations() != null) {
            for (Map.Entry<String, String> entry : placement.characterLocations().entrySet()) {
                if (entry.getKey().equalsIgnoreCase(character)
                        && entry.getValue() != null
                        && !entry.getValue().isBlank()
                        && !isAbsentLocation(entry.getValue())) {
                    return entry.getValue();
                }
            }
        }
        return fallback == null || fallback.isBlank() ? "centro" : fallback;
    }

    private static String cueLabel(String preview) {
        String text = preview == null ? "" : preview.strip();
        int colon = text.indexOf(':');
        if (colon <= 0 || colon > 42) {
            return "";
        }
        return text.substring(0, colon).strip();
    }

    private static String stripSpeaker(String preview) {
        String text = preview == null ? "" : preview.strip();
        int colon = text.indexOf(':');
        if (colon <= 0 || colon > 42) {
            return text;
        }
        return text.substring(colon + 1).strip();
    }

    private static boolean isSpecialTarget(String target) {
        String normalized = target == null ? "" : target.strip().toLowerCase(Locale.ROOT);
        return normalized.equals("publico")
                || normalized.equals("público")
                || normalized.equals("para si mismo")
                || normalized.equals("para sí mismo")
                || normalized.equals("entidad no presente en escenario");
    }

    private static boolean isSelfTarget(String target) {
        String normalized = target == null ? "" : target.strip().toLowerCase(Locale.ROOT);
        return normalized.equals("para si mismo") || normalized.equals("para sí mismo");
    }

    private static boolean isOffstageTarget(String target) {
        return "entidad no presente en escenario".equals(target == null ? "" : target.strip().toLowerCase(Locale.ROOT));
    }

    private static boolean isAbsentLocation(String location) {
        String normalized = location == null ? "" : location.strip().toLowerCase(Locale.ROOT);
        return normalized.equals("no presente") || normalized.equals("no presente en esta intervencion");
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        if (values == null || candidate == null || candidate.isBlank()) {
            return false;
        }
        return values.stream().anyMatch(value -> candidate.equalsIgnoreCase(value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> destinationLocations(DocuPodcastShellViewModel viewModel,
                                                     TheatreProjectLayer.TextActionPlacement placement) {
        if (placement == null) {
            return List.of("centro");
        }
        ArrayList<String> result = new ArrayList<>();
        for (String target : interactionTargets(placement.interactionTarget())) {
            if (isSelfTarget(target)) {
                addUnique(result, placement.origin());
            } else if (TheatreSpatialRoleIcon.isAudience(target)) {
                addUnique(result, audienceLocation());
            } else if (isOffstageTarget(target)) {
                addUnique(result, placement.destination());
            } else {
                String display = characterDisplayName(viewModel, characterIdForDisplay(viewModel, target));
                if (isCharacterAbsent(placement, display.isBlank() ? target : display)) {
                    continue;
                }
                addUnique(result, locationFor(placement, display.isBlank() ? target : display, placement.destination()));
            }
        }
        if (result.isEmpty()) {
            addUnique(result, placement.destination());
        }
        return List.copyOf(result);
    }

    private static List<String> interactionTargets(String target) {
        String normalized = target == null ? "" : target.strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        if (isSpecialTarget(normalized)) {
            return List.of(normalized);
        }
        String[] parts = normalized.split("\\s*(?:,|;|/|\\s+y\\s+)\\s*");
        ArrayList<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part.strip());
            }
        }
        return result.isEmpty() ? List.of(normalized) : List.copyOf(result);
    }

    private static String audienceLocation() {
        return "hacia el publico";
    }

    private static TheatreSpatialRoleIcon mergeRole(TheatreSpatialRoleIcon current, TheatreSpatialRoleIcon incoming) {
        if (current == TheatreSpatialRoleIcon.NARRATOR || incoming == TheatreSpatialRoleIcon.NARRATOR) {
            return TheatreSpatialRoleIcon.NARRATOR;
        }
        if (current == TheatreSpatialRoleIcon.AUDIENCE || incoming == TheatreSpatialRoleIcon.AUDIENCE) {
            return TheatreSpatialRoleIcon.AUDIENCE;
        }
        return TheatreSpatialRoleIcon.ACTOR;
    }

    private static void addUnique(List<String> values, String value) {
        if (isAbsentLocation(value)) {
            return;
        }
        String safe = value == null || value.isBlank() ? "centro" : value.strip();
        if (!values.contains(safe)) {
            values.add(safe);
        }
    }

    private static boolean isCharacterAbsent(TheatreProjectLayer.TextActionPlacement placement, String character) {
        if (placement == null || placement.characterLocations() == null || character == null || character.isBlank()) {
            return false;
        }
        return placement.characterLocations().entrySet().stream()
                .anyMatch(entry -> entry.getKey().equalsIgnoreCase(character) && isAbsentLocation(entry.getValue()));
    }

    private static StagePoint stagePoint(String location, DrawBounds mapBounds) {
        TheatreStageGeometry.StagePoint point = TheatreStageGeometry.pointFor(location);
        return new StagePoint(mapBounds.x() + point.scaledX(mapBounds.width()),
                mapBounds.y() + point.scaledY(mapBounds.height()));
    }

    private static final class MapCanvasPane extends StackPane {
        private final Canvas canvas = new Canvas(MAP_WIDTH, MAP_HEIGHT);
        private DocuPodcastShellViewModel viewModel;
        private Image background;
        private Map<String, TheatreProjectLayer.TextActionPlacement> placements = Map.of();

        private MapCanvasPane() {
            getStyleClass().add("theatre-fullscreen-map-pane");
            setAlignment(Pos.CENTER);
            setPadding(new Insets(8));
            setMinSize(0, 0);
            getChildren().add(canvas);
            widthProperty().addListener((obs, oldValue, newValue) -> redrawCurrent());
            heightProperty().addListener((obs, oldValue, newValue) -> redrawCurrent());
        }

        @Override
        protected double computePrefWidth(double height) {
            Insets insets = getInsets();
            return MAP_WIDTH + insets.getLeft() + insets.getRight();
        }

        @Override
        protected double computePrefHeight(double width) {
            Insets insets = getInsets();
            return MAP_HEIGHT + insets.getTop() + insets.getBottom();
        }

        @Override
        protected double computeMinWidth(double height) {
            return 0;
        }

        @Override
        protected double computeMinHeight(double width) {
            return 0;
        }

        private void redraw(DocuPodcastShellViewModel viewModel,
                            Image background,
                            Map<String, TheatreProjectLayer.TextActionPlacement> placements) {
            this.viewModel = viewModel;
            this.background = background;
            this.placements = placements == null ? Map.of() : Map.copyOf(placements);
            redrawCurrent();
        }

        private void redrawCurrent() {
            resizeMapCanvas();
            if (viewModel != null) {
                drawMap(canvas, viewModel, background, placements);
            }
        }

        private void resizeMapCanvas() {
            Insets insets = getInsets();
            double availableWidth = Math.max(1, getWidth() - insets.getLeft() - insets.getRight());
            double availableHeight = Math.max(1, getHeight() - insets.getTop() - insets.getBottom());
            if (availableWidth <= 2 || availableHeight <= 2) {
                availableWidth = MAP_WIDTH;
                availableHeight = MAP_HEIGHT;
            }
            double scale = Math.min(availableWidth / MAP_WIDTH, availableHeight / MAP_HEIGHT);
            double canvasWidth = Math.max(1, MAP_WIDTH * scale);
            double canvasHeight = Math.max(1, MAP_HEIGHT * scale);
            if (Math.abs(canvas.getWidth() - canvasWidth) > 0.5) {
                canvas.setWidth(canvasWidth);
            }
            if (Math.abs(canvas.getHeight() - canvasHeight) > 0.5) {
                canvas.setHeight(canvasHeight);
            }
        }
    }

    private record StagePoint(double x, double y) {
    }

    private record DrawBounds(double x, double y, double width, double height) {
    }

    private static final class MarkerGroup {
        private TheatreSpatialRoleIcon role;
        private final List<String> names;
        private final List<String> speakers = new ArrayList<>();
        private boolean selfLoop;

        private MarkerGroup(TheatreSpatialRoleIcon role, List<String> names) {
            this.role = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
            this.names = names == null ? new ArrayList<>() : names;
        }
    }

    private record Participant(String characterId,
                               String name,
                               String sceneId,
                               String location,
                               TheatreSpatialRoleIcon role,
                               boolean speaking) {
        private Participant {
            characterId = characterId == null ? "" : characterId;
            name = name == null ? "" : name.strip();
            sceneId = sceneId == null ? "" : sceneId;
            location = location == null || location.isBlank() ? "centro" : location.strip();
            role = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
        }
    }
}
