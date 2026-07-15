package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;
import java.time.Duration;

/** Local ComfyUI endpoint configuration. DocuPodcast sends HTTP requests only. */
public record ComfyUiConnectionSettings(String baseUrl, Duration timeout, Path outputDirectory) {
    public ComfyUiConnectionSettings {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:8188" : baseUrl.strip();
        timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
    }

    public static ComfyUiConnectionSettings defaults(Path outputDirectory) {
        return new ComfyUiConnectionSettings("http://127.0.0.1:8188", Duration.ofSeconds(20), outputDirectory);
    }
}
