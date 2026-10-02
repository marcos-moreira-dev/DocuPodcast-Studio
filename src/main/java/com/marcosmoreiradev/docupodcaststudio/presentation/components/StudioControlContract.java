package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.Control;

import java.util.Optional;

/** Attaches and reads the runtime proof used by the GUI audit. */
public final class StudioControlContract {
    public static final String PROPERTY_KEY = StudioControlContract.class.getName() + ".descriptor";
    public static final String STYLE_CLASS = "ui-studio-control";

    private StudioControlContract() { }

    public static <T extends Control> T mark(T control, StudioControlFamily family,
                                             StudioControlVariant variant, StudioControlDensity density) {
        if (control == null) return null;
        String accessible = control.getAccessibleText();
        if (accessible == null || accessible.isBlank()) accessible = control.getClass().getSimpleName();
        control.getProperties().put(PROPERTY_KEY,
                new StudioControlDescriptor(family, variant, density, accessible, ""));
        if (!control.getStyleClass().contains(STYLE_CLASS)) control.getStyleClass().add(STYLE_CLASS);
        return control;
    }

    public static void enrich(Node node, String accessibleName, String helpText) {
        if (!(node instanceof Control control)) return;
        descriptor(control).ifPresent(descriptor -> control.getProperties().put(PROPERTY_KEY,
                descriptor.withHelp(accessibleName, helpText)));
    }

    public static Optional<StudioControlDescriptor> descriptor(Control control) {
        if (control == null) return Optional.empty();
        Object value = control.getProperties().get(PROPERTY_KEY);
        return value instanceof StudioControlDescriptor descriptor ? Optional.of(descriptor) : Optional.empty();
    }
}
