package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.CollapsibleModuleSplitPane;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.scene.layout.BorderPane;

/** Audio mode workspace: collapsible editor plus an always-available assigned-track rail. */
public final class TheatreAudioTrackWorkspace extends BorderPane {
    private final CollapsibleModuleSplitPane split;

    public TheatreAudioTrackWorkspace(DocuPodcastShellViewModel viewModel) {
        TheatreAudioTrackPanel editor = new TheatreAudioTrackPanel(viewModel);
        TheatreAudioTrackRailView rail = new TheatreAudioTrackRailView(editor);
        split = new CollapsibleModuleSplitPane("Editar pista", editor, "Pistas asignadas", rail, 0.46, false);
        split.getStyleClass().add("theatre-audio-track-split");
        split.setMinWidth(0);
        setCenter(split);
    }

    public void showEditor() { split.showPrimary(); }
}
