package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentBackgroundImageFit;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Shared image visibility and fitting choices for Express and documentary video. */
public final class StudioImageBackgroundControls {
    public final ComboBox<Integer> opacity = StudioFormControls.comboBox();
    public final ToggleGroup fitGroup = new ToggleGroup();
    public final RadioButton contain = choice("Imagen completa con franjas", DocumentBackgroundImageFit.CONTAIN);
    public final RadioButton cover = choice("Rellenar con recorte/zoom", DocumentBackgroundImageFit.COVER);
    public final RadioButton blurred = choice("Fondo difuminado + imagen completa", DocumentBackgroundImageFit.BLUR_AND_CONTAIN);

    public StudioImageBackgroundControls() {
        opacity.getItems().setAll(10, 20, 35, 50, 65, 80, 100);
        opacity.setConverter(new StringConverter<>() {
            public String toString(Integer value) { return value == null ? "" : value + "%"; }
            public Integer fromString(String value) { return Integer.valueOf(value.replace("%", "").strip()); }
        });
        opacity.setValue(35);
        opacity.setMaxWidth(Double.MAX_VALUE);
        cover.setSelected(true);
    }
    private RadioButton choice(String label, DocumentBackgroundImageFit fit) {
        RadioButton button = StudioFormControls.radioButton(label);
        button.setWrapText(true);
        button.setToggleGroup(fitGroup);
        button.setUserData(fit);
        return button;
    }
    public VBox fitChoices() { return new VBox(7, contain, cover, blurred); }
    public DocumentBackgroundImageFit fit() {
        return fitGroup.getSelectedToggle() == null ? DocumentBackgroundImageFit.COVER
                : (DocumentBackgroundImageFit) fitGroup.getSelectedToggle().getUserData();
    }
    public void setFit(DocumentBackgroundImageFit fit) {
        fitGroup.getToggles().stream().filter(t -> t.getUserData() == fit).findFirst().ifPresent(fitGroup::selectToggle);
    }
}
