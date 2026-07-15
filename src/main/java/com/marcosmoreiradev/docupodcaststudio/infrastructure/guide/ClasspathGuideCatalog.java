package com.marcosmoreiradev.docupodcaststudio.infrastructure.guide;

import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideTopic;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideTopicId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads the integrated DocuPodcast guide from classpath Markdown resources. */
public final class ClasspathGuideCatalog implements GuideCatalog {
    private final List<GuideTopic> topics;

    public ClasspathGuideCatalog() {
        this.topics = List.copyOf(loadTopics());
    }

    @Override
    public List<GuideTopic> topics() {
        return topics;
    }

    private List<GuideTopic> loadTopics() {
        List<TopicResource> resources = List.of(
                new TopicResource(GuideTopicId.GETTING_STARTED, "Primeros pasos", "/help/topics/getting-started.md"),
                new TopicResource(GuideTopicId.IMPORT_WORD_NOTES, "Word/DOCX", "/help/topics/import-word-notes.md"),
                new TopicResource(GuideTopicId.READING_PROFILE, "Documento", "/help/topics/reading-profile.md"),
                new TopicResource(GuideTopicId.VOICES_CHARACTERS_STYLES, "Voces", "/help/topics/voices-characters-styles.md"),
                new TopicResource(GuideTopicId.AUTHORIZED_VOICES, "Voces", "/help/topics/authorized-voices.md"),
                new TopicResource(GuideTopicId.STORYBOARD_LIVE, "Visuales", "/help/topics/storyboard-live.md"),
                new TopicResource(GuideTopicId.AUDIO_GENERATION, "Audio", "/help/topics/audio-generation.md"),
                new TopicResource(GuideTopicId.PLAYBACK_SYNC, "Reproducción", "/help/topics/playback-sync.md"),
                new TopicResource(GuideTopicId.EXPORTING, "Exportación", "/help/topics/exporting.md"),
                new TopicResource(GuideTopicId.TROUBLESHOOTING, "Diagnóstico", "/help/topics/troubleshooting.md"),
                new TopicResource(GuideTopicId.GLOSSARY, "Glosario", "/help/topics/glossary.md")
        );
        List<GuideTopic> loaded = new ArrayList<>();
        for (TopicResource resource : resources) {
            loaded.add(load(resource));
        }
        return loaded;
    }

    private GuideTopic load(TopicResource resource) {
        try (InputStream stream = ClasspathGuideCatalog.class.getResourceAsStream(resource.path())) {
            if (stream == null) {
                throw new IllegalStateException("Missing guide topic resource: " + resource.path());
            }
            String markdown = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new GuideTopic(resource.id(), resource.category(), titleFrom(markdown, resource.id().name()), summaryFrom(markdown), markdown);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load guide topic: " + resource.path(), ex);
        }
    }

    private static String titleFrom(String markdown, String fallback) {
        return markdown.lines()
                .filter(line -> line.startsWith("# "))
                .map(line -> line.substring(2).trim())
                .findFirst()
                .orElse(fallback);
    }

    private static String summaryFrom(String markdown) {
        return markdown.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.startsWith("-"))
                .findFirst()
                .orElse("");
    }

    private record TopicResource(GuideTopicId id, String category, String path) {
    }
}
