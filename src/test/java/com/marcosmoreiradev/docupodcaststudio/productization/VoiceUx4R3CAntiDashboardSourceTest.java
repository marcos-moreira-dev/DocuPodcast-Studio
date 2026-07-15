package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-UX4R-3C: Vista Voces keeps the sober Teams-like administration surface without dashboards. */
final class VoiceUx4R3CAntiDashboardSourceTest {
    @Test
    void voicesHomeUsesSoberOperationalRowsInsteadOfMetricDashboard() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String profileCard = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileCard.java");
        String engineCard = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineModeCard.java");
        String css = read("src/main/resources/css/voice-library.css");

        assertFalse(view.contains("homeOperationalSummary"));
        assertFalse(view.contains("homeStatusRow"));
        assertTrue(view.contains("voice-engine-test-card"));
        assertTrue((profileCard + engineCard).contains("new InfoBadge"));
        assertTrue(view.contains("VoiceActionStrip.of"));
        assertTrue(view.contains("balancedMasterDetail(list, detail)"));
        assertFalse(view.contains("box.getChildren().addAll(summary, list);"));
        assertTrue(css.contains(".voice-engine-test-card"));
        assertFalse(view.contains("metricCard("));
        assertFalse(view.contains("voice-dashboard-metrics"));
        assertFalse(view.contains("voice-metric-card"));
        assertFalse(css.contains("voice-dashboard-metrics"));
        assertFalse(css.contains("voice-metric-card"));
    }

    @Test
    void voicesSidebarIsDarkFullHeightAndWithoutGradient() throws Exception {
        String navigation = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleNavigation.java");
        String css = read("src/main/resources/css/voice-library.css");
        String lowerCss = css.toLowerCase(Locale.ROOT);

        assertTrue(navigation.contains("setMaxHeight(Double.MAX_VALUE)"));
        assertTrue(navigation.contains("voice-module-navigation-spacer"));
        assertTrue(navigation.contains("VBox.setVgrow(spacer, Priority.ALWAYS)"));
        assertTrue(css.contains("#252A44"));
        assertTrue(css.contains("#383F66"));
        assertTrue(css.contains("#8B8CFF"));
        assertFalse(css.contains("#EAF0F8"));
        assertFalse(lowerCss.contains("linear-gradient"));
    }

    @Test
    void currentDocsRegisterThisAntiDashboardPass() throws Exception {
        String current = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String validation = read("VALIDATION.md");
        String doc = read("docs/productizacion/VOZ_UX4R_3C_SIDEBAR_OSCURO_ANTI_DASHBOARD.md");

        assertTrue(current.contains("VOZ-UX4R-3C"));
        assertTrue(validation.contains("VoiceUx4R3CAntiDashboardSourceTest"));
        assertTrue(doc.contains("sidebar oscuro"));
        assertTrue(doc.contains("sin dashboard"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
