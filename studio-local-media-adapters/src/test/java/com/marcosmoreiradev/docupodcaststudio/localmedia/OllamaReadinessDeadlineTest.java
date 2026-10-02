package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class OllamaReadinessDeadlineTest {
    @Test void acceptedConnectionWithoutHeadersEndsInsideTimeout() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket server = new ServerSocket(0)) {
            executor.submit(() -> { try (var ignored = server.accept()) {
                Thread.sleep(5_000L);
            } return null; });
            long started = System.nanoTime();
            boolean ready = new ManagedOllamaProcess.JavaHttpEndpointClient().ready(
                    URI.create("http://127.0.0.1:" + server.getLocalPort()),
                    Duration.ofMillis(250), CancellationToken.NONE);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertFalse(ready);
            assertTrue(elapsed < 1_500L, "elapsed=" + elapsed);
        }
    }

    @Test void cancellationAbortsReadinessBeforePost() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor();
             ServerSocket server = new ServerSocket(0)) {
            executor.submit(() -> { try (var ignored = server.accept()) {
                Thread.sleep(5_000L);
            } return null; });
            AtomicBoolean cancelled = new AtomicBoolean();
            executor.submit(() -> { Thread.sleep(120L); cancelled.set(true); return null; });
            assertThrows(InterruptedException.class, () ->
                    new ManagedOllamaProcess.JavaHttpEndpointClient().ready(
                            URI.create("http://127.0.0.1:" + server.getLocalPort()),
                            Duration.ofSeconds(5), cancelled::get));
        }
    }
}
