package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

/** Explicit boundary for operating-system file and directory chooser chrome. */
public final class NativeSourceChooser {
    private NativeSourceChooser() { }

    public static FileChooser fileChooser() { return new FileChooser(); }
    public static DirectoryChooser directoryChooser() { return new DirectoryChooser(); }
}
