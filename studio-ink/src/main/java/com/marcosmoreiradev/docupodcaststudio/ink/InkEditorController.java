package com.marcosmoreiradev.docupodcaststudio.ink;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shared bounded undo/redo controller independent from JavaFX and the concrete canvas. */
public final class InkEditorController<S> {
    private final int limit;
    private final Supplier<S> snapshot;
    private final Consumer<S> restore;
    private final ArrayDeque<S> undo = new ArrayDeque<>();
    private final ArrayDeque<S> redo = new ArrayDeque<>();

    public InkEditorController(int limit, Supplier<S> snapshot, Consumer<S> restore) {
        this.limit = Math.max(1, Math.min(500, limit));
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.restore = Objects.requireNonNull(restore, "restore");
    }

    /** Call immediately before a user mutation. */
    public void checkpoint() {
        undo.addLast(Objects.requireNonNull(snapshot.get(), "snapshot returned null"));
        while (undo.size() > limit) undo.removeFirst();
        redo.clear();
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public boolean undo() {
        if (undo.isEmpty()) return false;
        redo.addLast(Objects.requireNonNull(snapshot.get(), "snapshot returned null"));
        restore.accept(undo.removeLast());
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) return false;
        undo.addLast(Objects.requireNonNull(snapshot.get(), "snapshot returned null"));
        while (undo.size() > limit) undo.removeFirst();
        restore.accept(redo.removeLast());
        return true;
    }

    public void resetHistory() { undo.clear(); redo.clear(); }
}
