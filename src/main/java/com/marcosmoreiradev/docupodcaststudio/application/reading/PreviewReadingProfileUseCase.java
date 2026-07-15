package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;

import java.util.ArrayList;
import java.util.Objects;

/** Builds a non-mutating preview of how a profile would classify the current document. */
public final class PreviewReadingProfileUseCase {
    private final ApplyReadingProfileUseCase applyReadingProfileUseCase = new ApplyReadingProfileUseCase();

    public ReadingProfilePreview preview(ReadableDocument document, ReadingProfile profile) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(profile, "profile");
        ReadableDocument classified = applyReadingProfileUseCase.apply(document, profile);
        ArrayList<ReadingProfilePreviewItem> items = new ArrayList<>(document.blocks().size());
        for (DocumentBlock original : document.blocks()) {
            DocumentBlock proposed = classified.blockById(original.id()).orElse(original);
            items.add(new ReadingProfilePreviewItem(
                    original.id(),
                    original.preview(90),
                    original.type(),
                    proposed.type(),
                    original.manuallyOverridden()
            ));
        }
        return new ReadingProfilePreview(items);
    }
}
