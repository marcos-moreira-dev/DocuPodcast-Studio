package com.marcosmoreiradev.docupodcaststudio.ink;

import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputSample;

@FunctionalInterface
public interface InkCoordinateTransform {
    InkCoordinateTransform IDENTITY = sample -> sample;
    InkInputSample transform(InkInputSample sample);
}
