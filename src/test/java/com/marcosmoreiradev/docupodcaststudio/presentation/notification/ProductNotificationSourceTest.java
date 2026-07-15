package com.marcosmoreiradev.docupodcaststudio.presentation.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source guardrails for product notifications and technical detail dialogs. */
final class ProductNotificationSourceTest {
    @Test
    void notificationsAreSeparatedFromJavaFxAndPresenterShowsTechnicalDetail() throws Exception {
        String notification = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/notification/UserNotification.java"));
        String level = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/notification/UserNotificationLevel.java"));
        String presenter = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/notification/ExceptionAlertPresenter.java"));
        String styler = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/notification/DialogStyler.java"));
        String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));

        assertFalse(notification.contains("import javafx."), "UserNotification debe ser agnostico de JavaFX.");
        assertFalse(level.contains("import javafx."), "UserNotificationLevel debe ser agnostico de JavaFX.");
        assertTrue(notification.contains("technicalDetail"));
        assertTrue(notification.contains("failure(String headline, Throwable error)"));
        assertTrue(presenter.contains("TextArea details"));
        assertTrue(presenter.contains("setExpandableContent(details)"));
        assertTrue(styler.contains("product-dialog"));
        assertTrue(moduleInfo.contains("exports com.marcosmoreiradev.docupodcaststudio.presentation.notification"));
    }
}
