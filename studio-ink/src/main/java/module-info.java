module com.marcosmoreiradev.docupodcaststudio.ink {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires stylus;
    requires stylus.javafx;

    exports com.marcosmoreiradev.docupodcaststudio.ink;
    exports com.marcosmoreiradev.docupodcaststudio.ink.canvas;
    exports com.marcosmoreiradev.docupodcaststudio.ink.input;
    exports com.marcosmoreiradev.docupodcaststudio.ink.model;
}
