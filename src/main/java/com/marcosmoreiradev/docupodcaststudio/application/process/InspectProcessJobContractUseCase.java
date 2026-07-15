package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;

import java.util.List;

/** Reports the current convergence state of long-running local processes. */
public final class InspectProcessJobContractUseCase {
    private final LongRunningProcessJobRegistry registry;

    public InspectProcessJobContractUseCase() {
        this(new LongRunningProcessJobRegistry());
    }

    public InspectProcessJobContractUseCase(LongRunningProcessJobRegistry registry) {
        this.registry = registry;
    }

    public ProcessJobContractReport inspect() {
        List<ProcessJobKind> all = registry.productProcessKinds();
        List<ProcessJobKind> persistentNow = registry.persistentKindsAvailableNow();
        List<ProcessJobKind> pending = all.stream()
                .filter(kind -> !kind.currentlyBackedByPersistentJobs())
                .toList();
        return new ProcessJobContractReport(all, persistentNow, pending);
    }
}
