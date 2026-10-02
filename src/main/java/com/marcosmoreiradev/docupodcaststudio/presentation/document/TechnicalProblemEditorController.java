package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.ink.DrawingProfile;
import com.marcosmoreiradev.docupodcaststudio.ink.InkEditorSession;
import com.marcosmoreiradev.docupodcaststudio.ink.InkStateExporter;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCapabilities;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputListener;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputProvider;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputStatus;
import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkCanvasViewport;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Narrow controller that delegates input, history, zoom, export and lifetime to studio-ink. */
public final class TechnicalProblemEditorController<S> implements AutoCloseable {
    private final InkEditorSession<S> session;

    public TechnicalProblemEditorController(DrawingProfile profile,
                                            InkInputProvider provider,
                                            Supplier<S> snapshot,
                                            Consumer<S> restore,
                                            InkStateExporter<S> exporter) {
        session = new InkEditorSession<>(Objects.requireNonNull(profile, "profile"),
                Objects.requireNonNull(provider, "provider"), snapshot, restore, exporter);
    }

    public InkInputCapabilities inputCapabilities() { return session.inputCapabilities(); }
    public InkInputStatus inputStatus() { return session.inputStatus(); }
    public ReadOnlyObjectProperty<InkInputStatus> inputStatusProperty() { return session.inputStatusProperty(); }
    public void attach(InkCanvasViewport viewport, InkInputListener listener) { session.attach(viewport, listener); }
    public void detach() { session.detach(); }
    public void resetInputCoordinates() { session.resetInputCoordinates(); }
    public double zoomTo(double zoom) { return session.zoomTo(zoom); }
    public boolean ensureViewportCoverage(double width, double height) {
        return session.ensureViewportCoverage(width, height);
    }
    public void checkpoint() { session.checkpoint(); }
    public boolean undo() { return session.undo(); }
    public boolean redo() { return session.redo(); }
    public void restore(S state) { session.restore(state); }
    public Path export(Path destination) throws IOException { return session.export(destination); }
    @Override public void close() { session.close(); }
}
