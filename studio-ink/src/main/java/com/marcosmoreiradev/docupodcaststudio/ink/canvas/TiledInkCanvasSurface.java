package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPoint;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkTool;
import com.marcosmoreiradev.docupodcaststudio.ink.InkBrushMath;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tile-based drawing surface for ink workspaces. */
public class TiledInkCanvasSurface extends Pane {
    public static final double DEFAULT_WIDTH = 980;
    public static final double DEFAULT_HEIGHT = 1800;
    public static final double EDGE_GROW_THRESHOLD = 84;
    static final double CANVAS_TILE_SIZE = 1024;
    static final int MAX_CANVAS_COLUMNS = 6;
    static final int MAX_CANVAS_ROWS = 16;
    static final int EXPORT_SCALE = 4;
    static final long MAX_EXPORT_PIXELS = 48_000_000L;
    private static final double SCROLL_GROWTH_FACTOR = 1.25;
    private static final double EXPORT_MARGIN = 100;
    private static final double MIN_EXPORT_WIDTH = 720;
    private static final double MIN_EXPORT_HEIGHT = 520;

    private final Pane backgroundLayer = new Pane();
    private final Pane imageLayer = new Pane();
    private final Pane strokeLayer = new Pane();
    private final Pane liveStrokeLayer = new Pane();
    private final Pane inkInputLayer = new Pane();
    private final Rectangle inkInputHitArea = new Rectangle();
    private final List<CanvasTile> backgroundTiles = new ArrayList<>();
    private final List<CanvasTile> strokeTiles = new ArrayList<>();
    private final List<CanvasTile> liveStrokeTiles = new ArrayList<>();
    private final Set<CanvasTile> dirtyLiveStrokeTiles = new HashSet<>();
    private final List<InkCommand> inkCommands = new ArrayList<>();
    private final List<InkStrokeState> inkStrokes = new ArrayList<>();
    private int columns;
    private int rows;
    private Color background = Color.WHITE;
    private double contentWidth;
    private double contentHeight;
    private double contentMinX = Double.POSITIVE_INFINITY;
    private double contentMinY = Double.POSITIVE_INFINITY;
    private boolean vectorInkReliable = true;

    public TiledInkCanvasSurface() {
        getChildren().addAll(backgroundLayer, imageLayer, strokeLayer, liveStrokeLayer, inkInputLayer);
        for (Pane layer : canvasLayers()) {
            layer.setManaged(false);
        }
        getStyleClass().add("technical-problem-tiled-canvas-surface");
        setPickOnBounds(true);
        setCache(false);
        backgroundLayer.setPickOnBounds(false);
        imageLayer.setPickOnBounds(false);
        strokeLayer.setPickOnBounds(false);
        strokeLayer.setMouseTransparent(true);
        liveStrokeLayer.setPickOnBounds(false);
        liveStrokeLayer.setMouseTransparent(true);
        inkInputLayer.setPickOnBounds(true);
        inkInputLayer.setMouseTransparent(true);
        inkInputHitArea.setManaged(false);
        inkInputHitArea.setFill(Color.TRANSPARENT);
        inkInputHitArea.setStroke(null);
        inkInputHitArea.setMouseTransparent(false);
        inkInputLayer.getChildren().add(inkInputHitArea);
        ensureLogicalSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Pane imageLayer() {
        return imageLayer;
    }

    public Pane inkInputLayer() {
        return inkInputLayer;
    }

    public Rectangle inkInputTarget() {
        return inkInputHitArea;
    }

    public double logicalWidth() {
        return Math.max(CANVAS_TILE_SIZE, columns * CANVAS_TILE_SIZE);
    }

    public double logicalHeight() {
        return Math.max(CANVAS_TILE_SIZE, rows * CANVAS_TILE_SIZE);
    }

    public Color backgroundColor() {
        return background;
    }

    public void markContent(double x, double y) {
        if (Double.isFinite(x) && x > 0) {
            contentWidth = Math.max(contentWidth, x);
        }
        if (Double.isFinite(y) && y > 0) {
            contentHeight = Math.max(contentHeight, y);
        }
    }

    public void markContentBounds(double minX, double minY, double maxX, double maxY) {
        if (Double.isFinite(minX) && Double.isFinite(maxX) && maxX > minX) {
            contentMinX = Math.min(contentMinX, Math.max(0.0, minX));
            contentWidth = Math.max(contentWidth, maxX);
        }
        if (Double.isFinite(minY) && Double.isFinite(maxY) && maxY > minY) {
            contentMinY = Math.min(contentMinY, Math.max(0.0, minY));
            contentHeight = Math.max(contentHeight, maxY);
        }
    }

    public void ensureLogicalSize(double width, double height) {
        if (columns == 0 && rows == 0) {
            columns = 1;
            rows = 1;
            addTile(0, 0);
        }
        while (columns < MAX_CANVAS_COLUMNS && logicalWidth() < Math.ceil(width)) {
            addColumn();
        }
        while (rows < MAX_CANVAS_ROWS && logicalHeight() < Math.ceil(height)) {
            addRow();
        }
        syncSize();
    }

    public void growForPoint(double x, double y, double threshold) {
        boolean needsColumn = x >= logicalWidth() - threshold;
        boolean needsRow = y >= logicalHeight() - threshold;
        growByScroll(needsColumn, needsRow);
    }

    public boolean growByScroll(boolean growWidth, boolean growHeight) {
        double beforeWidth = logicalWidth();
        double beforeHeight = logicalHeight();
        double targetWidth = growWidth
                ? Math.max(beforeWidth + 1.0, beforeWidth * SCROLL_GROWTH_FACTOR)
                : beforeWidth;
        double targetHeight = growHeight
                ? Math.max(beforeHeight + 1.0, beforeHeight * SCROLL_GROWTH_FACTOR)
                : beforeHeight;
        ensureLogicalSize(targetWidth, targetHeight);
        syncSize();
        return logicalWidth() > beforeWidth || logicalHeight() > beforeHeight;
    }

    public boolean canGrowHorizontally() {
        return columns < MAX_CANVAS_COLUMNS;
    }

    public boolean canGrowVertically() {
        return rows < MAX_CANVAS_ROWS;
    }

    public void drawLine(double x1, double y1, double x2, double y2, Color color, double width) {
        if (!hasVisibleWidth(width)) {
            return;
        }
        double strokeWidth = visibleWidth(width);
        double minX = Math.min(x1, x2) - strokeWidth - 2.0;
        double minY = Math.min(y1, y2) - strokeWidth - 2.0;
        double maxX = Math.max(x1, x2) + strokeWidth + 2.0;
        double maxY = Math.max(y1, y2) + strokeWidth + 2.0;
        for (CanvasTile tile : strokeTiles) {
            if (!tile.intersects(minX, minY, maxX, maxY)) {
                continue;
            }
            GraphicsContext graphics = tile.canvas().getGraphicsContext2D();
            graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            graphics.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            graphics.setLineWidth(strokeWidth);
            graphics.setStroke(color == null ? Color.BLACK : color);
            graphics.strokeLine(x1 - tile.x(), y1 - tile.y(), x2 - tile.x(), y2 - tile.y());
            Graphics2D rasterGraphics = tile.raster().createGraphics();
            try {
                rasterGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                rasterGraphics.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                rasterGraphics.setColor(toAwtColor(color == null ? Color.BLACK : color));
                rasterGraphics.drawLine(
                        (int) Math.round(x1 - tile.x()),
                        (int) Math.round(y1 - tile.y()),
                        (int) Math.round(x2 - tile.x()),
                        (int) Math.round(y2 - tile.y()));
            } finally {
                rasterGraphics.dispose();
            }
        }
        inkCommands.add(InkCommand.line(InkCommandType.DRAW, x1, y1, x2, y2,
                0.0, 0.0, color == null ? Color.BLACK : color, strokeWidth));
        markContentBounds(minX, minY, maxX, maxY);
    }

    public void eraseLine(double x1, double y1, double x2, double y2, double width) {
        if (!hasVisibleWidth(width)) {
            return;
        }
        double strokeWidth = visibleWidth(width);
        double minX = Math.min(x1, x2) - strokeWidth - 2.0;
        double minY = Math.min(y1, y2) - strokeWidth - 2.0;
        double maxX = Math.max(x1, x2) + strokeWidth + 2.0;
        double maxY = Math.max(y1, y2) + strokeWidth + 2.0;
        for (CanvasTile tile : strokeTiles) {
            if (!tile.intersects(minX, minY, maxX, maxY)) {
                continue;
            }
            Graphics2D rasterGraphics = tile.raster().createGraphics();
            try {
                rasterGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                rasterGraphics.setComposite(AlphaComposite.Clear);
                rasterGraphics.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                rasterGraphics.drawLine(
                        (int) Math.round(x1 - tile.x()),
                        (int) Math.round(y1 - tile.y()),
                        (int) Math.round(x2 - tile.x()),
                        (int) Math.round(y2 - tile.y()));
            } finally {
                rasterGraphics.dispose();
            }
            refreshTileCanvasFromRaster(tile);
        }
        inkCommands.add(InkCommand.line(InkCommandType.ERASE, x1, y1, x2, y2,
                0.0, 0.0, Color.TRANSPARENT, strokeWidth));
        markContentBounds(minX, minY, maxX, maxY);
    }

    public void drawQuadratic(double startX, double startY, double controlX, double controlY,
                       double endX, double endY, Color color, double width) {
        if (!hasVisibleWidth(width)) {
            return;
        }
        double strokeWidth = visibleWidth(width);
        double minX = Math.min(Math.min(startX, controlX), endX) - strokeWidth - 2.0;
        double minY = Math.min(Math.min(startY, controlY), endY) - strokeWidth - 2.0;
        double maxX = Math.max(Math.max(startX, controlX), endX) + strokeWidth + 2.0;
        double maxY = Math.max(Math.max(startY, controlY), endY) + strokeWidth + 2.0;
        for (CanvasTile tile : strokeTiles) {
            if (!tile.intersects(minX, minY, maxX, maxY)) {
                continue;
            }
            GraphicsContext graphics = tile.canvas().getGraphicsContext2D();
            graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            graphics.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            graphics.setLineWidth(strokeWidth);
            graphics.setStroke(color == null ? Color.BLACK : color);
            graphics.beginPath();
            graphics.moveTo(startX - tile.x(), startY - tile.y());
            graphics.quadraticCurveTo(
                    controlX - tile.x(),
                    controlY - tile.y(),
                    endX - tile.x(),
                    endY - tile.y());
            graphics.stroke();

            Graphics2D rasterGraphics = tile.raster().createGraphics();
            try {
                rasterGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                rasterGraphics.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                rasterGraphics.setColor(toAwtColor(color == null ? Color.BLACK : color));
                rasterGraphics.draw(new QuadCurve2D.Double(
                        startX - tile.x(),
                        startY - tile.y(),
                        controlX - tile.x(),
                        controlY - tile.y(),
                        endX - tile.x(),
                        endY - tile.y()));
            } finally {
                rasterGraphics.dispose();
            }
        }
        inkCommands.add(InkCommand.quadratic(InkCommandType.DRAW, startX, startY, controlX, controlY,
                endX, endY, color == null ? Color.BLACK : color, strokeWidth));
        markContentBounds(minX, minY, maxX, maxY);
    }

    public void eraseQuadratic(double startX, double startY, double controlX, double controlY,
                        double endX, double endY, double width) {
        if (!hasVisibleWidth(width)) {
            return;
        }
        double strokeWidth = visibleWidth(width);
        double minX = Math.min(Math.min(startX, controlX), endX) - strokeWidth - 2.0;
        double minY = Math.min(Math.min(startY, controlY), endY) - strokeWidth - 2.0;
        double maxX = Math.max(Math.max(startX, controlX), endX) + strokeWidth + 2.0;
        double maxY = Math.max(Math.max(startY, controlY), endY) + strokeWidth + 2.0;
        for (CanvasTile tile : strokeTiles) {
            if (!tile.intersects(minX, minY, maxX, maxY)) {
                continue;
            }
            Graphics2D rasterGraphics = tile.raster().createGraphics();
            try {
                rasterGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                rasterGraphics.setComposite(AlphaComposite.Clear);
                rasterGraphics.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                rasterGraphics.draw(new QuadCurve2D.Double(
                        startX - tile.x(),
                        startY - tile.y(),
                        controlX - tile.x(),
                        controlY - tile.y(),
                        endX - tile.x(),
                        endY - tile.y()));
            } finally {
                rasterGraphics.dispose();
            }
            refreshTileCanvasFromRaster(tile);
        }
        inkCommands.add(InkCommand.quadratic(InkCommandType.ERASE, startX, startY, controlX, controlY,
                endX, endY, Color.TRANSPARENT, strokeWidth));
        markContentBounds(minX, minY, maxX, maxY);
    }

    public void beginLiveStroke() {
        clearLiveStroke();
    }

    public void clearLiveStroke() {
        if (dirtyLiveStrokeTiles.isEmpty()) {
            return;
        }
        for (CanvasTile tile : dirtyLiveStrokeTiles) {
            tile.canvas().getGraphicsContext2D().clearRect(0, 0, CANVAS_TILE_SIZE, CANVAS_TILE_SIZE);
            clearRaster(tile.raster());
        }
        dirtyLiveStrokeTiles.clear();
    }

    public void previewLine(double x1, double y1, double x2, double y2, Color color, double width, boolean erase) {
        if (erase) {
            drawFxLine(strokeTiles, x1, y1, x2, y2, Color.TRANSPARENT, width, true);
        } else {
            drawFxLine(liveStrokeTiles, x1, y1, x2, y2, color, width, false);
        }
    }

    @Override
    protected void layoutChildren() {
        double width = logicalWidth();
        double height = logicalHeight();
        for (Pane pane : canvasLayers()) {
            pane.resizeRelocate(0, 0, width, height);
        }
        resizeInkInputHitArea(width, height);
    }

    public void previewQuadratic(double startX, double startY, double controlX, double controlY,
                          double endX, double endY, Color color, double width, boolean erase) {
        if (erase) {
            drawFxQuadratic(strokeTiles, startX, startY, controlX, controlY, endX, endY,
                    Color.TRANSPARENT, width, true);
        } else {
            drawFxQuadratic(liveStrokeTiles, startX, startY, controlX, controlY, endX, endY,
                    color, width, false);
        }
    }

    public void commitInkStroke(InkStrokeState stroke) {
        if (stroke == null || stroke.points().isEmpty()) {
            clearLiveStroke();
            return;
        }
        InkCommandType type = InkCommandType.fromToken(stroke.type());
        if (type == InkCommandType.ERASE_REGION) {
            clearLiveStroke();
            return;
        }
        InkStrokeState safeStroke = new InkStrokeState(
                type.name(),
                type == InkCommandType.ERASE ? "#00000000" : toCssColor(parseColor(stroke.color())),
                Math.max(1.0, stroke.width()),
                stroke.points());
        List<InkCommand> commands = commandsForStroke(safeStroke);
        renderCommandsToFx(commands);
        inkStrokes.add(safeStroke);
        inkCommands.addAll(commands);
        for (InkCommand command : commands) {
            markCommandBounds(command);
        }
        clearLiveStroke();
        vectorInkReliable = true;
    }

    public void clearStrokes() {
        for (CanvasTile tile : strokeTiles) {
            tile.canvas().getGraphicsContext2D().clearRect(0, 0, CANVAS_TILE_SIZE, CANVAS_TILE_SIZE);
            clearRaster(tile.raster());
        }
        clearLiveStroke();
        contentWidth = 0;
        contentHeight = 0;
        contentMinX = Double.POSITIVE_INFINITY;
        contentMinY = Double.POSITIVE_INFINITY;
        inkCommands.clear();
        inkStrokes.clear();
        vectorInkReliable = true;
    }

    public void resetForEditableState(double width, double height, Color color) {
        backgroundTiles.clear();
        strokeTiles.clear();
        liveStrokeTiles.clear();
        dirtyLiveStrokeTiles.clear();
        backgroundLayer.getChildren().clear();
        strokeLayer.getChildren().clear();
        liveStrokeLayer.getChildren().clear();
        columns = 0;
        rows = 0;
        contentWidth = 0;
        contentHeight = 0;
        contentMinX = Double.POSITIVE_INFINITY;
        contentMinY = Double.POSITIVE_INFINITY;
        inkCommands.clear();
        inkStrokes.clear();
        vectorInkReliable = true;
        background = color == null ? Color.WHITE : color;
        ensureLogicalSize(Math.max(DEFAULT_WIDTH, width), Math.max(DEFAULT_HEIGHT, height));
        fillBackground(background);
    }

    public void fillBackground(Color color) {
        background = color == null ? Color.WHITE : color;
        for (CanvasTile tile : backgroundTiles) {
            fillTile(tile, background);
        }
    }

    public void drawSnapshot(WritableImage image) {
        clearStrokes();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return;
        }
        ensureLogicalSize(image.getWidth(), image.getHeight());
        drawImageIntoTiles(image, strokeTiles);
        markContentBounds(0, 0, image.getWidth(), image.getHeight());
        vectorInkReliable = false;
    }

    public void drawBackgroundImage(WritableImage image, Color fallbackBackground) {
        fillBackground(fallbackBackground == null ? background : fallbackBackground);
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return;
        }
        ensureLogicalSize(image.getWidth(), image.getHeight());
        drawImageIntoTiles(image, backgroundTiles);
        markContentBounds(0, 0, image.getWidth(), image.getHeight());
    }

    public WritableImage snapshotDrawing() {
        SnapshotSize size = snapshotSize(List.of(), InkCanvasExportOptions.undoSnapshot());
        WritableImage output = new WritableImage(size.width(), size.height());
        copyInk(output, size.scale(), size.originX(), size.originY());
        return output;
    }

    public WritableImage snapshotWithImages(List<ImageView> canvasImages) {
        return exportWithImages(canvasImages, InkCanvasExportOptions.premiumExternal()).image();
    }

    public InkCanvasExportResult exportWithImages(List<ImageView> canvasImages,
                                                   InkCanvasExportOptions options) {
        InkCanvasExportOptions safeOptions = options == null
                ? InkCanvasExportOptions.internalPersistence()
                : options;
        SnapshotSize size = snapshotSize(canvasImages, safeOptions);
        WritableImage output = new WritableImage(size.width(), size.height());
        fillImage(output, background);
        copyBackgroundTiles(output, size.scale(), size.originX(), size.originY());
        copyImages(canvasImages, output, size.scale(), size.originX(), size.originY());
        copyInk(output, size);
        return new InkCanvasExportResult(
                output,
                size.scale(),
                size.cropped(),
                (int) Math.ceil(size.logicalWidth()),
                (int) Math.ceil(size.logicalHeight()),
                size.warnings());
    }

    public WritableImage snapshotRegion(List<ImageView> canvasImages, double x, double y, double width, double height) {
        double safeX = Math.max(0.0, Math.min(logicalWidth(), x));
        double safeY = Math.max(0.0, Math.min(logicalHeight(), y));
        double safeWidth = Math.max(1.0, Math.min(logicalWidth() - safeX, width));
        double safeHeight = Math.max(1.0, Math.min(logicalHeight() - safeY, height));
        WritableImage output = new WritableImage(
                Math.max(1, (int) Math.ceil(safeWidth)),
                Math.max(1, (int) Math.ceil(safeHeight)));
        copyImages(canvasImages, output, 1, safeX, safeY);
        copyInk(output, 1, safeX, safeY);
        return output;
    }

    public void eraseRegion(double x, double y, double width, double height) {
        double minX = Math.max(0.0, Math.min(logicalWidth(), x));
        double minY = Math.max(0.0, Math.min(logicalHeight(), y));
        double maxX = Math.max(minX, Math.min(logicalWidth(), x + width));
        double maxY = Math.max(minY, Math.min(logicalHeight(), y + height));
        if (maxX <= minX || maxY <= minY) {
            return;
        }
        for (CanvasTile tile : strokeTiles) {
            if (!tile.intersects(minX, minY, maxX, maxY)) {
                continue;
            }
            double localX = Math.max(0.0, minX - tile.x());
            double localY = Math.max(0.0, minY - tile.y());
            double localMaxX = Math.min(CANVAS_TILE_SIZE, maxX - tile.x());
            double localMaxY = Math.min(CANVAS_TILE_SIZE, maxY - tile.y());
            double localWidth = localMaxX - localX;
            double localHeight = localMaxY - localY;
            if (localWidth <= 0 || localHeight <= 0) {
                continue;
            }
            GraphicsContext graphics = tile.canvas().getGraphicsContext2D();
            graphics.clearRect(localX, localY, localWidth, localHeight);
            Graphics2D rasterGraphics = tile.raster().createGraphics();
            try {
                rasterGraphics.setComposite(AlphaComposite.Clear);
                rasterGraphics.fillRect(
                        (int) Math.floor(localX),
                        (int) Math.floor(localY),
                        (int) Math.ceil(localWidth),
                        (int) Math.ceil(localHeight));
            } finally {
                rasterGraphics.dispose();
            }
            refreshTileCanvasFromRaster(tile);
        }
        inkCommands.add(InkCommand.regionErase(minX, minY, maxX - minX, maxY - minY));
    }

    public List<InkCommandState> inkCommandStates() {
        return inkCommands.stream()
                .map(InkCommand::state)
                .toList();
    }

    public List<InkStrokeState> inkStrokeStates() {
        return List.copyOf(inkStrokes);
    }

    public boolean vectorInkReliable() {
        return vectorInkReliable;
    }

    public List<InkStroke> applicationInkStrokes() {
        return inkStrokes.stream()
                .map(state -> new InkStroke(
                        InkTool.fromToken(state.type()),
                        state.color(),
                        state.width(),
                        state.points().stream()
                                .map(point -> InkPoint.of(point.x(), point.y(), point.nanos(), point.pressure()))
                                .toList()))
                .toList();
    }

    public void restoreApplicationInkStrokes(List<InkStroke> strokes) {
        if (strokes == null || strokes.isEmpty()) {
            return;
        }
        restoreInkStrokeStates(strokes.stream()
                .map(stroke -> new InkStrokeState(
                        stroke.tool().name(),
                        stroke.color(),
                        stroke.width(),
                        stroke.points().stream()
                                .map(point -> new InkPointState(point.x(), point.y(), point.nanos(), point.pressure()))
                                .toList()))
                .toList());
    }

    public void restoreInkStrokeStates(List<InkStrokeState> states) {
        if (states == null || states.isEmpty()) {
            return;
        }
        for (InkStrokeState state : states) {
            commitInkStroke(state);
        }
    }

    public void restoreInkCommandStates(List<InkCommandState> states) {
        if (states == null || states.isEmpty()) {
            return;
        }
        for (InkCommandState state : states) {
            if (state == null) {
                continue;
            }
            InkCommandType type = InkCommandType.fromToken(state.type());
            if (type == InkCommandType.ERASE_REGION) {
                eraseRegion(state.x1(), state.y1(), state.x2(), state.y2());
            } else if (state.quadratic()) {
                if (type == InkCommandType.ERASE) {
                    eraseQuadratic(state.x1(), state.y1(), state.controlX(), state.controlY(),
                            state.x2(), state.y2(), state.width());
                } else {
                    drawQuadratic(state.x1(), state.y1(), state.controlX(), state.controlY(),
                            state.x2(), state.y2(), parseColor(state.color()), state.width());
                }
            } else if (type == InkCommandType.ERASE) {
                eraseLine(state.x1(), state.y1(), state.x2(), state.y2(), state.width());
            } else {
                drawLine(state.x1(), state.y1(), state.x2(), state.y2(), parseColor(state.color()), state.width());
            }
        }
    }

    public void restoreInkUndoState(List<InkStrokeState> strokeStates, List<InkCommandState> commandStates) {
        clearStrokes();
        List<InkStrokeState> safeStrokes = strokeStates == null ? List.of() : List.copyOf(strokeStates);
        List<InkCommandState> safeCommands = commandStates == null ? List.of() : List.copyOf(commandStates);
        if (!safeCommands.isEmpty()) {
            restoreInkCommandStates(safeCommands);
            inkStrokes.clear();
            inkStrokes.addAll(safeStrokes);
            vectorInkReliable = true;
            return;
        }
        restoreInkStrokeStates(safeStrokes);
        vectorInkReliable = true;
    }

    private void addColumn() {
        int column = columns;
        columns++;
        if (rows == 0) {
            rows = 1;
        }
        for (int row = 0; row < rows; row++) {
            addTile(column, row);
        }
    }

    private void addRow() {
        int row = rows;
        rows++;
        if (columns == 0) {
            columns = 1;
        }
        for (int column = 0; column < columns; column++) {
            addTile(column, row);
        }
    }

    private void addTile(int column, int row) {
        double x = column * CANVAS_TILE_SIZE;
        double y = row * CANVAS_TILE_SIZE;
        Canvas backgroundCanvas = tileCanvas(x, y);
        Canvas strokeCanvas = tileCanvas(x, y);
        Canvas liveStrokeCanvas = tileCanvas(x, y);
        CanvasTile backgroundTile = new CanvasTile(backgroundCanvas, transparentRaster(), x, y);
        CanvasTile strokeTile = new CanvasTile(strokeCanvas, transparentRaster(), x, y);
        CanvasTile liveStrokeTile = new CanvasTile(liveStrokeCanvas, transparentRaster(), x, y);
        fillTile(backgroundTile, background);
        backgroundTiles.add(backgroundTile);
        strokeTiles.add(strokeTile);
        liveStrokeTiles.add(liveStrokeTile);
        backgroundLayer.getChildren().add(backgroundCanvas);
        strokeLayer.getChildren().add(strokeCanvas);
        liveStrokeLayer.getChildren().add(liveStrokeCanvas);
    }

    private static Canvas tileCanvas(double x, double y) {
        Canvas canvas = new Canvas(CANVAS_TILE_SIZE, CANVAS_TILE_SIZE);
        canvas.setLayoutX(x);
        canvas.setLayoutY(y);
        canvas.setCache(false);
        return canvas;
    }

    private SnapshotSize snapshotSize(List<ImageView> canvasImages, InkCanvasExportOptions options) {
        double minX = hasContent() ? contentMinX : 0.0;
        double minY = hasContent() ? contentMinY : 0.0;
        double maxX = hasContent() ? contentWidth : 0.0;
        double maxY = hasContent() ? contentHeight : 0.0;
        for (ImageView view : canvasImages == null ? List.<ImageView>of() : canvasImages) {
            ImageBounds bounds = imageBounds(view);
            if (bounds == null) {
                continue;
            }
            minX = Math.min(minX, bounds.minX());
            minY = Math.min(minY, bounds.minY());
            maxX = Math.max(maxX, bounds.maxX());
            maxY = Math.max(maxY, bounds.maxY());
        }
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || maxX <= minX || maxY <= minY) {
            minX = 0.0;
            minY = 0.0;
            maxX = Math.max(options.minWidth(), MIN_EXPORT_WIDTH);
            maxY = Math.max(options.minHeight(), MIN_EXPORT_HEIGHT);
        }
        double originX = options.cropToContent() ? Math.max(0.0, minX - options.margin()) : 0.0;
        double originY = options.cropToContent() ? Math.max(0.0, minY - options.margin()) : 0.0;
        double requiredWidth;
        double requiredHeight;
        if (options.fullLogicalCanvas()) {
            requiredWidth = logicalWidth();
            requiredHeight = logicalHeight();
        } else if (options.cropToContent()) {
            requiredWidth = maxX - originX + options.margin();
            requiredHeight = maxY - originY + options.margin();
        } else {
            requiredWidth = Math.max(maxX + options.margin(), options.minWidth());
            requiredHeight = Math.max(maxY + options.margin(), options.minHeight());
        }
        requiredWidth = Math.max(options.minWidth(), requiredWidth);
        requiredHeight = Math.max(options.minHeight(), requiredHeight);
        if (options.fullLogicalCanvas()) {
            requiredWidth = Math.min(logicalWidth(), requiredWidth);
            requiredHeight = Math.min(logicalHeight(), requiredHeight);
        } else if (!options.cropToContent()) {
            requiredWidth = Math.min(logicalWidth(), Math.max(requiredWidth, contentWidth + EXPORT_MARGIN));
            requiredHeight = Math.min(logicalHeight(), Math.max(requiredHeight, contentHeight + EXPORT_MARGIN));
        } else {
            requiredWidth = Math.min(logicalWidth() - originX, requiredWidth);
            requiredHeight = Math.min(logicalHeight() - originY, requiredHeight);
        }
        int requestedScale = Math.max(1, options.preferredScale());
        int scale = requestedScale;
        int width = Math.max(1, (int) Math.ceil(requiredWidth * scale));
        int height = Math.max(1, (int) Math.ceil(requiredHeight * scale));
        if (scale > 2 && (long) width * (long) height > options.maxPixelCount()) {
            scale = 2;
            width = Math.max(1, (int) Math.ceil(requiredWidth * scale));
            height = Math.max(1, (int) Math.ceil(requiredHeight * scale));
        }
        while (scale > 1 && (long) width * (long) height > options.maxPixelCount()) {
            scale--;
            width = Math.max(1, (int) Math.ceil(requiredWidth * scale));
            height = Math.max(1, (int) Math.ceil(requiredHeight * scale));
        }
        List<String> warnings = scale < requestedScale
                ? List.of("La exportacion bajo de " + requestedScale + "x a " + scale + "x para respetar el limite seguro de pixeles.")
                : List.of();
        return new SnapshotSize(width, height, scale, originX, originY, requiredWidth, requiredHeight,
                options.cropToContent(), warnings);
    }

    private boolean hasContent() {
        return contentWidth > 0.0 && contentHeight > 0.0
                && Double.isFinite(contentMinX) && Double.isFinite(contentMinY);
    }

    private void copyImages(List<ImageView> canvasImages, WritableImage output, int scale,
                            double originX, double originY) {
        for (ImageView view : canvasImages == null ? List.<ImageView>of() : canvasImages) {
            ImageBounds bounds = imageBounds(view);
            if (bounds == null) {
                continue;
            }
            copyScaledImage(view.getImage(), output,
                    (int) Math.round((bounds.minX() - originX) * scale),
                    (int) Math.round((bounds.minY() - originY) * scale),
                    Math.max(1, (int) Math.round(bounds.width() * scale)),
                    Math.max(1, (int) Math.round(bounds.height() * scale)));
        }
    }

    private static ImageBounds imageBounds(ImageView view) {
        if (view == null || view.getImage() == null) {
            return null;
        }
        double width = view.getFitWidth() > 0.0 ? view.getFitWidth() : view.getBoundsInParent().getWidth();
        double height = view.getFitHeight() > 0.0 ? view.getFitHeight() : view.getBoundsInParent().getHeight();
        if ((height <= 0.0 || !Double.isFinite(height)) && view.isPreserveRatio() && view.getImage().getWidth() > 0.0) {
            height = width * view.getImage().getHeight() / view.getImage().getWidth();
        }
        if ((width <= 0.0 || height <= 0.0)
                && view.getImage().getWidth() > 0.0 && view.getImage().getHeight() > 0.0) {
            width = view.getImage().getWidth();
            height = view.getImage().getHeight();
        }
        if (width <= 0.0 || height <= 0.0 || !Double.isFinite(width) || !Double.isFinite(height)) {
            return null;
        }
        return new ImageBounds(view.getLayoutX(), view.getLayoutY(), width, height);
    }

    private void copyStrokeTiles(WritableImage output, int scale, double originX, double originY) {
        for (CanvasTile tile : strokeTiles) {
            copyScaledImage(tile.raster(), output,
                    (int) Math.round((tile.x() - originX) * scale),
                    (int) Math.round((tile.y() - originY) * scale),
                    Math.max(1, (int) Math.round(CANVAS_TILE_SIZE * scale)),
                    Math.max(1, (int) Math.round(CANVAS_TILE_SIZE * scale)));
        }
    }

    private void copyInk(WritableImage output, int scale, double originX, double originY) {
        if (!vectorInkReliable) {
            copyStrokeTiles(output, scale, originX, originY);
            return;
        }
        BufferedImage ink = renderVectorInk(
                Math.max(1, (int) Math.ceil(output.getWidth())),
                Math.max(1, (int) Math.ceil(output.getHeight())),
                scale,
                originX,
                originY);
        copyScaledImage(ink, output, 0, 0, ink.getWidth(), ink.getHeight());
    }

    private void copyInk(WritableImage output, SnapshotSize size) {
        if (!vectorInkReliable) {
            copyStrokeTiles(output, size.scale(), size.originX(), size.originY());
            return;
        }
        copyInk(output, size.scale(), size.originX(), size.originY());
    }

    private BufferedImage renderVectorInk(int width, int height, int scale, double originX, double originY) {
        BufferedImage ink = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = ink.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            for (InkCommand command : inkCommands) {
                command.render(graphics, scale, originX, originY);
            }
        } finally {
            graphics.dispose();
        }
        return ink;
    }

    private void copyBackgroundTiles(WritableImage output, int scale, double originX, double originY) {
        for (CanvasTile tile : backgroundTiles) {
            copyScaledImage(tile.raster(), output,
                    (int) Math.round((tile.x() - originX) * scale),
                    (int) Math.round((tile.y() - originY) * scale),
                    Math.max(1, (int) Math.round(CANVAS_TILE_SIZE * scale)),
                    Math.max(1, (int) Math.round(CANVAS_TILE_SIZE * scale)));
        }
    }

    private void drawImageIntoTiles(WritableImage image, List<CanvasTile> targetTiles) {
        for (CanvasTile tile : targetTiles) {
            double sx = tile.x();
            double sy = tile.y();
            double sw = Math.min(CANVAS_TILE_SIZE, image.getWidth() - sx);
            double sh = Math.min(CANVAS_TILE_SIZE, image.getHeight() - sy);
            if (sw <= 0 || sh <= 0) {
                continue;
            }
            tile.canvas().getGraphicsContext2D().drawImage(image, sx, sy, sw, sh, 0, 0, sw, sh);
            copyImageToRaster(image, tile.raster(), sx, sy, sw, sh);
        }
    }

    private void syncSize() {
        double width = logicalWidth();
        double height = logicalHeight();
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        resize(width, height);
        for (Pane pane : canvasLayers()) {
            pane.setMinSize(width, height);
            pane.setPrefSize(width, height);
            pane.setMaxSize(width, height);
            pane.resizeRelocate(0, 0, width, height);
        }
        resizeInkInputHitArea(width, height);
    }

    private void resizeInkInputHitArea(double width, double height) {
        inkInputHitArea.setX(0);
        inkInputHitArea.setY(0);
        inkInputHitArea.setWidth(width);
        inkInputHitArea.setHeight(height);
        inkInputHitArea.toBack();
    }

    private List<Pane> canvasLayers() {
        return List.of(backgroundLayer, imageLayer, strokeLayer, liveStrokeLayer, inkInputLayer);
    }

    private void renderCommandsToFx(List<InkCommand> commands) {
        for (InkCommand command : commands) {
            if (command.type() == InkCommandType.ERASE_REGION) {
                continue;
            }
            if (command.quadratic()) {
                drawFxQuadratic(strokeTiles, command.x1(), command.y1(), command.controlX(), command.controlY(),
                        command.x2(), command.y2(), command.color(), command.width(),
                        command.type() == InkCommandType.ERASE);
            } else {
                drawFxLine(strokeTiles, command.x1(), command.y1(), command.x2(), command.y2(),
                        command.color(), command.width(), command.type() == InkCommandType.ERASE);
            }
        }
    }

    private List<InkCommand> commandsForStroke(InkStrokeState stroke) {
        List<InkPointState> points = stroke.points();
        if (points.isEmpty()) {
            return List.of();
        }
        InkCommandType type = InkCommandType.fromToken(stroke.type());
        Color color = type == InkCommandType.ERASE ? Color.TRANSPARENT : parseColor(stroke.color());
        double width = InkBrushMath.safeMaxWidth(stroke.width());
        List<InkCommand> commands = new ArrayList<>();
        if (points.size() == 1) {
            InkPointState point = points.get(0);
            double pointWidth = widthForPoint(width, point);
            if (pointWidth > 0.0) {
                commands.add(InkCommand.line(type, point.x(), point.y(), point.x() + 0.01, point.y() + 0.01,
                        0.0, 0.0, color, pointWidth));
            }
            return commands;
        }
        double lastX = points.get(0).x();
        double lastY = points.get(0).y();
        for (int i = 1; i < points.size(); i++) {
            InkPointState point = points.get(i);
            double commandWidth = widthForPoint(width, point);
            if (commandWidth > 0.0) {
                commands.add(InkCommand.line(type, lastX, lastY, point.x(), point.y(),
                        0.0, 0.0, color, commandWidth));
            }
            lastX = point.x();
            lastY = point.y();
        }
        return commands;
    }

    private static double widthForPoint(double maxWidth, InkPointState point) {
        return InkBrushMath.pressureWidth(maxWidth, point == null ? 1.0 : point.pressure());
    }

    private void drawFxLine(List<CanvasTile> targetTiles, double x1, double y1, double x2, double y2,
                            Color color, double width, boolean erase) {
        if (!hasVisibleWidth(width)) {
            return;
        }
        double strokeWidth = visibleWidth(width);
        double minX = Math.min(x1, x2) - strokeWidth - 2.0;
        double minY = Math.min(y1, y2) - strokeWidth - 2.0;
        double maxX = Math.max(x1, x2) + strokeWidth + 2.0;
        double maxY = Math.max(y1, y2) + strokeWidth + 2.0;
        for (CanvasTile tile : targetTiles) {
            if (!tile.intersects(minX, minY, maxX, maxY)) {
                continue;
            }
            if (targetTiles == liveStrokeTiles) {
                dirtyLiveStrokeTiles.add(tile);
            }
            GraphicsContext graphics = tile.canvas().getGraphicsContext2D();
            graphics.save();
            try {
                if (erase) {
                    graphics.clearRect(
                            minX - tile.x(),
                            minY - tile.y(),
                            maxX - minX,
                            maxY - minY);
                    continue;
                }
                graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                graphics.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                graphics.setLineWidth(strokeWidth);
                graphics.setStroke(color == null ? Color.BLACK : color);
                graphics.strokeLine(x1 - tile.x(), y1 - tile.y(), x2 - tile.x(), y2 - tile.y());
            } finally {
                graphics.restore();
            }
        }
    }

    private void drawFxQuadratic(List<CanvasTile> targetTiles, double startX, double startY,
                                 double controlX, double controlY, double endX, double endY,
                                 Color color, double width, boolean erase) {
        if (!hasVisibleWidth(width)) {
            return;
        }
        double strokeWidth = visibleWidth(width);
        double minX = Math.min(Math.min(startX, controlX), endX) - strokeWidth - 2.0;
        double minY = Math.min(Math.min(startY, controlY), endY) - strokeWidth - 2.0;
        double maxX = Math.max(Math.max(startX, controlX), endX) + strokeWidth + 2.0;
        double maxY = Math.max(Math.max(startY, controlY), endY) + strokeWidth + 2.0;
        for (CanvasTile tile : targetTiles) {
            if (!tile.intersects(minX, minY, maxX, maxY)) {
                continue;
            }
            if (targetTiles == liveStrokeTiles) {
                dirtyLiveStrokeTiles.add(tile);
            }
            GraphicsContext graphics = tile.canvas().getGraphicsContext2D();
            graphics.save();
            try {
                if (erase) {
                    graphics.clearRect(
                            minX - tile.x(),
                            minY - tile.y(),
                            maxX - minX,
                            maxY - minY);
                    continue;
                }
                graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                graphics.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                graphics.setLineWidth(strokeWidth);
                graphics.setStroke(color == null ? Color.BLACK : color);
                graphics.beginPath();
                graphics.moveTo(startX - tile.x(), startY - tile.y());
                graphics.quadraticCurveTo(
                        controlX - tile.x(),
                        controlY - tile.y(),
                        endX - tile.x(),
                        endY - tile.y());
                graphics.stroke();
            } finally {
                graphics.restore();
            }
        }
    }

    private void markCommandBounds(InkCommand command) {
        if (command == null || command.type() != InkCommandType.DRAW) {
            return;
        }
        double strokeWidth = visibleWidth(command.width());
        double minX = command.quadratic()
                ? Math.min(Math.min(command.x1(), command.controlX()), command.x2())
                : Math.min(command.x1(), command.x2());
        double minY = command.quadratic()
                ? Math.min(Math.min(command.y1(), command.controlY()), command.y2())
                : Math.min(command.y1(), command.y2());
        double maxX = command.quadratic()
                ? Math.max(Math.max(command.x1(), command.controlX()), command.x2())
                : Math.max(command.x1(), command.x2());
        double maxY = command.quadratic()
                ? Math.max(Math.max(command.y1(), command.controlY()), command.y2())
                : Math.max(command.y1(), command.y2());
        markContentBounds(minX - strokeWidth - 2.0, minY - strokeWidth - 2.0,
                maxX + strokeWidth + 2.0, maxY + strokeWidth + 2.0);
    }

    private static double visibleWidth(double width) {
        return Double.isFinite(width) ? Math.max(0.0, width) : 1.0;
    }

    private static boolean hasVisibleWidth(double width) {
        return Double.isFinite(width) && width > 0.0;
    }

    private static void fillTile(CanvasTile tile, Color color) {
        GraphicsContext graphics = tile.canvas().getGraphicsContext2D();
        graphics.setFill(color == null ? Color.WHITE : color);
        graphics.fillRect(0, 0, CANVAS_TILE_SIZE, CANVAS_TILE_SIZE);
        Graphics2D rasterGraphics = tile.raster().createGraphics();
        try {
            rasterGraphics.setColor(toAwtColor(color == null ? Color.WHITE : color));
            rasterGraphics.fillRect(0, 0, (int) CANVAS_TILE_SIZE, (int) CANVAS_TILE_SIZE);
        } finally {
            rasterGraphics.dispose();
        }
    }

    private static void fillImage(WritableImage target, Color color) {
        if (target == null || target.getPixelWriter() == null) {
            return;
        }
        int argb = toArgb(color == null ? Color.WHITE : color);
        PixelWriter writer = target.getPixelWriter();
        int width = Math.max(1, (int) Math.ceil(target.getWidth()));
        int height = Math.max(1, (int) Math.ceil(target.getHeight()));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setArgb(x, y, argb);
            }
        }
    }

    private static void copyScaledImage(Image source, WritableImage target,
                                        int targetX, int targetY, int targetWidth, int targetHeight) {
        if (source == null || target == null || source.getPixelReader() == null || target.getPixelWriter() == null
                || targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        PixelReader reader = source.getPixelReader();
        PixelReader targetReader = target.getPixelReader();
        PixelWriter writer = target.getPixelWriter();
        int sourceWidth = Math.max(1, (int) Math.ceil(source.getWidth()));
        int sourceHeight = Math.max(1, (int) Math.ceil(source.getHeight()));
        int outputWidth = Math.max(1, (int) Math.ceil(target.getWidth()));
        int outputHeight = Math.max(1, (int) Math.ceil(target.getHeight()));
        int startX = Math.max(0, -targetX);
        int startY = Math.max(0, -targetY);
        int endX = Math.min(targetWidth, outputWidth - targetX);
        int endY = Math.min(targetHeight, outputHeight - targetY);
        if (startX >= endX || startY >= endY) {
            return;
        }
        for (int y = startY; y < endY; y++) {
            int dy = targetY + y;
            double sy = sourceCoordinate(y, sourceHeight, targetHeight);
            for (int x = startX; x < endX; x++) {
                int dx = targetX + x;
                double sx = sourceCoordinate(x, sourceWidth, targetWidth);
                writer.setArgb(dx, dy, blend(targetReader.getArgb(dx, dy),
                        sampleBilinear(reader, sourceWidth, sourceHeight, sx, sy)));
            }
        }
    }

    private static void copyScaledImage(BufferedImage source, WritableImage target,
                                        int targetX, int targetY, int targetWidth, int targetHeight) {
        if (source == null || target == null || target.getPixelWriter() == null
                || targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        PixelReader targetReader = target.getPixelReader();
        PixelWriter writer = target.getPixelWriter();
        int sourceWidth = Math.max(1, source.getWidth());
        int sourceHeight = Math.max(1, source.getHeight());
        int outputWidth = Math.max(1, (int) Math.ceil(target.getWidth()));
        int outputHeight = Math.max(1, (int) Math.ceil(target.getHeight()));
        int startX = Math.max(0, -targetX);
        int startY = Math.max(0, -targetY);
        int endX = Math.min(targetWidth, outputWidth - targetX);
        int endY = Math.min(targetHeight, outputHeight - targetY);
        if (startX >= endX || startY >= endY) {
            return;
        }
        for (int y = startY; y < endY; y++) {
            int dy = targetY + y;
            double sy = sourceCoordinate(y, sourceHeight, targetHeight);
            for (int x = startX; x < endX; x++) {
                int dx = targetX + x;
                double sx = sourceCoordinate(x, sourceWidth, targetWidth);
                writer.setArgb(dx, dy, blend(targetReader.getArgb(dx, dy),
                        sampleBilinear(source, sx, sy)));
            }
        }
    }

    private static double sourceCoordinate(int targetOffset, int sourceSize, int targetSize) {
        if (targetSize <= 1 || sourceSize <= 1) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(sourceSize - 1.0,
                ((targetOffset + 0.5) * sourceSize / Math.max(1.0, targetSize)) - 0.5));
    }

    private static int sampleBilinear(PixelReader reader, int width, int height, double x, double y) {
        int x0 = Math.max(0, Math.min(width - 1, (int) Math.floor(x)));
        int y0 = Math.max(0, Math.min(height - 1, (int) Math.floor(y)));
        int x1 = Math.max(0, Math.min(width - 1, x0 + 1));
        int y1 = Math.max(0, Math.min(height - 1, y0 + 1));
        double fx = x - x0;
        double fy = y - y0;
        return bilinearArgb(
                reader.getArgb(x0, y0),
                reader.getArgb(x1, y0),
                reader.getArgb(x0, y1),
                reader.getArgb(x1, y1),
                fx,
                fy);
    }

    private static int sampleBilinear(BufferedImage source, double x, double y) {
        int width = source.getWidth();
        int height = source.getHeight();
        int x0 = Math.max(0, Math.min(width - 1, (int) Math.floor(x)));
        int y0 = Math.max(0, Math.min(height - 1, (int) Math.floor(y)));
        int x1 = Math.max(0, Math.min(width - 1, x0 + 1));
        int y1 = Math.max(0, Math.min(height - 1, y0 + 1));
        double fx = x - x0;
        double fy = y - y0;
        return bilinearArgb(
                source.getRGB(x0, y0),
                source.getRGB(x1, y0),
                source.getRGB(x0, y1),
                source.getRGB(x1, y1),
                fx,
                fy);
    }

    private static int bilinearArgb(int c00, int c10, int c01, int c11, double fx, double fy) {
        int a = bilinearChannel(c00 >>> 24, c10 >>> 24, c01 >>> 24, c11 >>> 24, fx, fy);
        int r = bilinearChannel((c00 >>> 16) & 0xff, (c10 >>> 16) & 0xff,
                (c01 >>> 16) & 0xff, (c11 >>> 16) & 0xff, fx, fy);
        int g = bilinearChannel((c00 >>> 8) & 0xff, (c10 >>> 8) & 0xff,
                (c01 >>> 8) & 0xff, (c11 >>> 8) & 0xff, fx, fy);
        int b = bilinearChannel(c00 & 0xff, c10 & 0xff, c01 & 0xff, c11 & 0xff, fx, fy);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int bilinearChannel(int c00, int c10, int c01, int c11, double fx, double fy) {
        double top = c00 + (c10 - c00) * fx;
        double bottom = c01 + (c11 - c01) * fx;
        return Math.max(0, Math.min(255, (int) Math.round(top + (bottom - top) * fy)));
    }

    private static void copyImageToRaster(WritableImage source, BufferedImage target,
                                          double sourceX, double sourceY, double sourceWidth, double sourceHeight) {
        if (source == null || target == null || source.getPixelReader() == null) {
            return;
        }
        PixelReader reader = source.getPixelReader();
        int width = Math.max(0, Math.min(target.getWidth(), (int) Math.ceil(sourceWidth)));
        int height = Math.max(0, Math.min(target.getHeight(), (int) Math.ceil(sourceHeight)));
        for (int y = 0; y < height; y++) {
            int sy = Math.min((int) source.getHeight() - 1, Math.max(0, (int) Math.floor(sourceY + y)));
            for (int x = 0; x < width; x++) {
                int sx = Math.min((int) source.getWidth() - 1, Math.max(0, (int) Math.floor(sourceX + x)));
                target.setRGB(x, y, blend(target.getRGB(x, y), reader.getArgb(sx, sy)));
            }
        }
    }

    private static BufferedImage transparentRaster() {
        return new BufferedImage((int) CANVAS_TILE_SIZE, (int) CANVAS_TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void clearRaster(BufferedImage raster) {
        Graphics2D graphics = raster.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, raster.getWidth(), raster.getHeight());
        } finally {
            graphics.dispose();
        }
    }

    private static void refreshTileCanvasFromRaster(CanvasTile tile) {
        GraphicsContext graphics = tile.canvas().getGraphicsContext2D();
        graphics.clearRect(0, 0, CANVAS_TILE_SIZE, CANVAS_TILE_SIZE);
        graphics.drawImage(bufferedToWritable(tile.raster()), 0, 0);
    }

    private static WritableImage bufferedToWritable(BufferedImage image) {
        WritableImage output = new WritableImage(image.getWidth(), image.getHeight());
        PixelWriter writer = output.getPixelWriter();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                writer.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return output;
    }

    private static java.awt.Color toAwtColor(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        return new java.awt.Color(clampColor(safe.getRed()), clampColor(safe.getGreen()), clampColor(safe.getBlue()), 255);
    }

    private static int blend(int dst, int src) {
        int alpha = (src >>> 24) & 0xff;
        if (alpha >= 255) {
            return src;
        }
        if (alpha <= 0) {
            return dst;
        }
        int inv = 255 - alpha;
        int r = (((src >>> 16) & 0xff) * alpha + ((dst >>> 16) & 0xff) * inv) / 255;
        int g = (((src >>> 8) & 0xff) * alpha + ((dst >>> 8) & 0xff) * inv) / 255;
        int b = ((src & 0xff) * alpha + (dst & 0xff) * inv) / 255;
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private static int toArgb(Color color) {
        Color safe = color == null ? Color.WHITE : color;
        int alpha = 0xff;
        int red = clampColor(safe.getRed());
        int green = clampColor(safe.getGreen());
        int blue = clampColor(safe.getBlue());
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int clampColor(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value * 255.0)));
    }

    private record ImageBounds(double minX, double minY, double width, double height) {
        private double maxX() {
            return minX + width;
        }

        private double maxY() {
            return minY + height;
        }
    }

    private record SnapshotSize(int width, int height, int scale, double originX, double originY,
                                double logicalWidth, double logicalHeight, boolean cropped,
                                List<String> warnings) {
    }

    private enum InkCommandType {
        DRAW,
        ERASE,
        ERASE_REGION;

        private static InkCommandType fromToken(String token) {
            if (token == null || token.isBlank()) {
                return DRAW;
            }
            try {
                return InkCommandType.valueOf(token.strip().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return DRAW;
            }
        }
    }

    public record InkCommandState(String type, double x1, double y1, double x2, double y2,
                                  double controlX, double controlY, String color, double width,
                                  boolean quadratic) {
    }

    public record InkPointState(double x, double y, long nanos, double pressure) {
        public InkPointState(double x, double y, long nanos) {
            this(x, y, nanos, 1.0);
        }

        public InkPointState {
            pressure = safePressure(pressure);
        }
    }

    public record InkStrokeState(String type, String color, double width, List<InkPointState> points) {
        public InkStrokeState {
            type = type == null || type.isBlank() ? InkCommandType.DRAW.name() : type;
            color = color == null || color.isBlank() ? "#000000ff" : color;
            width = Math.max(1.0, width);
            points = points == null ? List.of() : List.copyOf(points);
        }
    }

    private static double safePressure(double pressure) {
        return InkBrushMath.safePressure(pressure);
    }

    private record InkCommand(InkCommandType type, double x1, double y1, double x2, double y2,
                              double controlX, double controlY, Color color, double width,
                              boolean quadratic) {
        private static InkCommand line(InkCommandType type, double x1, double y1, double x2, double y2,
                                       double controlX, double controlY, Color color, double width) {
            return new InkCommand(type, x1, y1, x2, y2, controlX, controlY, color, width, false);
        }

        private static InkCommand quadratic(InkCommandType type, double startX, double startY,
                                            double controlX, double controlY, double endX, double endY,
                                            Color color, double width) {
            return new InkCommand(type, startX, startY, endX, endY, controlX, controlY, color, width, true);
        }

        private static InkCommand regionErase(double x, double y, double width, double height) {
            return new InkCommand(InkCommandType.ERASE_REGION, x, y, width, height, 0.0, 0.0,
                    Color.TRANSPARENT, 1.0, false);
        }

        private InkCommandState state() {
            return new InkCommandState(type.name(), x1, y1, x2, y2, controlX, controlY,
                    toCssColor(color), width, quadratic);
        }

        private void render(Graphics2D graphics, int scale, double originX, double originY) {
            if (type == InkCommandType.ERASE_REGION) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fill(new Rectangle2D.Double(
                        (x1 - originX) * scale,
                        (y1 - originY) * scale,
                        x2 * scale,
                        y2 * scale));
                graphics.setComposite(AlphaComposite.SrcOver);
                return;
            }
            if (!hasVisibleWidth(width * scale)) {
                return;
            }
            graphics.setComposite(type == InkCommandType.ERASE ? AlphaComposite.Clear : AlphaComposite.SrcOver);
            graphics.setStroke(new BasicStroke(
                    (float) visibleWidth(width * scale),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            if (type == InkCommandType.DRAW) {
                graphics.setColor(toAwtColor(color == null ? Color.BLACK : color));
            }
            double sx = (x1 - originX) * scale;
            double sy = (y1 - originY) * scale;
            double ex = (x2 - originX) * scale;
            double ey = (y2 - originY) * scale;
            if (quadratic) {
                graphics.draw(new QuadCurve2D.Double(
                        sx,
                        sy,
                        (controlX - originX) * scale,
                        (controlY - originY) * scale,
                        ex,
                        ey));
            } else {
                graphics.drawLine(
                        (int) Math.round(sx),
                        (int) Math.round(sy),
                        (int) Math.round(ex),
                        (int) Math.round(ey));
            }
            graphics.setComposite(AlphaComposite.SrcOver);
        }
    }

    private record CanvasTile(Canvas canvas, BufferedImage raster, double x, double y) {
        private boolean intersects(double minX, double minY, double maxX, double maxY) {
            return maxX >= x && maxY >= y && minX <= x + CANVAS_TILE_SIZE && minY <= y + CANVAS_TILE_SIZE;
        }
    }

    private static Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return Color.BLACK;
        }
        try {
            return Color.web(value.strip());
        } catch (IllegalArgumentException ignored) {
            return Color.BLACK;
        }
    }

    static String toCssColor(Color color) {
        Color safe = color == null ? Color.BLACK : color;
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x%02x",
                clampColor(safe.getRed()),
                clampColor(safe.getGreen()),
                clampColor(safe.getBlue()),
                clampColor(safe.getOpacity()));
    }
}
