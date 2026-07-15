package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsValidationReport;

import java.io.IOException;
import java.util.Objects;

/**
 * Coordinates reading comfort preferences outside the shell view-model.
 *
 * <p>The coordinator owns clamping, zoom calculation and persistence of the
 * document reader font size. The shell view-model keeps only JavaFX state and
 * user-facing status messages.</p>
 */
public final class ReadingComfortCoordinator {
    public static final int MIN_FONT_SIZE = 14;
    public static final int DEFAULT_FONT_SIZE = 18;
    public static final int MAX_FONT_SIZE = 28;

    private final ApplicationServices applicationServices;

    public ReadingComfortCoordinator(ApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
    }

    public int loadInitialFontSize() {
        try {
            OperationalSettings settings = applicationServices.settings().loadOperationalSettings().load();
            return clampFontSize(settings.readingDocument().baseFontSize());
        } catch (IOException ex) {
            return DEFAULT_FONT_SIZE;
        }
    }

    public int decrease(int currentSize) {
        return clampFontSize(currentSize - 1);
    }

    public int increase(int currentSize) {
        return clampFontSize(currentSize + 1);
    }

    public int clampFontSize(int value) {
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, value));
    }

    public int zoomPercent(int fontSize) {
        return Math.round((clampFontSize(fontSize) * 100.0f) / DEFAULT_FONT_SIZE);
    }

    public PersistedReadingFontSize persistFontSize(int nextSize) {
        try {
            OperationalSettings current = applicationServices.settings().loadOperationalSettings().load();
            OperationalSettings.ReadingDocumentSettings reading = current.readingDocument();
            OperationalSettings updated = new OperationalSettings(
                    new OperationalSettings.ReadingDocumentSettings(
                            clampFontSize(nextSize),
                            reading.lineSpacing(),
                            reading.keepActiveSentenceNearCenter()),
                    current.playbackBuffer(),
                    current.tts(),
                    current.video(),
                    current.imageGeneration(),
                    current.frameGeneration(),
                    current.compute(),
                    current.ocr(),
                    current.storage(),
                    current.diagnostics());
            OperationalSettingsValidationReport report = applicationServices.settings().saveOperationalSettings().save(updated);
            if (!report.errors().isEmpty()) {
                return PersistedReadingFontSize.failed("No se pudo guardar el tamaño de lectura: "
                        + String.join("; ", report.errors()) + ".");
            }
            return PersistedReadingFontSize.success();
        } catch (IOException ex) {
            return PersistedReadingFontSize.failed(
                    "Tamaño de lectura aplicado, pero no se pudo guardar la preferencia: " + ex.getMessage() + ".");
        }
    }

    public record PersistedReadingFontSize(boolean saved, String message) {
        public static PersistedReadingFontSize success() {
            return new PersistedReadingFontSize(true, "");
        }

        public static PersistedReadingFontSize failed(String message) {
            return new PersistedReadingFontSize(false, message == null ? "" : message.strip());
        }
    }
}
