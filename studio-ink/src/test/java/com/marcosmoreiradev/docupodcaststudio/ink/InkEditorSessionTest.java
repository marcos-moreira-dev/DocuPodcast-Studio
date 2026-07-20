package com.marcosmoreiradev.docupodcaststudio.ink;

import com.marcosmoreiradev.docupodcaststudio.ink.input.*;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class InkEditorSessionTest {
    @TempDir Path tempDir;

    @Test
    void normalizesCoordinatesAndPreservesPressure() {
        FakeProvider provider = new FakeProvider();
        AtomicReference<InkInputSample> received = new AtomicReference<>();
        try (InkEditorSession<String> session = session(provider, new AtomicReference<>("a"))) {
            session.coordinateTransform(sample -> new InkInputSample(sample.x() * 2, sample.y() - 20,
                    sample.nanos(), sample.pressure(), sample.cursor(), sample.primaryButtonDown(),
                    sample.eraserButton(), sample.rawPressure(), sample.inputSource()));
            session.attach(new Group(), listener(received));
            provider.listener.onStrokeMove(new InkInputSample(80, 10, 1, .42,
                    InkInputCursor.PEN, true, false, .73, "stylus"));
            assertEquals(100, received.get().x());
            assertEquals(0, received.get().y());
            assertEquals(.42, received.get().pressure());
            assertEquals(.73, received.get().rawPressure());
        }
    }

    @Test
    void boundsHistoryAndRestoresUndoRedo() {
        FakeProvider provider = new FakeProvider();
        AtomicReference<String> state = new AtomicReference<>("one");
        try (InkEditorSession<String> session = session(provider, state)) {
            session.checkpoint();
            state.set("two");
            session.checkpoint();
            state.set("three");
            assertTrue(session.undo());
            assertEquals("two", state.get());
            assertTrue(session.redo());
            assertEquals("three", state.get());
            session.restore("restored");
            assertFalse(session.canUndo());
            assertEquals("restored", state.get());
        }
    }

    @Test
    void exportsAndClosesProviderExactlyOnce() throws Exception {
        FakeProvider provider = new FakeProvider();
        AtomicReference<String> state = new AtomicReference<>("ink-state");
        Path output = tempDir.resolve("ink.txt");
        InkEditorSession<String> session = session(provider, state);
        assertEquals(output, session.export(output));
        assertEquals("ink-state", Files.readString(output));
        session.close();
        session.close();
        assertTrue(session.closed());
        assertEquals(1, provider.closed.get());
    }

    private static InkEditorSession<String> session(FakeProvider provider, AtomicReference<String> state) {
        DrawingProfile profile = new DrawingProfile("test", "Test", ViewportMode.FIXED,
                100, 80, InkInputPolicy.MOUSE_AND_NATIVE, List.of(DrawingToolId.PEN), 2, true,
                new DrawingExportProfile(1, false, true));
        return new InkEditorSession<>(profile, provider, state::get, state::set,
                (value, destination, exportProfile) -> {
                    Files.writeString(destination, value);
                    return destination;
                });
    }

    private static InkInputListener listener(AtomicReference<InkInputSample> received) {
        return new InkInputListener() {
            @Override public boolean onStrokeStart(InkInputSample sample) { received.set(sample); return true; }
            @Override public boolean onStrokeMove(InkInputSample sample) { received.set(sample); return true; }
            @Override public boolean onStrokeEnd(InkInputSample sample) { received.set(sample); return true; }
        };
    }

    private static final class FakeProvider implements InkInputProvider {
        private final AtomicInteger closed = new AtomicInteger();
        private InkInputListener listener;
        @Override public InkInputCapabilities capabilities() { return InkInputCapabilities.windowsPointer(); }
        @Override public void attach(javafx.scene.Node target, InkInputListener listener) { this.listener = listener; }
        @Override public void detach() { }
        @Override public void close() { closed.incrementAndGet(); }
    }
}
