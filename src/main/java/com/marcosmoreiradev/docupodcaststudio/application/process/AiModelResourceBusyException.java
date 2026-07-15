package com.marcosmoreiradev.docupodcaststudio.application.process;

/** Controlled error raised when another local AI model job already owns the GPU/VRAM budget. */
public final class AiModelResourceBusyException extends IllegalStateException {
    public AiModelResourceBusyException(String message) {
        super(message);
    }
}
