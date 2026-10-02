package com.marcosmoreiradev.docupodcaststudio.domain.batch;

/** Named logo/mascot sizes used by Video Express. */
public enum BrandingSize {
    SMALL("Pequeño", 10),
    MEDIUM("Mediano", 20),
    LARGE("Grande", 30);

    private final String displayName;
    private final int percent;

    BrandingSize(String displayName, int percent) {
        this.displayName = displayName;
        this.percent = percent;
    }

    public String displayName() {
        return displayName;
    }

    public int percent() {
        return percent;
    }

    public static BrandingSize fromPercent(int percent) {
        if (percent <= 12) return SMALL;
        if (percent >= 26) return LARGE;
        return MEDIUM;
    }
}
