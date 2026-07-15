package com.marcosmoreiradev.docupodcaststudio.application.resources;

import java.util.List;

/** Read-only catalog of official DocuPodcast AI resources. */
public interface AiResourceCatalog {
    List<AiResourceDescriptor> descriptors();
}
