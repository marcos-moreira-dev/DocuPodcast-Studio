module com.marcosmoreiradev.docupodcaststudio.local.media.adapters {
    requires com.marcosmoreiradev.docupodcaststudio.media.api;
    requires java.net.http;
    requires java.desktop;
    requires java.management;
    requires java.xml;
    requires jdk.management;
    requires mathcat4j.api;
    requires mathcat4j.jni.loader.jna;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    exports com.marcosmoreiradev.docupodcaststudio.localmedia;
}
