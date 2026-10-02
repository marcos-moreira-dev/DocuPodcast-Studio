package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

final class DocumentIndexReadingPreferencesTest {
    @TempDir Path temp;
    @BeforeAll static void startFx() throws Exception {
        var started = new CountDownLatch(1);
        try { Platform.startup(started::countDown); }
        catch (IllegalStateException alreadyStarted) { started.countDown(); }
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    @Test void showsPdfPreferencePersistsChoiceAndKeepsSearchLegibleWithoutHover() throws Exception {
        var repository = new JsonPreparedPdfDocumentRepository();
        String hash = "a".repeat(64);
        var manifest = new PdfDocumentManifest(3, "Prueba", "sample.pdf", hash, 1,
                "pdf-v2", Instant.EPOCH, Instant.EPOCH)
                .withReadingPreferences(PdfReadingStrategy.NATIVE_TEXT, PdfNativeTextProvider.PDFBOX);
        repository.initialize(temp, manifest);
        var source = new PreparedPdfSource(new PreparedPdfWorkspaceRef(temp, temp.resolve("sample.pdf"), hash), "Prueba");
        FutureTask<Void> task = new FutureTask<>(() -> {
            var pdf = new SimpleObjectProperty<>(source);
            var panel = new DocumentIndexPanel(new SimpleObjectProperty<ReadableDocument>(), pdf,
                    new SimpleStringProperty(""), ignored -> {}, new BuildDocumentOutlineUseCase(),
                    null, null, ignored -> {}, ignored -> {}, null)
                    .withReadingPreferences(new PdfReadingPreferencesUseCase(repository));
            var scene = new Scene(panel, 600, 750);
            for (String css : new String[]{"/css/tokens.css", "/css/components/actions.css", "/css/document/pdf-index-search.css"}) {
                scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
            }
            panel.applyCss();
            panel.layout();
            @SuppressWarnings("unchecked")
            var combo = (ComboBox<PdfReadingStrategy>) panel.lookup("#pdf-reading-strategy");
            assertNotNull(combo);
            assertEquals(PdfReadingStrategy.NATIVE_TEXT, combo.getValue());
            combo.setValue(PdfReadingStrategy.NATIVE_WITH_OCR);
            combo.fireEvent(new javafx.event.ActionEvent());
            assertEquals(PdfReadingStrategy.NATIVE_WITH_OCR, repository.loadManifest(temp).orElseThrow().readingStrategy());
            assertTrue(repository.loadPages(temp).isEmpty(), "Changing a preference must not prepare pages");
            Button search = panel.lookupAll(".button").stream().filter(node -> node instanceof Button b && b.getText().equals("Buscar"))
                    .map(Button.class::cast).findFirst().orElseThrow();
            assertFalse(search.isHover());
            assertEquals(Color.WHITE, search.getTextFill());
            assertNotEquals(Color.WHITE, search.getBackground().getFills().getFirst().getFill());
            pdf.set(null);
            assertNull(panel.lookup("#pdf-reading-strategy"), "Non-PDF sources must not display this preference");
            assertNotNull(panel.getCenter());
            return null;
        });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }
}
