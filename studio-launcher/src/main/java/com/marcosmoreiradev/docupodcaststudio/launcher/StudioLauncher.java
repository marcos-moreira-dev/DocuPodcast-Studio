package com.marcosmoreiradev.docupodcaststudio.launcher;

import com.marcosmoreiradev.docupodcaststudio.bootstrap.DesktopApplicationHost;
import com.marcosmoreiradev.docupodcaststudio.bootstrap.ResolvedVisualEngineLaunchPolicy;
import com.marcosmoreiradev.docupodcaststudio.bootstrap.VisualEngineLaunchPolicyResolver;
import com.marcosmoreiradev.docupodcaststudio.bootstrap.VoiceEngineDevicePolicyResolver;
import com.marcosmoreiradev.docupodcaststudio.localmedia.ComfyUiLaunchProfile;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaLayout;
import com.marcosmoreiradev.docupodcaststudio.ink.StudioInkPlatform;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkNativeRuntimeProbe;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Single production composition root for desktop, media adapters and engine administration. */
public final class StudioLauncher extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudioLauncher.class);
    private volatile DesktopApplicationHost desktopHost;
    @Override public void start(Stage stage) throws java.io.IOException {
        LauncherLayout layout = LauncherLayoutResolver.resolve();
        LocalSocketDirectory.configure(System.getProperties(), layout.runtimeRoot());
        // Compatibility entry points created by JavaFX controls still consult these properties.
        // Publish the already resolved layout so they never fall back to the launcher working dir.
        System.setProperty(LauncherLayoutResolver.APP_ROOT_PROPERTY, layout.installationRoot().toString());
        System.setProperty(LauncherLayoutResolver.RUNTIME_ROOT_PROPERTY, layout.runtimeRoot().toString());
        LOGGER.info("Composing local-first launcher installationRoot={} ({}) runtimeRoot={} ({})",
                layout.installationRoot(), layout.installationSource(), layout.runtimeRoot(), layout.runtimeSource());
        StudioInkPlatform ink = StudioInkPlatform.local();
        if (Boolean.getBoolean("docupodcast.launch.smoke")) {
            InkNativeRuntimeProbe.Readiness readiness = InkNativeRuntimeProbe.inspect();
            LOGGER.info("Native ink smoke readiness lectureStudio={} windowsPointer={} diagnostic={}",
                    readiness.lectureStudioLoaded(), readiness.windowsPointerAvailable(), readiness.diagnostic());
            if (readiness.windows() && !readiness.lectureStudioLoaded()) {
                throw new IllegalStateException("El runtime nativo de tinta no esta listo: " + readiness.diagnostic());
            }
        }
        VisualEngineLaunchPolicyResolver visualPolicyResolver = new VisualEngineLaunchPolicyResolver();
        VoiceEngineDevicePolicyResolver voicePolicyResolver = new VoiceEngineDevicePolicyResolver();
        desktopHost = new DesktopApplicationHost(LocalMediaAdapters.create(new LocalMediaLayout(
                layout.installationRoot(), layout.runtimeRoot()),
                requestedMemory -> launchProfile(
                        visualPolicyResolver.resolve(layout.runtimeRoot(), requestedMemory)),
                voicePolicyResolver::resolve),
                ink, layout.installationRoot(), layout.runtimeRoot());
        desktopHost.start(stage);
        if (Boolean.getBoolean("docupodcast.launch.smoke")) {
            LOGGER.info("Launcher smoke reached a visible main stage; closing cleanly");
            Platform.runLater(() -> {
                stage.close();
                Platform.exit();
            });
        }
    }

    @Override public void stop() {
        DesktopApplicationHost current = desktopHost;
        if (current != null) current.close();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static ComfyUiLaunchProfile launchProfile(ResolvedVisualEngineLaunchPolicy policy) {
        return ComfyUiLaunchProfile.resolved(
                policy.selectedDeviceId(),
                policy.displayName(),
                policy.backend(),
                policy.gpu(),
                policy.deviceArguments(),
                policy.memoryArguments(),
                policy::verifySystemStats);
    }
}
