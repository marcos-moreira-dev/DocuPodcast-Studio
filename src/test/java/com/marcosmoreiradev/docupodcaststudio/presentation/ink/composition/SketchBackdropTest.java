package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasSurface;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasExportOptions;
import com.marcosmoreiradev.docupodcaststudio.ink.model.*;
import javafx.application.Platform;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class SketchBackdropTest {
    @Test void vaultPreviewIncludesVectorOutlineInsteadOfOnlyTransparentRegions() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        FutureTask<Void> test = new FutureTask<>(() -> {
            InkStroke closed = new InkStroke(InkTool.DRAW,"#000000",4,List.of(
                    point(20,20),point(80,20),point(80,80),point(20,80),point(20,20)));
            Image preview = SketchBackdrop.renderPreview(100,100,List.of(closed),Color.TRANSPARENT,List.of());
            assertTrue(preview.getPixelReader().getColor(20,50).getOpacity() > 0.9);
            assertTrue(preview.getPixelReader().getColor(20,50).getBrightness() < 0.1);
            return null;
        });
        Platform.runLater(test);
        test.get(20,TimeUnit.SECONDS);
    }

    @Test void closedInteriorAndStageSurviveExportWithColoredInkOnTop() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        FutureTask<Void> test = new FutureTask<>(() -> {
            WritableImage stage = new WritableImage(100, 100);
            for (int y=0;y<100;y++) for(int x=0;x<100;x++) stage.getPixelWriter().setColor(x,y,Color.BLUE);
            InkStroke closed = new InkStroke(InkTool.DRAW,"#000000",2,List.of(
                    point(20,20),point(80,20),point(80,80),point(20,80),point(20,20)));
            Image backdrop = SketchBackdrop.render(stage,100,100,List.of(closed),Color.WHITE);
            assertEquals(Color.BLUE,backdrop.getPixelReader().getColor(5,5));
            assertEquals(Color.WHITE,backdrop.getPixelReader().getColor(50,50));
            InkStroke open = new InkStroke(InkTool.DRAW,"#000000",2,List.of(
                    point(20,80),point(20,20),point(80,20),point(80,80)));
            assertEquals(Color.BLUE, SketchBackdrop.render(stage,100,100,List.of(open),Color.WHITE)
                    .getPixelReader().getColor(50,50));
            InkCanvasSurface surface = new InkCanvasSurface();
            surface.resetForFixedEditableState(100,100,Color.TRANSPARENT);
            surface.restoreApplicationInkStrokes(List.of(closed,new InkStroke(InkTool.DRAW,"#ffff00",10,
                    List.of(point(40,50),point(60,50)))));
            ImageView view = new ImageView(backdrop);
            view.setFitWidth(100); view.setFitHeight(100);
            view.setOpacity(0.1); // Editor guide opacity must not attenuate the exported stage.
            Image output = surface.exportWithImages(List.of(view),new InkCanvasExportOptions(1,100000,false,0,100,100)).image();
            assertEquals(Color.BLUE,output.getPixelReader().getColor(5,5));
            assertEquals(Color.WHITE,output.getPixelReader().getColor(50,35));
            assertEquals(Color.YELLOW,output.getPixelReader().getColor(50,50));
            return null;
        });
        Platform.runLater(test);
        test.get(20,TimeUnit.SECONDS);
    }
    private static InkPoint point(double x,double y) { return InkPoint.of(x,y,1,1); }
}
