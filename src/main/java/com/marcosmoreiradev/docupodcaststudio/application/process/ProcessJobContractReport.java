package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;

import java.util.List;

/** Snapshot of how much of the T94 process-job contract is backed by persistent jobs. */
public record ProcessJobContractReport(
        List<ProcessJobKind> trackedKinds,
        List<ProcessJobKind> persistentKindsAvailableNow,
        List<ProcessJobKind> pendingPersistentKinds
) {
    public ProcessJobContractReport {
        trackedKinds = trackedKinds == null ? List.of() : List.copyOf(trackedKinds);
        persistentKindsAvailableNow = persistentKindsAvailableNow == null ? List.of() : List.copyOf(persistentKindsAvailableNow);
        pendingPersistentKinds = pendingPersistentKinds == null ? List.of() : List.copyOf(pendingPersistentKinds);
    }

    public boolean audioGenerationIsUnified() {
        return persistentKindsAvailableNow.contains(ProcessJobKind.TTS_AUDIO);
    }

    public boolean fullyUnified() {
        return pendingPersistentKinds.isEmpty();
    }

    public String summary() {
        return "Procesos rastreados: " + trackedKinds.size()
                + "; persistentes ahora: " + persistentKindsAvailableNow.size()
                + "; pendientes: " + pendingPersistentKinds.size();
    }
}
