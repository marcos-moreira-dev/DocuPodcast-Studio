package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyTextVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyVideoAudioOverlayPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildStudyProblemsProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemPdfExporter;

/** Estudio documental projections and simple text+audio video planning. */
public record DocumentStudyApplicationServices(
        BuildDocumentStudyProjectionUseCase buildDocumentStudyProjection,
        BuildStudyProblemsProjectionUseCase buildStudyProblemsProjection,
        BuildDocumentStudyTextVideoPlanUseCase buildDocumentStudyTextVideoPlan,
        BuildDocumentStudyVideoPlanUseCase buildDocumentStudyVideoPlan,
        BuildDocumentStudyVideoAudioOverlayPlanUseCase buildDocumentStudyVideoAudioOverlayPlan,
        StudyProblemPdfExporter studyProblemPdfExporter,
        com.marcosmoreiradev.docupodcaststudio.application.documentstudy.PrepareDocumentIllustrationsUseCase prepareIllustrations
) {
    public DocumentStudyApplicationServices(BuildDocumentStudyProjectionUseCase a, BuildStudyProblemsProjectionUseCase b,
            BuildDocumentStudyTextVideoPlanUseCase c, BuildDocumentStudyVideoPlanUseCase d,
            BuildDocumentStudyVideoAudioOverlayPlanUseCase e, StudyProblemPdfExporter f) {
        this(a, b, c, d, e, f, null);
    }
    public DocumentStudyApplicationServices(
            BuildDocumentStudyProjectionUseCase buildDocumentStudyProjection,
            BuildStudyProblemsProjectionUseCase buildStudyProblemsProjection,
            BuildDocumentStudyTextVideoPlanUseCase buildDocumentStudyTextVideoPlan,
            StudyProblemPdfExporter studyProblemPdfExporter) {
        this(buildDocumentStudyProjection, buildStudyProblemsProjection, buildDocumentStudyTextVideoPlan,
                new BuildDocumentStudyVideoPlanUseCase(),
                new BuildDocumentStudyVideoAudioOverlayPlanUseCase(), studyProblemPdfExporter);
    }
}
