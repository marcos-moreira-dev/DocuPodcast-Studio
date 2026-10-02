package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfPassiveNavigationArchitectureTest {
    @Test
    void visiblePageListenerOnlyObservesViewerState() throws Exception {
        String view = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        assertTrue(view.contains("visiblePageNumberProperty().addListener"));
        assertTrue(view.contains("observePdfVisiblePage"));
        assertFalse(view.contains("prepareVisibleWindow"));
    }

    @Test
    void navigationAdapterCanOnlyReprioritizeExistingWork() throws Exception {
        String coordinator = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/document/"
                + "PdfVisibleTextPreparationCoordinator.java");
        String passive = method(coordinator, "void observeVisiblePage(",
                "void preparePageNow(");
        assertTrue(passive.contains("reprioritizePending"));
        assertTrue(passive.contains("NAVIGATION_IS_PASSIVE"));
        assertFalse(passive.contains("scheduler.submit"));
        assertFalse(passive.contains("prepareScope.execute"));
    }

    @Test
    void navigationAndRestoreNeverOwnRetryAuthority() {
        assertFalse(com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfPreparationOrigin.NAVIGATION.retryAuthority());
        assertFalse(com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfPreparationOrigin.PROJECT_RESTORE.retryAuthority());
        assertFalse(com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfPreparationOrigin.LISTEN_DOCUMENT.retryAuthority());
        assertTrue(com.marcosmoreiradev.docupodcaststudio.application.document
                .PdfPreparationOrigin.PROCESS_COMPLETE.retryAuthority());
    }

    @Test
    void listenDocumentStartsAtPageOneWhileSelectionUsesItsOwnCommand()
            throws Exception {
        String coordinator = source("src/main/java/com/marcosmoreiradev/"
                + "docupodcaststudio/presentation/shell/workflow/"
                + "PdfNarratablePreparationCoordinator.java");
        String listen = method(coordinator,
                "public void prepareFastListenThenRun(",
                "public void prepareAudioThenRun(");
        assertTrue(listen.contains("int anchor = 1;"));
        assertFalse(listen.contains("selectedOrVisiblePage()"));
        assertTrue(coordinator.contains("public void prepareThenRun("));
    }

    private static String method(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        return source.substring(from, to);
    }

    private static String source(String relative) throws Exception {
        return Files.readString(Path.of(relative), StandardCharsets.UTF_8);
    }
}
