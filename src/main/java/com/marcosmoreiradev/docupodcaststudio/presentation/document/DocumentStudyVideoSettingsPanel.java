package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Project-wide documentary video settings, independent from slide editing. */
public final class DocumentStudyVideoSettingsPanel extends BorderPane {
    private final DocuPodcastShellViewModel viewModel;
    private final TextField title = StudioFormControls.textInput(new TextField(),
            "Titulo opcional mostrado en la zona superior de las diapositivas.");
    private final Spinner<Double> defaultTableDuration = StudioFormControls.spinner(
            new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(2.0, 60.0, 6.0, 1.0)),
            "Segundos durante los que se muestra cada tabla sin narracion.");
    private final VBox musicRows = new VBox(8);
    private boolean syncingControls;

    public DocumentStudyVideoSettingsPanel(DocuPodcastShellViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        getStyleClass().addAll("document-study-video-panel", "document-study-video-settings-panel");

        title.setPromptText("Titulo del video (opcional)");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setOnAction(event -> commitTitle());
        title.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!Boolean.TRUE.equals(focused)) commitTitle();
        });
        defaultTableDuration.setEditable(true);
        defaultTableDuration.valueProperty().addListener((obs, oldValue, value) -> {
            if (!syncingControls && value != null) {
                saveConfiguration(configuration().withDefaultTableDuration(value));
            }
        });

        GridPane general = new GridPane();
        general.setHgap(8);
        general.setVgap(7);
        general.add(fieldLabel("Titulo del video"), 0, 0);
        general.add(title, 0, 1);
        general.add(fieldLabel("Duracion predeterminada de tablas"), 0, 2);
        general.add(defaultTableDuration, 0, 3);
        GridPane.setHgrow(title, Priority.ALWAYS);
        GridPane.setHgrow(defaultTableDuration, Priority.ALWAYS);

        Button addMusic = ActionButtonFactory.secondary(
                "Agregar musica",
                "Importar una o varias pistas dentro de la carpeta del proyecto.",
                this::chooseMusic);
        VBox content = new VBox(12,
                sectionTitle("Ajustes generales"),
                hint("Estos valores se aplican al video completo, no a un parrafo individual."),
                general,
                new Separator(),
                sectionTitle("Musica de fondo"),
                hint("Las pistas se reproducen en secuencia y vuelven a empezar si el video continua."),
                addMusic,
                musicRows);
        content.setPadding(new Insets(14));
        content.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMinWidth(0);
        scroll.getStyleClass().addAll("document-study-video-scroll", "document-study-video-settings-scroll");
        setCenter(scroll);

        viewModel.documentMediaRevisionProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentProjectModeProperty().addListener((obs, oldValue, newValue) -> refresh());
        viewModel.currentDocumentProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        DocumentStudyVideoConfiguration configuration = configuration();
        syncingControls = true;
        try {
            if (!Objects.equals(title.getText(), configuration.videoTitle())) title.setText(configuration.videoTitle());
            defaultTableDuration.getValueFactory().setValue(configuration.defaultTableDurationSeconds());
        } finally {
            syncingControls = false;
        }
        boolean unavailable = !viewModel.documentaryVideoConfigurationAvailable();
        title.setDisable(unavailable);
        defaultTableDuration.setDisable(unavailable);
        refreshMusicRows();
    }

    private void chooseMusic() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Agregar musica de fondo");
        chooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Audio compatible", "*.wav", "*.mp3", "*.m4a", "*.flac", "*.ogg"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        List<File> selected = chooser.showOpenMultipleDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null || selected.isEmpty()) return;
        ArrayList<DocumentStudyMusicTrack> tracks = new ArrayList<>(configuration().musicTracks());
        try {
            for (File file : selected) tracks.add(viewModel.importDocumentaryMusic(file.toPath()));
            saveConfiguration(configuration().withMusicTracks(tracks));
        } catch (Exception ex) {
            showError("No se pudo importar la musica", ex);
        }
    }

    private void refreshMusicRows() {
        musicRows.getChildren().clear();
        List<DocumentStudyMusicTrack> tracks = configuration().musicTracks();
        if (tracks.isEmpty()) {
            musicRows.getChildren().add(hint("Sin musica de fondo."));
            return;
        }
        for (int index = 0; index < tracks.size(); index++) {
            musicRows.getChildren().add(musicRow(tracks.get(index), index, tracks.size()));
        }
    }

    private Node musicRow(DocumentStudyMusicTrack track, int index, int size) {
        String displayName = viewModel.currentProject()
                .flatMap(project -> project.assets().byId(track.assetId()))
                .map(ProjectAssetReference::displayName).orElse(track.assetId());
        Label name = new Label((index + 1) + ". " + compactName(displayName));
        name.setWrapText(true);
        Tooltip.install(name, new Tooltip(displayName));
        name.getStyleClass().add("document-study-video-music-name");
        Label duration = hint("Duracion: " + seconds(track.durationSeconds()));
        Slider volume = StudioFormControls.slider(new Slider(0.0, 1.0, track.volume()), "Volumen de esta pista.");
        volume.setBlockIncrement(0.05);
        Label value = new Label(percent(track.volume()));
        volume.valueProperty().addListener((obs, oldValue, newValue) -> value.setText(percent(newValue.doubleValue())));
        volume.setOnMouseReleased(event -> updateMusicVolume(track.id(), volume.getValue()));
        volume.setOnKeyReleased(event -> updateMusicVolume(track.id(), volume.getValue()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox volumeHeading = new HBox(8, fieldLabel("Volumen"), spacer, value);
        volumeHeading.setAlignment(Pos.CENTER_LEFT);
        volume.setMaxWidth(Double.MAX_VALUE);

        Button up = ActionButtonFactory.secondary("Subir", () -> moveMusic(index, -1));
        Button down = ActionButtonFactory.secondary("Bajar", () -> moveMusic(index, 1));
        Button remove = ActionButtonFactory.danger("Quitar", () -> removeMusic(track.id()));
        up.setDisable(index == 0);
        down.setDisable(index >= size - 1);
        HBox actions = new HBox(6, up, down, remove);
        VBox row = new VBox(6, name, duration, volumeHeading, volume, actions);
        row.getStyleClass().add("document-study-video-music-row");
        return row;
    }

    private void updateMusicVolume(String trackId, double volume) {
        saveConfiguration(configuration().withMusicTracks(configuration().musicTracks().stream()
                .map(track -> track.id().equals(trackId) ? track.withVolume(volume) : track).toList()));
    }

    private void moveMusic(int index, int delta) {
        ArrayList<DocumentStudyMusicTrack> tracks = new ArrayList<>(configuration().musicTracks());
        int target = index + delta;
        if (index < 0 || index >= tracks.size() || target < 0 || target >= tracks.size()) return;
        DocumentStudyMusicTrack value = tracks.remove(index);
        tracks.add(target, value);
        saveConfiguration(configuration().withMusicTracks(tracks));
    }

    private void removeMusic(String trackId) {
        saveConfiguration(configuration().withMusicTracks(configuration().musicTracks().stream()
                .filter(track -> !track.id().equals(trackId)).toList()));
    }

    private void commitTitle() {
        if (syncingControls) return;
        String value = normalize(title.getText());
        if (!value.equals(configuration().videoTitle())) saveConfiguration(configuration().withTitle(value));
    }

    private void saveConfiguration(DocumentStudyVideoConfiguration configuration) {
        viewModel.updateDocumentaryVideoConfiguration(configuration);
    }

    private DocumentStudyVideoConfiguration configuration() {
        return viewModel.documentaryVideoConfiguration();
    }

    private void showError(String header, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(getScene() == null ? null : getScene().getWindow());
        alert.setTitle("Video documental");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        alert.showAndWait();
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-study-video-section-title");
        return label;
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-study-video-field-label");
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-study-video-hint");
        return label;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static String seconds(double value) {
        return String.format(Locale.ROOT, "%.1f s", value);
    }

    private static String percent(double value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 100) + "%";
    }

    private static String compactName(String value) {
        String normalized = normalize(value);
        return normalized.length() <= 52 ? normalized : normalized.substring(0, 49).stripTrailing() + "...";
    }
}
