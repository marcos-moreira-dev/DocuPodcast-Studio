package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class NoExcessiveNestedPanelsSourceTest {
    @Test
    void productContractRejectsPanelNestingForItsOwnSake() throws Exception {
        String contract = Files.readString(Path.of("docs/productizacion/CONTRATO_VISUAL_OPERATIVO.md"));
        String roadmap = Files.readString(Path.of("docs/productizacion/PLAN_MAESTRO_TANDAS_49_63.md"));
        String cardsCss = Files.readString(Path.of("src/main/resources/css/components/cards.css"));

        assertTrue(contract.contains("paneles anidados sin necesidad") || contract.contains("panel dentro de panel"));
        assertTrue(roadmap.contains("no paneles anidados sin función") || roadmap.contains("paneles anidados"));
        assertTrue(cardsCss.contains("Avoid nested panels without purpose"));
    }
}
