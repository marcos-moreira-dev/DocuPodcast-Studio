package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasSurface;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions;
import javafx.scene.image.ImageView;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkRegions;
import javafx.scene.image.WritableImage;

/** Paint adapter for bounded regions supplied by the geometry engine. */
public final class SketchBackdrop {
    private SketchBackdrop() {}
    public record Fill(double x, double y, String color) {}

    public static Image render(Image stage, double width, double height,
                               List<InkStroke> strokes, Color interior) {
        return render(stage,width,height,strokes,interior,List.of());
    }

    public static Image render(Image stage, double width, double height,
                               List<InkStroke> strokes, Color interior, List<Fill> fills) {
        return render(stage, width, height, strokes, interior, fills, 1024);
    }

    private static Image render(Image stage, double width, double height,
                                List<InkStroke> strokes, Color interior, List<Fill> fills, int density) {
        Canvas canvas = new Canvas(width, height);
        var g = canvas.getGraphicsContext2D();
        if(stage!=null) g.drawImage(stage, 0, 0, width, height);
        else { g.setFill(interior); g.fillRect(0,0,width,height); }
        InkRegions regions=InkRegions.detect(strokes,width,height,density);
        Map<Integer,Color> colors=new HashMap<>();
        for(Fill fill:fills) {
            int region=regions.regionAt(fill.x(),fill.y());
            if(region>0) colors.put(region,Color.web(fill.color()));
        }
        WritableImage mask=new WritableImage(regions.width(),regions.height());
        for(int y=0;y<regions.height();y++) for(int x=0;x<regions.width();x++) {
            int region=regions.regionAtPixel(x,y);
            if(region>0) mask.getPixelWriter().setColor(x,y,colors.getOrDefault(region,interior));
        }
        g.drawImage(mask,0,0,width,height);
        return canvas.snapshot(null, null);
    }

    /** Small composite used by reusable-drawing cards; includes the vector outline. */
    public static Image renderPreview(double width, double height,
                                      List<InkStroke> strokes, Color interior, List<Fill> fills) {
        Image backdrop = render(null, width, height, strokes, interior, fills);
        InkCanvasSurface surface = new InkCanvasSurface();
        surface.resetForFixedEditableState(width, height, Color.TRANSPARENT);
        surface.restoreApplicationInkStrokes(strokes == null ? List.of() : strokes);
        return surface.exportWithImages(List.of(new ImageView(backdrop)),
                new InkCanvasExportOptions(1, 1_000_000, false, true, 0, width, height)).image();
    }

    /** Renders fills and source imagery at export density while preserving logical coordinates. */
    public static Image renderScaled(Image stage, double logicalWidth, double logicalHeight,
                                     List<InkStroke> strokes, Color interior, List<Fill> fills,
                                     int scale) {
        int safeScale = Math.max(1, scale);
        if (safeScale == 1) {
            return render(stage, logicalWidth, logicalHeight, strokes, interior, fills);
        }
        List<InkStroke> scaledStrokes = (strokes == null ? List.<InkStroke>of() : strokes).stream()
                .map(stroke -> new InkStroke(stroke.tool(), stroke.color(), stroke.width() * safeScale,
                        stroke.points().stream().map(point ->
                                com.marcosmoreiradev.docupodcaststudio.ink.model.InkPoint.of(
                                        point.x() * safeScale, point.y() * safeScale,
                                        point.nanos(), point.pressure())).toList()))
                .toList();
        List<Fill> scaledFills = (fills == null ? List.<Fill>of() : fills).stream()
                .map(fill -> new Fill(fill.x() * safeScale, fill.y() * safeScale, fill.color()))
                .toList();
        return render(stage, logicalWidth * safeScale, logicalHeight * safeScale,
                scaledStrokes, interior, scaledFills, 4096);
    }

}
