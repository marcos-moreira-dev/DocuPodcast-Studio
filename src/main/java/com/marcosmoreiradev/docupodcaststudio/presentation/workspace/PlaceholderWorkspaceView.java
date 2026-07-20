package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.UiState;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.UiStateView;

/** Legacy compatibility surface. New registrations use explicit operational states. */
@Deprecated(forRemoval = false)
public final class PlaceholderWorkspaceView extends UiStateView {
    public PlaceholderWorkspaceView(WorkspaceKind kind) {
        super(UiState.CAPABILITY_UNAVAILABLE, "NO DISPONIBLE", kind.displayName(),
                "Esta capacidad no está disponible en la configuración actual.");
        getStyleClass().add("placeholder-workspace");
    }
}
