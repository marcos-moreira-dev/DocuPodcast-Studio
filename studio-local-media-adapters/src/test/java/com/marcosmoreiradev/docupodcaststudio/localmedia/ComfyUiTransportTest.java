package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ComfyUiTransportTest {
    @Test
    void preservesPromptErrorBodyAndExplainsTheFailingFluxResource() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            byte[] body = """
                    {"error":{"type":"prompt_outputs_failed_validation",
                    "message":"Prompt outputs failed validation"},
                    "node_errors":{"12":{"class_type":"DualCLIPLoader",
                    "errors":[{"message":"[WinError 3] text_encoders path from old-copy"}]}}}
                    """.getBytes(StandardCharsets.UTF_8);
            CompletableFuture<Void> response = CompletableFuture.runAsync(() -> respondOnce(server, body));
            ComfyUiTransport transport = new ComfyUiTransport(
                    "http://127.0.0.1:" + server.getLocalPort());
            IOException failure = assertThrows(IOException.class, () ->
                    transport.execute("{}", ExecutionContext.defaults("transport-test"), "imagen"));
            response.join();
            assertAll(
                    () -> assertTrue(failure.getMessage().contains("codificadores de texto")),
                    () -> assertTrue(failure.getMessage().contains("HTTP 400")),
                    () -> assertTrue(failure.getMessage().contains("DualCLIPLoader")),
                    () -> assertTrue(failure.getMessage().contains("old-copy")));
        }
    }

    private static void respondOnce(ServerSocket server, byte[] body) {
        try (Socket socket = server.accept()) {
            var input = new java.io.BufferedReader(new java.io.InputStreamReader(
                    socket.getInputStream(), StandardCharsets.US_ASCII));
            String line;
            while ((line = input.readLine()) != null && !line.isEmpty()) {
                // Consume the complete request header before responding.
            }
            var output = socket.getOutputStream();
            output.write(("HTTP/1.1 400 Bad Request\r\nContent-Type: application/json; charset=utf-8\r\n"
                    + "Content-Length: " + body.length + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            output.write(body);
            output.flush();
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }
}
