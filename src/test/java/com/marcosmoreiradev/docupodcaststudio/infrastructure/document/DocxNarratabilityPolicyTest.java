package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DocxNarratabilityPolicyTest {
    @Test
    void distinguishesProseEquationsAndDamagedText() {
        assertEquals(DocxNarratabilityPolicy.Decision.NARRATABLE,
                DocxNarratabilityPolicy.assess(
                        "La poda reduce el espacio de búsqueda sin alterar la solución.").decision());
        assertEquals(DocxNarratabilityPolicy.Decision.MATHEMATICAL,
                DocxNarratabilityPolicy.assess("x² + y² = z²").decision());
        assertEquals(DocxNarratabilityPolicy.Decision.MATHEMATICAL,
                DocxNarratabilityPolicy.assess("\\frac{a}{b} = c").decision());
        assertEquals(DocxNarratabilityPolicy.Decision.UNCERTAIN,
                DocxNarratabilityPolicy.assess("Texto \uFFFD dañado").decision());
    }
}
