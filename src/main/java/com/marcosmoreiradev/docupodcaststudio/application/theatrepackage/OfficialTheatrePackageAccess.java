package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;
import java.io.IOException;
import java.nio.file.Path;
/** External package access; implementations own ZIP extraction and manifest parsing. */
public interface OfficialTheatrePackageAccess extends TheatrePackageScanner {
    String MANIFEST_FILE = "docupodcast-theatre.json";
    Source open(Path source) throws IOException;
    Path grammarFile(Path root) throws IOException;
    String presentationMode(Path root) throws IOException;
    interface Source extends AutoCloseable {
        Path root();
        void close() throws IOException;
    }
}
