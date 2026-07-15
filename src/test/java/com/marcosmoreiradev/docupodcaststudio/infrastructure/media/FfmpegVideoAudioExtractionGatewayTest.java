package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.application.media.VideoAudioExtractionResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegVideoAudioExtractionGatewayTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsAudioWithFakeFfmpegAndWritesSidecarEvidence() throws Exception {
        Path fakeFfmpeg = fakeFfmpegScript();
        FfmpegVideoAudioExtractionGateway gateway = new FfmpegVideoAudioExtractionGateway(
                new FfmpegToolDiscovery(fakeFfmpeg, null, false, true, "fake ffmpeg listo"));
        Path source = tempDir.resolve("escena.mp4");
        Path target = tempDir.resolve("media").resolve("escena.wav");
        Files.writeString(source, "video");

        VideoAudioExtractionResult result = gateway.extractAudio(source, target);

        assertTrue(result.successful());
        assertTrue(Files.isRegularFile(target));
        assertTrue(Files.isRegularFile(tempDir.resolve("media").resolve("escena-ffmpeg-stdout.log")));
        assertTrue(Files.isRegularFile(tempDir.resolve("media").resolve("escena-ffmpeg-stderr.log")));
        assertTrue(Files.readString(tempDir.resolve("media").resolve("escena-ffmpeg-extraction.txt")).contains("profile=ASSIGNABLE_AUDIO"));
    }

    private Path fakeFfmpegScript() throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path script = tempDir.resolve(windows ? "fake-ffmpeg.cmd" : "fake-ffmpeg.sh");
        if (windows) {
            Files.writeString(script, """
                    @echo off
                    set LAST=
                    :loop
                    if "%~1"=="" goto done
                    set "LAST=%~1"
                    shift
                    goto loop
                    :done
                    echo fake stdout
                    echo fake stderr 1>&2
                    > "%LAST%" echo wav
                    exit /b 0
                    """, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            Files.writeString(script, """
                    #!/bin/sh
                    last=""
                    for arg in "$@"; do last="$arg"; done
                    echo fake stdout
                    echo fake stderr >&2
                    printf wav > "$last"
                    """, java.nio.charset.StandardCharsets.UTF_8);
            script.toFile().setExecutable(true);
        }
        return script;
    }
}
