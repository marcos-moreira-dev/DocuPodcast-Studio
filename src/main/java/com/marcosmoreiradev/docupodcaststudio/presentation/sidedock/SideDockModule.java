package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import javafx.scene.Parent;

/** A single panel available in the vertical workspace side-dock. */
public interface SideDockModule {
    SideDockModuleId id();
    String title();
    String tooltip();
    default AppIcon icon() { return AppIcon.DEFAULT; }

    /** Compatibility only; rails render the explicit {@link #icon()} value. */
    @Deprecated(forRemoval = false)
    default String iconText() { return icon().accessibleText(); }
    boolean supports(SideDockContext context);
    Parent createView(SideDockContext context);
}
