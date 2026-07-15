package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocRefreshDecisionsHf1SourceTest {
    @Test
    void refreshSourceShowsDialogWhenDerivedArtifactsNeedReview() throws Exception {
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/SourceDocumentRefreshDecisionFactory.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(factory.contains("UserVisibleDecision.defensiveFallback"));
        assertTrue(factory.contains("audio queda obsoleto"));
        assertTrue(factory.contains("capas visuales/de voz requieren revisión"));
        assertTrue(vm.contains("refreshSourceDocumentDecision"));
        assertTrue(shell.contains("handleRefreshSourceDocument"));
        assertTrue(shell.contains("alertPresenter.showDecision(decision, owner())"));
    }
}
