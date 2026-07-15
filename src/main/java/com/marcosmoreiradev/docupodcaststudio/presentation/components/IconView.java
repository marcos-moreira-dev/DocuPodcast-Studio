package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;

/** Reusable PNG icon node for ribbon, sidebars and rails. Resources live under /icons/ui/. */
public final class IconView extends StackPane {
    private IconView(AppIcon icon, double size, String styleClass) {
        AppIcon resolved = icon == null ? AppIcon.DEFAULT : icon;
        getStyleClass().add(AppStyles.UI_ICON_VIEW);
        if (styleClass != null && !styleClass.isBlank()) {
            getStyleClass().add(styleClass);
        }
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setAlignment(Pos.CENTER);
        setAccessibleText(resolved.accessibleText());

        InputStream stream = IconView.class.getResourceAsStream(resolved.resourcePath());
        if (stream == null) {
            Label fallback = new Label("•");
            fallback.getStyleClass().add(AppStyles.UI_ICON_FALLBACK);
            getChildren().add(fallback);
            return;
        }
        Image image = new Image(stream);
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(size);
        view.setFitHeight(size);
        getChildren().add(view);
    }

    public static IconView ribbon(AppIcon icon) {
        return new IconView(icon, 34, AppStyles.UI_ICON_RIBBON);
    }

    public static IconView sideDock(AppIcon icon) {
        return new IconView(icon, 30, AppStyles.UI_ICON_SIDE_DOCK);
    }

    public static IconView rail(AppIcon icon) {
        return new IconView(icon, 24, AppStyles.UI_ICON_RAIL);
    }
}
