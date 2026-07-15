package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Reusable compact card for visual media rails. */
public final class MediaThumbnailCard extends HBox {
    private static final double THUMBNAIL_WIDTH = 84;
    private static final double THUMBNAIL_HEIGHT = 54;

    private final StackPane thumbnailBox = new StackPane();
    private final ImageView thumbnailImage = new ImageView();
    private final Label thumbnailPlaceholder = new Label();
    private final Label titleLabel = new Label();
    private final Label relationLabel = new Label();
    private final Label descriptionLabel = new Label();
    private final List<String> dynamicStateClasses = new ArrayList<>();

    public MediaThumbnailCard(
            String imageFileUri,
            String placeholderText,
            String title,
            String relation,
            String description,
            String stateClass,
            Runnable action) {
        this(imageFileUri, placeholderText, title, relation, description, stateClass, action, "", "", null);
    }

    public MediaThumbnailCard(
            String imageFileUri,
            String placeholderText,
            String title,
            String relation,
            String description,
            String stateClass,
            Runnable action,
            String badgeText,
            String cornerTooltip,
            Runnable cornerAction) {
        this(imageFileUri, placeholderText, title, relation, description, stateClass, action,
                badgeText, cornerTooltip, cornerAction, null, "", null);
    }

    public MediaThumbnailCard(
            String imageFileUri,
            String placeholderText,
            String title,
            String relation,
            String description,
            String stateClass,
            Runnable action,
            String badgeText,
            String cornerTooltip,
            Runnable cornerAction,
            AppIcon emptyActionIcon,
            String emptyActionTooltip,
            Runnable emptyAction) {
        super(8);
        getStyleClass().add(AppStyles.UI_MEDIA_THUMBNAIL_CARD);
        setAlignment(Pos.TOP_LEFT);
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        configureThumbnailSkeleton();

        VBox copy = new VBox(3);
        copy.setMinWidth(0);
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.getStyleClass().add(AppStyles.UI_MEDIA_CARD_COPY);
        configureCopyLabel(titleLabel, AppStyles.UI_MEDIA_CARD_TITLE);
        configureCopyLabel(relationLabel, AppStyles.UI_MEDIA_CARD_RELATION);
        configureCopyLabel(descriptionLabel, AppStyles.UI_MEDIA_CARD_DESCRIPTION);
        copy.getChildren().addAll(titleLabel, relationLabel, descriptionLabel);
        HBox.setHgrow(copy, Priority.ALWAYS);
        getChildren().addAll(thumbnailBox, copy);

        Tooltip.install(this, new Tooltip("Seleccionar el contenido o medio relacionado"));
        update(imageFileUri, placeholderText, title, relation, description, stateClass, action,
                badgeText, cornerTooltip, cornerAction, emptyActionIcon, emptyActionTooltip, emptyAction);
    }

    /** Updates an existing card without replacing its ImageView or dropping the current bitmap. */
    public void update(
            String imageFileUri,
            String placeholderText,
            String title,
            String relation,
            String description,
            String stateClass,
            Runnable action,
            String badgeText,
            String cornerTooltip,
            Runnable cornerAction,
            AppIcon emptyActionIcon,
            String emptyActionTooltip,
            Runnable emptyAction) {
        updateStateClasses(stateClass);
        setOnMouseClicked(action == null ? null : event -> action.run());
        titleLabel.setText(safe(title, "Medio"));
        relationLabel.setText(safe(relation, "Sin fragmento asociado"));
        descriptionLabel.setText(safe(description, "Sin descripcion."));
        updateThumbnail(imageFileUri, placeholderText);
        thumbnailBox.getChildren().setAll(thumbnailImage, thumbnailPlaceholder);
        addThumbnailOverlays(thumbnailBox, badgeText, cornerTooltip, cornerAction,
                emptyActionIcon, emptyActionTooltip, emptyAction);
    }

    private void configureThumbnailSkeleton() {
        thumbnailBox.getStyleClass().add(AppStyles.UI_MEDIA_THUMBNAIL);
        thumbnailBox.setMinSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        thumbnailBox.setPrefSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        thumbnailImage.setFitWidth(THUMBNAIL_WIDTH);
        thumbnailImage.setFitHeight(THUMBNAIL_HEIGHT);
        thumbnailImage.setPreserveRatio(true);
        thumbnailPlaceholder.getStyleClass().add(AppStyles.UI_MEDIA_THUMBNAIL_LABEL);
        thumbnailPlaceholder.getStyleClass().add("document-media-thumbnail-label");
    }

    private static void configureCopyLabel(Label label, String styleClass) {
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
    }

    private void updateThumbnail(String imageFileUri, String placeholderText) {
        boolean hasImage = imageFileUri != null && !imageFileUri.isBlank();
        thumbnailBox.getStyleClass().removeAll("document-media-thumbnail", "document-media-thumbnail-empty");
        thumbnailBox.getStyleClass().add(hasImage ? "document-media-thumbnail" : "document-media-thumbnail-empty");
        thumbnailPlaceholder.setText(safe(placeholderText, "Medio"));
        if (!hasImage) {
            StableImageLoader.shared().load(thumbnailImage, "", THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true, ignored -> {});
            showThumbnailPlaceholder(true);
            return;
        }
        StableImageLoader.shared().load(
                thumbnailImage,
                imageFileUri,
                THUMBNAIL_WIDTH,
                THUMBNAIL_HEIGHT,
                true,
                state -> showThumbnailPlaceholder((state == StableImageLoader.LoadState.ERROR && thumbnailImage.getImage() == null)
                        || (state == StableImageLoader.LoadState.LOADING && thumbnailImage.getImage() == null)));
    }

    private void showThumbnailPlaceholder(boolean show) {
        thumbnailImage.setVisible(!show);
        thumbnailPlaceholder.setVisible(show);
        thumbnailPlaceholder.setManaged(show);
    }

    private void updateStateClasses(String stateClass) {
        getStyleClass().removeAll(dynamicStateClasses);
        dynamicStateClasses.clear();
        if (stateClass == null || stateClass.isBlank()) {
            return;
        }
        for (String cssClass : stateClass.split("\\s+")) {
            if (!cssClass.isBlank()) {
                dynamicStateClasses.add(cssClass);
                getStyleClass().add(cssClass);
            }
        }
    }

    private void addThumbnailOverlays(
            StackPane box,
            String badgeText,
            String cornerTooltip,
            Runnable cornerAction,
            AppIcon emptyActionIcon,
            String emptyActionTooltip,
            Runnable emptyAction) {
        if (badgeText != null && !badgeText.isBlank()) {
            Label badge = new Label(badgeText);
            badge.getStyleClass().add("document-media-thumbnail-badge");
            StackPane.setAlignment(badge, Pos.BOTTOM_LEFT);
            box.getChildren().add(badge);
        }
        if (cornerAction != null) {
            Button toggle = new Button("Alt");
            toggle.getStyleClass().add("document-media-thumbnail-corner-action");
            toggle.setFocusTraversable(false);
            toggle.setOnAction(event -> {
                cornerAction.run();
                event.consume();
            });
            Tooltip.install(toggle, new Tooltip(safe(cornerTooltip, "Alternar variante visual")));
            StackPane.setAlignment(toggle, Pos.TOP_RIGHT);
            box.getChildren().add(toggle);
        }
        if (emptyAction != null && emptyActionIcon != null) {
            Button action = ActionButtonFactory.iconOnly(
                    emptyActionIcon,
                    safe(emptyActionTooltip, "Completar variante"),
                    emptyAction,
                    "document-media-thumbnail-corner-action");
            StackPane.setAlignment(action, Pos.BOTTOM_RIGHT);
            box.getChildren().add(action);
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
