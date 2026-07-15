package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;

import java.util.Arrays;
import java.util.List;

/** Registry of process families that should converge on the common T94 job contract. */
public final class LongRunningProcessJobRegistry {
    public List<ProcessJobKind> productProcessKinds() {
        return List.of(
                ProcessJobKind.TTS_AUDIO,
                ProcessJobKind.MEDIA_PREPARATION,
                ProcessJobKind.ENGINE_SETUP,
                ProcessJobKind.VIDEO_RENDER,
                ProcessJobKind.MANAGED_DOWNLOAD,
                ProcessJobKind.VISUAL_GENERATION,
                ProcessJobKind.FINAL_EXPORT
        );
    }

    public List<ProcessJobKind> persistentKindsAvailableNow() {
        return productProcessKinds().stream()
                .filter(ProcessJobKind::currentlyBackedByPersistentJobs)
                .toList();
    }

    public boolean known(ProcessJobKind kind) {
        return kind != null && Arrays.asList(ProcessJobKind.values()).contains(kind);
    }
}
