package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.util.List;

/** Readiness view for Estudio documental export and study actions. */
public record DocumentStudyReadiness(
        boolean documentLoaded,
        boolean readingPrepared,
        boolean audioComplete,
        boolean textAudioVideoExportable,
        List<String> missingRequirements
) {
    public DocumentStudyReadiness {
        missingRequirements = missingRequirements == null ? List.of() : List.copyOf(missingRequirements);
        textAudioVideoExportable = documentLoaded && readingPrepared && audioComplete && missingRequirements.isEmpty();
    }
}
