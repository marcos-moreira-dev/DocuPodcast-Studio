module com.marcosmoreiradev.docupodcaststudio {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.xml;
    requires java.desktop;
    requires java.prefs;
    requires java.net.http;
    requires jdk.httpserver;
    requires jdk.management;
    requires org.apache.pdfbox;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires stylus;
    requires stylus.javafx;
    requires com.marcosmoreiradev.docupodcaststudio.media.api;
    requires com.marcosmoreiradev.docupodcaststudio.ink;

    exports com.marcosmoreiradev.docupodcaststudio;
    exports com.marcosmoreiradev.docupodcaststudio.bootstrap;
}
