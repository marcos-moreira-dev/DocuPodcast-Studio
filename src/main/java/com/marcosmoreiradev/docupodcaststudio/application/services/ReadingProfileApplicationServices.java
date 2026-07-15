package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.reading.ApplyReadingProfileUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.CreateDefaultReadingProfileUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.PreviewReadingProfileUseCase;

/** Reading profile use cases for Word-first structure review. */
public record ReadingProfileApplicationServices(
        CreateDefaultReadingProfileUseCase createDefaultProfile,
        ApplyReadingProfileUseCase applyReadingProfile,
        PreviewReadingProfileUseCase previewReadingProfile
) {
}
