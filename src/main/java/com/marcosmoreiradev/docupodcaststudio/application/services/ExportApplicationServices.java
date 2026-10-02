package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.export.ExportDiagnosticReportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportPodcastAudioUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportPodcastWavUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportProjectBundleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.InspectExportReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.InspectFinalAudioExportReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectExportFormatPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.video.ExportSimpleVideoPackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.ExportNarrativeVideoUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.RenderFinalVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.InspectFinalVideoSmokeReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.ExportDocumentStudyVideoUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ExportTheatreVideoUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.observability.SupportBundleExporter;

/** Export use cases and policies. */
public record ExportApplicationServices(
        ExportPodcastWavUseCase exportPodcastWav,
        ExportPodcastAudioUseCase exportPodcastAudio,
        ExportDiagnosticReportUseCase exportDiagnosticReport,
        SupportBundleExporter exportSupportBundle,
        ExportProjectBundleUseCase exportProjectBundle,
        InspectExportReadinessUseCase inspectExportReadiness,
        InspectFinalAudioExportReadinessUseCase inspectFinalAudioExportReadiness,
        ExportSimpleVideoPackageUseCase exportSimpleVideoPackage,
        RenderFinalVideoPlanUseCase renderFinalVideoPlan,
        ExportDocumentStudyVideoUseCase exportDocumentStudyVideo,
        ExportNarrativeVideoUseCase exportNarrativeVideo,
        ExportTheatreVideoUseCase exportTheatreVideo,
        InspectFinalVideoSmokeReadinessUseCase inspectFinalVideoSmokeReadiness,
        ProjectExportFormatPolicy exportFormatPolicy
) {
}
