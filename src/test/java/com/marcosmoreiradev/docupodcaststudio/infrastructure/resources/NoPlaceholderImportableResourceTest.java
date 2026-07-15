package com.marcosmoreiradev.docupodcaststudio.infrastructure.resources;

import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceKind;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NoPlaceholderImportableResourceTest {
    @Test
    void importableResourcesDoNotContainTemplatePlaceholders() throws Exception {
        OfficialAiResourceCatalog catalog = new OfficialAiResourceCatalog();
        for (var descriptor : catalog.descriptors()) {
            if (!descriptor.importable()) {
                continue;
            }
            try (InputStream stream = getClass().getResourceAsStream(descriptor.classpathLocation())) {
                assertNotNull(stream, descriptor.classpathLocation());
                String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertFalse(text.contains("{{"), descriptor.targetRelativePath());
                assertFalse(text.contains("PLACEHOLDER"), descriptor.targetRelativePath());
            }
        }
    }

    @Test
    void templateWithPlaceholdersIsExplicitlyNonImportable() throws Exception {
        OfficialAiResourceCatalog catalog = new OfficialAiResourceCatalog();
        var template = catalog.descriptors().stream()
                .filter(descriptor -> descriptor.kind() == AiResourceKind.AI_TEMPLATE)
                .findFirst()
                .orElseThrow();

        assertFalse(template.importable());
        try (InputStream stream = getClass().getResourceAsStream(template.classpathLocation())) {
            assertNotNull(stream, template.classpathLocation());
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("{{"));
            assertTrue(text.contains("importable: false"));
        }
    }
}
