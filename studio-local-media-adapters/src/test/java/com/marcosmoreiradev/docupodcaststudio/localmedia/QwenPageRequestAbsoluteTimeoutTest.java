package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QwenPageRequestAbsoluteTimeoutTest {
    @Test
    void textOnlyRequestPreservesTheOperationSpecificTimeout() {
        Duration deadline = Duration.ofMinutes(5);
        var request = QwenVisualAnalysisEngine.chatRequest(
                URI.create("http://127.0.0.1:11434/api/chat"), "{}", false,
                deadline);
        assertEquals(deadline, request.timeout().orElseThrow());
    }

    @Test
    void pageRequestTimesOutWhileBackendWithholdsHeaders() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CountDownLatch accepted = new CountDownLatch(1);
            Thread backend = Thread.ofVirtual().start(() -> {
                try (var socket = server.accept()) {
                    accepted.countDown();
                    // Read nothing and send no HTTP status/headers. The client
                    // must still reach the complete-request deadline.
                    Thread.sleep(Duration.ofSeconds(2));
                } catch (IOException | InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
            Duration deadline = Duration.ofMillis(250);
            var request = QwenVisualAnalysisEngine.chatRequest(
                    URI.create("http://127.0.0.1:" + server.getLocalPort()
                            + "/api/chat"), "{}", true, deadline);

            long started = System.nanoTime();
            assertThrows(HttpTimeoutException.class, () -> HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString()));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - started);

            assertTrue(accepted.await(1, TimeUnit.SECONDS));
            assertEquals(deadline, request.timeout().orElseThrow());
            assertTrue(elapsedMs < 1500, "timeout previo a headers no fue efectivo");
            backend.interrupt();
        }
    }
}
