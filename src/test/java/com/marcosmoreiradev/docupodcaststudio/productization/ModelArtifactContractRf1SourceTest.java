package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** MODEL-ARTIFACT-CONTRACT-RF1: local engine artifacts use one inspectable contract. */
final class ModelArtifactContractRf1SourceTest {
    @Test
    void standardContractsCoverXttsPiperAndFfmpeg() throws Exception {
        String contract = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/ModelArtifactContract.java"));

        assertTrue(contract.contains("xttsAdvancedVoice"));
        assertTrue(contract.contains("piperLocalVoice"));
        assertTrue(contract.contains("ffmpegVideoAudio"));
        assertTrue(contract.contains("model.pth"));
        assertTrue(contract.contains("piper.exe"));
        assertTrue(contract.contains("ffmpeg.exe"));
    }
}
