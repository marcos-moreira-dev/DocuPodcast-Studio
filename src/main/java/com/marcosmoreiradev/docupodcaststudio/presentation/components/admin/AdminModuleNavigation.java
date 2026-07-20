package com.marcosmoreiradev.docupodcaststudio.presentation.components.admin;

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
import java.util.Objects;

/** Keyboard-accessible left navigation shared by administrative mini-applications. */
public class AdminModuleNavigation<M> extends VBox {
    public AdminModuleNavigation(List<? extends AdminModuleSpec<M>> descriptors,
                                 ObjectProperty<M> selectedModule) {
        super(8);
        Objects.requireNonNull(selectedModule, "selectedModule");
        getStyleClass().add("voice-module-navigation");
        setPadding(new Insets(14, 12, 14, 12));
        setFillWidth(true);
        setMaxHeight(Double.MAX_VALUE);
        ToggleGroup toggleGroup = new ToggleGroup();
        String currentGroup = "";
        for (AdminModuleSpec<M> descriptor : descriptors == null
                ? List.<AdminModuleSpec<M>>of() : descriptors) {
            if (!descriptor.group().equals(currentGroup)) {
                currentGroup = descriptor.group();
                Label groupLabel = new Label(currentGroup);
                groupLabel.getStyleClass().add("voice-module-group-label");
                getChildren().add(groupLabel);
            }
            ToggleButton button = new ToggleButton(descriptor.title());
            button.getStyleClass().add("voice-module-row");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAccessibleText(descriptor.title() + ". " + descriptor.description());
            button.setTooltip(new Tooltip(descriptor.description()));
            button.setToggleGroup(toggleGroup);
            button.setSelected(Objects.equals(selectedModule.get(), descriptor.id()));
            button.setOnAction(event -> {
                if (!button.isSelected()) button.setSelected(true);
                selectedModule.set(descriptor.id());
            });
            selectedModule.addListener((obs, oldValue, newValue) ->
                    button.setSelected(Objects.equals(newValue, descriptor.id())));
            getChildren().add(button);
        }
        Region spacer = new Region();
        spacer.getStyleClass().add("voice-module-navigation-spacer");
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);
    }
}
