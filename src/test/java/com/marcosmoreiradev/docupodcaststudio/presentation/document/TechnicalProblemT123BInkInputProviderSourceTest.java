package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemT123BInkInputProviderSourceTest {
    @Test
    void inputProviderContractExistsForFutureNativeStylusBackends() throws Exception {
        String provider = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputProvider.java");
        String sample = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputSample.java");
        String capabilities = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/InkInputCapabilities.java");

        assertTrue(provider.contains("interface InkInputProvider"));
        assertTrue(provider.contains("attach(Node target, InkInputListener listener)"));
        assertTrue(provider.contains("detach()"));
        assertTrue(sample.contains("record InkInputSample"));
        assertTrue(sample.contains("double pressure"));
        assertTrue(sample.contains("InkInputCursor cursor"));
        assertTrue(sample.contains("requestsEraser()"));
        assertTrue(capabilities.contains("boolean pressure"));
        assertTrue(capabilities.contains("javafxMouse()"));
    }

    @Test
    void javafxFallbackConsumesOnlyInkEventsHandledByTheDialog() throws Exception {
        String fallback = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/ink/input/JavaFxMouseInputProvider.java");

        assertTrue(fallback.contains("class JavaFxMouseInputProvider implements InkInputProvider"));
        assertTrue(fallback.contains("target.sceneToLocal(event.getSceneX(), event.getSceneY())"));
        assertTrue(fallback.contains("new InkInputSample("));
        assertTrue(fallback.contains("InkInputCursor.MOUSE"));
        assertTrue(fallback.contains("event.consume();"));
        assertTrue(fallback.contains("target.addEventFilter(MouseEvent.MOUSE_PRESSED"));
        assertTrue(fallback.contains("target.removeEventFilter(MouseEvent.MOUSE_DRAGGED"));
        assertFalse(fallback.contains("target.addEventHandler(MouseEvent.MOUSE_PRESSED"));
    }

    @Test
    void technicalProblemDialogUsesProviderInsteadOfDirectMouseInkHandlers() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");

        assertTrue(dialog.contains("InkInputProviderFactory.createLectureStudioOnly()"));
        assertTrue(dialog.contains("inkInputProvider.attach(drawingSurface.inkInputTarget(),"));
        assertTrue(dialog.contains("disposeCanvasInput()"));
        assertTrue(dialog.contains("currentInputPressure"));
        assertTrue(dialog.contains("currentInputEraser"));
        assertTrue(dialog.contains("double pressure"));
        assertFalse(dialog.contains("drawingSurface.addEventHandler(MouseEvent.MOUSE_PRESSED"));
        assertFalse(dialog.contains("drawingSurface.addEventHandler(MouseEvent.MOUSE_DRAGGED"));
        assertFalse(dialog.contains("drawingSurface.addEventHandler(MouseEvent.MOUSE_RELEASED"));
    }

    @Test
    void nativeRuntimeDependencyIsDeclaredOnlyOnTheTransversalInkBoundary() throws Exception {
        String pom = read("pom.xml");
        String module = read("src/main/java/module-info.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");

        assertTrue(pom.contains("org.lecturestudio.stylus"));
        assertTrue(pom.contains("<artifactId>stylus-javafx</artifactId>"));
        assertTrue(pom.contains("--patch-module=stylus=${lecturestudio.stylus.native.jar}"));
        assertTrue(module.contains("requires stylus;"));
        assertTrue(module.contains("requires stylus.javafx;"));
        assertFalse(module.contains("requires stylus.windows.x86_64;"));
        assertFalse(dialog.contains("org.lecturestudio.stylus"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
