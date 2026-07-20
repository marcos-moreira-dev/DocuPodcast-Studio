package com.marcosmoreiradev.docupodcaststudio.ink;

import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import javafx.scene.Node;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owns input, normalized samples, history, zoom, restoration, export and provider lifetime. */
public final class InkEditorSession<S> implements AutoCloseable {
    private final DrawingProfile profile;
    private final InkInputProvider provider;
    private final Supplier<S> snapshot;
    private final Consumer<S> restore;
    private final InkStateExporter<S> exporter;
    private final InkEditorController<S> history;
    private InkCoordinateTransform coordinateTransform = InkCoordinateTransform.IDENTITY;
    private double zoom = 1.0;
    private boolean attached;
    private boolean closed;

    public InkEditorSession(DrawingProfile profile, InkInputProvider provider, Supplier<S> snapshot,
                            Consumer<S> restore, InkStateExporter<S> exporter) {
        this.profile = Objects.requireNonNull(profile, "drawing profile");
        this.provider = Objects.requireNonNull(provider, "input provider");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.restore = Objects.requireNonNull(restore, "restore");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.history = new InkEditorController<>(profile.historyLimit(), snapshot, restore);
    }

    public DrawingProfile profile() { return profile; }
    public double zoom() { return zoom; }
    public boolean attached() { return attached; }
    public boolean closed() { return closed; }

    public void attach(Node target, InkInputListener listener) {
        ensureOpen();
        Objects.requireNonNull(target, "input target");
        Objects.requireNonNull(listener, "input listener");
        if (attached) provider.detach();
        provider.attach(target, normalized(listener));
        attached = true;
    }

    public void coordinateTransform(InkCoordinateTransform transform) {
        ensureOpen();
        coordinateTransform = Objects.requireNonNullElse(transform, InkCoordinateTransform.IDENTITY);
        provider.resetCoordinateState();
    }

    public double zoomTo(double requested) {
        ensureOpen();
        zoom = profile.zoomEnabled() && Double.isFinite(requested)
                ? Math.max(0.1, Math.min(8.0, requested)) : 1.0;
        provider.resetCoordinateState();
        return zoom;
    }

    public void checkpoint() { ensureOpen(); history.checkpoint(); }
    public boolean canUndo() { return history.canUndo(); }
    public boolean canRedo() { return history.canRedo(); }
    public boolean undo() { ensureOpen(); return history.undo(); }
    public boolean redo() { ensureOpen(); return history.redo(); }

    public void restore(S state) {
        ensureOpen();
        restore.accept(Objects.requireNonNull(state, "state"));
        history.resetHistory();
        provider.resetCoordinateState();
    }

    public Path export(Path destination) throws IOException {
        ensureOpen();
        return exporter.export(snapshot.get(), Objects.requireNonNull(destination, "destination"), profile.exportProfile());
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        attached = false;
        provider.close();
    }

    private InkInputListener normalized(InkInputListener delegate) {
        return new InkInputListener() {
            @Override public void onHover(InkInputSample sample) { delegate.onHover(normalize(sample)); }
            @Override public boolean onStrokeStart(InkInputSample sample) { return delegate.onStrokeStart(normalize(sample)); }
            @Override public boolean onStrokeMove(InkInputSample sample) { return delegate.onStrokeMove(normalize(sample)); }
            @Override public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
                return delegate.onStrokeMoveBatch(samples == null ? List.of() : samples.stream().map(InkEditorSession.this::normalize).toList());
            }
            @Override public boolean onStrokeEnd(InkInputSample sample) { return delegate.onStrokeEnd(normalize(sample)); }
        };
    }

    private InkInputSample normalize(InkInputSample sample) {
        InkInputSample transformed = coordinateTransform.transform(Objects.requireNonNull(sample, "input sample"));
        double x = Math.max(0.0, Math.min(profile.logicalWidth(), transformed.x()));
        double y = Math.max(0.0, Math.min(profile.logicalHeight(), transformed.y()));
        return new InkInputSample(x, y, transformed.nanos(), transformed.pressure(), transformed.cursor(),
                transformed.primaryButtonDown(), transformed.eraserButton(), transformed.rawPressure(), transformed.inputSource());
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("ink editor session is closed");
    }
}
