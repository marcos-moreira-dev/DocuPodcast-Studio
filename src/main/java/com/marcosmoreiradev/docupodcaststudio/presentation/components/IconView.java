package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reusable PNG icon node for ribbon, sidebars and rails. Resources live under /icons/ui/. */
public final class IconView extends StackPane {
    private static final Map<String, Image> IMAGES = new ConcurrentHashMap<>();

    private static Image loadImage(AppIcon icon, double size) {
        return IMAGES.computeIfAbsent(icon.resourcePath() + "@" + size, key -> {
            try (InputStream stream = IconView.class.getResourceAsStream(icon.resourcePath())) {
                if (stream == null) return null;
                Image image = new Image(stream, size, size, true, true);
                return image.isError() ? null : image;
            } catch (IOException exception) {
                return null;
            }
        });
    }
    private IconView(AppIcon icon, double size, String styleClass) {
        AppIcon resolved = icon == null ? AppIcon.DEFAULT : icon;
        getStyleClass().add(AppStyles.UI_ICON_VIEW);
        if (resolved.isProductAsset()) {
            getStyleClass().add(AppStyles.UI_ICON_PRODUCT);
        }
        if (styleClass != null && !styleClass.isBlank()) {
            getStyleClass().add(styleClass);
        }
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setAlignment(Pos.CENTER);
        setAccessibleText(resolved.accessibleText());

        Image image = loadImage(resolved, size);
        if (image == null) {
            Label fallback = new Label("•");
            fallback.getStyleClass().add(AppStyles.UI_ICON_FALLBACK);
            getChildren().add(fallback);
            return;
        }
        // Product illustrations are authored at high resolution. Decode them at
        // their actual UI size so repeated ribbon/dock instances do not retain
        // full 1254 px bitmaps in memory.
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(size);
        view.setFitHeight(size);
        getChildren().add(view);
    }

    public static IconView ribbon(AppIcon icon) {
        return new IconView(icon, productSize(icon, 48, 34), AppStyles.UI_ICON_RIBBON);
    }

    public static IconView sideDock(AppIcon icon) {
        return new IconView(icon, productSize(icon, 44, 30), AppStyles.UI_ICON_SIDE_DOCK);
    }

    public static IconView rail(AppIcon icon) {
        return new IconView(icon, productSize(icon, 32, 24), AppStyles.UI_ICON_RAIL);
    }

    public static IconView welcome(AppIcon icon) {
        return new IconView(icon, productSize(icon, 54, 34), AppStyles.UI_ICON_WELCOME);
    }

    private static double productSize(AppIcon icon, double productSize, double regularSize) {
        AppIcon resolved = icon == null ? AppIcon.DEFAULT : icon;
        return resolved.isProductAsset() ? productSize : regularSize;
    }
}
