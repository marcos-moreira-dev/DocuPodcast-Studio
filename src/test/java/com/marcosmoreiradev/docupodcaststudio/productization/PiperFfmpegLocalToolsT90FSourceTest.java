package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PiperFfmpegLocalToolsT90FSourceTest {
    @Test
    void t90fAddsRepoLocalPiperFfmpegPreflightWithoutPathFallback() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/engines/InspectLocalVoiceMediaToolsUseCase.java"));
        String script = Files.readString(Path.of("scripts/tts/preflight-piper-ffmpeg.ps1"));
        String launcher = Files.readString(Path.of("scripts/24-verificar-piper-ffmpeg-local.bat"));
        String doc = Files.readString(Path.of("docs/productizacion/T90F_PIPER_FFMPEG_AUTOCONTENIDOS.md"));

        assertTrue(useCase.contains("tools/piper/piper.exe"));
        assertTrue(useCase.contains("models/tts/piper/voices"));
        assertTrue(useCase.contains("tools/ffmpeg/bin/ffmpeg.exe"));
        assertTrue(useCase.contains("No se usa PATH"));
        assertTrue(script.contains("T90F_PIPER_FFMPEG_PREFLIGHT_REPORT.md"));
        assertTrue(script.contains("No se usa PATH ni instalaciones globales"));
        assertTrue(launcher.contains("preflight-piper-ffmpeg.ps1"));
        assertTrue(doc.contains("Piper"));
        assertTrue(doc.contains("FFmpeg"));
        assertFalse(script.contains("Get-Command ffmpeg"));
        assertFalse(script.contains("where.exe ffmpeg"));
    }
}
