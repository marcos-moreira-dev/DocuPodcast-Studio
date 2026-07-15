package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.guide.GetGuideTopicUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.guide.GuideCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.guide.SearchGuideTopicsUseCase;

/** Integrated guide use cases. */
public record GuideApplicationServices(
        GuideCatalog catalog,
        GetGuideTopicUseCase getTopic,
        SearchGuideTopicsUseCase searchTopics
) {
}
