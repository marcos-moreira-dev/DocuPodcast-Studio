package com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

public final class JavaFxMouseInputProvider implements InkInputProvider {
    private Node target;
    private InkInputListener listener;
    private EventHandler<MouseEvent> movedHandler;
    private EventHandler<MouseEvent> pressedHandler;
    private EventHandler<MouseEvent> draggedHandler;
    private EventHandler<MouseEvent> releasedHandler;
    private boolean strokeActive;

    @Override
    public InkInputCapabilities capabilities() {
        return InkInputCapabilities.javafxMouse();
    }

    @Override
    public void attach(Node target, InkInputListener listener) {
        detach();
        if (target == null || listener == null) {
            return;
        }
        this.target = target;
        this.listener = listener;
        movedHandler = event -> this.listener.onHover(sample(event));
        pressedHandler = event -> {
            strokeActive = this.listener.onStrokeStart(sample(event));
            if (strokeActive) {
                event.consume();
            }
        };
        draggedHandler = event -> {
            if (strokeActive && this.listener.onStrokeMove(sample(event))) {
                event.consume();
            }
        };
        releasedHandler = event -> {
            if (!strokeActive) {
                return;
            }
            boolean consumed = this.listener.onStrokeEnd(sample(event));
            strokeActive = false;
            if (consumed) {
                event.consume();
            }
        };
        target.addEventFilter(MouseEvent.MOUSE_MOVED, movedHandler);
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
        target.addEventFilter(MouseEvent.MOUSE_DRAGGED, draggedHandler);
        target.addEventFilter(MouseEvent.MOUSE_RELEASED, releasedHandler);
    }

    @Override
    public void detach() {
        if (target != null) {
            if (movedHandler != null) {
                target.removeEventFilter(MouseEvent.MOUSE_MOVED, movedHandler);
            }
            if (pressedHandler != null) {
                target.removeEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
            }
            if (draggedHandler != null) {
                target.removeEventFilter(MouseEvent.MOUSE_DRAGGED, draggedHandler);
            }
            if (releasedHandler != null) {
                target.removeEventFilter(MouseEvent.MOUSE_RELEASED, releasedHandler);
            }
        }
        strokeActive = false;
        target = null;
        listener = null;
        movedHandler = null;
        pressedHandler = null;
        draggedHandler = null;
        releasedHandler = null;
    }

    private InkInputSample sample(MouseEvent event) {
        Point2D local = target.sceneToLocal(event.getSceneX(), event.getSceneY());
        return new InkInputSample(
                local.getX(),
                local.getY(),
                System.nanoTime(),
                1.0,
                InkInputCursor.MOUSE,
                event.isPrimaryButtonDown(),
                false);
    }
}
