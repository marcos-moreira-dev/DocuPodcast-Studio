package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppCommandDispatcherTest {
    @Test
    void dispatchesRegisteredHandlerOnlyOnce() {
        AtomicInteger calls = new AtomicInteger();
        AppCommandDispatcher dispatcher = AppCommandDispatcher.emptyOfficial()
                .register(AppCommandId.OPEN_SOURCE_DOCUMENT, calls::incrementAndGet);

        AppCommandDispatchResult result = dispatcher.dispatch(AppCommandId.OPEN_SOURCE_DOCUMENT);

        assertTrue(result.dispatched());
        assertEquals(1, calls.get());
    }

    @Test
    void reportsMissingHandlerForRegisteredCommand() {
        AppCommandDispatcher dispatcher = AppCommandDispatcher.emptyOfficial();

        AppCommandDispatchResult result = dispatcher.dispatch(AppCommandId.LISTEN_DOCUMENT);

        assertFalse(result.dispatched());
        assertTrue(result.message().contains("handler"));
    }
}
