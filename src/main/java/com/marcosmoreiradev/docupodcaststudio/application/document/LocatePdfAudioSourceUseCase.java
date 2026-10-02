package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;

import java.util.Optional;

/** Resolves persisted audio provenance to a visual page and region selection. */
public final class LocatePdfAudioSourceUseCase {
    public Optional<PdfAudioSourceLocation> locate(AudioSegmentSnapshot segment) {
        if (segment == null || segment.sourceFingerprint().sourceRegions().isEmpty()) return Optional.empty();
        int page = segment.sourceFingerprint().sourceRegions().getFirst().pageNumber();
        return Optional.of(new PdfAudioSourceLocation(page,
                segment.sourceFingerprint().sourceRegions().stream()
                        .map(ref -> ref.regionId()).toList()));
    }
}
