package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.recording.PrepareRecordingActionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.recording.StartAudioRecordingUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.recording.StopAudioRecordingUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.recording.CancelAudioRecordingUseCase;

/** Recording and human-audio preparation use cases. */
public record RecordingApplicationServices(
        PrepareRecordingActionUseCase prepareRecordingAction,
        StartAudioRecordingUseCase startAudioRecording,
        StopAudioRecordingUseCase stopAudioRecording,
        CancelAudioRecordingUseCase cancelAudioRecording
) {
}
