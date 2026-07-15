package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemT124InkArchitectureSourceTest {
    @Test
    void technicalProblemDialogConsumesTransversalInkInfrastructure() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String engine = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java");

        assertTrue(dialog.contains("com.marcosmoreiradev.docupodcaststudio.presentation.ink.input.InkInputProvider"));
        assertTrue(dialog.contains("InkInputProviderFactory.createLectureStudioOnly()"));
        assertTrue(dialog.contains("InkRealtimeStrokeEngine"));
        assertTrue(dialog.contains("InkCanvasViewportCoordinateMapper.mapInside"));
        assertTrue(dialog.contains("inputDiagnosticText()"));
        assertTrue(dialog.contains("capabilities.providerName()"));
        assertTrue(dialog.contains("Entrada: \" + provider"));
        assertTrue(dialog.contains("currentInputRawPressure"));
        assertTrue(dialog.contains("currentInputCursor"));
        assertTrue(dialog.contains("currentInputSource + \" MOUSE - sin presion variable\""));
        assertTrue(dialog.contains("currentInputSource.startsWith(\"LectureStudio\")"));
        assertTrue(dialog.contains("currentInputSource + \" \" + cursor"));
        assertTrue(dialog.contains("refreshInputDiagnostic()"));
        assertTrue(dialog.contains("onStrokeMoveBatch"));
        assertTrue(dialog.contains("inkEngine.begin"));
        assertTrue(dialog.contains("inkEngine.move"));
        assertTrue(dialog.contains("inkEngine.end"));
        assertTrue(dialog.contains("inkEngine.flushAll"));
        assertTrue(dialog.contains("InkWorkspaceStateSerializer.toJson"));
        assertTrue(engine.contains("AnimationTimer"));
        assertTrue(engine.contains("FRAME_BUDGET_NANOS"));
        assertTrue(engine.contains("Deque<RawPoint>"));
        assertTrue(engine.contains("previewQuadratic"));
        assertTrue(engine.contains("CommittedStroke"));
        assertFalse(dialog.contains("InkStrokePipeline.resampleLine"));
        assertFalse(dialog.contains("import com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input.InkInputProvider;"));
    }

    @Test
    void stylusProviderIsTransversalStrictAndIsolatedBehindFactory() throws Exception {
        String stylus = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/LectureStudioStylusInputProvider.java");
        String windowsPointer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/WindowsPointerInkInputProvider.java");
        String wintab = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/WintabInkInputProvider.java");
        String capabilities = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/InkInputCapabilities.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/InkInputProviderFactory.java");
        String pom = read("pom.xml");
        String module = read("src/main/java/module-info.java");

        assertTrue(windowsPointer.contains("InkInputCapabilities.windowsPointer()"));
        assertTrue(windowsPointer.contains("nativePacketsSeen"));
        assertTrue(windowsPointer.contains("GetPointerFramePenInfoHistory"));
        assertTrue(windowsPointer.contains("installNativeHookWhenReady()"));
        assertTrue(windowsPointer.contains("observeScene()"));
        assertTrue(windowsPointer.contains("observeWindow(scene)"));
        assertTrue(windowsPointer.contains("observeShowing(window)"));
        assertTrue(wintab.contains("docupodcast.ink.enableExperimentalWintab"));
        assertTrue(wintab.contains("JavaFxMouseInputProvider"));
        assertFalse(stylus.contains("org.lecturestudio.stylus.javafx.JavaFxStylusManager"));
        assertTrue(stylus.contains("WindowsNativeWindowHandleResolver.resolveHandle(target)"));
        assertTrue(stylus.contains("stylusManager.attachStylusListener(stylusListener, Long.valueOf(attachedWindowHandle))"));
        assertTrue(stylus.contains("stylusManager.enableStylusListener(stylusListener, true)"));
        assertTrue(stylus.contains("stylusManager.detachStylusListener(stylusListener, Long.valueOf(attachedWindowHandle))"));
        assertTrue(stylus.contains("NativeInkCoordinateSpaceResolver"));
        assertTrue(stylus.contains("coordinateResolver.translate(target, rawX, rawY, strokeActive, cursor)"));
        assertTrue(stylus.contains("lastAcceptedTargetLocalPoint"));
        assertTrue(stylus.contains("inputSourceForCurrentPacket()"));
        assertFalse(stylus.contains("bestTargetLocalPoint"));
        assertFalse(stylus.contains("lastTranslatedLocalPoint"));
        assertTrue(stylus.contains("pendingStylusEvents"));
        assertTrue(stylus.contains("onStrokeMoveBatch"));
        assertTrue(stylus.contains("window.showingProperty().addListener(showingListener)"));
        assertTrue(stylus.contains("sampleFromStylusEvent"));
        assertTrue(stylus.contains("nativeEventsSeen"));
        assertTrue(stylus.contains("private long nativeHandle"));
        assertTrue(stylus.contains("InkInputCapabilities.lectureStudioStylus()"));
        assertTrue(stylus.contains("createStrictOrUnavailable()"));
        assertTrue(stylus.contains("StylusManager.class.getResourceAsStream(\"/\" + NATIVE_LIBRARY_RESOURCE)"));
        assertTrue(capabilities.contains("lectureStudioStylus()"));
        assertTrue(capabilities.contains("lectureStudioWaiting("));
        assertTrue(capabilities.contains("lectureStudioUnavailable("));
        assertTrue(capabilities.contains("windowsPointer()"));
        assertTrue(factory.contains("createLectureStudioOnly()"));
        assertTrue(factory.contains("LectureStudioStylusInputProvider.createStrictOrUnavailable()"));
        assertTrue(factory.contains("WindowsPointerInkInputProvider.tryCreate(lectureStudio)"));
        assertTrue(factory.contains("new MouseFirstInkInputProvider(createNativeProvider())"));
        assertTrue(factory.contains("NoopInkInputProvider.INSTANCE"));
        assertTrue(factory.indexOf("WindowsPointerInkInputProvider.tryCreate(noFallback)")
                < factory.indexOf("LectureStudioStylusInputProvider.tryCreate(windowsPointer)"));
        assertFalse(factory.contains("WintabInkInputProvider.tryCreate()"));
        assertFalse(factory.contains("JavaFxMouseInputProvider::new"));
        assertTrue(pom.contains("org.lecturestudio.stylus"));
        assertTrue(pom.contains("--patch-module=stylus=${lecturestudio.stylus.native.jar}"));
        assertTrue(module.contains("requires stylus;"));
        assertTrue(module.contains("requires stylus.javafx;"));
        assertFalse(module.contains("requires stylus.windows.x86_64;"));
    }

    @Test
    void inkApplicationModelIsNotCoupledToTechnicalProblemOrTheatreUi() throws Exception {
        String state = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ink/InkWorkspaceState.java");
        String hook = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ink/TheatreFrameSketchInkHook.java");

        assertFalse(state.contains("TechnicalProblem"));
        assertFalse(state.contains("documentstudy"));
        assertTrue(hook.contains("InkWorkspaceState"));
        assertFalse(hook.contains("presentation"));
    }

    @Test
    void theatreDoesNotDependOnDocumentStudyForFutureSketches() throws Exception {
        String module = read("src/main/java/module-info.java");
        String theatreHook = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ink/TheatreFrameSketchInkHook.java");

        assertTrue(module.contains("exports com.marcosmoreiradev.docupodcaststudio.application.ink;"));
        assertTrue(theatreHook.contains("record TheatreFrameSketchInkHook"));
        assertFalse(theatreHook.contains("TechnicalProblem"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
