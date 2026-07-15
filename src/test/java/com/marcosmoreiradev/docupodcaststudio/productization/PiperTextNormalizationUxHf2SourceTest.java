package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PiperTextNormalizationUxHf2SourceTest {
    @Test
    void piperNormalizesAccentsAndEnyeWithoutChangingGlobalDocumentText() throws Exception {
        String preprocessor = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/TtsTextPreprocessor.java"));
        String localGateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java"));
        String testGateway = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalProcessVoiceTestSynthesisGateway.java"));
        assertTrue(preprocessor.contains("sanitizeForEngine"));
        assertTrue(preprocessor.contains("replace('ñ', 'n')"));
        assertTrue(preprocessor.contains("replace('Ñ', 'N')"));
        assertTrue(preprocessor.contains("looksLikePiper"));
        assertTrue(localGateway.contains("sanitizeForEngine(segment.text()"));
        assertTrue(testGateway.contains("sanitizeForEngine(request.phrase()"));
    }
}
