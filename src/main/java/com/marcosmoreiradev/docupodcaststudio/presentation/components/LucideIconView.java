package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight renderer for bundled Lucide SVG stroke icons. */
public final class LucideIconView extends StackPane {
    private static final java.util.Map<String, String> SVG_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final System.Logger LOG = System.getLogger(LucideIconView.class.getName());
    private static final double SOURCE_SIZE = 24.0;
    private static final Pattern PATH = Pattern.compile("<path\\s+[^>]*d=\"([^\"]+)\"[^>]*/?>");
    private static final Pattern LINE = Pattern.compile("<line\\s+([^>]*)/?>");
    private static final Pattern CIRCLE = Pattern.compile("<circle\\s+([^>]*)/?>");
    private static final Pattern RECT = Pattern.compile("<rect\\s+([^>]*)/?>");
    private static final Pattern ELLIPSE = Pattern.compile("<ellipse\\s+([^>]*)/?>");
    private static final Pattern POLYLINE = Pattern.compile("<polyline\\s+([^>]*)/?>");
    private static final Pattern POLYGON = Pattern.compile("<polygon\\s+([^>]*)/?>");
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z][a-zA-Z0-9-]*)=\"([^\"]*)\"");

    public LucideIconView(String iconName, double size) {
        String resolved = iconName == null || iconName.isBlank() ? "help" : iconName.strip();
        double safeSize = Math.max(12.0, size);
        getStyleClass().add("ui-lucide-icon");
        setMinSize(safeSize, safeSize);
        setPrefSize(safeSize, safeSize);
        setMaxSize(safeSize, safeSize);
        setAlignment(Pos.CENTER);

        Group group = parseIcon(resolved);
        if (group == null) {
            Label fallback = new Label("?");
            fallback.getStyleClass().add(AppStyles.UI_ICON_FALLBACK);
            getChildren().add(fallback);
            return;
        }
        double scale = safeSize / SOURCE_SIZE;
        group.setScaleX(scale);
        group.setScaleY(scale);
        getChildren().add(group);
    }

    public static LucideIconView of(String iconName) {
        return new LucideIconView(iconName, 20);
    }

    private static Group parseIcon(String iconName) {
        String svg = readSvg(iconName);
        if (svg == null || svg.isBlank()) {
            return null;
        }
        Group group = new Group();
        appendPaths(group, svg);
        appendLines(group, svg);
        appendCircles(group, svg);
        appendRects(group, svg);
        appendEllipses(group, svg);
        appendPointShapes(group, svg, POLYLINE, false);
        appendPointShapes(group, svg, POLYGON, true);
        return group.getChildren().isEmpty() ? null : group;
    }

    private static String readSvg(String iconName) {
        String cached = SVG_CACHE.get(iconName);
        if (cached != null) return cached;
        String resource = "/icons/lucide/" + iconName + ".svg";
        try (InputStream input = LucideIconView.class.getResourceAsStream(resource)) {
            if (input == null) {
                LOG.log(System.Logger.Level.WARNING, "Icono no disponible: " + resource);
                return null;
            }
            String svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            SVG_CACHE.put(iconName, svg);
            return svg;
        } catch (IOException ex) {
            LOG.log(System.Logger.Level.WARNING, "No se pudo leer el icono " + resource, ex);
            return null;
        }
    }

    private static void appendPaths(Group group, String svg) {
        Matcher matcher = PATH.matcher(svg);
        while (matcher.find()) {
            SVGPath path = new SVGPath();
            path.setContent(matcher.group(1));
            styleStrokeShape(path);
            group.getChildren().add(path);
        }
    }

    private static void appendLines(Group group, String svg) {
        Matcher matcher = LINE.matcher(svg);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            Line line = new Line(
                    attr(attributes, "x1", 0),
                    attr(attributes, "y1", 0),
                    attr(attributes, "x2", 0),
                    attr(attributes, "y2", 0));
            styleStrokeShape(line);
            group.getChildren().add(line);
        }
    }

    private static void appendCircles(Group group, String svg) {
        Matcher matcher = CIRCLE.matcher(svg);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            Circle circle = new Circle(
                    attr(attributes, "cx", 0),
                    attr(attributes, "cy", 0),
                    attr(attributes, "r", 0));
            styleStrokeShape(circle);
            group.getChildren().add(circle);
        }
    }

    private static void appendRects(Group group, String svg) {
        Matcher matcher = RECT.matcher(svg);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            Rectangle rect = new Rectangle(
                    attr(attributes, "x", 0),
                    attr(attributes, "y", 0),
                    attr(attributes, "width", 0),
                    attr(attributes, "height", 0));
            double rx = attr(attributes, "rx", attr(attributes, "ry", 0));
            rect.setArcWidth(rx * 2.0);
            rect.setArcHeight(rx * 2.0);
            styleStrokeShape(rect);
            group.getChildren().add(rect);
        }
    }

    private static void appendEllipses(Group group, String svg) {
        Matcher matcher = ELLIPSE.matcher(svg);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            Ellipse ellipse = new Ellipse(attr(attributes, "cx", 0), attr(attributes, "cy", 0),
                    attr(attributes, "rx", 0), attr(attributes, "ry", 0));
            styleStrokeShape(ellipse);
            group.getChildren().add(ellipse);
        }
    }

    private static void appendPointShapes(Group group, String svg, Pattern pattern, boolean closed) {
        Matcher matcher = pattern.matcher(svg);
        while (matcher.find()) {
            String points = stringAttr(matcher.group(1), "points");
            if (points.isBlank()) continue;
            Shape shape = closed ? new Polygon() : new Polyline();
            javafx.collections.ObservableList<Double> target = closed
                    ? ((Polygon) shape).getPoints() : ((Polyline) shape).getPoints();
            for (String token : points.trim().split("[ ,]+")) {
                try { target.add(Double.parseDouble(token)); } catch (NumberFormatException ignored) { }
            }
            styleStrokeShape(shape);
            group.getChildren().add(shape);
        }
    }

    private static double attr(String attributes, String name, double fallback) {
        Matcher matcher = ATTRIBUTE.matcher(attributes == null ? "" : attributes);
        while (matcher.find()) {
            if (name.equals(matcher.group(1))) {
                try {
                    return Double.parseDouble(matcher.group(2));
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private static String stringAttr(String attributes, String name) {
        Matcher matcher = ATTRIBUTE.matcher(attributes == null ? "" : attributes);
        while (matcher.find()) if (name.equals(matcher.group(1))) return matcher.group(2);
        return "";
    }

    private static void styleStrokeShape(Shape shape) {
        shape.getStyleClass().add("ui-lucide-icon-shape");
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(Color.web("#111827"));
        shape.setStrokeWidth(2.0);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }
}
