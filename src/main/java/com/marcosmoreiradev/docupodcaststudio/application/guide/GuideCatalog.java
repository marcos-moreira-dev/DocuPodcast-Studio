package com.marcosmoreiradev.docupodcaststudio.application.guide;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Read-only catalog of integrated guide topics. */
public interface GuideCatalog {
    List<GuideTopic> topics();

    default Optional<GuideTopic> topic(GuideTopicId id) {
        return topics().stream().filter(topic -> topic.id() == id).findFirst();
    }

    default List<GuideSearchResult> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        return topics().stream()
                .map(topic -> new GuideSearchResult(topic, score(topic, normalizedQuery)))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static int score(GuideTopic topic, String query) {
        int score = 0;
        if (normalize(topic.title()).contains(query)) {
            score += 12;
        }
        if (normalize(topic.category()).contains(query)) {
            score += 5;
        }
        if (normalize(topic.summary()).contains(query)) {
            score += 4;
        }
        if (normalize(topic.bodyMarkdown()).contains(query)) {
            score += 2;
        }
        return score;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccents.toLowerCase(Locale.ROOT).trim();
    }
}
