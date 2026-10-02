package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.compute.AssessComputeAccelerationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectComputeEnvironmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectXttsCudaSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.PrepareXttsPytorchCudaUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.RunXttsCudaSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.engines.AppStartupEnginePreflightUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.engines.BuildHumanEnginePreflightSummaryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.engines.InspectAiEnginesPreflightUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectPiperSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsSmokeTestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectLocalTheatreImageSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadXttsOfficialModelUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadPiperPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadLocalTheatreImagePackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportXttsModelFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportPiperVoiceFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportLocalTheatreImagePackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxComponentImportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportLocalTheatreImageRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PrepareLocalTheatreImageRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PrepareXttsPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.SelectPiperAsEngineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.SelectXttsAsEngineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.RunXttsReadinessSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ConfirmXttsSmokePlaybackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.AuditEngineArtifactsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.BuildGuiSmokeChecklistUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.ImportFfmpegRuntimeFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.DownloadFfmpegPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.InspectFinalVideoRuntimeStatusUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SaveOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ValidateOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DownloadTesseractPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ImportTesseractRuntimeFolderUseCase;

/** Operational settings use cases. */
public record SettingsApplicationServices(
        LoadOperationalSettingsUseCase loadOperationalSettings,
        SaveOperationalSettingsUseCase saveOperationalSettings,
        ValidateOperationalSettingsUseCase validateOperationalSettings,
        InspectComputeEnvironmentUseCase inspectComputeEnvironment,
        AssessComputeAccelerationUseCase assessComputeAcceleration,
        InspectXttsCudaSmokeUseCase inspectXttsCudaSmoke,
        PrepareXttsPytorchCudaUseCase prepareXttsPytorchCuda,
        RunXttsCudaSmokeUseCase runXttsCudaSmoke,
        InspectAiEnginesPreflightUseCase inspectAiEnginesPreflight,
        AppStartupEnginePreflightUseCase appStartupEnginePreflight,
        BuildHumanEnginePreflightSummaryUseCase buildHumanEnginePreflightSummary,
        InspectXttsSetupReadinessUseCase inspectXttsSetupReadiness,
        InspectXttsSmokeTestUseCase inspectXttsSmokeTest,
        RunXttsReadinessSmokeUseCase runXttsReadinessSmoke,
        ConfirmXttsSmokePlaybackUseCase confirmXttsSmokePlayback,
        PrepareXttsPortableRuntimeUseCase prepareXttsPortableRuntime,
        DownloadXttsOfficialModelUseCase downloadXttsOfficialModel,
        ImportXttsModelFolderUseCase importXttsModelFolder,
        SelectXttsAsEngineUseCase selectXttsAsEngine,
        InspectPiperSetupReadinessUseCase inspectPiperSetupReadiness,
        DownloadPiperPortableRuntimeUseCase downloadPiperPortableRuntime,
        SelectPiperAsEngineUseCase selectPiperAsEngine,
        ImportPiperVoiceFolderUseCase importPiperVoiceFolder,
        ImportFfmpegRuntimeFolderUseCase importFfmpegRuntimeFolder,
        FfmpegRuntimeProbeUseCase inspectFfmpegRuntime,
        InspectFinalVideoRuntimeStatusUseCase inspectFinalVideoRuntimeStatus,
        DownloadFfmpegPortableRuntimeUseCase downloadFfmpegPortableRuntime,
        InspectLocalTheatreImageSetupReadinessUseCase inspectLocalTheatreImageSetupReadiness,
        PrepareLocalTheatreImageRuntimeUseCase prepareLocalTheatreImageRuntime,
        ImportLocalTheatreImageRuntimeUseCase importLocalTheatreImageRuntime,
        DownloadLocalTheatreImagePackageUseCase downloadLocalTheatreImagePackage,
        ImportLocalTheatreImagePackageUseCase importLocalTheatreImagePackage,
        FluxComponentImportUseCase importFluxComponents,
        AuditEngineArtifactsUseCase auditEngineArtifacts,
        BuildGuiSmokeChecklistUseCase buildGuiSmokeChecklist,
        DownloadTesseractPortableRuntimeUseCase downloadTesseractPortableRuntime,
        ImportTesseractRuntimeFolderUseCase importTesseractRuntimeFolder
) {
    public DependencyPreparationService dependencyPreparation() {
        return new DependencyPreparationService(this);
    }
}
