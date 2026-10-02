package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreSpatialOverlayLegendTest {
    @Test
    void distributesDistinctCastNamesAcrossRequestedRows() {
        List<List<String>> rows = TheatreSpatialOverlayLegend.rows(
                List.of("CONCHA", "ELOY", "CONCHA", "LADRÓN", "PUEBLO"), 2);

        assertEquals(List.of("CONCHA", "LADRÓN"), rows.get(0));
        assertEquals(List.of("ELOY", "PUEBLO"), rows.get(1));
    }
}
