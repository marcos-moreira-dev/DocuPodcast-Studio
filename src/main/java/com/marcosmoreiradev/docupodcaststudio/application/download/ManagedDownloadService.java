package com.marcosmoreiradev.docupodcaststudio.application.download;

import java.io.IOException;
import java.util.function.Consumer;

/** Transversal service for engine/tool downloads. Implementations live in infrastructure. */
public interface ManagedDownloadService {
    DownloadResult download(DownloadRequest request, Consumer<DownloadProgress> progressConsumer) throws IOException;
}
