package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class EmbeddedFfmpegContractSourceTest {
    @Test
    void embeddedFfmpegAnd2kVideoContractAreDocumented() throws Exception {
        String contract = read("docs/productizacion/CONTRATO_FFMPEG_EMBEBIDO_VIDEO_2K.md");
        assertTrue(contract.contains("tools/ffmpeg"));
        assertTrue(contract.contains("ffmpeg.exe"));
        assertTrue(contract.contains("ffprobe.exe"));
        assertTrue(contract.contains("mínimo 2K"));
        assertTrue(contract.contains("2560x1440"));
        assertTrue(contract.contains("No se debe exigir al usuario modificar `PATH`"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
