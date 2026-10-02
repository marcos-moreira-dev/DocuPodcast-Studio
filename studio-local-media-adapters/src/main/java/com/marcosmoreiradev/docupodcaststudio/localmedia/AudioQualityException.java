package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;

/** Only acoustic validation failures are eligible for the bounded quality retry. */
final class AudioQualityException extends IOException {
    AudioQualityException(String message) { super(message); }
}
