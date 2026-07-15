package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemT129T130NativeInkSourceTest {
    @Test
    void jnaDependenciesAndModulesAreDeclared() throws Exception {
        String pom = read("pom.xml");
        String module = read("src/main/java/module-info.java");

        assertTrue(pom.contains("net.java.dev.jna"));
        assertTrue(pom.contains("<artifactId>jna</artifactId>"));
        assertTrue(pom.contains("<artifactId>jna-platform</artifactId>"));
        assertTrue(pom.contains("<version>5.19.1</version>"));
        assertTrue(module.contains("requires com.sun.jna;"));
        assertTrue(module.contains("requires com.sun.jna.platform;"));
    }

    @Test
    void windowsPointerProviderUsesRealPointerMessagesAndHistory() throws Exception {
        String provider = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/WindowsPointerInkInputProvider.java");

        assertTrue(provider.contains("WM_POINTERDOWN"));
        assertTrue(provider.contains("WM_POINTERUPDATE"));
        assertTrue(provider.contains("WM_POINTERUP"));
        assertTrue(provider.contains("WM_POINTERLEAVE"));
        assertTrue(provider.contains("GetPointerPenInfo"));
        assertTrue(provider.contains("GetPointerFramePenInfoHistory"));
        assertTrue(provider.contains("RegisterPointerInputTarget(hwnd, PT_PEN)"));
        assertTrue(provider.contains("UnregisterPointerInputTarget(hwnd, PT_PEN)"));
        assertTrue(provider.contains("InkInputCapabilities.windowsPointer()"));
        assertTrue(provider.contains("nativePacketsSeen"));
        assertTrue(provider.contains("onStrokeMoveBatch"));
        assertTrue(provider.contains("installNativeHookWhenReady()"));
        assertTrue(provider.contains("window.showingProperty().addListener(showingListener)"));
        assertTrue(provider.contains("Windows Pointer esperando que la ventana sea visible."));
        assertTrue(provider.contains("normalizePressure(rawPressure)"));
        assertTrue(provider.contains("pressure <= 4096.0"));
        assertTrue(provider.contains("pressure <= 8192.0"));
        assertTrue(provider.contains("65535.0"));
        assertTrue(provider.contains("Windows Pointer instalado, esperando paquetes reales"));
    }

    @Test
    void providerFactoryKeepsDefaultFallbacksAndProvidesStrictLectureStudioMode() throws Exception {
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/InkInputProviderFactory.java");
        String wintab = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/WintabInkInputProvider.java");
        String lectureStudio = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/LectureStudioStylusInputProvider.java");
        String mouseFirst = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/MouseFirstInkInputProvider.java");
        String windowsPointer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/input/WindowsPointerInkInputProvider.java");

        assertTrue(factory.contains("new MouseFirstInkInputProvider(createNativeProvider())"));
        assertTrue(factory.contains("createLectureStudioOnly()"));
        assertTrue(factory.contains("LectureStudioStylusInputProvider.createStrictOrUnavailable()"));
        assertTrue(factory.contains("WindowsPointerInkInputProvider.tryCreate(lectureStudio)"));
        assertTrue(factory.contains("WindowsPointerInkInputProvider.tryCreate(noFallback)"));
        assertTrue(factory.contains("LectureStudioStylusInputProvider.tryCreate(windowsPointer)"));
        assertTrue(windowsPointer.contains("tryCreate(InkInputProvider fallback)"));
        assertTrue(mouseFirst.contains("new JavaFxMouseInputProvider()"));
        assertTrue(mouseFirst.contains("mouseProvider.attach(target"));
        assertTrue(mouseFirst.contains("nativeProvider.attach(target"));
        assertTrue(wintab.contains("Wintab esta reservado como compatibilidad v2"));
        assertTrue(wintab.contains("InkInputCapabilities.javafxMouse"));
        assertTrue(lectureStudio.contains("this.fallback = fallback == null ? NoopInkInputProvider.INSTANCE : fallback"));
        assertTrue(lectureStudio.contains("if (!strict)"));
        assertTrue(lectureStudio.contains("InkInputCapabilities.lectureStudioWaiting"));
        assertTrue(lectureStudio.contains("InkInputCapabilities.lectureStudioUnavailable"));
        assertTrue(lectureStudio.contains("InkInputCapabilities.lectureStudioStylus()"));
        assertTrue(lectureStudio.contains("nativeEventsSeen"));
    }

    @Test
    void technicalProblemDialogDoesNotImportNativeLibrariesDirectly() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");

        assertFalse(dialog.contains("com.sun.jna"));
        assertTrue(dialog.contains("inputDiagnosticText()"));
        assertTrue(dialog.contains("capabilities.providerName()"));
        assertTrue(dialog.contains("refreshInputDiagnostic()"));
        assertTrue(dialog.contains("onStrokeMoveBatch"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
