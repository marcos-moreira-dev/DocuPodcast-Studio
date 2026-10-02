package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreSpatialActionMapPanelPolicyTest {
    @Test
    void resolvesSpeakerFromTheatreMetadataWhenVisibleTextContainsOnlyDialogue() {
        assertEquals("CAMPESINO_1", TheatreSpatialActionMapPanel.resolveSpeakerName(
                "CAMPESINO_1", "", "", "¿Y en dónde más cabe, ovejita?", false));
    }

    @Test
    void metadataSpeakerWinsOverAStalePlacementSpeaker() {
        assertEquals("CONCHA", TheatreSpatialActionMapPanel.resolveSpeakerName(
                "CONCHA", "CONCHA", "CAMPESINO_1", "Yo quiero la palabra", false));
    }

    @Test
    void speakerCannotBeSelectedAsTheirOwnReceiverByAccident() {
        assertEquals(List.of("CONCHA", "PUBLICO"),
                TheatreSpatialActionMapPanel.withoutSpeaker(
                        List.of("CAMPESINO_1", "CONCHA", "PUBLICO"), "CAMPESINO_1"));
    }
}
