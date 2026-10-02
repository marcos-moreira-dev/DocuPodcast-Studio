package com.marcosmoreiradev.docupodcaststudio.ink;

import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCapabilities;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputStatus;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasViewport;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

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
    private final ReadOnlyObjectWrapper<InkInputStatus> inputStatus;
    private InkCoordinateTransform coordinateTransform = InkCoordinateTransform.IDENTITY;
    private InkCanvasViewport viewport;
    private InkInputSample lastMappedSample;
    private double lastRawPressure = Double.NaN;
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
        this.inputStatus = new ReadOnlyObjectWrapper<>(InkInputStatus.idle(provider.capabilities()));
    }

    public DrawingProfile profile() { return profile; }
    public double zoom() { return zoom; }
    public boolean attached() { return attached; }
    public boolean closed() { return closed; }
    public InkInputCapabilities inputCapabilities() { return provider.capabilities(); }
    public ReadOnlyObjectProperty<InkInputStatus> inputStatusProperty() { return inputStatus.getReadOnlyProperty(); }
    public InkInputStatus inputStatus() { return inputStatus.get(); }

    public void attach(InkCanvasViewport viewport, InkInputListener listener) {
        ensureOpen();
        this.viewport = Objects.requireNonNull(viewport, "ink viewport");
        Objects.requireNonNull(listener, "input listener");
        if (attached) provider.detach();
        provider.attach(viewport.inputTarget(), normalized(listener));
        attached = true;
    }

    public void coordinateTransform(InkCoordinateTransform transform) {
        ensureOpen();
        coordinateTransform = Objects.requireNonNullElse(transform, InkCoordinateTransform.IDENTITY);
        provider.resetCoordinateState();
    }

    public void resetInputCoordinates() {
        ensureOpen();
        provider.resetCoordinateState();
    }

    public void detach() {
        ensureOpen();
        if (!attached) return;
        provider.detach();
        attached = false;
    }

    public double zoomTo(double requested) {
        ensureOpen();
        zoom = profile.zoomEnabled() && Double.isFinite(requested)
                ? Math.max(0.1, Math.min(8.0, requested)) : 1.0;
        return zoom;
    }

    public boolean ensureViewportCoverage(double width, double height) {
        ensureOpen();
        if (viewport == null) return false;
        return viewport.ensureCoverage(width, height, zoom);
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
        viewport = null;
        provider.close();
    }

    private InkInputListener normalized(InkInputListener delegate) {
        return new InkInputListener() {
            @Override public void onHover(InkInputSample sample) {
                InkInputSample mapped = normalize(sample);
                if (mapped != null) delegate.onHover(mapped);
            }
            @Override public boolean onStrokeStart(InkInputSample sample) {
                InkInputSample mapped = normalize(sample);
                lastMappedSample = mapped;
                return mapped != null && delegate.onStrokeStart(mapped);
            }
            @Override public boolean onStrokeMove(InkInputSample sample) {
                InkInputSample mapped = normalize(sample);
                if (mapped == null) return false;
                lastMappedSample = mapped;
                boolean consumed = delegate.onStrokeMove(mapped);
                if (viewport.growNear(mapped)) provider.resetCoordinateState();
                return consumed;
            }
            @Override public boolean onStrokeMoveBatch(List<InkInputSample> samples) {
                List<InkInputSample> mapped = samples == null ? List.of() : samples.stream()
                        .map(InkEditorSession.this::normalize).filter(Objects::nonNull).toList();
                if (mapped.isEmpty()) return false;
                lastMappedSample = mapped.get(mapped.size() - 1);
                boolean consumed = delegate.onStrokeMoveBatch(mapped);
                if (viewport.growNear(lastMappedSample)) provider.resetCoordinateState();
                return consumed;
            }
            @Override public boolean onStrokeEnd(InkInputSample sample) {
                InkInputSample mapped = normalize(sample);
                if (mapped == null) mapped = lastMappedSample;
                lastMappedSample = null;
                return mapped != null && delegate.onStrokeEnd(mapped);
            }
        };
    }

    private InkInputSample normalize(InkInputSample sample) {
        InkInputSample transformed = coordinateTransform.transform(Objects.requireNonNull(sample, "input sample"));
        updateInputStatus(transformed);
        return viewport == null ? transformed : viewport.mapInside(transformed).orElse(null);
    }

    private void updateInputStatus(InkInputSample sample) {
        InkInputCapabilities capabilities = provider.capabilities();
        boolean variable = inputStatus.get().pressureVariable();
        if (Double.isFinite(sample.rawPressure()) && Double.isFinite(lastRawPressure)
                && Math.abs(sample.rawPressure() - lastRawPressure) > 0.01) {
            variable = true;
        }
        if (Double.isFinite(sample.rawPressure())) lastRawPressure = sample.rawPressure();
        boolean nativeActive = sample.cursor() == InkInputCursor.PEN
                || sample.cursor() == InkInputCursor.ERASER
                || sample.inputSource().startsWith("LectureStudio")
                || sample.inputSource().startsWith("Windows Pointer");
        InkInputStatus next = new InkInputStatus(capabilities.providerName(), sample.inputSource(), sample.cursor(),
                sample.rawPressure(), sample.pressure(), variable, nativeActive, capabilities.fallbackReason());
        // Position changes belong to the stroke stream, not the status UI.
        if (!next.equals(inputStatus.get())) inputStatus.set(next);
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("ink editor session is closed");
    }
}
