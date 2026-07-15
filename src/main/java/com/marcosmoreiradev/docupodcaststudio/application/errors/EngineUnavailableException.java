package com.marcosmoreiradev.docupodcaststudio.application.errors;

/** Selected engine cannot be used for the requested document operation. */
public final class EngineUnavailableException extends ApplicationPreconditionException {
    private final String engineName;

    public EngineUnavailableException(String engineName, String message, String technicalDetail) {
        super((engineName == null || engineName.isBlank() ? "Motor de voz" : engineName.strip()) + " no está disponible",
                message,
                technicalDetail);
        this.engineName = engineName == null ? "" : engineName.strip();
    }

    public String engineName() {
        return engineName;
    }
}
