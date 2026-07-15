package com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input;

public interface InkInputListener {
    void onHover(InkInputSample sample);

    boolean onStrokeStart(InkInputSample sample);

    boolean onStrokeMove(InkInputSample sample);

    boolean onStrokeEnd(InkInputSample sample);
}
