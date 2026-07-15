package com.marcosmoreiradev.docupodcaststudio.infrastructure.resources;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Guardrail: official AI resources are document helpers, not importable script contracts. */
final class AiResourcesImportabilityTest {
    @Test
    void officialCatalogDoesNotExposeImportableMarkdownContracts() {
        OfficialAiResourceCatalog catalog = new OfficialAiResourceCatalog();

        long importable = catalog.descriptors().stream().filter(descriptor -> descriptor.importable()).count();

        assertEquals(0L, importable);
        assertFalse(catalog.descriptors().stream().anyMatch(descriptor -> descriptor.contract().equals("docupodcast-script-v1")));
        assertFalse(catalog.descriptors().stream()
                .filter(descriptor -> descriptor.kind().name().contains("TEMPLATE"))
                .anyMatch(descriptor -> descriptor.importable()));
    }

    @Test
    void officialResourcesDoNotDeclareLegacyScriptContract() throws Exception {
        OfficialAiResourceCatalog catalog = new OfficialAiResourceCatalog();
        for (var descriptor : catalog.descriptors()) {
            try (InputStream stream = getClass().getResourceAsStream(descriptor.classpathLocation())) {
                assertNotNull(stream, descriptor.classpathLocation());
                String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertFalse(text.contains("docupodcast-script-v1"), descriptor.targetRelativePath());
                assertFalse(text.contains("importable: true"), descriptor.targetRelativePath());
            }
        }
    }
}
