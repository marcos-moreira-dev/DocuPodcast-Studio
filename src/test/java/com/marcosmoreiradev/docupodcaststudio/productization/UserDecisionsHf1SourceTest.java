package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** USER-DECISIONS-HF1: defensive decisions must be visible and typed failures must be product-facing. */
final class UserDecisionsHf1SourceTest {
    @Test
    void applicationDecisionContractIsPresentationAgnostic() throws Exception {
        String decision = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/decisions/UserVisibleDecision.java");
        String result = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/decisions/OperationResult.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/decisions/DefensiveDecisionPolicy.java");

        assertTrue(decision.contains("requiresDialog"));
        assertTrue(decision.contains("defensiveFallback"));
        assertTrue(result.contains("dialogDecisions"));
        assertTrue(policy.contains("fallbackChangedUserIntent"));
        assertTrue(policy.contains("GPU to CPU"));
        assertFalse(decision.contains("import javafx."));
        assertFalse(decision.contains("presentation."));
        assertFalse(result.contains("import javafx."));
    }

    @Test
    void typedExceptionsCarryUserFacingDetails() throws Exception {
        String precondition = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/ApplicationPreconditionException.java");
        String infrastructure = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/InfrastructureOperationException.java");
        String process = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/ExternalProcessFailedException.java");
        String engine = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/EngineUnavailableException.java");

        assertTrue(precondition.contains("implements UserFacingApplicationException"));
        assertTrue(infrastructure.contains("implements UserFacingApplicationException"));
        assertTrue(process.contains("exitCode"));
        assertTrue(process.contains("commandAuditId"));
        assertTrue(process.contains("lastOutput"));
        assertTrue(process.contains("logPath"));
        assertTrue(engine.contains("extends ApplicationPreconditionException"));
    }

    @Test
    void presentationCanShowDecisionsAndUserFacingExceptionsAsDialogs() throws Exception {
        String notification = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/notification/UserNotification.java");
        String presenter = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/notification/ExceptionAlertPresenter.java");

        assertTrue(notification.contains("fromDecision(UserVisibleDecision decision)"));
        assertTrue(notification.contains("UserFacingApplicationException userFacing"));
        assertTrue(presenter.contains("showDecision(UserVisibleDecision decision"));
        assertTrue(presenter.contains("showDialogDecisions(List<UserVisibleDecision> decisions"));
        assertTrue(presenter.contains("filter(UserVisibleDecision::requiresDialog)"));
    }

    @Test
    void unavailableAdvancedVoiceThrowsTypedEngineExceptionInsteadOfGenericIllegalState() throws Exception {
        String gateway = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/SettingsAwareAudioGenerationGateway.java");

        assertTrue(gateway.contains("EngineUnavailableException"));
        assertTrue(gateway.contains("El gateway defensivo bloqueó la generación"));
        assertFalse(gateway.contains("throw new IllegalStateException(message)"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
