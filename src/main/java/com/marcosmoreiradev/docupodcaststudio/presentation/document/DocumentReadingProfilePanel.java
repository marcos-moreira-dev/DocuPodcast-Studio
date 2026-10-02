package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;

import com.marcosmoreiradev.docupodcaststudio.application.reading.ReadingProfilePreview;
import com.marcosmoreiradev.docupodcaststudio.application.reading.ReadingProfilePreviewItem;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.HeadingDetectionRules;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ImageNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Editable Reading Profile panel with preview before applying the rules to the imported document. */
public final class DocumentReadingProfilePanel extends ScrollPane {
    private final ObservableValue<ReadableDocument> documentProperty;
    private final ObservableValue<ReadingProfile> profileProperty;
    private final Consumer<ReadingProfile> onProfileSaved;
    private final Consumer<ReadingProfile> onProfileApplied;
    private final Function<ReadingProfile, ReadingProfilePreview> previewProvider;
    private final VBox content = new VBox(8);

    public DocumentReadingProfilePanel(ObservableValue<ReadableDocument> documentProperty,
                                       ObservableValue<ReadingProfile> profileProperty,
                                       Consumer<ReadingProfile> onProfileSaved,
                                       Consumer<ReadingProfile> onProfileApplied,
                                       Function<ReadingProfile, ReadingProfilePreview> previewProvider) {
        this.documentProperty = Objects.requireNonNull(documentProperty, "documentProperty");
        this.profileProperty = Objects.requireNonNull(profileProperty, "profileProperty");
        this.onProfileSaved = Objects.requireNonNull(onProfileSaved, "onProfileSaved");
        this.onProfileApplied = Objects.requireNonNull(onProfileApplied, "onProfileApplied");
        this.previewProvider = Objects.requireNonNull(previewProvider, "previewProvider");
        getStyleClass().add("document-side-scroll");
        content.getStyleClass().add("document-side-content");
        setFitToWidth(true);
        setContent(content);
        ChangeListener<Object> listener = (obs, oldValue, newValue) -> render();
        documentProperty.addListener(listener);
        profileProperty.addListener(listener);
        render();
    }

    private void render() {
        content.getChildren().clear();
        ReadingProfile profile = profileProperty.getValue() == null ? ReadingProfile.academicDefaults() : profileProperty.getValue();
        ReadableDocument document = documentProperty.getValue();

        title("Perfil de lectura");
        muted("Ajusta cómo DocuPodcast interpreta tu Word antes de preparar la lectura. Los cambios manuales por bloque se respetan.");

        TextField name = StudioFormControls.textField(profile.name());
        name.setMaxWidth(Double.MAX_VALUE);
        TextArea description = StudioFormControls.textArea(profile.description());
        description.setWrapText(true);
        description.setPrefRowCount(3);

        TextField titleKeywords = StudioFormControls.textField(toCsv(profile.headingRules().titleStyleKeywords()));
        TextField headingKeywords = StudioFormControls.textField(toCsv(profile.headingRules().headingStyleKeywords()));
        TextField subheadingKeywords = StudioFormControls.textField(toCsv(profile.headingRules().subheadingStyleKeywords()));
        Spinner<Integer> maxBoldWords = StudioFormControls.spinner(1, 60, profile.headingRules().maxShortBoldWords());
        maxBoldWords.setEditable(true);
        CheckBox shortBoldAsSubheading = StudioFormControls.checkBox("Texto breve en negrita puede ser subtítulo");
        shortBoldAsSubheading.setSelected(profile.headingRules().treatShortBoldParagraphAsSubheading());

        ComboBox<ImageNarrationPolicy> imagePolicy = StudioFormControls.comboBox(FXCollections.observableArrayList(ImageNarrationPolicy.values()));
        imagePolicy.setValue(profile.imagePolicy());
        imagePolicy.setMaxWidth(Double.MAX_VALUE);
        ComboBox<TableNarrationPolicy> tablePolicy = StudioFormControls.comboBox(FXCollections.observableArrayList(TableNarrationPolicy.values()));
        tablePolicy.setValue(profile.tablePolicy());
        tablePolicy.setMaxWidth(Double.MAX_VALUE);

        section("Identidad");
        content.getChildren().add(fieldGrid(
                row("Nombre", name),
                row("Descripción", description)
        ));

        section("Reglas de estructura");
        content.getChildren().add(fieldGrid(
                row("Título principal", titleKeywords),
                row("Título", headingKeywords),
                row("Subtítulo", subheadingKeywords),
                row("Máx. palabras negrita", maxBoldWords)
        ));
        content.getChildren().add(shortBoldAsSubheading);

        section("Imágenes y tablas");
        content.getChildren().add(fieldGrid(
                row("Imágenes", imagePolicy),
                row("Tablas", tablePolicy)
        ));

        VBox previewBox = new VBox(5);
        previewBox.getStyleClass().add("document-reading-profile-preview");
        renderPreview(previewBox, buildProfile(profile, name, description, titleKeywords, headingKeywords, subheadingKeywords,
                maxBoldWords, shortBoldAsSubheading, imagePolicy, tablePolicy), document);

        var preview = ActionButtonFactory.secondary("Previsualizar impacto", () -> renderPreview(previewBox, buildProfile(profile, name, description, titleKeywords,
                headingKeywords, subheadingKeywords, maxBoldWords, shortBoldAsSubheading, imagePolicy, tablePolicy), document));
        preview.setDisable(document == null);

        var save = ActionButtonFactory.secondary("Guardar perfil en el proyecto", () -> onProfileSaved.accept(buildProfile(profile, name, description, titleKeywords,
                headingKeywords, subheadingKeywords, maxBoldWords, shortBoldAsSubheading, imagePolicy, tablePolicy)));

        var apply = ActionButtonFactory.primary("Guardar y aplicar al documento", () -> onProfileApplied.accept(buildProfile(profile, name, description, titleKeywords,
                headingKeywords, subheadingKeywords, maxBoldWords, shortBoldAsSubheading, imagePolicy, tablePolicy)));
        apply.setDisable(document == null);

        section("Previsualización");
        content.getChildren().addAll(preview, previewBox, save, apply);
    }

    private ReadingProfile buildProfile(ReadingProfile base,
                                        TextField name,
                                        TextArea description,
                                        TextField titleKeywords,
                                        TextField headingKeywords,
                                        TextField subheadingKeywords,
                                        Spinner<Integer> maxBoldWords,
                                        CheckBox shortBoldAsSubheading,
                                        ComboBox<ImageNarrationPolicy> imagePolicy,
                                        ComboBox<TableNarrationPolicy> tablePolicy) {
        return new ReadingProfile(
                base.id(),
                name.getText(),
                description.getText(),
                new HeadingDetectionRules(
                        csvToList(titleKeywords.getText()),
                        csvToList(headingKeywords.getText()),
                        csvToList(subheadingKeywords.getText()),
                        maxBoldWords.getValue(),
                        shortBoldAsSubheading.isSelected()
                ),
                imagePolicy.getValue(),
                tablePolicy.getValue()
        );
    }

    private void renderPreview(VBox previewBox, ReadingProfile profile, ReadableDocument document) {
        previewBox.getChildren().clear();
        if (document == null) {
            muted(previewBox, "Abre un Word/DOCX para previsualizar cómo el perfil reclasifica bloques.");
            return;
        }
        ReadingProfilePreview preview = previewProvider.apply(profile);
        muted(previewBox, "Cambios previstos: " + preview.changedCount()
                + " · overrides manuales respetados: " + preview.manualOverrideCount()
                + " · títulos/subtítulos propuestos: "
                + (preview.proposedCount(DocumentBlockType.TITLE) + preview.proposedCount(DocumentBlockType.HEADING) + preview.proposedCount(DocumentBlockType.SUBHEADING))
                + " · ignorados: " + preview.proposedCount(DocumentBlockType.IGNORED));
        int shown = 0;
        for (ReadingProfilePreviewItem item : preview.changedItems()) {
            if (shown >= 12) {
                muted(previewBox, "…y " + (preview.changedCount() - shown) + " cambios más.");
                break;
            }
            muted(previewBox, item.blockId() + ": " + item.currentType().displayName() + " → "
                    + item.proposedType().displayName() + " · " + item.previewText());
            shown++;
        }
        if (preview.changedCount() == 0) {
            muted(previewBox, "El perfil no cambiaría la clasificación actual del documento.");
        }
    }

    private GridPane fieldGrid(Object[]... rows) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("document-profile-grid");
        grid.setHgap(6);
        grid.setVgap(6);
        for (int i = 0; i < rows.length; i++) {
            Label label = new Label((String) rows[i][0]);
            label.getStyleClass().add("document-side-muted");
            grid.add(label, 0, i);
            grid.add((javafx.scene.Node) rows[i][1], 1, i);
        }
        return grid;
    }

    private Object[] row(String label, javafx.scene.Node node) {
        return new Object[]{label, node};
    }

    private void title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-side-title");
        content.getChildren().add(label);
    }

    private void section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("document-side-subtitle");
        content.getChildren().add(label);
    }

    private void muted(String text) {
        muted(content, text);
    }

    private void muted(VBox target, String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("document-side-muted");
        target.getChildren().add(label);
    }

    private static String toCsv(List<String> values) {
        return String.join(", ", values);
    }

    private static List<String> csvToList(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
