package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.application.Platform;
import javafx.scene.control.Control;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudioControlContractTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void everyOfficialPrimitiveCarriesAnAuditableDescriptor() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                List<Control> controls = List.of(
                        ActionButtonFactory.primary("Aceptar"),
                        StudioFormControls.textField(), StudioFormControls.textArea(),
                        StudioFormControls.checkBox("Activo"), StudioFormControls.radioButton("Uno"),
                        StudioFormControls.comboBox(), StudioFormControls.spinner(),
                        StudioFormControls.slider(), StudioFormControls.colorPicker(),
                        StudioFormControls.toggleButton("Modo"), StudioCollectionControls.listView(),
                        StudioCollectionControls.treeView(), StudioCollectionControls.tableView(),
                        StudioFeedbackControls.progressBar(), StudioFeedbackControls.progressIndicator(),
                        StudioViewportControls.scrollPane(), StudioViewportControls.splitPane(),
                        StudioNavigationControls.tabPane(), StudioNavigationControls.menuBar(),
                        StudioAccordion.accordion());
                controls.forEach(control -> assertTrue(StudioControlContract.descriptor(control).isPresent(),
                        control.getClass().getName()));
            } catch (Throwable throwable) { failure.set(throwable); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
}
