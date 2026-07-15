package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegAudioNormalizationGatewayTest {
    @TempDir
    Path tempDir;

    @Test
    void normalizesAudioWithFakeFfmpegAndWritesSidecarEvidence() throws Exception {
        Path fakeFfmpeg = fakeFfmpegScript();
        FfmpegAudioNormalizationGateway gateway = new FfmpegAudioNormalizationGateway(
                new FfmpegToolDiscovery(fakeFfmpeg, null, false, true, "fake ffmpeg listo"));
        Path source = tempDir.resolve("voz.m4a");
        Path target = tempDir.resolve("media").resolve("voz-assignable.wav");
        Files.writeString(source, "audio");

        AudioNormalizationResult result = gateway.normalize(source, target, AudioNormalizationProfile.ASSIGNABLE_AUDIO);

        assertTrue(result.successful());
        assertEquals(AudioNormalizationProfile.ASSIGNABLE_AUDIO, result.profile());
        assertTrue(Files.isRegularFile(target));
        assertTrue(Files.isRegularFile(tempDir.resolve("media").resolve("voz-assignable-ffmpeg-stdout.log")));
        assertTrue(Files.isRegularFile(tempDir.resolve("media").resolve("voz-assignable-ffmpeg-stderr.log")));
        assertTrue(Files.readString(tempDir.resolve("media").resolve("voz-assignable-ffmpeg-normalization.txt")).contains("profile=ASSIGNABLE_AUDIO"));
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
