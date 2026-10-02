package com.marcosmoreiradev.docupodcaststudio.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineAdministration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineAdministrationRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class MediaEnginePlatformShutdownTest {
    @Test
    void globalShutdownClosesOwnedAdministrationRuntime() {
        AtomicBoolean closed = new AtomicBoolean();
        EngineAdministrationRegistry administration = new EngineAdministrationRegistry()
                .register(new CloseableAdministration(closed));
        MediaEnginePlatform platform = new MediaEnginePlatform(
                null, null, null, null, null, null, null, administration);

        platform.close();

        assertTrue(closed.get());
    }

    private static final class CloseableAdministration
            implements EngineAdministration, AutoCloseable {
        private final AtomicBoolean closed;

        private CloseableAdministration(AtomicBoolean closed) {
            this.closed = closed;
        }

        @Override public EngineId engineId() { return new EngineId("shutdown-test"); }
        @Override public List<EngineActionDescriptor> actions() { return List.of(); }
        @Override public EngineActionResult execute(EngineActionRequest request,
                                                    ExecutionContext context) {
            throw new UnsupportedOperationException();
        }
        @Override public void close() { closed.set(true); }
    }
}
