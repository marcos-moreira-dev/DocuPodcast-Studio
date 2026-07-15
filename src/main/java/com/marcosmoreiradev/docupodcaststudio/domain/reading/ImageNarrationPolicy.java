package com.marcosmoreiradev.docupodcaststudio.domain.reading;

/** How image blocks should be converted into future narration. */
public enum ImageNarrationPolicy {
    READ_DESCRIPTION_OR_OMIT("Leer descripción si existe; si no, omitir"),
    ANNOUNCE_IMAGE_WITHOUT_DESCRIPTION("Anunciar también imágenes sin descripción"),
    IGNORE_IMAGES("Ignorar imágenes");

    private final String displayName;

    ImageNarrationPolicy(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
