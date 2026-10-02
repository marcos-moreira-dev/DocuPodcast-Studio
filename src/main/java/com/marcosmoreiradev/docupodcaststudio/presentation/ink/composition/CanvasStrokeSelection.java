package com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition;

import com.marcosmoreiradev.docupodcaststudio.ink.canvas.TiledInkCanvasSurface;
import com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkSelectionGeometry;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkStroke;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.Consumer;
import com.marcosmoreiradev.docupodcaststudio.ink.geometry.InkRegions;
import com.marcosmoreiradev.docupodcaststudio.ink.model.InkPoint;

/** Vector editing adapter. Geometry stays in studio-ink; clipboard lasts for the app session. */
public final class CanvasStrokeSelection {
    /** Optional non-ink objects participate in the same frame without rasterizing ink. */
    public interface EditableObject {
        Rectangle2D bounds();
        Consumer<ObjectTransform> captureTransform();
        Supplier<EditableObject> copyFactory();
        void remove();
    }
    public record ObjectTransform(double cx, double cy, double dx, double dy, double scale, double degrees) {}
    private Supplier<List<? extends EditableObject>> objects = List::of;
    private final Set<EditableObject> selectedObjects = new LinkedHashSet<>();
    private Set<EditableObject> marqueeObjects = Set.of();
    private List<Consumer<ObjectTransform>> objectTransforms = List.of();
    private List<Supplier<EditableObject>> objectClipboard = List.of();
    private Point2D transformCenter;
    public void setObjectAccess(Supplier<List<? extends EditableObject>> access) { objects = access; }

    private static List<InkStroke> clipboard = List.of();
    private static List<SketchBackdrop.Fill> clipboardFills = List.of();
    private Supplier<List<SketchBackdrop.Fill>> readFills = List::of;
    private Consumer<List<SketchBackdrop.Fill>> writeFills = fills -> {};
    private List<SketchBackdrop.Fill> dragFills = List.of();
    private InkRegions dragRegions;
    private final TiledInkCanvasSurface surface;
    private final Runnable checkpoint;
    private final Rectangle target = new Rectangle();
    private final Rectangle bounds = new Rectangle();
    private final Rectangle marquee = new Rectangle();
    private final List<Label> scaleHandles = new ArrayList<>();
    private final Label rotationHandle = new Label("↻");
    private final Line rotationGuide = new Line();
    private final Set<Integer> selected = new LinkedHashSet<>();
    private final List<Set<Integer>> groups = new ArrayList<>();
    private Set<Integer> marqueeBase = Set.of();
    private boolean marqueeSelecting;
    private List<InkStroke> start = List.of();
    private Point2D anchor;
    private boolean changed;
    private Gesture gesture;
    private Runnable onChanged = () -> {};

    public void setOnChanged(Runnable action) { onChanged = action; }

    public void setFillAccess(Supplier<List<SketchBackdrop.Fill>> read, Consumer<List<SketchBackdrop.Fill>> write) {
        readFills=read;
        writeFills=write;
    }

    public CanvasStrokeSelection(TiledInkCanvasSurface surface, Runnable checkpoint) {
        this.surface = surface;
        this.checkpoint = checkpoint;
        target.setFill(Color.TRANSPARENT);
        target.setManaged(false);
        bounds.setManaged(false);
        bounds.setMouseTransparent(true);
        bounds.setFill(Color.TRANSPARENT);
        bounds.setStroke(Color.web("#6257cd"));
        bounds.getStrokeDashArray().addAll(6.0, 4.0);
        bounds.getStyleClass().add("studio-canvas-selection-bounds");
        marquee.setManaged(false);
        marquee.setMouseTransparent(true);
        marquee.setVisible(false);
        marquee.getStyleClass().add("studio-canvas-selection-marquee");
        rotationGuide.setManaged(false);
        rotationGuide.setMouseTransparent(true);
        rotationGuide.getStyleClass().add("studio-canvas-selection-rotation-guide");
        surface.inkInputLayer().getChildren().addAll(target, bounds, marquee, rotationGuide);
        installTransformHandles();
        target.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            anchor = surface.sceneToLocal(e.getSceneX(), e.getSceneY());
            start = surface.applicationInkStrokes();
            int hit = -1;
            for (int i = start.size() - 1; i >= 0; i--) {
                if (InkSelectionGeometry.hitTest(start.get(i), anchor.getX(), anchor.getY())) { hit = i; break; }
            }
            boolean insideSelection = hasSelection() && bounds.contains(anchor.getX(), anchor.getY());
            EditableObject objectHit = objects.get().stream().filter(o -> o.bounds().contains(anchor)).reduce((a,b) -> b).orElse(null);
            if (!e.isShiftDown() && !selected.contains(hit) && !selectedObjects.contains(objectHit) && !insideSelection) {
                selected.clear(); selectedObjects.clear();
            }
            if (hit < 0 && insideSelection && !e.isShiftDown()) {
                marqueeSelecting = false;
            } else if (hit >= 0) {
                if (e.isShiftDown() && selected.contains(hit)) deselectStrokeAndGroup(hit);
                else selectStrokeAndGroup(hit);
                marqueeSelecting = false;
            } else if (objectHit != null) {
                if (e.isShiftDown() && selectedObjects.contains(objectHit)) selectedObjects.remove(objectHit);
                else selectedObjects.add(objectHit);
                marqueeSelecting = false;
            } else {
                marqueeSelecting = true;
                marqueeObjects = e.isShiftDown() ? Set.copyOf(selectedObjects) : Set.of();
                marqueeBase = e.isShiftDown() ? Set.copyOf(selected) : Set.of();
                marquee.setX(anchor.getX()); marquee.setY(anchor.getY());
                marquee.setWidth(0); marquee.setHeight(0); marquee.setVisible(true);
            }
            changed = false;
            dragFills=List.copyOf(readFills.get());
            dragRegions=null;
            captureObjects(start);
            refresh();
            e.consume();
        });
        target.setOnMouseDragged(e -> {
            if (anchor == null) return;
            Point2D p = surface.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (marqueeSelecting) {
                double x = Math.min(anchor.getX(), p.getX()), y = Math.min(anchor.getY(), p.getY());
                double width = Math.abs(p.getX() - anchor.getX()), height = Math.abs(p.getY() - anchor.getY());
                marquee.setX(x); marquee.setY(y); marquee.setWidth(width); marquee.setHeight(height);
                selectIntersecting(new Rectangle2D(x, y, width, height), marqueeBase);
                e.consume();
                return;
            }
            if (!hasSelection()) return;
            if (!changed && p.distance(anchor) < 2) return;
            if (!surface.vectorInkReliable()) return;
            if (!changed) { checkpoint.run(); changed = true; dragRegions=selectedRegions(start); }
            moveFills(dragFills,dragRegions,start,p.getX()-anchor.getX(),p.getY()-anchor.getY(),1,0);
            applyObjects(p.getX()-anchor.getX(),p.getY()-anchor.getY(),1,0);
            replace(transformed(start, p.getX()-anchor.getX(), p.getY()-anchor.getY(), 1, 0));
            onChanged.run();
            e.consume();
        });
        target.setOnMouseReleased(e -> {
            anchor = null; start = List.of(); transformCenter = null;
            marqueeSelecting = false; marqueeBase = Set.of(); marquee.setVisible(false);
            onChanged.run();
        });
        setActive(false);
    }

    public void setActive(boolean active) {
        target.setWidth(surface.logicalWidth());
        target.setHeight(surface.logicalHeight());
        target.setVisible(active);
        target.setMouseTransparent(!active);
        if (!active) { selected.clear(); selectedObjects.clear(); }
        refresh();
    }

    /** Moves the current vector selection and its associated solid fills. */
    public void translate(double dx, double dy) {
        transformSelection(dx, dy, 1, 0);
    }

    public void selectAll() {
        selected.clear();
        selectedObjects.clear(); selectedObjects.addAll(objects.get());
        for (int i=0; i<surface.applicationInkStrokes().size(); i++) selected.add(i);
        refresh();
    }

    /** Treats the current selection as one object for later clicks and transforms. */
    public void groupSelection() {
        if (selected.size() < 2) return;
        groups.removeIf(group -> !Collections.disjoint(group, selected));
        groups.add(Set.copyOf(selected));
        refresh();
    }

    public void ungroupSelection() {
        if (selected.isEmpty()) return;
        groups.removeIf(group -> !Collections.disjoint(group, selected));
        refresh();
    }

    int selectedStrokeCount() { return selected.size(); }

    public Set<EditableObject> selectedObjects() { return Set.copyOf(selectedObjects); }

    public boolean hasSelection() { return !selected.isEmpty() || !selectedObjects.isEmpty(); }

    /** Portable vector payload used by theatre's project-scoped drawing vault. */
    public SelectionSnapshot selectionSnapshot() {
        List<InkStroke> strokes = surface.applicationInkStrokes();
        List<InkStroke> picked = selected.stream().filter(i -> i < strokes.size()).map(strokes::get).toList();
        if (picked.isEmpty()) return new SelectionSnapshot(List.of(), List.of());
        InkRegions regions = selectedRegions(strokes);
        List<SketchBackdrop.Fill> pickedFills = readFills.get().stream()
                .filter(fill -> regions.regionAt(fill.x(), fill.y()) > 0).toList();
        return new SelectionSnapshot(picked, pickedFills);
    }

    public void insert(SelectionSnapshot snapshot) {
        if (snapshot == null || snapshot.strokes().isEmpty() || !surface.vectorInkReliable()) return;
        checkpoint.run();
        List<InkStroke> strokes = new ArrayList<>(surface.applicationInkStrokes());
        selected.clear();
        for (InkStroke stroke : snapshot.strokes()) {
            selected.add(strokes.size());
            strokes.add(stroke);
        }
        if (selected.size() > 1) groups.add(Set.copyOf(selected));
        List<SketchBackdrop.Fill> nextFills = new ArrayList<>(readFills.get());
        nextFills.addAll(snapshot.fills());
        writeFills.accept(List.copyOf(nextFills));
        replace(strokes);
        onChanged.run();
    }

    public String groupsMetadata() {
        return groups.stream()
                .filter(group -> group.size() > 1)
                .map(group -> group.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                .collect(java.util.stream.Collectors.joining(";"));
    }

    public void restoreGroups(String encoded) {
        groups.clear();
        if (encoded != null && !encoded.isBlank()) {
            for (String groupToken : encoded.split(";")) {
                LinkedHashSet<Integer> group = new LinkedHashSet<>();
                for (String indexToken : groupToken.split(",")) {
                    try { group.add(Integer.parseInt(indexToken.strip())); }
                    catch (NumberFormatException ignored) { }
                }
                if (group.size() > 1) groups.add(Set.copyOf(group));
            }
        }
        refresh();
    }

    private void selectStrokeAndGroup(int hit) {
        selected.add(hit);
        groups.stream().filter(group -> group.contains(hit)).findFirst().ifPresent(selected::addAll);
    }

    private void deselectStrokeAndGroup(int hit) {
        Optional<Set<Integer>> group = groups.stream().filter(candidate -> candidate.contains(hit)).findFirst();
        if (group.isPresent()) selected.removeAll(group.get());
        else selected.remove(hit);
    }

    private void selectIntersecting(Rectangle2D area, Set<Integer> base) {
        selected.clear();
        selected.addAll(base);
        selectedObjects.clear(); selectedObjects.addAll(marqueeObjects);
        objects.get().stream().filter(o -> area.intersects(o.bounds())).forEach(selectedObjects::add);
        List<InkStroke> strokes = surface.applicationInkStrokes();
        for (int i = 0; i < strokes.size(); i++) {
            var strokeBounds = InkSelectionGeometry.bounds(strokes.get(i));
            if (area.intersects(strokeBounds.minX(), strokeBounds.minY(),
                    Math.max(1, strokeBounds.maxX() - strokeBounds.minX()),
                    Math.max(1, strokeBounds.maxY() - strokeBounds.minY()))) {
                selectStrokeAndGroup(i);
            }
        }
        refresh();
    }

    public void transform(double scale, double degrees) {
        transformSelection(0, 0, scale, degrees);
    }

    private void transformSelection(double dx, double dy, double scale, double degrees) {
        if (!hasSelection() || !surface.vectorInkReliable()) return;
        checkpoint.run();
        List<InkStroke> before=surface.applicationInkStrokes();
        captureObjects(before);
        moveFills(readFills.get(),selectedRegions(before),before,dx,dy,scale,degrees);
        applyObjects(dx,dy,scale,degrees);
        replace(transformed(before, dx, dy, scale, degrees));
        transformCenter = null;
        onChanged.run();
    }

    public void deleteSelection() {
        if (!hasSelection() || !surface.vectorInkReliable()) return;
        checkpoint.run();
        List<InkStroke> strokes = new ArrayList<>(surface.applicationInkStrokes());
        List<Integer> indices = selected.stream().sorted(Comparator.reverseOrder()).toList();
        for (int index : indices) if (index < strokes.size()) strokes.remove(index);
        selected.clear();
        groups.clear();
        selectedObjects.forEach(EditableObject::remove); selectedObjects.clear();
        replace(strokes);
        onChanged.run();
    }

    public void copy() {
        objectClipboard = selectedObjects.stream().map(EditableObject::copyFactory).toList();
        List<InkStroke> strokes = surface.applicationInkStrokes();
        clipboard = selected.stream().filter(i -> i < strokes.size()).map(strokes::get).toList();
        InkRegions regions=selectedRegions(strokes);
        clipboardFills=readFills.get().stream().filter(fill->regions.regionAt(fill.x(),fill.y())>0).toList();
    }

    public void paste() {
        if ((clipboard.isEmpty() && objectClipboard.isEmpty()) || !surface.vectorInkReliable()) return;
        checkpoint.run();
        List<InkStroke> strokes = new ArrayList<>(surface.applicationInkStrokes());
        selected.clear();
        selectedObjects.clear();
        objectClipboard.stream().map(Supplier::get).forEach(selectedObjects::add);
        for (InkStroke stroke : clipboard) {
            selected.add(strokes.size());
            strokes.add(InkSelectionGeometry.transform(stroke, new InkSelectionGeometry.Transform(24,24,1,1,0)));
        }
        if (selected.size() > 1) groups.add(Set.copyOf(selected));
        List<SketchBackdrop.Fill> fills=new ArrayList<>(readFills.get());
        for(var fill:clipboardFills) fills.add(new SketchBackdrop.Fill(fill.x()+24,fill.y()+24,fill.color()));
        writeFills.accept(List.copyOf(fills));
        replace(strokes);
        onChanged.run();
    }

    private List<InkStroke> transformed(List<InkStroke> strokes, double dx, double dy, double scale, double angle) {
        List<InkStroke> result = new ArrayList<>(strokes);
        double[] box = box(strokes);
        double cx=transformCenter == null ? (box[0]+box[2])/2 : transformCenter.getX(), cy=transformCenter == null ? (box[1]+box[3])/2 : transformCenter.getY();
        var transform=new InkSelectionGeometry.Transform(dx,dy,scale,scale,angle);
        for (int i : selected) {
            if (i >= strokes.size()) continue;
            InkStroke stroke=strokes.get(i);
            result.set(i,new InkStroke(stroke.tool(),stroke.color(),stroke.width(),stroke.points().stream()
                    .map(point->InkSelectionGeometry.transformPoint(point,cx,cy,transform)).toList()));
        }
        return result;
    }

    private InkRegions selectedRegions(List<InkStroke> strokes) {
        return InkRegions.detect(selected.stream().filter(i->i<strokes.size()).map(strokes::get).toList(),
                surface.logicalWidth(),surface.logicalHeight());
    }

    private void moveFills(List<SketchBackdrop.Fill> fills,InkRegions regions,List<InkStroke> strokes,
                           double dx,double dy,double scale,double angle) {
        double[] b=box(strokes);
        double cx=transformCenter == null ? (b[0]+b[2])/2 : transformCenter.getX(),cy=transformCenter == null ? (b[1]+b[3])/2 : transformCenter.getY();
        var transform=new InkSelectionGeometry.Transform(dx,dy,scale,scale,angle);
        List<SketchBackdrop.Fill> moved=fills.stream().map(fill->{
            if(regions.regionAt(fill.x(),fill.y())==0) return fill;
            InkPoint p=InkSelectionGeometry.transformPoint(InkPoint.of(fill.x(),fill.y(),0,1),cx,cy,transform);
            return new SketchBackdrop.Fill(p.x(),p.y(),fill.color());
        }).toList();
        writeFills.accept(moved);
    }

    private void replace(List<InkStroke> strokes) {
        if (!surface.vectorInkReliable()) return;
        surface.replaceInkStrokeStates(strokes.stream().map(stroke -> new TiledInkCanvasSurface.InkStrokeState(
                stroke.tool().name(), stroke.color(), stroke.width(), stroke.points().stream().map(p ->
                new TiledInkCanvasSurface.InkPointState(p.x(), p.y(), p.nanos(), p.pressure())).toList())).toList());
        refresh();
    }

    private double[] box(List<InkStroke> strokes) {
        double[] b={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(int i:selected) if(i<strokes.size()) {
            var r=InkSelectionGeometry.bounds(strokes.get(i));
            b[0]=Math.min(b[0],r.minX()); b[1]=Math.min(b[1],r.minY());
            b[2]=Math.max(b[2],r.maxX()); b[3]=Math.max(b[3],r.maxY());
        }
        for (EditableObject object : selectedObjects) {
            Rectangle2D r = object.bounds();
            b[0]=Math.min(b[0],r.getMinX()); b[1]=Math.min(b[1],r.getMinY());
            b[2]=Math.max(b[2],r.getMaxX()); b[3]=Math.max(b[3],r.getMaxY());
        }
        return b;
    }

    private void installTransformHandles() {
        for (String arrow : List.of("↖", "↗", "↙", "↘")) {
            Label handle = transformHandle(arrow, Cursor.SE_RESIZE, "Arrastra para redimensionar la selección.");
            handle.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> beginGesture(event, GestureKind.SCALE));
            handle.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> updateGesture(event, GestureKind.SCALE));
            handle.addEventFilter(MouseEvent.MOUSE_RELEASED, this::finishGesture);
            scaleHandles.add(handle);
        }
        rotationHandle.getStyleClass().addAll("studio-canvas-selection-handle", "rotation");
        rotationHandle.setManaged(false);
        rotationHandle.setCursor(Cursor.HAND);
        rotationHandle.setAccessibleText("Girar selección");
        rotationHandle.setTooltip(new javafx.scene.control.Tooltip("Arrastra alrededor de la selección para girarla."));
        rotationHandle.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> beginGesture(event, GestureKind.ROTATE));
        rotationHandle.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> updateGesture(event, GestureKind.ROTATE));
        rotationHandle.addEventFilter(MouseEvent.MOUSE_RELEASED, this::finishGesture);
        surface.inkInputLayer().getChildren().addAll(scaleHandles);
        surface.inkInputLayer().getChildren().add(rotationHandle);
    }

    private Label transformHandle(String text, Cursor cursor, String tooltip) {
        Label handle = new Label(text);
        handle.setManaged(false);
        handle.setCursor(cursor);
        handle.setAccessibleText("Redimensionar selección");
        handle.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        handle.getStyleClass().add("studio-canvas-selection-handle");
        return handle;
    }

    private void beginGesture(MouseEvent event, GestureKind kind) {
        if (event.getButton() != MouseButton.PRIMARY || !hasSelection() || !surface.vectorInkReliable()) return;
        List<InkStroke> strokes = surface.applicationInkStrokes();
        captureObjects(strokes);
        double[] selectedBox = box(strokes);
        Point2D center = new Point2D((selectedBox[0] + selectedBox[2]) / 2, (selectedBox[1] + selectedBox[3]) / 2);
        Point2D pointer = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
        gesture = new Gesture(kind, strokes, List.copyOf(readFills.get()), selectedRegions(strokes), center,
                Math.max(1, pointer.distance(center)), Math.atan2(pointer.getY() - center.getY(), pointer.getX() - center.getX()));
        checkpoint.run();
        event.consume();
    }

    private void updateGesture(MouseEvent event, GestureKind kind) {
        if (gesture == null || gesture.kind() != kind) return;
        Point2D pointer = surface.sceneToLocal(event.getSceneX(), event.getSceneY());
        double scale = 1;
        double degrees = 0;
        if (kind == GestureKind.SCALE) {
            scale = Math.max(0.1, Math.min(10, pointer.distance(gesture.center()) / gesture.anchorDistance()));
        } else {
            double angle = Math.atan2(pointer.getY() - gesture.center().getY(), pointer.getX() - gesture.center().getX());
            degrees = Math.toDegrees(angle - gesture.anchorAngle());
        }
        moveFills(gesture.fills(), gesture.regions(), gesture.strokes(), 0, 0, scale, degrees);
        applyObjects(0,0,scale,degrees);
        replace(transformed(gesture.strokes(), 0, 0, scale, degrees));
        changed = true;
        onChanged.run();
        event.consume();
    }

    private void finishGesture(MouseEvent event) {
        if (gesture == null) return;
        gesture = null;
        transformCenter = null;
        if (changed) onChanged.run();
        changed = false;
        event.consume();
    }

    public void refresh() {
        target.setWidth(surface.logicalWidth());
        target.setHeight(surface.logicalHeight());
        var strokes=surface.applicationInkStrokes();
        selected.removeIf(i -> i>=strokes.size());
        groups.removeIf(group -> group.stream().anyMatch(i -> i >= strokes.size()));
        selectedObjects.retainAll(objects.get());
        bounds.setVisible(target.isVisible() && hasSelection());
        boolean handlesVisible = target.isVisible() && hasSelection();
        scaleHandles.forEach(handle -> { handle.setVisible(handlesVisible); handle.setMouseTransparent(!handlesVisible); });
        rotationHandle.setVisible(handlesVisible);
        rotationHandle.setMouseTransparent(!handlesVisible);
        rotationGuide.setVisible(handlesVisible);
        if(!hasSelection()) return;
        double[] b=box(strokes);
        bounds.setX(b[0]); bounds.setY(b[1]);
        bounds.setWidth(b[2]-b[0]); bounds.setHeight(b[3]-b[1]);
        double size = 22;
        double[][] positions = {
                {b[0] - size / 2, b[1] - size / 2}, {b[2] - size / 2, b[1] - size / 2},
                {b[0] - size / 2, b[3] - size / 2}, {b[2] - size / 2, b[3] - size / 2}
        };
        for (int i = 0; i < scaleHandles.size(); i++) {
            scaleHandles.get(i).resizeRelocate(positions[i][0], positions[i][1], size, size);
            scaleHandles.get(i).toFront();
        }
        double centerX = (b[0] + b[2]) / 2;
        double rotationY = Math.max(size / 2, b[1] - 38);
        rotationGuide.setStartX(centerX); rotationGuide.setStartY(b[1]);
        rotationGuide.setEndX(centerX); rotationGuide.setEndY(rotationY + size / 2);
        rotationHandle.resizeRelocate(centerX - size / 2, rotationY - size / 2, size, size);
        rotationGuide.toFront();
        rotationHandle.toFront();
    }

    private void captureObjects(List<InkStroke> strokes) {
        double[] b = box(strokes);
        transformCenter = new Point2D((b[0]+b[2])/2, (b[1]+b[3])/2);
        objectTransforms = selectedObjects.stream().map(EditableObject::captureTransform).toList();
    }

    private void applyObjects(double dx, double dy, double scale, double degrees) {
        ObjectTransform t = new ObjectTransform(transformCenter.getX(), transformCenter.getY(), dx, dy, scale, degrees);
        objectTransforms.forEach(apply -> apply.accept(t));
    }

    private enum GestureKind { SCALE, ROTATE }

    private record Gesture(GestureKind kind, List<InkStroke> strokes, List<SketchBackdrop.Fill> fills,
                           InkRegions regions, Point2D center, double anchorDistance, double anchorAngle) {}

    public record SelectionSnapshot(List<InkStroke> strokes, List<SketchBackdrop.Fill> fills) {
        public SelectionSnapshot {
            strokes = strokes == null ? List.of() : List.copyOf(strokes);
            fills = fills == null ? List.of() : List.copyOf(fills);
        }
    }
}
