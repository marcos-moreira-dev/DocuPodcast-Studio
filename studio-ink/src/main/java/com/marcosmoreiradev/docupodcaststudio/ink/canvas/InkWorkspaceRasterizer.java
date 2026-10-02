package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

import com.marcosmoreiradev.docupodcaststudio.ink.model.*;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Renders saved pages through the same ink renderer as the editor, without opening a dialog. */
public final class InkWorkspaceRasterizer {
    private InkWorkspaceRasterizer() { }
    public static BufferedImage render(InkWorkspaceState state, String title) throws IOException {
        if (Platform.isFxApplicationThread()) return renderFx(state,title);
        FutureTask<BufferedImage> task = new FutureTask<>(() -> renderFx(state,title));
        Platform.runLater(task);
        try { return task.get(90, TimeUnit.SECONDS); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IOException("Exportación interrumpida",ex); }
        catch (ExecutionException | TimeoutException ex) { task.cancel(false); throw new IOException("No se pudo renderizar la hoja",ex); }
    }
    private static BufferedImage renderFx(InkWorkspaceState state, String title) throws IOException {
        if (state.logicalWidth()*state.logicalHeight() > 64_000_000L) throw new IOException("Hoja demasiado grande para exportación");
        TiledInkCanvasSurface surface = new TiledInkCanvasSurface();
        surface.resetForEditableState(state.logicalWidth(),state.logicalHeight(),Color.web(state.background()));
        surface.setPaperPattern(state.metadata().getOrDefault("paperPattern","blank"));
        surface.restoreApplicationInkStrokes(state.strokes());
        List<ImageView> images = new ArrayList<>();
        int index = 0;
        for (InkPlacedImage item : state.images()) {
            index++;
            Image image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(item.inlineImageData())));
            if (image.isError()) throw new IOException("Imagen de la hoja ilegible");
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true); view.setFitWidth(item.fitWidth());
            view.setLayoutX(item.x()); view.setLayoutY(item.y());
            view.setOpacity(Double.parseDouble(state.metadata().getOrDefault("imageOpacity."+index,"1")));
            images.add(view);
        }
        surface.markContentBounds(0,0,720,64);
        var result = surface.exportWithImages(images,new InkCanvasExportOptions(2,32_000_000L,true,100,720,520));
        var image = result.image();
        BufferedImage output = new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[output.getWidth()*output.getHeight()];
        image.getPixelReader().getPixels(0,0,output.getWidth(),output.getHeight(),javafx.scene.image.PixelFormat.getIntArgbInstance(),pixels,0,output.getWidth());
        output.setRGB(0,0,output.getWidth(),output.getHeight(),pixels,0,output.getWidth());
        var g = output.createGraphics();
        try { g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(java.awt.Color.BLACK); g.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,18*result.scale()));
            g.drawString(title,24*result.scale(),40*result.scale()); } finally { g.dispose(); }
        return output;
    }
}
