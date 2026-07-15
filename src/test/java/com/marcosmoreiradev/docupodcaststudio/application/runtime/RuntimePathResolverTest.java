package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimePathResolverTest {
    @TempDir
    Path temp;

    @Test
    void explicitSystemPropertyWinsAndNormalizesLayout() {
        String previous = System.getProperty(RuntimePathResolver.APP_ROOT_PROPERTY);
        try {
            System.setProperty(RuntimePathResolver.APP_ROOT_PROPERTY, temp.toString());
            RuntimePathResolution resolution = RuntimePathResolver.defaultResolver().resolve(Path.of("ignored"));
            assertTrue(resolution.explicit());
            assertEquals(temp.toAbsolutePath().normalize(), resolution.applicationRoot());
            assertEquals(temp.resolve("tools").toAbsolutePath().normalize(), resolution.layout().toolsRoot());
            assertEquals(temp.resolve("models").toAbsolutePath().normalize(), resolution.layout().modelsRoot());
            assertEquals(temp.resolve("tools/ffmpeg/bin/ffmpeg.exe").toAbsolutePath().normalize(), resolution.layout().ffmpegExecutable());
        } finally {
            if (previous == null) {
                System.clearProperty(RuntimePathResolver.APP_ROOT_PROPERTY);
            } else {
                System.setProperty(RuntimePathResolver.APP_ROOT_PROPERTY, previous);
            }
        }
    }

    @Test
    void resolvesConfiguredRelativePathsAgainstApplicationRoot() {
        ApplicationRuntimeLayout layout = new ApplicationRuntimeLayout(temp);
        assertEquals(temp.resolve("custom/ffmpeg.exe").toAbsolutePath().normalize(), layout.resolveConfiguredPath("custom/ffmpeg.exe"));
        assertEquals(temp.resolve("tools/piper/piper.exe").toAbsolutePath().normalize(),
                layout.resolveConfiguredPathOrBundled("", "tools/piper/piper.exe"));
    }
}
