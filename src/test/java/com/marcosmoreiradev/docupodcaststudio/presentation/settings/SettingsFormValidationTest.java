package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import javafx.application.Platform;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

final class SettingsFormValidationTest {
    @BeforeAll static void startFx() throws Exception {
        var latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
    @Test void invalidNumbersCannotBeSavedAndDirtyStateTracksEdits() throws Exception {
        fx(() -> {
            var form = new SettingsFormModel(OperationalSettings.defaults());
            form.markSaved();
            assertFalse(form.dirty());
            form.baseFontSize.setText("abc");
            assertTrue(form.dirty());
            assertThrows(IllegalArgumentException.class, form::toSettings);
            form.baseFontSize.setText("18");
            assertFalse(form.dirty());
            form.lineSpacing.setText("NaN");
            assertThrows(IllegalArgumentException.class, form::toSettings);
            form.lineSpacing.setText("1.35");
            form.initialReadySegments.setText("26");
            assertThrows(IllegalArgumentException.class, form::toSettings);
            form.initialReadySegments.setText("5");
            form.lookaheadSegments.setText("2");
            assertThrows(IllegalArgumentException.class, form::toSettings);
        });
    }
    @Test void discoveryDoesNotSilentlyReplaceUnavailablePreferences() throws Exception {
        fx(() -> {
            var form = new SettingsFormModel(OperationalSettings.defaults());
            form.computeSelectedDeviceId.setValue("cuda:9");
            form.videoEncoderPolicy.setValue("NVIDIA_NVENC");
            form.refreshComputeDeviceChoices(java.util.List.of());
            form.refreshVideoEncoderChoices(java.util.List.of());
            assertEquals("cuda:9", form.toSettings().compute().selectedDeviceId());
            assertEquals("NVIDIA_NVENC", form.videoEncoderPolicy.getValue());
            assertNotEquals("AUTO", form.computePolicy.getConverter().toString("AUTO"));
            assertFalse(form.videoEncoderPolicy.isEditable());
        });
    }
    private static void fx(Runnable action) throws Exception {
        var task = new FutureTask<Void>(() -> { action.run(); return null; });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }
}
