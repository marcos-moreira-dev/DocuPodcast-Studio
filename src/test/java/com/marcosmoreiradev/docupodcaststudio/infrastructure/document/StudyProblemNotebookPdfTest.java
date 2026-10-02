package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemPdfPage;
import com.marcosmoreiradev.docupodcaststudio.ink.model.*;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class StudyProblemNotebookPdfTest {
    @TempDir Path root;
    @Test void rendersTwoSavedPagesThroughTheSharedCanvasRenderer() throws Exception {
        var ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException running) { ready.countDown(); }
        assertTrue(ready.await(10,TimeUnit.SECONDS));
        var pages = new ArrayList<StudyProblemPdfPage>();
        for(String color : List.of("#ff0000ff", "#0000ffff")) {
            var state = InkWorkspaceState.create(720,520,color,List.of(),List.of(),Map.of());
            pages.add(new StudyProblemPdfPage("Ejercicio",root.resolve("unused.png"),InkWorkspaceStateSerializer.toJson(state)));
        }
        Path output=root.resolve("notebook.pdf");
        new PdfBoxStudyProblemPdfExporter().export(pages,output);
        try(var pdf=Loader.loadPDF(output.toFile())) {
            assertEquals(2,pdf.getNumberOfPages());
            var renderer = new PDFRenderer(pdf);
            for(int i=0;i<2;i++) {
                var image=renderer.renderImage(i);
                assertEquals(i==0 ? 0xff0000 : 0x0000ff,
                        image.getRGB(image.getWidth()/2,image.getHeight()/2)&0xffffff);
            }
        }
    }
}
