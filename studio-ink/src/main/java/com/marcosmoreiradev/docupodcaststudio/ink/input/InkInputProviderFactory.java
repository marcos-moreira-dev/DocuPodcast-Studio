package com.marcosmoreiradev.docupodcaststudio.ink.input;

import com.marcosmoreiradev.docupodcaststudio.ink.InkInputPolicy;

public final class InkInputProviderFactory {
    private InkInputProviderFactory() {
    }

    public static InkInputProvider createDefault() {
        return create(InkInputPolicy.MOUSE_AND_NATIVE);
    }

    public static InkInputProvider create(InkInputPolicy policy) {
        InkInputPolicy safe = policy == null ? InkInputPolicy.MOUSE_AND_NATIVE : policy;
        return switch (safe) {
            case NATIVE_REQUIRED -> createNativeOnly();
            case NATIVE_PREFERRED -> new MouseFirstInkInputProvider(createNativeOnly());
            case MOUSE_AND_NATIVE -> new MouseFirstInkInputProvider(createNativeProvider());
        };
    }

    public static InkInputProvider createNativeOnly() {
        InkInputProvider lectureStudio = LectureStudioStylusInputProvider.createStrictOrUnavailable();
        return WindowsPointerInkInputProvider.tryCreate(lectureStudio)
                .orElse(lectureStudio);
    }

    public static InkInputProvider createLectureStudioOnly() {
        return createNativeOnly();
    }

    private static InkInputProvider createNativeProvider() {
        InkInputProvider noFallback = NoopInkInputProvider.INSTANCE;
        InkInputProvider windowsPointer = WindowsPointerInkInputProvider.tryCreate(noFallback)
                .orElse(noFallback);
        return LectureStudioStylusInputProvider.tryCreate(windowsPointer)
                .orElse(windowsPointer);
    }
}
