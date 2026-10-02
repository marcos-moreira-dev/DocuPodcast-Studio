package com.marcosmoreiradev.docupodcaststudio.presentation.accessibility;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioAccordion;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioControlContract;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioControlRuntimeAudit;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFeedbackControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.NativeDecisionDialog;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the headless JavaFX/TestFX lane before product-level journeys are added. */
class JavaFxAccessibilityE2ETest extends ApplicationTest {
    private Button primaryAction;
    private Button iconAction;
    private Label status;
    private TitledPane accordionPane;
    private VBox accordionContent;
    private TextField styledField;
    private Slider styledSlider;
    private CheckBox styledCheckBox;
    private ProgressBar styledProgress;
    private VBox root;

    @Override public void start(Stage stage) {
        status = new Label("Listo");
        status.setId("status");
        primaryAction = ActionButtonFactory.primary("Generar");
        primaryAction.setId("primary-action");
        primaryAction.setAccessibleText("Generar contenido multimedia");
        primaryAction.setOnAction(event -> status.setText("Generación iniciada"));
        iconAction = ActionButtonFactory.sideDockRail(AppIcon.IMAGE,
                () -> status.setText("Imagen abierta"));
        iconAction.setId("icon-action");
        accordionContent = new VBox(new Label("Contenido blanco"));
        accordionPane = StudioAccordion.pane("Opciones", accordionContent);
        Accordion accordion = StudioAccordion.accordion(accordionPane);
        accordion.setExpandedPane(accordionPane);
        styledField = StudioFormControls.textField();
        styledField.setPromptText("Nombre");
        styledSlider = StudioFormControls.slider(0, 100, 42);
        styledCheckBox = StudioFormControls.checkBox("Usar frame dibujado");
        styledCheckBox.setSelected(true);
        styledProgress = StudioFeedbackControls.progressBar(0.62);
        root = new VBox(primaryAction, iconAction, status, styledField, styledSlider, styledCheckBox,
                styledProgress, accordion);
        Scene scene = new Scene(root, 360, 380);
        scene.getStylesheets().add(getClass().getResource("/css/docupodcast-light.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    @Test void keyboardFocusAndAccessibleNameWorkHeadlessly() {
        clickOn("#primary-action");
        assertTrue(primaryAction.isFocused());
        assertEquals("Generar contenido multimedia", primaryAction.getAccessibleText());
        assertEquals("Generación iniciada", status.getText());
    }

    @Test void iconOnlyActionHasAnAccessibleNameAndWorksWithoutMouse() {
        clickOn("#primary-action");
        press(KeyCode.TAB);
        release(KeyCode.TAB);
        assertTrue(iconAction.isFocused());
        assertEquals("Imagen", iconAction.getAccessibleText());
        press(KeyCode.ENTER);
        release(KeyCode.ENTER);
        assertEquals("Imagen abierta", status.getText());
    }

    @Test void accordionUsesPurpleHeaderWhiteTextAndWhiteExpandedContent() {
        interact(() -> accordionPane.applyCss());
        Region title = (Region) accordionPane.lookup(".title");
        assertEquals(Color.web("#5B3FA3"), title.getBackground().getFills().getFirst().getFill());
        assertEquals(Color.WHITE, accordionContent.getBackground().getFills().getFirst().getFill());
        assertTrue(accordionPane.getAccessibleText().contains("expandible"));
    }

    @Test void shortDecisionKeepsNativeButtonsInsideProductDialogChrome() {
        java.util.concurrent.atomic.AtomicReference<Alert> reference = new java.util.concurrent.atomic.AtomicReference<>();
        interact(() -> reference.set(NativeDecisionDialog.create(null, Alert.AlertType.CONFIRMATION,
                "Continuar", ButtonType.OK, ButtonType.CANCEL)));
        Alert alert = reference.get();
        assertTrue(alert.getDialogPane().getStyleClass().contains("product-dialog"));
        Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        assertFalse(ok.getStyleClass().contains("ui-action-button"));
        interact(alert::close);
    }

    @Test void officialFormControlReceivesTheProductSkinAndContract() {
        interact(styledField::applyCss);
        assertEquals(Color.WHITE, styledField.getBackground().getFills().getFirst().getFill());
        assertEquals(Color.web("#D2D8E5"), styledField.getBorder().getStrokes().getFirst().getTopStroke());
        assertTrue(StudioControlContract.descriptor(styledField).isPresent());
    }

    @Test void sliderAndProgressUseBoundedSharedSkinsAndRuntimeMetadata() {
        interact(() -> { root.applyCss(); root.layout(); });
        Region track = (Region) styledSlider.lookup(".track");
        Region thumb = (Region) styledSlider.lookup(".thumb");
        assertTrue(track.getHeight() < 20, "slider track must stay bounded");
        assertTrue(thumb.getWidth() < 40 && thumb.getHeight() < 40, "slider thumb must stay bounded");
        assertTrue(styledProgress.getStyleClass().contains("ui-progress-bar"));
        assertTrue(StudioControlRuntimeAudit.audit(root).isEmpty(),
                () -> StudioControlRuntimeAudit.audit(root).toString());
    }

    @Test void checkboxDecoratesOnlyItsBoxAndNeverReceivesTheGenericFormContainer() {
        interact(() -> { root.applyCss(); root.layout(); });
        assertTrue(styledCheckBox.getStyleClass().contains(StudioFormControls.CHECK_BOX));
        assertFalse(styledCheckBox.getStyleClass().contains(StudioFormControls.FORM_CONTROL));
        Region box = (Region) styledCheckBox.lookup(".box");
        assertEquals(Color.web("#5B5FC7"), box.getBackground().getFills().getFirst().getFill());
        assertTrue(StudioControlContract.descriptor(styledCheckBox).isPresent());
    }
}
