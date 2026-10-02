package com.marcosmoreiradev.docupodcaststudio.domain.audio;

/**
 * User-facing amount of audio to prepare from the current document selection.
 *
 * <p>The fragment limit is exact. The page window is only the amount of PDF
 * source that should be prepared before rebuilding the reading projection.</p>
 */
public enum DocumentAudioPreparationExtent {
    FEW("Pocos fragmentos", "Hasta 12 fragmentos de voz", 12, 1),
    SHORT_READING("Lectura breve", "Hasta 30 fragmentos de voz", 30, 3),
    MEDIUM_READING("Lectura intermedia", "Hasta 75 fragmentos de voz", 75, 8),
    LARGE_READING("Lectura amplia", "Hasta 150 fragmentos de voz", 150, 20),
    ALL_FROM_SELECTION("Todo el documento de golpe",
            "Todo lo narrable desde el fragmento seleccionado", 0, 0),
    SINGLE_FRAGMENT("Solo este fragmento",
            "Un único fragmento de voz", 1, 1);

    private final String displayName;
    private final String detail;
    private final int maximumVoiceFragments;
    private final int preparationPageCount;

    DocumentAudioPreparationExtent(
            String displayName,
            String detail,
            int maximumVoiceFragments,
            int preparationPageCount) {
        this.displayName = displayName;
        this.detail = detail;
        this.maximumVoiceFragments = maximumVoiceFragments;
        this.preparationPageCount = preparationPageCount;
    }

    public String displayName() {
        return displayName;
    }

    public String detail() {
        return detail;
    }

    public int maximumVoiceFragments() {
        return maximumVoiceFragments;
    }

    public int preparationPageCount() {
        return preparationPageCount;
    }

    public boolean allFromSelection() {
        return maximumVoiceFragments == 0;
    }

    public static DocumentAudioPreparationExtent fromSliderValue(double value) {
        DocumentAudioPreparationExtent[] legacy = {
                FEW, SHORT_READING, MEDIUM_READING, LARGE_READING,
                ALL_FROM_SELECTION};
        int index = Math.max(0, Math.min(legacy.length - 1,
                (int) Math.round(value)));
        return legacy[index];
    }

    public double sliderValue() {
        return switch (this) {
            case FEW -> 0;
            case SHORT_READING -> 1;
            case MEDIUM_READING -> 2;
            case LARGE_READING -> 3;
            case ALL_FROM_SELECTION -> 4;
            case SINGLE_FRAGMENT -> 0;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
