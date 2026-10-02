package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/**
 * Central factory for product action buttons.
 *
 * <p>Workspaces must not hand-build repeated action buttons with scattered style classes. They
 * should request an intent-oriented button here (primary, secondary, danger, rail or transport)
 * and keep only domain-specific enablement/handlers in the workspace.</p>
 */
public final class ActionButtonFactory {
    private ActionButtonFactory() {
    }

    public static Button primary(String text, Runnable action) {
        return action(text, action, AppStyles.UI_ACTION_BUTTON, AppStyles.UI_ACTION_BUTTON_PRIMARY);
    }

    public static Button primary(String text) {
        return primary(text, (Runnable) null);
    }

    public static Button primary(String text, String tooltip, Runnable action) {
        Button button = primary(text, action);
        installTooltip(button, tooltip);
        return button;
    }

    public static Button secondary(String text, Runnable action) {
        return action(text, action, AppStyles.UI_ACTION_BUTTON, AppStyles.UI_ACTION_BUTTON_SECONDARY);
    }

    public static Button secondary(String text) {
        return secondary(text, (Runnable) null);
    }

    public static Button secondary(String text, String tooltip, Runnable action) {
        Button button = secondary(text, action);
        installTooltip(button, tooltip);
        return button;
    }

    public static Button warning(String text, Runnable action) {
        return action(text, action, AppStyles.UI_ACTION_BUTTON, AppStyles.UI_ACTION_BUTTON_WARNING);
    }

    public static Button warning(String text, String tooltip, Runnable action) {
        Button button = warning(text, action);
        installTooltip(button, tooltip);
        return button;
    }

    public static Button danger(String text, Runnable action) {
        return action(text, action, AppStyles.UI_ACTION_BUTTON, AppStyles.UI_ACTION_BUTTON_DANGER);
    }

    public static Button danger(String text) {
        return danger(text, (Runnable) null);
    }

    public static Button danger(String text, String tooltip, Runnable action) {
        Button button = danger(text, action);
        installTooltip(button, tooltip);
        return button;
    }

    public static Button rail(String text, Runnable action) {
        return action(text, action, AppStyles.UI_ACTION_BUTTON, AppStyles.UI_RAIL_ACTION_BUTTON);
    }

    public static Button transport(String text, Runnable action) {
        return action(text, action, AppStyles.UI_ACTION_BUTTON, AppStyles.UI_TRANSPORT_BUTTON);
    }

    public static Button transportIcon(AppIcon icon, String tooltip, Runnable action) {
        Button button = action("", action, AppStyles.UI_ACTION_BUTTON, AppStyles.UI_TRANSPORT_BUTTON, "ui-transport-icon-button");
        button.setGraphic(IconView.rail(icon));
        installTooltip(button, tooltip);
        return button;
    }

    public static Button sideDockRail(String text, Runnable action) {
        return action(text, action, AppStyles.UI_SIDE_DOCK_RAIL_BUTTON);
    }

    public static Button sideDockRail(AppIcon icon, Runnable action) {
        Button button = action("", action, AppStyles.UI_SIDE_DOCK_RAIL_BUTTON);
        button.setGraphic(IconView.sideDock(icon));
        installTooltip(button, icon == null ? AppIcon.DEFAULT.accessibleText() : icon.accessibleText());
        return button;
    }

    public static Button sideDockRail(AppIcon icon, String tooltip, Runnable action) {
        Button button = sideDockRail(icon, action);
        installTooltip(button, tooltip);
        return button;
    }

    public static Button iconOnly(AppIcon icon, Runnable action, String... styleClasses) {
        Button button = action("", action, styleClasses);
        button.setGraphic(IconView.rail(icon));
        button.setAccessibleText(icon == null ? AppIcon.DEFAULT.accessibleText() : icon.accessibleText());
        return button;
    }

    public static Button iconOnly(AppIcon icon, String tooltip, Runnable action, String... styleClasses) {
        Button button = iconOnly(icon, action, styleClasses);
        installTooltip(button, tooltip);
        return button;
    }

    public static Button sideDockHide(String text, Runnable action) {
        return action(text, action, AppStyles.UI_SIDE_DOCK_HIDE_BUTTON);
    }

    private static void installTooltip(Button button, String text) {
        if (button != null && text != null && !text.isBlank()) {
            Tooltip tooltip = new Tooltip(text);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(360);
            button.setTooltip(tooltip);
            button.setAccessibleText(text);
        }
    }

    private static Button action(String text, Runnable action, String... styleClasses) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClasses);
        button.setWrapText(true);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setMinHeight(Region.USE_PREF_SIZE);
        button.setPrefHeight(Region.USE_COMPUTED_SIZE);
        button.setMaxWidth(Double.MAX_VALUE);
        if (action != null) {
            button.setOnAction(event -> action.run());
        }
        SemanticActionIcons.decorate(button, text);
        StudioControlVariant variant = java.util.Arrays.asList(styleClasses).contains(AppStyles.UI_ACTION_BUTTON_DANGER)
                ? StudioControlVariant.DANGER
                : java.util.Arrays.asList(styleClasses).contains(AppStyles.UI_ACTION_BUTTON_WARNING)
                ? StudioControlVariant.WARNING
                : java.util.Arrays.asList(styleClasses).contains(AppStyles.UI_ACTION_BUTTON_PRIMARY)
                ? StudioControlVariant.PRIMARY : StudioControlVariant.SECONDARY;
        return StudioControlContract.mark(button, StudioControlFamily.ACTION, variant, StudioControlDensity.REGULAR);
    }
}
