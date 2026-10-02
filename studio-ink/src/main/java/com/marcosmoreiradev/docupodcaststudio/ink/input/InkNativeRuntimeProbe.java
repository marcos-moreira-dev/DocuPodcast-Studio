package com.marcosmoreiradev.docupodcaststudio.ink.input;

import java.util.Locale;
import java.util.Optional;

/** Read-only readiness probe for the native ink runtimes assembled by the launcher. */
public final class InkNativeRuntimeProbe {
    private InkNativeRuntimeProbe() { }

    public static Readiness inspect() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Optional<String> lectureStudioFailure = LectureStudioStylusInputProvider.probeNativeClasses();
        InkInputProvider windowsPointer = WindowsPointerInkInputProvider
                .tryCreate(NoopInkInputProvider.INSTANCE)
                .orElse(null);
        boolean windowsPointerAvailable = windowsPointer != null;
        if (windowsPointer != null) windowsPointer.close();
        String diagnostic = lectureStudioFailure.orElseGet(() -> windows
                ? "LectureStudio stylus nativo listo."
                : "La tinta nativa solo se habilita en Windows.");
        return new Readiness(windows, lectureStudioFailure.isEmpty(), windowsPointerAvailable, diagnostic);
    }

    public record Readiness(boolean windows, boolean lectureStudioLoaded,
                            boolean windowsPointerAvailable, String diagnostic) {
        public Readiness {
            diagnostic = diagnostic == null ? "" : diagnostic;
        }
    }
}
