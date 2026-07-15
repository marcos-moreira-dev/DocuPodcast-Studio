package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;

/** Provides the first Word-first reading profile used by the Document workspace. */
public final class CreateDefaultReadingProfileUseCase {
    public ReadingProfile create() {
        return ReadingProfile.academicDefaults();
    }
}
