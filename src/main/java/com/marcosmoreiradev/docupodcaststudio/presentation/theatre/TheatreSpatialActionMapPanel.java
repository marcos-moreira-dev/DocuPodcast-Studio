package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportEstimate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreSpatialRoleIcon;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreStageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SectionHeader;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Fused spatial map and action map module for theatre scenes. */
public final class TheatreSpatialActionMapPanel extends BorderPane {
    private static final double SPATIAL_IMAGE_FIT_WIDTH = 760.0;
    private static final double SPATIAL_IMAGE_FIT_HEIGHT = 476.0;
    private static final double SPATIAL_MARKER_SIZE = 101.0;
    private static final double SPATIAL_ARROW_TRIM_FACTOR = 0.56;
    private static final double SPATIAL_SELF_LOOP_SCALE_FACTOR = 0.34;
    private static final double SPATIAL_SELF_LOOP_RIGHT_OFFSET_FACTOR = 0.44;
    private static final double SPATIAL_SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR = 0.10;
    private static final List<String> STAGE_LOCATIONS = List.of(
            "fondo derecha",
            "fondo centro",
            "fondo izquierda",
            "centro derecha",
            "centro",
            "centro izquierda",
            "frente derecha",
            "frente centro",
            "frente izquierda",
            "hacia el publico",
            "extra diegetico",
            "diegetico");
    private static final List<String> SPECIAL_INTERACTION_TARGETS = List.of(
            "Publico",
            "Para si mismo",
            "Entidad no presente en escenario");
    private static final String ALL_REMAINING_TARGET = "Todos los personajes restantes";
    private static final String ABSENT_LOCATION = "no presente";

    private final DocuPodcastShellViewModel viewModel;
    private final IntervencionBoundaryStore boundaryStore;
    private final TheatreSceneFoldList actionFolds;
    private final StringProperty selectedIntervencion = new SimpleStringProperty("");
    private final Map<String, TheatreProjectLayer.TextActionPlacement> placements = new LinkedHashMap<>();
    private Canvas spatialOverlay;

    public TheatreSpatialActionMapPanel(DocuPodcastShellViewModel viewModel, IntervencionBoundaryStore boundaryStore) {
        this.viewModel = viewModel;
        this.boundaryStore = boundaryStore == null ? new IntervencionBoundaryStore() : boundaryStore;
        getStyleClass().add("theatre-spatial-action-map-panel");
        setPadding(new Insets(10));
        actionFolds = new TheatreSceneFoldList(viewModel, this::sceneActionCanvas);
        this.boundaryStore.revisionProperty().addListener((obs, oldValue, newValue) -> actionFolds.refresh());

        viewModel.activeTextActionPlacementProperty().addListener((obs, oldValue, newValue) -> drawSpatialOverlay());
        viewModel.focusedTheatreSceneIdProperty().addListener((obs, oldValue, newValue) -> drawSpatialOverlay());
        viewModel.currentScriptProperty().addListener((obs, oldValue, newValue) -> loadPlacementsFromProject());
        loadPlacementsFromProject();
        TheatreInterventionSelectionBridge.bind(viewModel, selectedIntervencion, this.boundaryStore);

        VBox body = new VBox(12,
                spatialSection(),
                new SectionHeader("Mapa de acciones", "Cada escena dibuja la secuencia Intervencion 1, Intervencion 2 y siguientes en una linea recta en zigzag."),
                actionFolds);
        body.getStyleClass().add("theatre-map-body");

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("document-context-scroll");
        setCenter(scroll);
    }

    private Node spatialSection() {
        VBox box = new VBox(8);
        box.setFillWidth(false);
        box.setAlignment(Pos.TOP_LEFT);
        box.getStyleClass().add("theatre-spatial-reference-section");
        SectionHeader header = new SectionHeader("Mapa espacial", "Referencia superior del escenario para orientar posiciones.");
        HBox headerRow = new HBox(8, header);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, Priority.ALWAYS);
        box.getChildren().add(headerRow);

        Label modeLabel = new Label("Modo acompañante:");
        modeLabel.getStyleClass().add("theatre-mode-label");
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton fragRadio = new RadioButton("Fragmentos visuales");
        fragRadio.getStyleClass().add("theatre-spatial-radio-button");
        fragRadio.setToggleGroup(modeGroup);
        fragRadio.setSelected("fragments".equals(viewModel.spatialFrameModeProperty().get()));
        RadioButton charRadio = new RadioButton("Fotos de personajes");
        charRadio.getStyleClass().add("theatre-spatial-radio-button");
        charRadio.setToggleGroup(modeGroup);
        charRadio.setSelected("characters".equals(viewModel.spatialFrameModeProperty().get()));
        RadioButton noneRadio = new RadioButton("Sin acompañante");
        noneRadio.getStyleClass().add("theatre-spatial-radio-button");
        noneRadio.setToggleGroup(modeGroup);
        noneRadio.setSelected(!"fragments".equals(viewModel.spatialFrameModeProperty().get()) && !"characters".equals(viewModel.spatialFrameModeProperty().get()));
        modeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == fragRadio) {
                viewModel.setSpatialFrameMode("fragments");
            } else if (newValue == charRadio) {
                viewModel.setSpatialFrameMode("characters");
            } else {
                viewModel.setSpatialFrameMode("none");
            }
        });

        Button fullscreenBtn = ActionButtonFactory.secondary("Ver mapa en pantalla completa",
                () -> TheatreFullscreenMapView.show(viewModel, placements,
                        getScene() == null ? List.of() : getScene().getStylesheets(),
                        viewModel::runDocumentPrimaryAction));
        Button packagesBtn = ActionButtonFactory.secondary("Exportar paquetes IA", this::exportarPaquetesIaMasivos);
        FlowPane radioFlow = new FlowPane(14, 8, fragRadio, charRadio, noneRadio);
        radioFlow.getStyleClass().add("theatre-spatial-mode-radios");
        radioFlow.setPrefWrapLength(520);
        VBox modeBox = new VBox(6, modeLabel, radioFlow);
        modeBox.getStyleClass().add("theatre-spatial-mode-box");
        modeBox.setPadding(new Insets(8, 10, 8, 0));
        VBox controls = new VBox(8, modeBox, new HBox(8, fullscreenBtn, packagesBtn));
        controls.getStyleClass().add("theatre-spatial-controls");
        controls.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(controls);

        StackPane frame = spatialMapFrame(SPATIAL_IMAGE_FIT_WIDTH, SPATIAL_IMAGE_FIT_HEIGHT, true);
        StackPane.setAlignment(frame, Pos.CENTER);
        box.getChildren().add(frame);
        return box;
    }

    private void exportarPaquetesIaMasivos() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Elegir carpeta destino para paquetes IA teatrales");
        viewModel.currentProjectDirectory().filter(Files::isDirectory).map(Path::toFile).ifPresent(chooser::setInitialDirectory);
        java.io.File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) return;
        try {
            TheatreContextExportEstimate estimate = viewModel.estimateTheatreContextPackages(TheatreContextExportScope.all());
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Exportar paquetes IA");
            confirm.setHeaderText("Se exportaran " + estimate.packages() + " paquetes de intervencion");
            confirm.setContentText("Destino:\n" + selected.toPath()
                    + "\n\nTamaño estimado: " + humanBytes(estimate.estimatedBytes())
                    + "\nArchivos estimados: " + estimate.files()
                    + "\n\nEsto puede ocupar bastante espacio porque los assets se copian por intervencion.");
            Window owner = getScene() == null ? null : getScene().getWindow();
            if (owner != null) confirm.initOwner(owner);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            var result = viewModel.exportTheatreContextPackages(TheatreContextExportScope.all(), selected.toPath());
            Alert done = new Alert(Alert.AlertType.INFORMATION);
            done.setTitle("Paquetes IA exportados");
            done.setHeaderText(result.packages() + " paquetes creados");
            done.setContentText("Carpeta raiz:\n" + result.root());
            if (owner != null) done.initOwner(owner);
            done.showAndWait();
        } catch (IOException | RuntimeException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se pudieron exportar paquetes IA");
            alert.setHeaderText("Error al preparar paquetes masivos");
            alert.setContentText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            Window owner = getScene() == null ? null : getScene().getWindow();
            if (owner != null) alert.initOwner(owner);
            alert.showAndWait();
        }
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.ROOT, "%.1f MB", mb);
        return String.format(Locale.ROOT, "%.2f GB", mb / 1024.0);
    }

    private StackPane spatialMapFrame(double width, double height, boolean primaryOverlay) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("theatre-spatial-reference-frame");
        frame.setPrefSize(width + 24, height + 24);
        frame.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        frame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        Optional<String> referenceUri = spatialReferenceUri();
        if (referenceUri.isEmpty()) {
            Label fallback = new Label("Mapa espacial no configurado.");
            fallback.getStyleClass().add("document-media-empty-note");
            frame.getChildren().add(fallback);
        } else {
            ImageView image = new ImageView(new Image(referenceUri.get(), true));
            image.setFitWidth(width);
            image.setFitHeight(height);
            image.setPreserveRatio(true);
            image.setSmooth(true);
            Canvas overlay = new Canvas(width, height);
            overlay.getStyleClass().add("theatre-spatial-overlay-canvas");
            overlay.setMouseTransparent(true);
            if (primaryOverlay) {
                spatialOverlay = overlay;
            }
            drawSpatialOverlay(overlay);
            frame.getChildren().addAll(image, overlay);
        }
        return frame;
    }

    private Optional<String> spatialReferenceUri() {
        return focusedSpatialReferenceUri();
    }

    private Optional<String> focusedSpatialReferenceUri() {
        String focusedSceneId = viewModel.focusedTheatreSceneIdProperty().get();
        Optional<TheatreProjectLayer.Scene> focused = viewModel.theatreScenes().stream()
                .filter(scene -> scene.id().equals(focusedSceneId))
                .findFirst();
        return focused.or(() -> viewModel.theatreScenes().stream()
                        .filter(scene -> scene.spatialMapAssetId() != null && !scene.spatialMapAssetId().isBlank())
                        .findFirst())
                .flatMap(scene -> viewModel.projectImageAssetUri(scene.spatialMapAssetId()));
    }

    private Node sceneActionCanvas(TheatreProjectLayer.Scene scene) {
        ReadableDocument document = viewModel.currentDocumentProperty().get();
        NarrationScriptDocument script = viewModel.currentScriptProperty().get();
        List<IntervencionCatalogo.IntervencionInfo> aliases = intervencionesParaEscena(scene);
        TheatreTextSequenceCanvas canvas = new TheatreTextSequenceCanvas(
                scene.displayName(),
                TheatreWorkspaceEmptyState.interventionSequenceMessage(document, script),
                aliases,
                selectedIntervencion,
                TheatreInterventionSelectionBridge.blockSelector(viewModel, scene),
                alias -> mostrarEditorAccion(scene, alias),
                alias -> exportarContextoIntervencion(scene, alias));
        ScrollPane canvasScroll = new ScrollPane(canvas);
        canvasScroll.getStyleClass().add("theatre-action-canvas-scroll");
        canvasScroll.setFitToWidth(false);
        canvasScroll.setFitToHeight(false);
        canvasScroll.setPrefViewportHeight(320);
        canvasScroll.setMinViewportHeight(260);
        canvasScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        canvasScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox box = new VBox(5);
        TheatreWorkspaceEmptyState.preparationNotice(document, script)
                .map(TheatreSpatialActionMapPanel::stateNote)
                .ifPresent(box.getChildren()::add);
        box.getChildren().add(canvasScroll);
        box.getStyleClass().add("theatre-action-canvas-shell");
        return box;
    }

    private void exportarContextoIntervencion(TheatreProjectLayer.Scene scene, IntervencionCatalogo.IntervencionInfo alias) {
        if (scene == null || alias == null) {
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Elegir carpeta para paquete IA");
        viewModel.currentProjectDirectory()
                .filter(Files::isDirectory)
                .map(Path::toFile)
                .ifPresent(chooser::setInitialDirectory);
        java.io.File selected = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) {
            return;
        }
        try {
            Path folder = viewModel.exportTheatreInterventionContext(scene.id(), alias.alias(), selected.toPath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Paquete IA creado");
            alert.setHeaderText("Contexto visual de " + TheatreZigzagLayout.displayLabel(alias.alias()) + " exportado");
            alert.setContentText("Carpeta creada:\n" + folder
                    + "\n\nEstas imagenes sirven como contexto rapido para una IA generadora de imagen unificada.");
            Window owner = getScene() == null ? null : getScene().getWindow();
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        } catch (IOException | RuntimeException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se pudo exportar el paquete IA");
            alert.setHeaderText("No se pudo crear el contexto de " + TheatreZigzagLayout.displayLabel(alias.alias()));
            alert.setContentText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            Window owner = getScene() == null ? null : getScene().getWindow();
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        }
    }

    private List<IntervencionCatalogo.IntervencionInfo> intervencionesParaEscena(TheatreProjectLayer.Scene scene) {
        List<IntervencionCatalogo.IntervencionInfo> aliases = IntervencionCatalogo.intervenciones(
                viewModel.currentDocumentProperty().get(),
                viewModel.currentScriptProperty().get());
        return IntervencionNumberingScene.intervencionesParaEscena(
                aliases,
                viewModel.theatreScenes(),
                boundaryStore,
                scene);
    }

    private void mostrarEditorAccion(TheatreProjectLayer.Scene scene, IntervencionCatalogo.IntervencionInfo alias) {
        if (scene == null || alias == null) {
            return;
        }
        TheatreProjectLayer.TextActionPlacement existing = placements.getOrDefault(placementKey(scene.id(), alias.alias()), TheatreProjectLayer.TextActionPlacement.empty());
        Optional<TheatreProjectLayer.TextActionPlacement> previous = colocacionAnterior(scene, alias);
        Label title = new Label("Editar " + alias.alias() + " - " + cueLabel(alias.preview()));
        title.getStyleClass().add("theatre-character-dialog-title");
        Label subtitle = new Label("Define quien habla, hacia quien se dirige y donde queda cada participante en el escenario.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("theatre-spatial-dialog-subtitle");

        VBox characterPositions = new VBox(8);
        characterPositions.getStyleClass().add("theatre-spatial-dialog-field-list");
        String speakerName = cueLabel(alias.preview());
        ComboBox<String> origin = locationCombo(existing.origin().isBlank() ? STAGE_LOCATIONS.get(4) : existing.origin());
        Map<String, CheckBox> interactionInputs = new LinkedHashMap<>();
        FlowPane interactionTargets = interactionTargetSelector(existing.interactionTarget(), speakerName, interactionInputs);
        Map<String, ComboBox<String>> characterLocationInputs = new LinkedHashMap<>();
        for (String character : actionReceiverOptions(speakerName)) {
            ComboBox<String> combo = locationCombo(existing.characterLocations().getOrDefault(character, STAGE_LOCATIONS.get(4)), true);
            characterLocationInputs.put(character, combo);
            characterPositions.getChildren().add(formRow(character, combo));
        }
        bindPresenceToInteractionTargets(interactionInputs, characterLocationInputs, speakerName);

        CheckBox preservePrevious = new CheckBox("Preservar las posiciones de los personajes del texto anterior");
        preservePrevious.setDisable(previous.isEmpty());
        preservePrevious.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!selected || previous.isEmpty()) {
                return;
            }
            TheatreProjectLayer.TextActionPlacement prev = previous.get();
            origin.setValue(prev.origin().isBlank() ? origin.getValue() : prev.origin());
            applyInteractionSelections(interactionInputs, prev.interactionTarget(), speakerName);
            characterLocationInputs.forEach((character, combo) ->
                    combo.setValue(prev.characterLocations().getOrDefault(character, combo.getValue())));
        });

        Button accept = ActionButtonFactory.primary("Aceptar", () -> {
            Map<String, String> characterLocations = new LinkedHashMap<>();
            characterLocationInputs.forEach((character, combo) -> characterLocations.put(character, combo.getValue()));
            String interactionTarget = selectedInteractionTargets(interactionInputs, characterLocationInputs, speakerName);
            TheatreProjectLayer.TextActionPlacement updated = new TheatreProjectLayer.TextActionPlacement(
                    alias.alias(),
                    scene.id(),
                    characterIdForDisplay(cueLabel(alias.preview()), existing.characterId()),
                    origin.getValue(),
                    computedDestination(origin.getValue(), interactionTarget, characterLocations, speakerName),
                    interactionTarget,
                    Map.copyOf(characterLocations));
            placements.put(placementKey(scene.id(), alias.alias()), updated);
            viewModel.saveTheatreTextActionPlacement(updated);
            drawSpatialOverlay();
            ((Stage) origin.getScene().getWindow()).close();
        });
        Button cancel = ActionButtonFactory.secondary("Cancelar", () -> ((Stage) origin.getScene().getWindow()).close());

        VBox interactionGroup = dialogGroup("Interaccion de personajes",
                formRow("Ubicacion de " + speakerName, origin),
                formRow("Interactua con", interactionTargets));
        VBox positionsGroup = dialogGroup("Ubicacion de personajes en el escenario",
                characterPositions,
                preservePrevious);
        HBox actions = new HBox(10, accept, cancel);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("theatre-spatial-dialog-actions");

        VBox root = new VBox(14, title, subtitle, interactionGroup, positionsGroup, actions);
        root.setPadding(new Insets(22));
        root.getStyleClass().addAll("theatre-character-dialog-root", "theatre-spatial-dialog-root");

        Stage stage = new Stage();
        Window owner = getScene() == null ? null : getScene().getWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Editar " + alias.alias());
        Scene dialog = new Scene(root, 600, 680);
        if (getScene() != null) {
            dialog.getStylesheets().addAll(getScene().getStylesheets());
        }
        stage.setScene(dialog);
        stage.show();
    }

    private VBox dialogGroup(String title, Node... children) {
        Label label = new Label(title);
        label.getStyleClass().add("theatre-spatial-dialog-group-title");
        VBox group = new VBox(10);
        group.getStyleClass().add("theatre-spatial-dialog-group");
        group.getChildren().add(label);
        group.getChildren().addAll(children);
        return group;
    }

    private Optional<TheatreProjectLayer.TextActionPlacement> colocacionAnterior(TheatreProjectLayer.Scene scene, IntervencionCatalogo.IntervencionInfo alias) {
        List<IntervencionCatalogo.IntervencionInfo> aliases = intervencionesParaEscena(scene);
        for (int i = 1; i < aliases.size(); i++) {
            if (aliases.get(i).alias().equals(alias.alias())) {
                return Optional.ofNullable(placements.get(placementKey(scene.id(), aliases.get(i - 1).alias())));
            }
        }
        return Optional.empty();
    }

    private ComboBox<String> locationCombo(String selected) {
        return locationCombo(selected, false);
    }

    private ComboBox<String> locationCombo(String selected, boolean allowAbsent) {
        ComboBox<String> combo = new ComboBox<>();
        ArrayList<String> options = new ArrayList<>(STAGE_LOCATIONS);
        if (allowAbsent) {
            options.add(ABSENT_LOCATION);
        }
        combo.getItems().setAll(options);
        String initial = selected == null || selected.isBlank() ? STAGE_LOCATIONS.get(4) : selected;
        combo.setValue(isAbsentLocation(initial) ? ABSENT_LOCATION : initial);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getStyleClass().add("theatre-spatial-dialog-combo");
        return combo;
    }

    private HBox formRow(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("theatre-spatial-dialog-field-label");
        label.setMinWidth(190);
        HBox row = new HBox(12, label, control);
        row.getStyleClass().add("theatre-spatial-dialog-field-row");
        HBox.setHgrow(control, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label stateNote(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-media-empty-note");
        return label;
    }

    private FlowPane interactionTargetSelector(String currentTargets, String speakerName, Map<String, CheckBox> inputs) {
        FlowPane flow = new FlowPane(10, 8);
        flow.getStyleClass().add("theatre-spatial-dialog-targets");
        for (String target : interactionTargetOptions(speakerName)) {
            CheckBox check = new CheckBox(target);
            check.getStyleClass().add("theatre-spatial-dialog-target-check");
            inputs.put(target, check);
            flow.getChildren().add(check);
        }
        applyInteractionSelections(inputs, currentTargets, speakerName);
        return flow;
    }

    private void bindPresenceToInteractionTargets(Map<String, CheckBox> inputs,
                                                  Map<String, ComboBox<String>> characterLocationInputs,
                                                  String speakerName) {
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        characterLocationInputs.values().forEach(combo ->
                combo.valueProperty().addListener((obs, oldValue, newValue) ->
                        updateInteractionTargetAvailability(inputs, characterLocationInputs, speakerName)));
        CheckBox allRemaining = inputs.get(ALL_REMAINING_TARGET);
        if (allRemaining != null) {
            allRemaining.selectedProperty().addListener((obs, oldValue, newValue) ->
                    updateInteractionTargetAvailability(inputs, characterLocationInputs, speakerName));
        }
        updateInteractionTargetAvailability(inputs, characterLocationInputs, speakerName);
    }

    private static void updateInteractionTargetAvailability(Map<String, CheckBox> inputs,
                                                            Map<String, ComboBox<String>> characterLocationInputs,
                                                            String speakerName) {
        CheckBox allRemaining = inputs.get(ALL_REMAINING_TARGET);
        boolean allRemainingSelected = allRemaining != null && allRemaining.isSelected();
        boolean hasPresentRemaining = inputs.keySet().stream()
                .filter(name -> !ALL_REMAINING_TARGET.equals(name))
                .filter(name -> !TheatreSpatialRoleIcon.isAudience(name))
                .filter(name -> speakerName == null || speakerName.isBlank() || !name.equalsIgnoreCase(speakerName))
                .anyMatch(name -> !targetAbsent(name, characterLocationInputs));
        if (allRemaining != null) {
            allRemaining.setDisable(!hasPresentRemaining);
            if (!hasPresentRemaining) {
                allRemaining.setSelected(false);
                allRemainingSelected = false;
            }
        }
        for (Map.Entry<String, CheckBox> entry : inputs.entrySet()) {
            String target = entry.getKey();
            CheckBox check = entry.getValue();
            if (ALL_REMAINING_TARGET.equals(target)) {
                continue;
            }
            boolean absent = targetAbsent(target, characterLocationInputs);
            if (allRemainingSelected || absent) {
                check.setSelected(false);
            }
            check.setDisable(allRemainingSelected || absent);
        }
    }

    private void applyInteractionSelections(Map<String, CheckBox> inputs, String targets, String speakerName) {
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        inputs.values().forEach(check -> {
            check.setDisable(false);
            check.setSelected(false);
        });
        List<String> selected = interactionTargets(targets);
        boolean allRemainingSelected = selected.stream().anyMatch(value -> value.equalsIgnoreCase(ALL_REMAINING_TARGET));
        if (allRemainingSelected && inputs.containsKey(ALL_REMAINING_TARGET)) {
            inputs.get(ALL_REMAINING_TARGET).setSelected(true);
            return;
        }
        boolean selfSelected = selected.stream().anyMatch(TheatreSpatialActionMapPanel::isSelfTarget);
        inputs.forEach((target, check) -> check.setSelected(
                selected.stream().anyMatch(value -> value.equalsIgnoreCase(target))
                        || (selfSelected && !speakerName.isBlank() && target.equalsIgnoreCase(speakerName))));
    }

    private static String selectedInteractionTargets(Map<String, CheckBox> inputs,
                                                     Map<String, ComboBox<String>> characterLocationInputs,
                                                     String speakerName) {
        if (inputs == null || inputs.isEmpty()) {
            return "";
        }
        CheckBox allRemaining = inputs.get(ALL_REMAINING_TARGET);
        if (allRemaining != null && allRemaining.isSelected()) {
            return inputs.keySet().stream()
                    .filter(name -> !ALL_REMAINING_TARGET.equals(name))
                    .filter(name -> !TheatreSpatialRoleIcon.isAudience(name))
                    .filter(name -> speakerName == null || speakerName.isBlank() || !name.equalsIgnoreCase(speakerName))
                    .filter(name -> !targetAbsent(name, characterLocationInputs))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        }
        return inputs.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .filter(entry -> !entry.getValue().isDisabled())
                .filter(entry -> !ALL_REMAINING_TARGET.equals(entry.getKey()))
                .filter(entry -> !targetAbsent(entry.getKey(), characterLocationInputs))
                .map(Map.Entry::getKey)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static boolean targetAbsent(String target, Map<String, ComboBox<String>> characterLocationInputs) {
        if (target == null || target.isBlank() || characterLocationInputs == null || characterLocationInputs.isEmpty()) {
            return false;
        }
        return characterLocationInputs.entrySet().stream()
                .anyMatch(entry -> entry.getKey().equalsIgnoreCase(target)
                        && isAbsentLocation(entry.getValue().getValue()));
    }

    private String computedDestination(String origin,
                                       String interactionTarget,
                                       Map<String, String> characterLocations,
                                       String speakerName) {
        for (String target : interactionTargets(interactionTarget)) {
            if (TheatreSpatialRoleIcon.isAudience(target)) {
                return "hacia el publico";
            }
            if (!speakerName.isBlank() && target.equalsIgnoreCase(speakerName)) {
                return origin == null || origin.isBlank() ? STAGE_LOCATIONS.get(4) : origin;
            }
            if (characterLocations != null) {
                for (Map.Entry<String, String> entry : characterLocations.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(target)
                            && entry.getValue() != null
                            && !entry.getValue().isBlank()
                            && !isAbsentLocation(entry.getValue())) {
                        return entry.getValue();
                    }
                }
            }
        }
        return origin == null || origin.isBlank() ? STAGE_LOCATIONS.get(4) : origin;
    }

    private List<String> detectedCharacterOptions() {
        ArrayList<String> names = new ArrayList<>();
        TheatreCharacterDetector.detect(
                        viewModel.currentDocumentProperty().get(),
                        viewModel.currentScriptProperty().get(),
                        viewModel.theatreCharacterProfiles())
                .forEach(character -> names.add(character.displayName()));
        return List.copyOf(names);
    }

    private List<String> interactionTargetOptions(String speakerName) {
        ArrayList<String> names = new ArrayList<>(detectedCharacterOptions());
        if (speakerName != null && !speakerName.isBlank()
                && names.stream().noneMatch(name -> name.equalsIgnoreCase(speakerName))) {
            names.add(0, speakerName);
        }
        if (names.stream().anyMatch(name -> speakerName == null || !name.equalsIgnoreCase(speakerName))) {
            names.add(ALL_REMAINING_TARGET);
        }
        if (names.stream().noneMatch(TheatreSpatialRoleIcon::isAudience)) {
            names.add("Publico");
        }
        return List.copyOf(names);
    }

    private List<String> actionReceiverOptions(String speakerName) {
        String speaker = speakerName == null ? "" : speakerName.strip();
        return detectedCharacterOptions().stream()
                .filter(name -> speaker.isBlank() || !name.equalsIgnoreCase(speaker))
                .filter(name -> !isSpecialTarget(name))
                .toList();
    }

    private void drawSpatialOverlay() {
        if (spatialOverlay == null) {
            return;
        }
        drawSpatialOverlay(spatialOverlay);
    }

    private void drawSpatialOverlay(Canvas overlay) {
        if (overlay == null) {
            return;
        }
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
        String activeAlias = viewModel.activePlacementAliasProperty().get();
        List<TheatreProjectLayer.TextActionPlacement> visible = visiblePlacements();
        visible.forEach(placement -> {
            StagePoint origin = stagePoint(placement.origin());
            destinationLocations(placement).forEach(destination ->
                    drawActionArrow(gc, origin, stagePoint(destination), placement.intervencionId().equals(activeAlias)));
        });
        drawCharacterLocationLabels(gc, visible);
        gc.setLineWidth(2);
    }

    private static void drawActionArrow(GraphicsContext gc, StagePoint origin, StagePoint destination, boolean active) {
        boolean samePoint = Math.abs(origin.x() - destination.x()) < 0.5 && Math.abs(origin.y() - destination.y()) < 0.5;
        if (samePoint) {
            drawSelfLoop(gc, origin, active);
            return;
        }
        double trim = SPATIAL_MARKER_SIZE * SPATIAL_ARROW_TRIM_FACTOR;
        strokeArrow(gc, origin, destination, Color.web("rgba(0, 0, 0, 0.34)"), active ? 11 : 10, 19, trim, trim);
        strokeArrow(gc, origin, destination, Color.WHITE, active ? 9 : 8, 16, trim, trim);
        strokeArrow(gc, origin, destination, active ? Color.web("#D97706") : Color.web("#020617"), active ? 5 : 4, 13, trim, trim);
    }

    private static void drawSelfLoop(GraphicsContext gc, StagePoint origin, boolean active) {
        double size = SPATIAL_MARKER_SIZE * SPATIAL_SELF_LOOP_SCALE_FACTOR;
        double x = clamp(origin.x() + SPATIAL_MARKER_SIZE * SPATIAL_SELF_LOOP_RIGHT_OFFSET_FACTOR,
                4,
                SPATIAL_IMAGE_FIT_WIDTH - size * 1.14 - 4);
        double y = clamp(origin.y() + size * 0.06,
                4,
                SPATIAL_IMAGE_FIT_HEIGHT - size - 4);
        strokeSelfLoop(gc, x, y, size, Color.web("rgba(0, 0, 0, 0.34)"), active ? 7 : 6, 10);
        strokeSelfLoop(gc, x, y, size, Color.WHITE, active ? 5 : 4, 8);
        strokeSelfLoop(gc, x, y, size, active ? Color.web("#D97706") : Color.web("#020617"), active ? 3 : 2.5, 6);
    }

    private static void strokeSelfLoop(GraphicsContext gc,
                                       double x,
                                       double y,
                                       double size,
                                       Color color,
                                       double lineWidth,
                                       double headLength) {
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        double startX = x + size * 0.10;
        double startY = y + size * 0.72;
        double c1x = x + size * 1.08;
        double c1y = y + size * 0.94;
        double c2x = x + size * 1.08;
        double c2y = y + size * 0.08;
        double ex = x + size * 0.12;
        double ey = y + size * 0.24;
        gc.beginPath();
        gc.moveTo(startX, startY);
        gc.bezierCurveTo(c1x, c1y, c2x, c2y, ex, ey);
        gc.stroke();
        double tangentAngle = Math.atan2(ey - c2y, ex - c2x);
        fillArrowHead(gc, ex, ey, tangentAngle, headLength);
    }

    private static void fillArrowHead(GraphicsContext gc,
                                      double tipX,
                                      double tipY,
                                      double angle,
                                      double headLength) {
        double spread = Math.toRadians(28);
        gc.fillPolygon(
                new double[]{
                        tipX,
                        tipX - headLength * Math.cos(angle - spread),
                        tipX - headLength * Math.cos(angle + spread)
                },
                new double[]{
                        tipY,
                        tipY - headLength * Math.sin(angle - spread),
                        tipY - headLength * Math.sin(angle + spread)
                },
                3);
    }

    private static void strokeArrow(GraphicsContext gc,
                                    StagePoint origin,
                                    StagePoint destination,
                                    Color color,
                                    double lineWidth,
                                    double headLength,
                                    double trimStart,
                                    double trimEnd) {
        double distance = Math.hypot(destination.x() - origin.x(), destination.y() - origin.y());
        if (distance <= trimStart + trimEnd + 1) {
            return;
        }
        double startX = origin.x() + (destination.x() - origin.x()) / distance * trimStart;
        double startY = origin.y() + (destination.y() - origin.y()) / distance * trimStart;
        double endX = destination.x() - (destination.x() - origin.x()) / distance * trimEnd;
        double endY = destination.y() - (destination.y() - origin.y()) / distance * trimEnd;
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.strokeLine(startX, startY, endX, endY);
        double angle = Math.atan2(endY - startY, endX - startX);
        double spread = Math.toRadians(24);
        double headWidth = Math.max(2, lineWidth * 0.78);
        gc.setLineWidth(headWidth);
        gc.strokeLine(endX, endY,
                endX - headLength * Math.cos(angle - spread),
                endY - headLength * Math.sin(angle - spread));
        gc.strokeLine(endX, endY,
                endX - headLength * Math.cos(angle + spread),
                endY - headLength * Math.sin(angle + spread));
    }

    private static void drawSpatialMarker(GraphicsContext gc, StagePoint point, TheatreSpatialRoleIcon role) {
        double size = SPATIAL_MARKER_SIZE;
        Image icon = TheatreSpatialIconSet.image(role);
        if (icon != null) {
            gc.drawImage(icon, point.x() - size / 2.0, point.y() - size / 2.0, size, size);
            return;
        }
        gc.setFill(Color.web("rgba(255, 255, 255, 0.90)"));
        gc.fillOval(point.x() - size / 2.0, point.y() - size / 2.0, size, size);
        gc.setStroke(Color.web("#020617"));
        gc.setLineWidth(2.5);
        gc.strokeOval(point.x() - size / 2.0, point.y() - size / 2.0, size, size);
    }

    private List<TheatreProjectLayer.TextActionPlacement> visiblePlacements() {
        String activeAlias = viewModel.activePlacementAliasProperty().get();
        if (activeAlias == null || activeAlias.isBlank()) {
            return List.of();
        }
        if (activeAlias != null && !activeAlias.isBlank()) {
            Optional<TheatreProjectLayer.TextActionPlacement> active = placements.values().stream()
                    .filter(placement -> activeAlias.equals(placement.intervencionId()))
                    .findFirst();
            if (active.isPresent()) {
                return List.of(active.get());
            }
        }
        return List.of();
    }

    private void loadPlacementsFromProject() {
        placements.clear();
        for (TheatreProjectLayer.TextActionPlacement tap : viewModel.theatreTextActionPlacements()) {
            placements.put(placementKey(tap.sceneId(), tap.intervencionId()), tap);
        }
        drawSpatialOverlay();
    }

    private void prepareSpatialVideoFrames(String mode) {
        int aliases = intervencionesParaFramesVideo().size();
        String label = "characters".equals(mode) ? "mapa y personajes" : "mapa y fragmentos visuales";
        viewModel.updateStatusMessage("Frames teatrales preparados para exportar video con " + label
                + ": " + aliases + " segmentos de texto.");
    }

    private List<IntervencionCatalogo.IntervencionInfo> intervencionesParaFramesVideo() {
        List<TheatreProjectLayer.Scene> scenes = viewModel.theatreScenes();
        String focusedSceneId = viewModel.focusedTheatreSceneIdProperty().get();
        Optional<TheatreProjectLayer.Scene> focused = scenes.stream()
                .filter(scene -> scene.id().equals(focusedSceneId))
                .findFirst();
        return focused.or(() -> scenes.stream().findFirst())
                .map(this::intervencionesParaEscena)
                .orElseGet(() -> IntervencionCatalogo.intervenciones(
                        viewModel.currentDocumentProperty().get(),
                        viewModel.currentScriptProperty().get()));
    }

    private void drawCharacterLocationLabels(GraphicsContext gc, List<TheatreProjectLayer.TextActionPlacement> visible) {
        Map<String, MarkerGroup> markersByLocation = new LinkedHashMap<>();
        List<String> selfLoopLocations = selfLoopLocations(visible);
        visible.forEach(placement -> markerParticipants(placement).forEach(participant -> {
            MarkerGroup group = markersByLocation.computeIfAbsent(participant.location(),
                    ignored -> new MarkerGroup(participant.role(), new ArrayList<>()));
            group.role = mergeRole(group.role, participant.role());
            if (containsIgnoreCase(selfLoopLocations, participant.location())) {
                group.selfLoop = true;
            }
            if (!participant.name().isBlank() && !group.names().contains(participant.name())) {
                group.names().add(participant.name());
            }
            if (participant.speaking() && !participant.name().isBlank() && !group.speakers().contains(participant.name())) {
                group.speakers().add(participant.name());
            }
        }));
        markersByLocation.forEach((location, group) -> {
            StagePoint point = markerPointForSelfLoop(stagePoint(location), group.selfLoop());
            drawSpatialMarker(gc, point, group.role());
            drawCharacterLabel(gc, point, group.names(), group.speakers(), 0);
        });
    }

    private List<String> selfLoopLocations(List<TheatreProjectLayer.TextActionPlacement> visible) {
        ArrayList<String> result = new ArrayList<>();
        visible.forEach(placement -> {
            StagePoint origin = stagePoint(placement.origin());
            destinationLocations(placement).forEach(destination -> {
                if (sameStagePoint(origin, stagePoint(destination))) {
                    addUnique(result, placement.origin());
                }
            });
        });
        return List.copyOf(result);
    }

    private static boolean sameStagePoint(StagePoint first, StagePoint second) {
        return Math.abs(first.x() - second.x()) < 0.5 && Math.abs(first.y() - second.y()) < 0.5;
    }

    private static StagePoint markerPointForSelfLoop(StagePoint point, boolean selfLoop) {
        if (!selfLoop) {
            return point;
        }
        double half = SPATIAL_MARKER_SIZE / 2.0;
        double x = clamp(point.x() - SPATIAL_MARKER_SIZE * SPATIAL_SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR,
                half + 4,
                SPATIAL_IMAGE_FIT_WIDTH - half - 4);
        return new StagePoint(x, point.y());
    }

    private List<MarkerParticipant> markerParticipants(TheatreProjectLayer.TextActionPlacement placement) {
        if (placement == null) {
            return List.of();
        }
        LinkedHashMap<String, MarkerParticipant> result = new LinkedHashMap<>();
        String speaker = characterDisplayName(placement.characterId());
        if (!speaker.isBlank()) {
            result.put(speaker, new MarkerParticipant(speaker, locationFor(placement, speaker, placement.origin()),
                    TheatreSpatialRoleIcon.forSpeaker(speaker), true));
        }
        for (String target : interactionTargets(placement.interactionTarget())) {
            if (isSelfTarget(target)) {
                continue;
            }
            if (TheatreSpatialRoleIcon.isAudience(target)) {
                result.putIfAbsent("PUBLICO", new MarkerParticipant("PUBLICO", audienceLocation(),
                        TheatreSpatialRoleIcon.AUDIENCE, false));
                continue;
            }
            if (isOffstageTarget(target)) {
                continue;
            }
            String display = characterDisplayName(characterIdForDisplay(target, ""));
            if (display.isBlank()) {
                display = target.strip();
            }
            if (isCharacterAbsent(placement, display)) {
                continue;
            }
            if (!display.equalsIgnoreCase(speaker)) {
                result.put(display, new MarkerParticipant(display, locationFor(placement, display, placement.destination()),
                        TheatreSpatialRoleIcon.forSpeaker(display), false));
            }
        }
        if (TheatreSpatialRoleIcon.isAudience(placement.destination())) {
            result.putIfAbsent("PUBLICO", new MarkerParticipant("PUBLICO", audienceLocation(),
                    TheatreSpatialRoleIcon.AUDIENCE, false));
        }
        placement.characterLocations().forEach((character, location) -> {
            if (character == null || character.isBlank() || isSpecialTarget(character) || isAbsentLocation(location)) {
                return;
            }
            String display = characterDisplayName(characterIdForDisplay(character, ""));
            if (display.isBlank()) {
                display = character.strip();
            }
            result.putIfAbsent(display, new MarkerParticipant(display, location, TheatreSpatialRoleIcon.forSpeaker(display), false));
        });
        return List.copyOf(result.values());
    }

    private String locationFor(TheatreProjectLayer.TextActionPlacement placement, String character, String fallback) {
        for (Map.Entry<String, String> entry : placement.characterLocations().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(character)
                    && entry.getValue() != null
                    && !entry.getValue().isBlank()
                    && !isAbsentLocation(entry.getValue())) {
                return entry.getValue();
            }
        }
        return fallback == null || fallback.isBlank() ? "centro" : fallback;
    }

    private String characterIdForDisplay(String displayName, String fallback) {
        String normalized = displayName == null ? "" : displayName.strip();
        if (normalized.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        for (TheatreProjectLayer.CharacterProfile character : viewModel.theatreCharacterProfiles()) {
            if (character.displayName().equalsIgnoreCase(normalized)
                    || character.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(normalized))) {
                return character.id();
            }
        }
        return fallback == null ? "" : fallback;
    }

    private String characterDisplayName(String characterId) {
        String normalized = characterId == null ? "" : characterId.strip();
        if (normalized.isBlank()) {
            return "";
        }
        return viewModel.theatreCharacterProfiles().stream()
                .filter(character -> character.id().equals(normalized))
                .map(TheatreProjectLayer.CharacterProfile::displayName)
                .findFirst()
                .orElse("");
    }

    private static boolean isSpecialTarget(String target) {
        String normalized = target == null ? "" : target.strip().toLowerCase(Locale.ROOT);
        return normalized.equals("publico")
                || normalized.equals("público")
                || normalized.equals("para si mismo")
                || normalized.equals("para sí mismo")
                || normalized.equals("entidad no presente en escenario");
    }

    private static boolean isSelfTarget(String target) {
        String normalized = target == null ? "" : target.strip().toLowerCase(Locale.ROOT);
        return normalized.equals("para si mismo") || normalized.equals("para sí mismo");
    }

    private static boolean isOffstageTarget(String target) {
        return "entidad no presente en escenario".equals(target == null ? "" : target.strip().toLowerCase(Locale.ROOT));
    }

    private static boolean isAbsentLocation(String location) {
        String normalized = location == null ? "" : location.strip().toLowerCase(Locale.ROOT);
        return normalized.equals(ABSENT_LOCATION) || normalized.equals("no presente en esta intervencion");
    }

    private List<String> destinationLocations(TheatreProjectLayer.TextActionPlacement placement) {
        if (placement == null) {
            return List.of("centro");
        }
        ArrayList<String> result = new ArrayList<>();
        for (String target : interactionTargets(placement.interactionTarget())) {
            if (isSelfTarget(target)) {
                addUnique(result, placement.origin());
            } else if (TheatreSpatialRoleIcon.isAudience(target)) {
                addUnique(result, audienceLocation());
            } else if (isOffstageTarget(target)) {
                addUnique(result, placement.destination());
            } else {
                String display = characterDisplayName(characterIdForDisplay(target, ""));
                if (isCharacterAbsent(placement, display.isBlank() ? target : display)) {
                    continue;
                }
                addUnique(result, locationFor(placement, display.isBlank() ? target : display, placement.destination()));
            }
        }
        if (result.isEmpty()) {
            addUnique(result, placement.destination());
        }
        return List.copyOf(result);
    }

    private static boolean isCharacterAbsent(TheatreProjectLayer.TextActionPlacement placement, String character) {
        if (placement == null || placement.characterLocations() == null || character == null || character.isBlank()) {
            return false;
        }
        return placement.characterLocations().entrySet().stream()
                .anyMatch(entry -> entry.getKey().equalsIgnoreCase(character) && isAbsentLocation(entry.getValue()));
    }

    private static List<String> interactionTargets(String target) {
        String normalized = target == null ? "" : target.strip();
        if (normalized.isBlank()) {
            return List.of();
        }
        if (isSpecialTarget(normalized)) {
            return List.of(normalized);
        }
        String[] parts = normalized.split("\\s*(?:,|;|/|\\s+y\\s+)\\s*");
        ArrayList<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part.strip());
            }
        }
        return result.isEmpty() ? List.of(normalized) : List.copyOf(result);
    }

    private static String audienceLocation() {
        return "hacia el publico";
    }

    private static TheatreSpatialRoleIcon mergeRole(TheatreSpatialRoleIcon current, TheatreSpatialRoleIcon incoming) {
        if (current == TheatreSpatialRoleIcon.NARRATOR || incoming == TheatreSpatialRoleIcon.NARRATOR) {
            return TheatreSpatialRoleIcon.NARRATOR;
        }
        if (current == TheatreSpatialRoleIcon.AUDIENCE || incoming == TheatreSpatialRoleIcon.AUDIENCE) {
            return TheatreSpatialRoleIcon.AUDIENCE;
        }
        return TheatreSpatialRoleIcon.ACTOR;
    }

    private static void addUnique(List<String> values, String value) {
        if (isAbsentLocation(value)) {
            return;
        }
        String safe = value == null || value.isBlank() ? "centro" : value.strip();
        if (!values.contains(safe)) {
            values.add(safe);
        }
    }

    private static void drawCharacterLabel(GraphicsContext gc,
                                           StagePoint point,
                                           List<String> characters,
                                           List<String> speakers,
                                           int stackIndex) {
        int shown = Math.min(4, characters.size());
        double lineHeight = 17;
        double width = characterLabelWidth(characters);
        double height = Math.max(22, 9 + shown * lineHeight);
        double x = Math.max(4, Math.min(SPATIAL_IMAGE_FIT_WIDTH - width - 4, point.x() - width / 2 + (stackIndex % 2) * 8));
        double y = Math.max(4, Math.min(SPATIAL_IMAGE_FIT_HEIGHT - height - 4,
                point.y() + SPATIAL_MARKER_SIZE / 2.0 + 8 + (stackIndex * 14)));
        gc.setFill(Color.web("rgba(15, 23, 42, 0.76)"));
        gc.fillRoundRect(x, y, width, height, 5, 5);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
        for (int i = 0; i < shown; i++) {
            String character = characters.get(i) == null ? "" : characters.get(i);
            double textX = x + 8;
            if (containsIgnoreCase(speakers, character)) {
                double cy = y + 10 + i * lineHeight;
                gc.setFill(Color.web("#F59E0B"));
                gc.fillOval(x + 8, cy - 4, 7, 7);
                gc.setFill(Color.WHITE);
                textX = x + 20;
            }
            gc.fillText(character, textX, y + 14 + i * lineHeight);
        }
        if (characters.size() > shown) {
            gc.fillText("+" + (characters.size() - shown), x + 8, y + 14 + shown * lineHeight);
        }
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        if (values == null || candidate == null || candidate.isBlank()) {
            return false;
        }
        return values.stream().anyMatch(value -> candidate.equalsIgnoreCase(value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double characterLabelWidth(List<String> characters) {
        int maxLength = 0;
        for (String character : characters) {
            maxLength = Math.max(maxLength, character == null ? 0 : character.length());
        }
        return Math.max(146, Math.min(SPATIAL_IMAGE_FIT_WIDTH - 8, 22 + maxLength * 7.4));
    }

    private static StagePoint stagePoint(String location) {
        TheatreStageGeometry.StagePoint point = TheatreStageGeometry.pointFor(location);
        return new StagePoint(point.x(), point.y());
    }

    private static String cueLabel(String preview) {
        String text = preview == null ? "" : preview.strip();
        if (text.isBlank()) {
            return "";
        }
        if (text.startsWith("(") || text.startsWith("[")) {
            return "ACOTACION";
        }
        int colon = text.indexOf(':');
        if (colon <= 0 || colon > 42) {
            return "";
        }
        return text.substring(0, colon).strip();
    }

    private static String placementKey(String sceneId, String alias) {
        return (sceneId == null ? "" : sceneId) + "::" + (alias == null ? "" : alias);
    }

    private record StagePoint(double x, double y) {
    }

    private static final class MarkerGroup {
        private TheatreSpatialRoleIcon role;
        private final List<String> names;
        private final List<String> speakers = new ArrayList<>();
        private boolean selfLoop;

        private MarkerGroup(TheatreSpatialRoleIcon role, List<String> names) {
            this.role = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
            this.names = names == null ? new ArrayList<>() : names;
        }

        private TheatreSpatialRoleIcon role() {
            return role;
        }

        private List<String> names() {
            return names;
        }

        private List<String> speakers() {
            return speakers;
        }

        private boolean selfLoop() {
            return selfLoop;
        }
    }

    private record MarkerParticipant(String name, String location, TheatreSpatialRoleIcon role, boolean speaking) {
        private MarkerParticipant {
            name = name == null ? "" : name.strip();
            location = location == null || location.isBlank() ? "centro" : location.strip();
            role = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
        }
    }

    private static final class SceneActionCanvas extends Canvas {
        private final TheatreProjectLayer.Scene scene;
        private final List<IntervencionCatalogo.IntervencionInfo> aliases;
        private final StringProperty selectedIntervencion;
        private final Consumer<String> blockSelection;
        private final Consumer<IntervencionCatalogo.IntervencionInfo> editorIntervencion;
        private final List<TheatreZigzagLayout.HitBox> hitBoxes = new ArrayList<>();

        SceneActionCanvas(
                TheatreProjectLayer.Scene scene,
                List<IntervencionCatalogo.IntervencionInfo> aliases,
                StringProperty selectedIntervencion,
                Consumer<String> blockSelection,
                Consumer<IntervencionCatalogo.IntervencionInfo> editorIntervencion) {
            super(TheatreZigzagLayout.WIDTH,
                    TheatreZigzagLayout.heightFor(aliases == null ? 0 : aliases.size()));
            this.scene = scene;
            this.aliases = aliases == null ? List.of() : List.copyOf(aliases);
            this.selectedIntervencion = selectedIntervencion;
            this.blockSelection = blockSelection == null ? ignored -> { } : blockSelection;
            this.editorIntervencion = editorIntervencion == null ? ignored -> { } : editorIntervencion;
            getStyleClass().add("theatre-action-canvas");
            setOnMouseClicked(this::selectAliasAt);
            selectedIntervencion.addListener((obs, oldValue, newValue) -> draw());
            draw();
        }

        private void selectAliasAt(MouseEvent event) {
            for (TheatreZigzagLayout.HitBox hit : hitBoxes) {
                if (hit.containsEdit(event.getX(), event.getY())) {
                    aliases.stream()
                            .filter(alias -> alias.alias().equals(hit.alias()))
                            .findFirst()
                            .ifPresent(editorIntervencion);
                    event.consume();
                    return;
                }
                if (hit.contains(event.getX(), event.getY())) {
                    selectedIntervencion.set(hit.alias());
                    if (!hit.blockId().isBlank()) {
                        blockSelection.accept(hit.blockId());
                    }
                    event.consume();
                    return;
                }
            }
            TheatreCanvasSelectionSupport.clearOnEmptyPrimaryClick(event, selectedIntervencion, blockSelection);
        }

        private void draw() {
            GraphicsContext gc = getGraphicsContext2D();
            double width = getWidth();
            double height = getHeight();
            hitBoxes.clear();
            gc.clearRect(0, 0, width, height);
            gc.setFill(Color.web("#F8FAFE"));
            gc.fillRoundRect(0, 0, width, height, 10, 10);
            gc.setStroke(Color.web("#D8E0EA"));
            gc.strokeRoundRect(0.5, 0.5, width - 1, height - 1, 10, 10);

            gc.setFill(Color.web("#334155"));
            gc.setFont(javafx.scene.text.Font.font("System", 11));
            gc.fillText(scene.displayName(), 14, 18);
            if (aliases.isEmpty()) {
                gc.setFill(Color.web("#64748B"));
                gc.fillText("Prepara la lectura para dibujar Intervencion 1, Intervencion 2 y siguientes.", 14, 76);
                return;
            }

            List<TheatreZigzagLayout.StagePoint> points = TheatreZigzagLayout.pointsFor(aliases.size(), width);
            for (int i = 0; i < points.size() - 1; i++) {
                TheatreZigzagLayout.drawArrow(gc, points.get(i), points.get(i + 1));
            }
            for (int i = 0; i < points.size(); i++) {
                IntervencionCatalogo.IntervencionInfo alias = aliases.get(i);
                TheatreZigzagLayout.StagePoint point = points.get(i);
                boolean selected = alias.alias().equals(selectedIntervencion.get());
                gc.setFill(Color.web(selected ? "#DCFCE7" : "#FFFFFF"));
                gc.setStroke(Color.web(selected ? "#16A34A" : "#8FB2EA"));
                gc.setLineWidth(selected ? 2.5 : 1.5);
                gc.fillRoundRect(point.x(), point.y(), point.width(), point.height(), 8, 8);
                gc.strokeRoundRect(point.x(), point.y(), point.width(), point.height(), 8, 8);
                gc.setFill(Color.web(selected ? "#166534" : "#1E3A8A"));
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
                gc.fillText(TheatreZigzagLayout.displayLabel(alias.alias()), point.x() + 12, point.y() + 21);
                String cue = TheatreZigzagLayout.speakerLabel(alias.preview());
                if (!cue.isBlank()) {
                    gc.setFill(Color.web(selected ? "#15803D" : "#475569"));
                    gc.setFont(javafx.scene.text.Font.font("System", 9.5));
                    gc.fillText(TheatreZigzagLayout.ellipsize(cue, 15), point.x() + 12, point.y() + 42);
                }
                double editX = point.x() + point.width() - 22;
                double editY = point.y() + 6;
                TheatreZigzagLayout.drawEditActionButton(gc, editX, editY);
                hitBoxes.add(new TheatreZigzagLayout.HitBox(alias.alias(), alias.blockId(), point.x(), point.y(), point.width(), point.height(),
                        editX, editY, TheatreZigzagLayout.ACTION_BUTTON_SIZE, TheatreZigzagLayout.ACTION_BUTTON_SIZE,
                        -100, -100, 0, 0));
            }
        }
    }
}
