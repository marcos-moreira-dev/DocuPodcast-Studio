package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAudioTrackTimelineEntry;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** Virtualized rail of assigned theatre tracks, kept visible when the editor is collapsed. */
public final class TheatreAudioTrackRailView extends VBox {
    public TheatreAudioTrackRailView(TheatreAudioTrackPanel editor) {
        getStyleClass().add("theatre-audio-track-rail");
        setPadding(new Insets(12));
        setSpacing(8);
        Label title = new Label("Pistas asignadas");
        title.getStyleClass().add("document-media-action-label");
        ListView<TheatreAudioTrackTimelineEntry> tracks = new ListView<>(editor.trackEntries());
        tracks.setMinWidth(0);
        tracks.setMaxWidth(Double.MAX_VALUE);
        tracks.setPlaceholder(new Label("No hay pistas asignadas."));
        tracks.setCellFactory(list -> new TrackCell(editor));
        tracks.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, value) -> {
            if (value != null && !sameTrack(value, editor.selectedTrackProperty().get())) {
                editor.selectTrack(value, false);
            }
        });
        editor.selectedTrackProperty().addListener((obs, oldValue, value) -> {
            if (value == null) tracks.getSelectionModel().clearSelection();
            else if (!sameTrack(tracks.getSelectionModel().getSelectedItem(), value)) {
                editor.trackEntry(value.track().id()).ifPresent(tracks.getSelectionModel()::select);
            }
        });
        VBox.setVgrow(tracks, Priority.ALWAYS);
        getChildren().addAll(title, tracks);
    }

    private static boolean sameTrack(TheatreAudioTrackTimelineEntry left, TheatreAudioTrackTimelineEntry right) {
        return left != null && right != null && left.track().id().equals(right.track().id());
    }

    private static final class TrackCell extends ListCell<TheatreAudioTrackTimelineEntry> {
        private final TheatreAudioTrackPanel editor;

        private TrackCell(TheatreAudioTrackPanel editor) {
            this.editor = editor;
            setOnMouseClicked(event -> {
                TheatreAudioTrackTimelineEntry entry = getItem();
                if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1 || entry == null || isEmpty()) {
                    return;
                }
                getListView().getSelectionModel().select(getIndex());
                Platform.runLater(() -> editor.trackEntry(entry.track().id())
                        .ifPresent(current -> editor.selectTrack(current, true)));
                event.consume();
            });
        }

        @Override protected void updateItem(TheatreAudioTrackTimelineEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (getGraphic() instanceof VBox oldCard) {
                oldCard.prefWidthProperty().unbind();
            }
            if (empty || item == null) { setGraphic(null); setText(null); return; }
            Label title = new Label(item.assetDisplayName().isBlank() ? item.track().assetId() : item.assetDisplayName());
            title.getStyleClass().add("document-media-card-title");
            title.setWrapText(true);
            String fade = item.track().gentleFade() ? " · fade 0,5 s" : "";
            Label detail = new Label(item.startSegmentId() + " · " + format(item.track().sourceStartSeconds())
                    + "-" + format(item.track().effectiveEndSeconds()) + " s · "
                    + Math.round(item.track().volume() * 100.0) + "%" + fade + " · "
                    + item.affectedSegmentIds().size() + " fragmentos");
            detail.setWrapText(true);
            VBox card = new VBox(4, title, detail);
            card.getStyleClass().add("theatre-audio-track-card");
            card.setPadding(new Insets(8));
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);
            if (getListView() != null) card.prefWidthProperty().bind(getListView().widthProperty().subtract(20));
            setGraphic(card);
            setText(null);
            setMaxWidth(Double.MAX_VALUE);
            setGraphicTextGap(0);
        }

        private static String format(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    }
}
