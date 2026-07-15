package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CSS-TOKENS-HF1 keeps JavaFX CSS tokens resolvable and status-bar hover subtle. */
final class CssTokensHf1SourceTest {
    @Test
    void documentContextCheckboxUsesResolvedInkToken() throws Exception {
        String tokens = read("src/main/resources/css/tokens.css");
        String document = read("src/main/resources/css/document-reader.css");
        assertTrue(document.contains("-fx-text-fill: -dp-ink;"));
        assertTrue(tokens.contains("-dp-ink:"), "-dp-ink debe estar definido para evitar warnings CSS en JavaFX.");
    }

    @Test
    void statusBarButtonsUsePastelHoverNotDarkHover() throws Exception {
        String tokens = read("src/main/resources/css/tokens.css");
        String status = read("src/main/resources/css/statusbar.css");
        assertTrue(tokens.contains("-docu-status-hover-pastel"));
        assertTrue(status.contains(".status-process-button:hover"));
        assertTrue(status.contains(".status-generation-button:hover"));
        assertTrue(status.contains(".reading-zoom-button:hover"));
        assertTrue(status.contains("-fx-background-color: -docu-status-hover-pastel;"));
        assertFalse(status.contains(".status-process-button:hover {\n    -fx-background-color: -docu-bg-hover;"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
