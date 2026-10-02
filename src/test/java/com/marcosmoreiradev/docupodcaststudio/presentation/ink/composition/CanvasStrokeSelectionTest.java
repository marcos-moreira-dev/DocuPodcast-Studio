package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasSurface;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CanvasStrokeSelectionTest {
    @Test void fillsRotateWithClosedSelectionAndTravelThroughClipboard() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch(IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        FutureTask<Void> test=new FutureTask<>(() -> {
            InkCanvasSurface source=new InkCanvasSurface();
            source.commitInkStroke(new InkCanvasSurface.InkStrokeState("DRAW","#000000",2,List.of(
                    new InkCanvasSurface.InkPointState(100,100,1,1),
                    new InkCanvasSurface.InkPointState(160,100,2,1),
                    new InkCanvasSurface.InkPointState(160,160,3,1),
                    new InkCanvasSurface.InkPointState(100,160,4,1),
                    new InkCanvasSurface.InkPointState(100,100,5,1))));
            var colors=new java.util.ArrayList<>(List.of(new SketchBackdrop.Fill(120,130,"#ffff00"),
                    new SketchBackdrop.Fill(500,500,"#ff0000")));
            var original=List.copyOf(colors);
            var history=new java.util.ArrayList<List<SketchBackdrop.Fill>>();
            CanvasStrokeSelection selection=new CanvasStrokeSelection(source,()->history.add(List.copyOf(colors)));
            selection.setFillAccess(()->List.copyOf(colors),updated->{colors.clear();colors.addAll(updated);});
            selection.setActive(true);
            selection.selectAll();
            selection.transform(2,90);
            assertEquals(130,colors.get(0).x(),0.001);
            assertEquals(110,colors.get(0).y(),0.001);
            assertEquals(original.get(1),colors.get(1),"Unselected exterior seed must stay unchanged");
            assertEquals(original,history.get(0),"Checkpoint must precede fill mutation");
            selection.copy();
            InkCanvasSurface destination=new InkCanvasSurface();
            var pasted=new java.util.ArrayList<SketchBackdrop.Fill>();
            CanvasStrokeSelection other=new CanvasStrokeSelection(destination,()->{});
            other.setFillAccess(()->List.copyOf(pasted),updated->{pasted.clear();pasted.addAll(updated);});
            other.paste();
            assertEquals(1,pasted.size());
            assertEquals(154,pasted.get(0).x(),0.001);
            assertEquals(134,pasted.get(0).y(),0.001);
            assertEquals("#ffff00",pasted.get(0).color());
            assertTrue(com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkRegions.detect(
                    destination.applicationInkStrokes(),destination.logicalWidth(),destination.logicalHeight())
                    .regionAt(pasted.get(0).x(),pasted.get(0).y())>0);
            return null;
        });
        Platform.runLater(test);
        test.get(20,TimeUnit.SECONDS);
    }

    @Test void groupScalingAndClipboardPreserveSpacingAcrossFrames() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch(IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        FutureTask<Void> test=new FutureTask<>(() -> {
            InkCanvasSurface first=new InkCanvasSurface();
            first.commitInkStroke(stroke(100));
            first.commitInkStroke(stroke(200));
            AtomicInteger checkpoints=new AtomicInteger();
            CanvasStrokeSelection selection=new CanvasStrokeSelection(first, checkpoints::incrementAndGet);
            selection.setActive(true);
            selection.selectAll();
            selection.transform(2,0);
            var strokes=first.applicationInkStrokes();
            assertEquals(200,strokes.get(1).points().get(0).x()-strokes.get(0).points().get(0).x(),0.001);
            assertEquals(1,checkpoints.get());
            selection.copy();
            InkCanvasSurface second=new InkCanvasSurface();
            CanvasStrokeSelection other=new CanvasStrokeSelection(second, () -> {});
            other.paste();
            assertEquals(2,second.applicationInkStrokes().size());
            assertEquals(strokes.get(0).points().get(0).x()+24,second.applicationInkStrokes().get(0).points().get(0).x(),0.001);
            assertEquals(strokes.get(0).points().get(0).pressure(),second.applicationInkStrokes().get(0).points().get(0).pressure());
            assertEquals(2,first.applicationInkStrokes().size());
            return null;
        });
        Platform.runLater(test);
        test.get(20,TimeUnit.SECONDS);
    }

    @Test void translationMovesSolidFillWithItsSelectedContour() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch(IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        FutureTask<Void> test=new FutureTask<>(() -> {
            InkCanvasSurface surface=new InkCanvasSurface();
            surface.commitInkStroke(new InkCanvasSurface.InkStrokeState("DRAW","#000000",3,List.of(
                    new InkCanvasSurface.InkPointState(100,100,1,1),
                    new InkCanvasSurface.InkPointState(160,100,2,1),
                    new InkCanvasSurface.InkPointState(160,160,3,1),
                    new InkCanvasSurface.InkPointState(100,160,4,1),
                    new InkCanvasSurface.InkPointState(100,100,5,1))));
            var fills=new java.util.ArrayList<>(List.of(new SketchBackdrop.Fill(130,130,"#ffffff")));
            CanvasStrokeSelection selection=new CanvasStrokeSelection(surface,()->{});
            selection.setFillAccess(()->List.copyOf(fills),updated->{fills.clear();fills.addAll(updated);});
            selection.setActive(true);
            selection.selectAll();
            selection.translate(75,40);
            assertEquals(205,fills.get(0).x(),0.001);
            assertEquals(170,fills.get(0).y(),0.001);
            assertTrue(com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkRegions.detect(
                    surface.applicationInkStrokes(),surface.logicalWidth(),surface.logicalHeight())
                    .regionAt(fills.get(0).x(),fills.get(0).y())>0);
            assertEquals(4, surface.inkInputLayer().lookupAll(".studio-canvas-selection-handle").stream()
                    .filter(node -> !(node.getStyleClass().contains("rotation"))).count());
            return null;
        });
        Platform.runLater(test);
        test.get(20,TimeUnit.SECONDS);
    }

    @Test void marqueeSelectsMultipleStrokesAndMovesThemAsOneObject() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch(IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        FutureTask<Void> test=new FutureTask<>(() -> {
            InkCanvasSurface surface=new InkCanvasSurface();
            surface.commitInkStroke(stroke(100));
            surface.commitInkStroke(stroke(130));
            surface.commitInkStroke(stroke(400));
            CanvasStrokeSelection selection=new CanvasStrokeSelection(surface,()->{});
            selection.setActive(true);
            var method=CanvasStrokeSelection.class.getDeclaredMethod("selectIntersecting",
                    javafx.geometry.Rectangle2D.class,java.util.Set.class);
            method.setAccessible(true);
            method.invoke(selection,new javafx.geometry.Rectangle2D(90,90,70,50),java.util.Set.of());
            assertEquals(2,selection.selectedStrokeCount());
            selection.groupSelection();
            selection.translate(10,0);
            assertEquals(110,surface.applicationInkStrokes().get(0).points().get(0).x(),0.001);
            assertEquals(140,surface.applicationInkStrokes().get(1).points().get(0).x(),0.001);
            assertEquals(400,surface.applicationInkStrokes().get(2).points().get(0).x(),0.001);
            return null;
        });
        Platform.runLater(test);
        test.get(20,TimeUnit.SECONDS);
    }

    @Test void groupsAndPortableSelectionSurviveAFrameBoundary() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch(IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        FutureTask<Void> test=new FutureTask<>(() -> {
            InkCanvasSurface source=new InkCanvasSurface();
            source.commitInkStroke(stroke(100));
            source.commitInkStroke(stroke(130));
            CanvasStrokeSelection selection=new CanvasStrokeSelection(source,()->{});
            selection.setActive(true);
            selection.selectAll();
            selection.groupSelection();
            assertEquals("0,1",selection.groupsMetadata());

            InkCanvasSurface destination=new InkCanvasSurface();
            CanvasStrokeSelection restored=new CanvasStrokeSelection(destination,()->{});
            restored.insert(selection.selectionSnapshot());
            restored.restoreGroups(selection.groupsMetadata());
            restored.setActive(true);
            restored.selectAll();
            restored.translate(20,15);
            assertEquals(120,destination.applicationInkStrokes().get(0).points().get(0).x(),0.001);
            assertEquals(150,destination.applicationInkStrokes().get(1).points().get(0).x(),0.001);
            return null;
        });
        Platform.runLater(test);
        test.get(20,TimeUnit.SECONDS);
    }

    private static InkCanvasSurface.InkStrokeState stroke(double x) {
        return new InkCanvasSurface.InkStrokeState("DRAW","#000000",4,List.of(
                new InkCanvasSurface.InkPointState(x,100,1,0.5),
                new InkCanvasSurface.InkPointState(x+10,110,2,0.8)));
    }
}
