package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.voice.AssignVoiceToSegmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.CreateDefaultVoiceLibraryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.MaterializeVoiceLibraryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ImportVoiceSampleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.DownloadVoiceReferenceSampleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.DeleteVoiceReferenceSampleUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.BuildVoiceRegistrationWizardPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.BuildVoiceToneRecordingPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.BuildVoiceAssignmentOptionsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.GenerateVoiceTestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ResolveVoiceToneReferenceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ValidateVoiceLibraryUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceCapabilityPolicy;

/** Application services related to voice profiles, characters and styles. */
public record VoiceApplicationServices(
        CreateDefaultVoiceLibraryUseCase createDefaultVoiceLibrary,
        ValidateVoiceLibraryUseCase validateVoiceLibrary,
        AssignVoiceToSegmentUseCase assignVoiceToSegment,
        MaterializeVoiceLibraryUseCase materializeVoiceLibrary,
        ImportVoiceSampleUseCase importVoiceSample,
        DownloadVoiceReferenceSampleUseCase downloadVoiceReferenceSample,
        DeleteVoiceReferenceSampleUseCase deleteVoiceReferenceSample,
        BuildVoiceRegistrationWizardPlanUseCase buildVoiceRegistrationWizardPlan,
        BuildVoiceToneRecordingPlanUseCase buildVoiceToneRecordingPlan,
        BuildVoiceAssignmentOptionsUseCase buildVoiceAssignmentOptions,
        ResolveVoiceToneReferenceUseCase resolveVoiceToneReference,
        GenerateVoiceTestUseCase generateVoiceTest,
        VoiceCapabilityPolicy voiceCapabilityPolicy
) {
}
