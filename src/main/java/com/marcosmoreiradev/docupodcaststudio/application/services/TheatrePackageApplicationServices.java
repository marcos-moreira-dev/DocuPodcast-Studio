package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.RefreshTheatrePackageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatrepackage.TheatreImportStateRepository;

import java.util.Objects;

/** Use cases and ports required by the linked theatre-package workflow. */
public record TheatrePackageApplicationServices(
        RefreshTheatrePackageUseCase refresh,
        TheatreImportStateRepository importState) {

    public TheatrePackageApplicationServices {
        Objects.requireNonNull(refresh, "refresh");
        Objects.requireNonNull(importState, "importState");
    }
}
