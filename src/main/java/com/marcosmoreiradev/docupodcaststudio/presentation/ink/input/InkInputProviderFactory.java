package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

public final class InkInputProviderFactory {
    private InkInputProviderFactory() {
    }

    public static InkInputProvider createDefault() {
        return new MouseFirstInkInputProvider(createNativeProvider());
    }

    public static InkInputProvider createLectureStudioOnly() {
        InkInputProvider lectureStudio = LectureStudioStylusInputProvider.createStrictOrUnavailable();
        return WindowsPointerInkInputProvider.tryCreate(lectureStudio)
                .orElse(lectureStudio);
    }

    private static InkInputProvider createNativeProvider() {
        InkInputProvider noFallback = NoopInkInputProvider.INSTANCE;
        InkInputProvider windowsPointer = WindowsPointerInkInputProvider.tryCreate(noFallback)
                .orElse(noFallback);
        return LectureStudioStylusInputProvider.tryCreate(windowsPointer)
                .orElse(windowsPointer);
    }
}
