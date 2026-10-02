package com.marcosmoreiradev.docupodcaststudio.presentation.batch;

import com.marcosmoreiradev.docupodcaststudio.domain.export.AudioExportFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

final class DocumentVideoBatchWindowTest {
    @BeforeAll static void startFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); }
        catch (IllegalStateException alreadyStarted) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    @Test void documentRowSupportsInheritedVisibilityRemovalAndAudioWithoutErasingSettings() throws Exception {
        fx(() -> {
            var context = context();
            try {
                var map = field(context.window, "documentBackgrounds", java.util.Map.class);
                map.put("one.docx",new DocumentBackgroundOverride("missing.png",null));
                var row = (javafx.scene.Parent)invoke(context.window,"documentBackgroundRow",String.class,"one.docx");
                var combo = (ComboBox<Integer>)row.lookupAll(".combo-box").stream().findFirst().orElseThrow();
                assertEquals(-1,combo.getValue());
                assertTrue(row.lookupAll(".label").stream().map(Label.class::cast).anyMatch(l -> l.getText().contains("no disponible")));
                combo.setValue(60);
                assertEquals(0.6,((DocumentBackgroundOverride)map.get("one.docx")).visibility());
                field(context.window,"audioOutput",RadioButton.class).setSelected(true);
                assertTrue(combo.isDisabled());
                assertTrue(((DocumentVideoBatchProfile)invoke(context.window,"profile")).documentBackgrounds().containsKey("one.docx"));
                field(context.window,"videoOutput",RadioButton.class).setSelected(true);
                row.lookupAll(".button").stream().map(Button.class::cast).filter(b -> b.getText().equals("Quitar personalización")).findFirst().orElseThrow().fire();
                assertTrue(map.isEmpty());
                assertTrue(((DocumentVideoBatchProfile)invoke(context.window,"profile")).documentBackgrounds().isEmpty());
            } finally { context.queue.close(); context.host.close(); }
            return null;
        });
    }

    @Test void outputChoiceHidesVisualSettingsAndPersistsTheSelectedFormat() throws Exception {
        fx(() -> {
            var context = context();
            try {
                RadioButton audio = field(context.window, "audioOutput", RadioButton.class);
                audio.setSelected(true);
                @SuppressWarnings("unchecked")
                ComboBox<AudioExportFormat> format = field(context.window, "audioFormat", ComboBox.class);
                format.setValue(AudioExportFormat.AAC);
                var profile = (DocumentVideoBatchProfile) invoke(context.window, "profile");
                assertTrue(profile.audioOnly());
                assertEquals(AudioExportFormat.AAC, profile.audioFormat());
                var panes = context.queue.getScene().getRoot().lookupAll(".titled-pane");
                assertFalse(panes.isEmpty());
                assertTrue(panes.stream().filter(node -> ((TitledPane) node).getText().contains("Apariencia"))
                        .allMatch(node -> !node.isVisible() && !node.isManaged()));
                assertFalse(context.host.isShowing());
                assertTrue(context.queue.isShowing());
            } finally { context.queue.close(); context.host.close(); }
            return null;
        });
    }

    @Test void pauseAndCompletionKeepQueueUntilAcceptAndPrepareHomeBeforeShowingHost() throws Exception {
        Context context = fx(DocumentVideoBatchWindowTest::context);
        try {
            fx(() -> { start(context); context.port.finish(true); return null; });
            fx(() -> {
                assertTrue(context.queue.isShowing());
                assertFalse(context.host.isShowing());
                context.port.finish(false);
                return null;
            });
            fx(() -> {
                assertFalse(context.home.get());
                assertTrue(context.queue.isShowing());
                Button accept = context.queue.getScene().getRoot().lookupAll(".button").stream()
                        .filter(node -> node instanceof Button button && button.getText().equals("Aceptar y volver a Inicio"))
                        .map(Button.class::cast).findFirst().orElseThrow();
                accept.fire();
                assertTrue(context.home.get());
                assertTrue(context.host.isShowing());
                assertFalse(context.queue.isShowing());
                return null;
            });
        } finally { fx(() -> { context.queue.close(); context.host.close(); return null; }); }
    }

    @Test void cancelAllWaitsForExecutorBeforeRestoringHome() throws Exception {
        Context context = fx(DocumentVideoBatchWindowTest::context);
        try {
            fx(() -> {
                start(context);
                Platform.runLater(() -> {
                    for (Window window : List.copyOf(Window.getWindows())) {
                        if (window.getScene() != null && window.getScene().getRoot() instanceof DialogPane pane) {
                            var yes = pane.lookupButton(ButtonType.YES);
                            if (yes instanceof Button button) { button.fire(); break; }
                        }
                    }
                });
                invoke(context.window, "cancelEntireQueue");
                assertTrue(context.port.cancelled);
                assertTrue(context.queue.isShowing());
                assertFalse(context.host.isShowing());
                assertFalse(context.home.get());
                context.port.finish(false);
                return null;
            });
            fx(() -> {
                assertFalse(context.queue.isShowing());
                assertTrue(context.host.isShowing());
                assertTrue(context.home.get());
                return null;
            });
        } finally { fx(() -> { context.queue.close(); context.host.close(); return null; }); }
    }

    @Test void appearancePreviewTracksTypographyColorsAndBrandingState() throws Exception {
        fx(() -> {
            var context = context();
            try {
                @SuppressWarnings("unchecked")
                ComboBox<String> family = field(context.window, "fontFamily", ComboBox.class);
                @SuppressWarnings("unchecked")
                ComboBox<String> titleFamily = field(context.window, "titleFontFamily", ComboBox.class);
                @SuppressWarnings("unchecked")
                Spinner<Integer> size = field(context.window, "fontSize", Spinner.class);
                ColorPicker textColor = field(context.window, "textColor", ColorPicker.class);
                CheckBox branding = field(context.window, "brandingEnabled", CheckBox.class);
                @SuppressWarnings("unchecked")
                ComboBox<BrandingSize> brandingSize = field(context.window, "brandingSize", ComboBox.class);
                Label previewText = field(context.window, "previewText", Label.class);
                Label previewTitle = field(context.window, "previewAccent", Label.class);
                javafx.scene.image.ImageView logo = field(context.window, "previewLogo", javafx.scene.image.ImageView.class);
                ProgressIndicator busy = field(context.window, "busy", ProgressIndicator.class);

                assertFalse(family.isEditable());
                assertEquals(java.util.Set.copyOf(javafx.scene.text.Font.getFamilies()),
                        java.util.Set.copyOf(family.getItems()));
                assertNotNull(family.getCellFactory());
                assertNotNull(family.getButtonCell());
                assertTrue(busy.getStyleClass().contains("ui-progress-indicator"));
                assertTrue(context.queue.getScene().getRoot().lookupAll(".label").stream()
                        .map(Label.class::cast).map(Label::getText)
                        .anyMatch("Color de títulos y elementos secundarios"::equals));
                assertEquals(List.of(BrandingSize.SMALL, BrandingSize.MEDIUM, BrandingSize.LARGE),
                        brandingSize.getItems());
                assertEquals(BrandingSize.MEDIUM, brandingSize.getValue());

                family.setValue("Georgia");
                titleFamily.setValue("Verdana");
                size.getValueFactory().setValue(72);
                textColor.setValue(javafx.scene.paint.Color.web("#123456"));
                branding.setSelected(true);
                brandingSize.setValue(BrandingSize.LARGE);

                assertTrue(previewText.getStyle().contains("Georgia"));
                assertTrue(previewTitle.getStyle().contains("Verdana"));
                assertTrue(previewText.getStyle().contains("51.84px"));
                assertTrue(previewText.getStyle().contains("#123456"));
                assertFalse(logo.isVisible(), "An enabled but missing file must not create a broken logo preview");
                var profile = (DocumentVideoBatchProfile) invoke(context.window, "profile");
                assertEquals(30, profile.branding().sizePercent());
                Label requirement = field(context.window, "createRequirement", Label.class);
                invoke(context.window, "showConfigurationSavedFeedback", String.class,
                        "Configuración actualizada correctamente.");
                assertEquals("Configuración actualizada correctamente.", requirement.getText());
                assertTrue(requirement.getStyle().contains("#15803D"));
                invoke(context.window, "updateCreateEnabled");
                assertFalse(requirement.getText().contains("actualizada correctamente"));
            } finally { context.queue.close(); context.host.close(); }
            return null;
        });
    }

    @Test void setupKeepsOneStableResponsiveWidth() throws Exception {
        fx(() -> {
            var context = context();
            try {
                ScrollPane scroll = (ScrollPane) context.queue.getScene().getRoot().lookup(".scroll-pane");
                assertNotNull(scroll);
                assertTrue(scroll.isFitToWidth());
                assertEquals(ScrollPane.ScrollBarPolicy.NEVER, scroll.getHbarPolicy());

                javafx.scene.image.ImageView background = field(
                        context.window, "previewBackground", javafx.scene.image.ImageView.class);
                javafx.scene.image.ImageView foreground = field(
                        context.window, "previewForeground", javafx.scene.image.ImageView.class);
                @SuppressWarnings("unchecked")
                ComboBox<Integer> opacity = field(context.window, "backgroundImageOpacity", ComboBox.class);
                assertTrue(opacity.getItems().contains(10));
                assertFalse(background.isManaged(),
                        "A preview child bound to its parent width must not feed that parent's preferred width");
                background.setImage(new javafx.scene.image.WritableImage(1920, 1080));
                background.setVisible(true);
                context.queue.getScene().getRoot().applyCss();
                context.queue.getScene().getRoot().layout();
                double firstWidth = scroll.getContent().getLayoutBounds().getWidth();
                for (int pulse = 0; pulse < 20; pulse++) {
                    context.queue.getScene().getRoot().layout();
                }
                double settledWidth = scroll.getContent().getLayoutBounds().getWidth();
                assertEquals(firstWidth, settledWidth, 0.5,
                        "The preview must not expand its scroll content on successive layout pulses");
                assertTrue(settledWidth <= scroll.getViewportBounds().getWidth() + 1.0,
                        "The setup form must stay inside the visible viewport");
                StackPane preview = field(context.window, "appearancePreview", StackPane.class);
                assertEquals(16.0 / 9.0, preview.getWidth() / preview.getHeight(), 0.03,
                        "The 2K preview must keep the same 16:9 aspect ratio as its rendered frame");
                foreground.setImage(new javafx.scene.image.WritableImage(1000, 1000));
                foreground.setVisible(true);
                invoke(context.window, "centerPreviewForeground");
                assertTrue(foreground.getLayoutX() > 0,
                        "A contained landscape image must be centered instead of sticking to the left edge");
            } finally { context.queue.close(); context.host.close(); }
            return null;
        });
    }

    @Test void advancedEnginesAreVisibleAndBecomePartOfTheBatchProfile() throws Exception {
        fx(() -> {
            var context = context();
            try {
                @SuppressWarnings("unchecked")
                ComboBox<DocumentVideoBatchExecutionPort.EngineChoice> voice =
                        field(context.window, "voiceEngine", ComboBox.class);
                @SuppressWarnings("unchecked")
                ComboBox<DocumentVideoBatchExecutionPort.EngineChoice> ai =
                        field(context.window, "aiEngine", ComboBox.class);
                CheckBox interpret = field(context.window, "interpretImages", CheckBox.class);
                assertTrue(context.queue.getScene().getRoot().lookupAll(".titled-pane").stream()
                        .map(TitledPane.class::cast)
                        .anyMatch(pane -> pane.getText().contains("Configuración avanzada de motores")));
                assertEquals("piper", voice.getValue().id());
                assertTrue(ai.isDisabled());
                interpret.setSelected(true);
                assertFalse(ai.isDisabled());
                ai.setValue(ai.getItems().stream().filter(choice -> choice.id().equals("qwen3-vl-local"))
                        .findFirst().orElseThrow());
                voice.setValue(voice.getItems().stream().filter(choice -> choice.id().equals("xtts"))
                        .findFirst().orElseThrow());
                var profile = (DocumentVideoBatchProfile) invoke(context.window, "profile");
                assertEquals("xtts", profile.voiceEngineId());
                assertEquals("qwen3-vl-local", profile.aiEngineId());
                assertEquals("xtts", context.port.selectedGlobalVoiceEngine);
            } finally { context.queue.close(); context.host.close(); }
            return null;
        });
    }

    private static void start(Context context) throws Exception {
        var method = DocumentVideoBatchWindow.class.getDeclaredMethod("startProduction", DocumentVideoBatchProject.class, Path.class);
        method.setAccessible(true);
        method.invoke(context.window, context.port.project, Path.of("target/test-express/queue.json").toAbsolutePath());
    }

    private static Context context() throws Exception {
        Stage host = new Stage();
        host.setScene(new Scene(new StackPane(new Label("Editor")), 400, 200));
        host.show();
        AtomicBoolean home = new AtomicBoolean();
        FakeExecution port = new FakeExecution();
        var window = new DocumentVideoBatchWindow(host, path -> {}, port, () -> {
            assertFalse(host.isShowing(), "Prepare Home before restoring the host");
            home.set(true);
        }, com.marcosmoreiradev.docupodcaststudio.application.services.BatchApplicationServices.create(
                new com.marcosmoreiradev.docupodcaststudio.infrastructure.json.JsonDocumentVideoBatchRepository(),
                new com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository()));
        invoke(window, "showWindow");
        return new Context(host, field(window, "stage", Stage.class), window, port, home);
    }

    private record Context(Stage host, Stage queue, DocumentVideoBatchWindow window, FakeExecution port, AtomicBoolean home) { }
    private static final class FakeExecution implements DocumentVideoBatchExecutionPort {
        private final DocumentVideoBatchProject project = new DocumentVideoBatchProject(1, "batch", "Audios", "source", "target", "queue.json",
                new DocumentVideoBatchProfile(null, 6, false, null, BatchOutputKind.AUDIO, AudioExportFormat.WAV),
                List.of(), 0, Instant.now(), Instant.now());
        private Listener listener;
        private boolean running;
        private boolean cancelled;
        private String selectedGlobalVoiceEngine = "";
        public void start(DocumentVideoBatchProject project, Path descriptor, Listener listener) { this.listener = listener; running = true; }
        public boolean running() { return running; }
        public void requestPause() { }
        public void cancelCurrent() { }
        public void cancelAll() { cancelled = true; }
        @Override public EngineConfiguration engineConfiguration() {
            return new EngineConfiguration(
                    List.of(new EngineChoice("", "Configuración general", "", true),
                            new EngineChoice("piper", "Piper", "Listo", true),
                            new EngineChoice("xtts", "XTTS", "Listo", true)),
                    List.of(new EngineChoice("", "Automático", "", true),
                            new EngineChoice("qwen3-vl-local", "Qwen3-VL", "Listo", true)),
                    "piper", "");
        }
        @Override public void selectGlobalVoiceEngine(String engineId) {
            selectedGlobalVoiceEngine = engineId;
        }
        private void finish(boolean paused) {
            running = false;
            listener.finished(project, new BatchRunResult(0, 0, 0, cancelled ? 1 : 0, paused, "Listo"));
        }
    }
    private static Object invoke(Object object, String name) throws Exception {
        var method = object.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(object);
    }
    private static Object invoke(Object object, String name, Class<?> parameterType, Object argument) throws Exception {
        var method = object.getClass().getDeclaredMethod(name, parameterType);
        method.setAccessible(true);
        return method.invoke(object, argument);
    }
    private static <T> T field(Object object, String name, Class<T> type) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return type.cast(field.get(object));
    }
    private static <T> T fx(java.util.concurrent.Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action); Platform.runLater(task); return task.get(20, TimeUnit.SECONDS);
    }
}
