package com.marcosmoreiradev.docupodcaststudio.localmedia;

/** Whether DocuPodcast has authority to terminate a local runtime process. */
enum RuntimeOwnership {
    MANAGED_PRIVATE_RUNTIME,
    EXTERNAL_RUNTIME;

    boolean mayTerminate() {
        return this == MANAGED_PRIVATE_RUNTIME;
    }
}
