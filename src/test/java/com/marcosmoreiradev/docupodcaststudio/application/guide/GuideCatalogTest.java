package com.marcosmoreiradev.docupodcaststudio.application.guide;

import com.marcosmoreiradev.docupodcaststudio.infrastructure.guide.ClasspathGuideCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuideCatalogTest {
    @Test
    void loadsOfficialTopicsAndKeepsWordFirst() {
        GuideCatalog catalog = new ClasspathGuideCatalog();

        assertEquals(11, catalog.topics().size());
        assertEquals(GuideTopicId.GETTING_STARTED, catalog.topics().get(0).id());
        assertEquals(GuideTopicId.IMPORT_WORD_NOTES, catalog.topics().get(1).id());
        assertTrue(catalog.topic(GuideTopicId.IMPORT_WORD_NOTES).orElseThrow().bodyMarkdown().contains("Word/DOCX"));
    }

    @Test
    void searchFindsWordVoiceVisualesAndRetryTopics() {
        GuideCatalog catalog = new ClasspathGuideCatalog();

        assertFalse(catalog.search("word").isEmpty());
        assertFalse(catalog.search("voz autorizada").isEmpty());
        assertFalse(catalog.search("visual").isEmpty());
        assertFalse(catalog.search("reintentar").isEmpty());
    }
}
