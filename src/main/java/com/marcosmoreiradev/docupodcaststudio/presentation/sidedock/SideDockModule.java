package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import javafx.scene.Parent;

/** A single panel available in the vertical workspace side-dock. */
public interface SideDockModule {
    SideDockModuleId id();
    String title();
    String tooltip();
    String iconText();
    boolean supports(SideDockContext context);
    Parent createView(SideDockContext context);
}
