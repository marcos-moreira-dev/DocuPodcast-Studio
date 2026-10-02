package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ProjectSourceCopyNoticeDialogArchitectureTest {
    @Test
    void sourceCopyNoticeIsDeferredAndNeverBlocksInsideAnimationPulse()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/"
                        + "presentation/dialogs/ProjectSourceCopyNoticeDialog.java"),
                StandardCharsets.UTF_8);
        assertAll(
                () -> assertFalse(source.contains("showAndWait")),
                () -> assertTrue(source.contains("Platform.runLater")),
                () -> assertTrue(source.contains("setOnHidden")),
                () -> assertTrue(source.contains("alert.show()")));
    }
}
