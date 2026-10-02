package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentStructurePanelIconTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); }
        catch (IllegalStateException alreadyStarted) { latch.countDown(); }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void documentWordsDoNotAddActionIconsToBlockMarkers() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            var document = new ReadableDocument("Auditoria", SourceDocumentFormat.UNKNOWN,
                    Path.of("audit.txt"), List.of(
                    DocumentBlock.of("B1", DocumentBlockType.IGNORED, "Cerrar el circuito", ""),
                    DocumentBlock.of("B2", DocumentBlockType.PARAGRAPH, "Editar una imagen", ""),
                    DocumentBlock.of("B3", DocumentBlockType.PARAGRAPH, "Una frase sin acciones", "")));
            AtomicReference<String> selected = new AtomicReference<>();
            var panel = new DocumentStructurePanel(new SimpleObjectProperty<>(document), selected::set);
            var rows = panel.getContent().lookupAll(".document-structure-item");
            assertEquals(3, rows.size());
            for (var node : rows) {
                Button button = (Button) node;
                assertNull(button.getGraphic(), "Content words must not create an extra semantic icon");
                button.fire();
                assertTrue(button.getText().contains(selected.get()));
                if (selected.get().equals("B1")) assertTrue(button.getText().startsWith("× "));
            }
            return null;
        });
        Platform.runLater(task);
        task.get(15, TimeUnit.SECONDS);
    }
}
