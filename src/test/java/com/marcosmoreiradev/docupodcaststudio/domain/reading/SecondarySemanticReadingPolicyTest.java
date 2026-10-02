package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SecondarySemanticReadingPolicyTest {
    @Test
    void exposesExactlyTheFourProductChoices() {
        assertEquals(List.of(
                        "Omitir todo tipo de componentes",
                        "Interpretar todo (sin descripciones largas)",
                        "Solo interpretar cuadros y ecuaciones",
                        "Solo interpretar imágenes y extras"),
                Arrays.stream(SecondarySemanticReadingPolicy.values())
                        .map(SecondarySemanticReadingPolicy::displayName)
                        .toList());
    }

    @Test
    void filtersAfterClassification() {
        assertTrue(SecondarySemanticReadingPolicy.TABLES_AND_EQUATIONS
                .includes(SecondarySemanticComponentKind.TABLE));
        assertTrue(SecondarySemanticReadingPolicy.TABLES_AND_EQUATIONS
                .includes(SecondarySemanticComponentKind.EQUATION));
        assertFalse(SecondarySemanticReadingPolicy.TABLES_AND_EQUATIONS
                .includes(SecondarySemanticComponentKind.IMAGE));
        assertTrue(SecondarySemanticReadingPolicy.IMAGES_AND_EXTRAS
                .includes(SecondarySemanticComponentKind.EXTRA));
        assertFalse(SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF
                .includes(SecondarySemanticComponentKind.NONE));
    }
}
