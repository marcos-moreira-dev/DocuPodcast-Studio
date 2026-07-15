package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.audio.CancelAudioGenerationJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.DeletePersistedAudioJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListAudioGenerationJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.InspectAudioJobMaintenanceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListPersistedAudioJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.GetAudioEngineDescriptorUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListAudioEngineAvailabilityUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.InspectAudioEngineReadinessUiUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.LoadPersistedAudioJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ListAudioProcessDiagnosticsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ManualAudioSegmentJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ResumePersistedAudioJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.SubmitAudioGenerationJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.BuildAudioVoiceProductionProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.audio.RegenerateTheatreInterventionAudioUseCase;

/** Audio job use cases. */
public record AudioApplicationServices(
        SubmitAudioGenerationJobUseCase submitAudioGenerationJob,
        ResumePersistedAudioJobUseCase resumePersistedAudioJob,
        CancelAudioGenerationJobUseCase cancelAudioGenerationJob,
        ListAudioGenerationJobsUseCase listAudioGenerationJobs,
        ListPersistedAudioJobsUseCase listPersistedAudioJobs,
        LoadPersistedAudioJobUseCase loadPersistedAudioJob,
        DeletePersistedAudioJobsUseCase deletePersistedAudioJobs,
        GetAudioEngineDescriptorUseCase getAudioEngineDescriptor,
        ListAudioEngineAvailabilityUseCase listAudioEngineAvailability,
        InspectAudioEngineReadinessUiUseCase inspectAudioEngineReadinessUi,
        ListAudioProcessDiagnosticsUseCase listAudioProcessDiagnostics,
        InspectAudioJobMaintenanceUseCase inspectAudioJobMaintenance,
        ManualAudioSegmentJobUseCase manualAudioSegmentJob,
        BuildAudioVoiceProductionProjectionUseCase buildAudioVoiceProductionProjection,
        RegenerateTheatreInterventionAudioUseCase regenerateTheatreInterventionAudio
) {
    public AudioApplicationServices(
            SubmitAudioGenerationJobUseCase submitAudioGenerationJob,
            ResumePersistedAudioJobUseCase resumePersistedAudioJob,
            CancelAudioGenerationJobUseCase cancelAudioGenerationJob,
            ListAudioGenerationJobsUseCase listAudioGenerationJobs,
            ListPersistedAudioJobsUseCase listPersistedAudioJobs,
            LoadPersistedAudioJobUseCase loadPersistedAudioJob,
            DeletePersistedAudioJobsUseCase deletePersistedAudioJobs,
            GetAudioEngineDescriptorUseCase getAudioEngineDescriptor,
            ListAudioEngineAvailabilityUseCase listAudioEngineAvailability,
            InspectAudioEngineReadinessUiUseCase inspectAudioEngineReadinessUi,
            ListAudioProcessDiagnosticsUseCase listAudioProcessDiagnostics,
            InspectAudioJobMaintenanceUseCase inspectAudioJobMaintenance,
            ManualAudioSegmentJobUseCase manualAudioSegmentJob) {
        this(submitAudioGenerationJob, resumePersistedAudioJob, cancelAudioGenerationJob, listAudioGenerationJobs,
                listPersistedAudioJobs, loadPersistedAudioJob, deletePersistedAudioJobs, getAudioEngineDescriptor,
                listAudioEngineAvailability, inspectAudioEngineReadinessUi, listAudioProcessDiagnostics,
                inspectAudioJobMaintenance, manualAudioSegmentJob, new BuildAudioVoiceProductionProjectionUseCase(),
                new RegenerateTheatreInterventionAudioUseCase(submitAudioGenerationJob, loadPersistedAudioJob, manualAudioSegmentJob));
    }
}
