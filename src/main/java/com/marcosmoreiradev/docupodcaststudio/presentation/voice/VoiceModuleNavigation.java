package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Left navigation for the modular Voices micro-workspace. Rows are intentionally sober. */
public final class VoiceModuleNavigation extends VBox {
    public VoiceModuleNavigation(List<VoiceModuleDescriptor> descriptors, ObjectProperty<VoiceModuleId> selectedModule) {
        super(8);
        getStyleClass().add("voice-module-navigation");
        setPadding(new Insets(14, 12, 14, 12));
        setFillWidth(true);
        setMaxHeight(Double.MAX_VALUE);
        ToggleGroup group = new ToggleGroup();
        String currentGroup = "";
        for (VoiceModuleDescriptor descriptor : descriptors) {
            if (!descriptor.group().equals(currentGroup)) {
                currentGroup = descriptor.group();
                Label groupLabel = new Label(currentGroup);
                groupLabel.getStyleClass().add("voice-module-group-label");
                getChildren().add(groupLabel);
            }
            ToggleButton button = new ToggleButton(descriptor.title());
            button.getStyleClass().add("voice-module-row");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setTooltip(new Tooltip(descriptor.description()));
            button.setToggleGroup(group);
            button.setSelected(selectedModule.get() == descriptor.id());
            button.setOnAction(event -> selectedModule.set(descriptor.id()));
            selectedModule.addListener((obs, oldValue, newValue) -> button.setSelected(newValue == descriptor.id()));
            getChildren().add(button);
        }
        Region spacer = new Region();
        spacer.getStyleClass().add("voice-module-navigation-spacer");
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);
    }
}
