package com.marcosmoreiradev.docupodcaststudio.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LauncherLayoutResolverTest {

    @TempDir
    Path temporary;

    @Test
    void explicitInstallationAndRuntimeRootsNeverDependOnTheWorkingDirectory() {
        Properties properties = new Properties();
        Path installation = temporary.resolve("installed app");
        Path runtime = temporary.resolve("managed runtime");
        properties.setProperty(LauncherLayoutResolver.APP_ROOT_PROPERTY, installation.toString());
        properties.setProperty(LauncherLayoutResolver.RUNTIME_ROOT_PROPERTY, runtime.toString());

        LauncherLayout layout = LauncherLayoutResolver.resolve(
                properties,
                Map.of(),
                temporary.resolve("arbitrary-working-directory"),
                temporary.resolve("launcher.jar")
        );

        assertEquals(installation.toAbsolutePath().normalize(), layout.installationRoot());
        assertEquals(runtime.toAbsolutePath().normalize(), layout.runtimeRoot());
        assertEquals("explicit", layout.installationSource());
        assertEquals("explicit", layout.runtimeSource());
    }

    @Test
    void developmentLayoutIsResolvedFromTheLauncherCodeSource() throws Exception {
        Path repository = temporary.resolve("repo");
        Files.createDirectories(repository.resolve("studio-launcher/target/classes"));
        Files.createDirectories(repository.resolve("scripts/tts"));
        Files.createDirectories(repository.resolve("models"));
        Files.createDirectories(repository.resolve("tools"));
        Files.writeString(repository.resolve("pom.xml"), "<project/>");

        LauncherLayout layout = LauncherLayoutResolver.resolve(
                new Properties(),
                Map.of(),
                temporary.resolve("foreign-working-directory"),
                repository.resolve("studio-launcher/target/classes")
        );

        assertEquals(repository.toAbsolutePath().normalize(), layout.installationRoot());
        assertEquals(repository.toAbsolutePath().normalize(), layout.runtimeRoot());
        assertEquals("development-layout", layout.installationSource());
        assertEquals("development-layout", layout.runtimeSource());
    }

    @Test
    void packagedLauncherUsesItsInstallationButKeepsMutableRuntimeInLocalAppData() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user.home", temporary.resolve("home").toString());
        Path localAppData = temporary.resolve("local-app-data");
        Path launcherJar = temporary.resolve("installation/app/studio-launcher.jar");
        Files.createDirectories(launcherJar.getParent());
        Files.writeString(launcherJar, "launcher");

        LauncherLayout layout = LauncherLayoutResolver.resolve(
                properties,
                Map.of("LOCALAPPDATA", localAppData.toString()),
                temporary.resolve("foreign-working-directory"),
                launcherJar
        );

        assertEquals(launcherJar.getParent().toAbsolutePath().normalize(), layout.installationRoot());
        assertEquals(localAppData.resolve("DocuPodcastStudio/runtime").toAbsolutePath().normalize(),
                layout.runtimeRoot());
        assertEquals("code-source", layout.installationSource());
        assertEquals("local-app-data", layout.runtimeSource());
    }
}
