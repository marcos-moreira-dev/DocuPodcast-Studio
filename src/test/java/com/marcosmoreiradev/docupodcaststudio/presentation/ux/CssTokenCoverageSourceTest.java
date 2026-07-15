package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CssTokenCoverageSourceTest {
    @Test
    void cssModulesAreFedBySharedTokensAndModernAssembler() throws Exception {
        String assembler = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));
        String tokens = Files.readString(Path.of("src/main/resources/css/tokens.css"));
        String cards = Files.readString(Path.of("src/main/resources/css/components/cards.css"));
        String actions = Files.readString(Path.of("src/main/resources/css/components/actions.css"));
        String mediaRail = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"));

        assertTrue(assembler.contains("tokens.css"));
        assertTrue(assembler.contains("components/actions.css"));
        assertTrue(assembler.contains("components/cards.css"));
        assertTrue(assembler.contains("components/media-rail.css"));
        assertTrue(assembler.contains("Tanda 50 — hoja ensambladora"));
        assertTrue(tokens.contains("-docu-bg-app"));
        assertTrue(tokens.contains("-docu-bg-card"));
        assertTrue(tokens.contains("-docu-text-muted"));
        assertTrue(cards.contains("-docu-shadow-soft"));
        assertTrue(actions.contains("-docu-accent"));
        assertTrue(mediaRail.contains("compact modern rail"));
    }
}
