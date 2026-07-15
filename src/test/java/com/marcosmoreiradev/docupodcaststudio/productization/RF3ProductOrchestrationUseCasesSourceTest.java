package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RF3ProductOrchestrationUseCasesSourceTest {
    @Test
    void listeningSessionDecisionLivesInApplication() throws Exception {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PrepareListeningSessionUseCase.java");
        String request = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PrepareListeningSessionRequest.java");
        String state = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/ListeningSessionState.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentNarrationCoordinator.java");
        String vm = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(useCase.contains("PrepareDocumentListeningUseCase"));
        assertTrue(useCase.contains("ListeningSessionReadiness"));
        assertTrue(request.contains("ReadableDocument document"));
        assertTrue(state.contains("from(DocumentListenPlan plan)"));
        assertTrue(services.contains("PrepareListeningSessionUseCase prepareListeningSession"));
        assertTrue(coordinator.contains("prepareListeningSession()"));
        assertTrue(vm.contains("documentNarration.listeningSession"));
        assertFalse(vm.contains("DocumentListenFlowState.of("));
    }

    @Test
    void rf3IsDocumentedAsNoUxChangeRefactor() throws Exception {
        String doc = read("docs/productizacion/RF3_USE_CASES_ORQUESTACION_PRODUCTO.md");
        String readme = read("README.md");
        String handoff = read("AI_HANDOFF.md");
        String validation = read("VALIDATION.md");

        assertTrue(doc.contains("PrepareListeningSessionUseCase"));
        assertTrue(doc.contains("sin cambio visual"));
        assertTrue(readme.contains("RF3"));
        assertTrue(handoff.contains("PrepareListeningSessionUseCase"));
        assertTrue(validation.contains("RF3"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
