package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF5B applies the generated product mark as the real app/installer icon. */
final class ProductIconBrandingPf5BSourceTest {
    @Test
    void appLoadsBrandingIconsAndResourcesExist() throws Exception {
        String app = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/DocuPodcastStudioApp.java"));
        assertTrue(app.contains("loadStageIcons(stage)"));
        assertTrue(app.contains("/branding/docupodcast-icon-32.png"));
        assertTrue(app.contains("/branding/docupodcast-icon-64.png"));
        assertTrue(app.contains("/branding/docupodcast-icon-256.png"));
        assertTrue(Files.exists(Path.of("src/main/resources/branding/docupodcast-icon-32.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/branding/docupodcast-icon-64.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/branding/docupodcast-icon-256.png")));
    }

    @Test
    void packagingScriptsUseWindowsIco() throws Exception {
        String appImage = Files.readString(Path.of("scripts/14-app-image-completa.bat"));
        String msi = Files.readString(Path.of("scripts/15-msi-completo.bat"));
        assertTrue(appImage.contains("packaging\\windows\\docupodcast-icon.ico"));
        assertTrue(appImage.contains("--icon \"%ICON_ICO%\""));
        assertTrue(msi.contains("packaging\\windows\\docupodcast-icon.ico"));
        assertTrue(msi.contains("--icon \"%ICON_ICO%\""));
        assertTrue(Files.exists(Path.of("packaging/windows/docupodcast-icon.ico")));
        assertTrue(Files.exists(Path.of("packaging/windows/docupodcast-icon.png")));
    }
}
