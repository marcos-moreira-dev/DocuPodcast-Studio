package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Optional live sampler progress, scoped to the submitted job. HTTP history remains authoritative. */
final class ComfyUiProgressStream implements WebSocket.Listener, AutoCloseable {
    private final String promptId;
    private final StringBuilder fragments = new StringBuilder();
    private volatile String detail = "Esperando al motor; todavía no hay pasos de generación disponibles.";
    private WebSocket socket;

    ComfyUiProgressStream(String promptId) { this.promptId = promptId; }

    static ComfyUiProgressStream open(HttpClient client, String baseUrl, String clientId, String promptId)
            throws InterruptedException {
        var stream = new ComfyUiProgressStream(promptId);
        var future = client.newWebSocketBuilder().connectTimeout(java.time.Duration.ofSeconds(3))
                .buildAsync(URI.create(baseUrl.replaceFirst("^http", "ws") + "/ws?clientId=" + clientId), stream);
        try { stream.socket = future.get(3, TimeUnit.SECONDS); }
        catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException unavailable) {
            future.thenAccept(WebSocket::abort);
            stream.detail = "Esperando resultado; el motor no ofrece progreso en vivo para esta conexión.";
        } catch (InterruptedException cancelled) {
            future.thenAccept(WebSocket::abort);
            throw cancelled;
        }
        return stream;
    }

    public void onOpen(WebSocket ws) { ws.request(1); }
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        if (fragments.length() + data.length() > 65536) fragments.setLength(0);
        else fragments.append(data);
        if (last) {
            accept(fragments.toString());
            fragments.setLength(0);
        }
        ws.request(1);
        return null;
    }
    void accept(String json) {
        if (!promptId.equals(field(json, "prompt_id"))) return;
        String type = field(json, "type");
        if ("progress".equals(type)) {
            int value = number(json, "value"), max = number(json, "max");
            if (value >= 0 && max > 0 && value <= max)
                detail = "Generando imagen · paso " + value + " de " + max + ".";
        } else if ("executing".equals(type)) {
            detail = "El motor está ejecutando una etapa de la imagen; esperando su progreso.";
        }
    }
    String detail() { return detail; }
    private static String field(String json, String key) {
        var match = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return match.find() ? match.group(1) : "";
    }
    private static int number(String json, String key) {
        var match = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        try { return match.find() ? Integer.parseInt(match.group(1)) : -1; }
        catch (NumberFormatException invalid) { return -1; }
    }
    public void close() { if (socket != null) socket.abort(); }
}
