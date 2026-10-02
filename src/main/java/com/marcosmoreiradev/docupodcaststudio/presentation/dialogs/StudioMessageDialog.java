package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioViewportControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.DialogStyler;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Window;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Transversal presentation policy for native JavaFX message and decision dialogs.
 *
 * <p>The human-readable message is never truncated or rewritten. Technical
 * diagnostics can be supplied separately as selectable, expandable content.</p>
 */
public final class StudioMessageDialog {
    private static final double MIN_CONTENT_WIDTH = 420.0;
    private static final double PREFERRED_CONTENT_WIDTH = 620.0;
    private static final double SCREEN_WIDTH_FACTOR = 0.72;
    private static final double SCREEN_HEIGHT_FACTOR = 0.82;
    private static final double MESSAGE_VIEWPORT_HEIGHT_FACTOR = 0.42;
    private static final int LONG_MESSAGE_THRESHOLD = 900;

    private StudioMessageDialog() {
    }

    public static Alert create(
            Window owner,
            Alert.AlertType type,
            String title,
            String header,
            String message,
            String technicalDetail,
            ButtonType... buttons) {
        Alert alert = buttons == null || buttons.length == 0
                ? NativeDialogResponse.alert(type)
                : NativeDialogResponse.alert(type, "", buttons);
        configure(alert, owner, title, header, message, technicalDetail);
        return alert;
    }

    public static void configure(
            Alert alert,
            Window owner,
            String title,
            String header,
            String message,
            String technicalDetail) {
        Objects.requireNonNull(alert, "alert");
        alert.setTitle(safe(title));
        alert.setHeaderText(blankToNull(header));

        Label content = new Label(safe(message));
        content.getStyleClass().add("studio-message-content");
        content.setWrapText(true);
        content.setMinWidth(0);
        content.setPrefWidth(preferredContentWidth(owner));
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMinHeight(Region.USE_PREF_SIZE);
        content.setAccessibleText(safe(message));
        String safeMessage = safe(message);
        if (safeMessage.length() > LONG_MESSAGE_THRESHOLD
                || safeMessage.lines().count() > 14) {
            ScrollPane messageViewport = StudioViewportControls.scrollPane(content);
            messageViewport.getStyleClass().add("studio-message-scroll");
            messageViewport.setFitToWidth(true);
            messageViewport.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            messageViewport.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            double viewportHeight = Math.max(180.0,
                    visualBounds(owner).getHeight() * MESSAGE_VIEWPORT_HEIGHT_FACTOR);
            messageViewport.setPrefViewportHeight(viewportHeight);
            messageViewport.setMaxHeight(viewportHeight);
            messageViewport.setAccessibleText("Mensaje desplazable");
            alert.getDialogPane().setContent(messageViewport);
        } else {
            alert.getDialogPane().setContent(content);
        }

        if (technicalDetail != null && !technicalDetail.isBlank()) {
            TextArea details = StudioFormControls.textArea(technicalDetail.strip());
            details.getStyleClass().add("studio-message-details");
            details.setEditable(false);
            details.setWrapText(true);
            details.setPrefColumnCount(80);
            details.setPrefRowCount(9);
            details.setAccessibleText("Detalles técnicos del mensaje");
            alert.getDialogPane().setExpandableContent(details);
        } else {
            alert.getDialogPane().setExpandableContent(null);
        }

        alert.getDialogPane().setMinWidth(MIN_CONTENT_WIDTH);
        alert.getDialogPane().setPrefWidth(preferredContentWidth(owner));
        alert.getDialogPane().setMaxHeight(
                visualBounds(owner).getHeight() * SCREEN_HEIGHT_FACTOR);
        alert.setResizable(true);
        DialogStyler.apply(alert, owner);
    }

    public static String technicalDetail(Throwable error) {
        if (error == null) {
            return "";
        }
        StringWriter output = new StringWriter();
        error.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    private static double preferredContentWidth(Window owner) {
        Rectangle2D bounds = visualBounds(owner);
        return Math.max(MIN_CONTENT_WIDTH,
                Math.min(PREFERRED_CONTENT_WIDTH, bounds.getWidth() * SCREEN_WIDTH_FACTOR));
    }

    private static Rectangle2D visualBounds(Window owner) {
        if (owner != null) {
            return Screen.getScreensForRectangle(
                            owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())
                    .stream()
                    .findFirst()
                    .orElse(Screen.getPrimary())
                    .getVisualBounds();
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
