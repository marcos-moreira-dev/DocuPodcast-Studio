package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.assets.RegisterProjectAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.assets.RemoveProjectAssetUseCase;

/** Asset-related use cases. */
public record AssetApplicationServices(
        RegisterProjectAssetUseCase registerProjectAsset,
        RemoveProjectAssetUseCase removeProjectAsset
) {
}
