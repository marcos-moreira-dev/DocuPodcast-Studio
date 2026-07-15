package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Sober left navigation for the theatrical AI micro-workspace. */
final class TheatreAiModuleNavigation extends VBox {
    TheatreAiModuleNavigation(List<TheatreAiModuleDescriptor> descriptors, ObjectProperty<TheatreAiModuleId> selectedModule) {
        super(8);
        getStyleClass().add("voice-module-navigation");
        setPadding(new Insets(14, 12, 14, 12));
        setFillWidth(true);
        setMaxHeight(Double.MAX_VALUE);
        ToggleGroup group = new ToggleGroup();
        String currentGroup = "";
        for (TheatreAiModuleDescriptor descriptor : descriptors) {
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
