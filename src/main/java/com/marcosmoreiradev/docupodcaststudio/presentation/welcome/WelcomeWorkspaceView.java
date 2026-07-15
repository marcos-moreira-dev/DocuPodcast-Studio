package com.marcosmoreiradev.docupodcaststudio.presentation.welcome;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.EmptyStateView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.InfoBadge;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.IconView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.LucideIconView;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.RibbonIconCatalog;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioFormControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Desktop start screen for DocuPodcast Studio.
 *
 * <p>T103A corrects the first modern home iteration: Inicio must feel like a desktop application
 * start page, not a web landing page. The layout follows the proven DMS-style composition:
 * compact actions on the left, product orientation in the center and recent projects on the right.</p>
 */
public final class WelcomeWorkspaceView extends StackPane {
    private final Runnable openDocument;
    private final Runnable openProject;
    private final Runnable newProject;
    private final Runnable openTechnicalProblemExpress;
    private final Runnable openExamples;
    private final Runnable initialSetup;
    private final Runnable openGuide;
    private final ObservableList<RecentProjectEntry> recentProjects;
    private final Consumer<Path> openRecentProject;
    private VBox recentProjectsList;

    public WelcomeWorkspaceView() {
        this(null, null, null, null, null, null);
    }

    public WelcomeWorkspaceView(Runnable openDocument, Runnable openProject, Runnable newProject) {
        this(openDocument, openProject, newProject, null, null, null);
    }

    public WelcomeWorkspaceView(Runnable openDocument, Runnable openProject, Runnable newProject, Runnable openExamples) {
        this(openDocument, openProject, newProject, openExamples, null, null);
    }

    public WelcomeWorkspaceView(Runnable openDocument, Runnable openProject, Runnable newProject, Runnable openExamples, Runnable initialSetup, Runnable openGuide) {
        this(openDocument, openProject, newProject, openExamples, initialSetup, openGuide,
                FXCollections.observableArrayList(), path -> { });
    }

    public WelcomeWorkspaceView(Runnable openDocument,
                                Runnable openProject,
                                Runnable newProject,
                                Runnable openExamples,
                                Runnable initialSetup,
                                Runnable openGuide,
                                ObservableList<RecentProjectEntry> recentProjects,
                                Consumer<Path> openRecentProject) {
        this(openDocument, openProject, newProject, null, openExamples, initialSetup, openGuide,
                recentProjects, openRecentProject);
    }

    public WelcomeWorkspaceView(Runnable openDocument,
                                Runnable openProject,
                                Runnable newProject,
                                Runnable openTechnicalProblemExpress,
                                Runnable openExamples,
                                Runnable initialSetup,
                                Runnable openGuide,
                                ObservableList<RecentProjectEntry> recentProjects,
                                Consumer<Path> openRecentProject) {
        this.openDocument = openDocument == null ? () -> { } : openDocument;
        this.openProject = openProject == null ? () -> { } : openProject;
        this.newProject = newProject == null ? () -> { } : newProject;
        this.openTechnicalProblemExpress = openTechnicalProblemExpress == null ? () -> { } : openTechnicalProblemExpress;
        this.openExamples = openExamples == null ? () -> { } : openExamples;
        this.initialSetup = initialSetup == null ? () -> { } : initialSetup;
        this.openGuide = openGuide == null ? () -> { } : openGuide;
        this.recentProjects = recentProjects == null ? FXCollections.observableArrayList() : recentProjects;
        this.openRecentProject = openRecentProject == null ? path -> { } : openRecentProject;
        getStyleClass().addAll("welcome-root", "welcome-desktop-home", "welcome-modern-home");
        getChildren().add(scrollableHome());
    }

    private ScrollPane scrollableHome() {
        ScrollPane scroll = new ScrollPane(stage());
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("welcome-scroll");
        return scroll;
    }

    private StackPane stage() {
        StackPane stage = new StackPane();
        stage.getStyleClass().add("welcome-stage");
        stage.getChildren().addAll(decorativeLayer(), desktopGrid());
        return stage;
    }

    private StackPane decorativeLayer() {
        StackPane layer = new StackPane();
        layer.setMouseTransparent(true);
        layer.getStyleClass().add("welcome-art-layer");

        Circle softOrb = new Circle(170);
        softOrb.getStyleClass().addAll("welcome-orb", "welcome-orb-soft");
        softOrb.setTranslateX(-560);
        softOrb.setTranslateY(300);

        ImageView watermark = watermarkLogo();
        StackPane.setAlignment(watermark, Pos.BOTTOM_RIGHT);
        watermark.setTranslateX(104);
        watermark.setTranslateY(76);

        layer.getChildren().addAll(softOrb, watermark);
        return layer;
    }

    private ImageView watermarkLogo() {
        InputStream stream = WelcomeWorkspaceView.class.getResourceAsStream("/branding/docupodcast-watermark.png");
        ImageView view = new ImageView();
        if (stream != null) {
            view.setImage(new Image(stream));
        }
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(420);
        view.setMouseTransparent(true);
        view.getStyleClass().add("welcome-product-watermark");
        return view;
    }

    private HBox desktopGrid() {
        HBox grid = new HBox(34);
        grid.setAlignment(Pos.CENTER);
        grid.getStyleClass().add("welcome-desktop-grid");

        Node actions = startActions();
        Node center = centerIntro();
        Node recents = recentColumn();
        HBox.setHgrow(center, Priority.ALWAYS);
        grid.getChildren().addAll(actions, separator(), center, separator(), recents);
        return grid;
    }

    private VBox startActions() {
        VBox column = new VBox(18);
        column.setAlignment(Pos.TOP_LEFT);
        column.getStyleClass().add("welcome-start-column");
        column.getChildren().addAll(
                sectionTitle("Inicio"),
                commandRow(AppCommandId.OPEN_SETTINGS, "Configuración inicial", "Prepara Voz local simple para escuchar rápido. Voz IA avanzada y Video local quedan como mejoras posteriores.", initialSetup),
                commandRow(AppCommandId.OPEN_SOURCE_DOCUMENT, "Abrir documento", "Importar Word/DOCX, PDF con texto u OCR local, Markdown o TXT.", openDocument),
                commandRow(AppCommandId.OPEN_PROJECT, "Abrir proyecto", "Abrir un proyecto existente.", openProject),
                commandRow(AppCommandId.NEW_PROJECT, "Nuevo proyecto", "Crear un proyecto vacío y preparar la fuente.", newProject),
                commandRow(AppCommandId.PREPARE_TECHNICAL_PROBLEM, "Problema Técnico Express", "Abrir una ventana aislada para importar imágenes, escribir con tableta y exportar PNG.", openTechnicalProblemExpress),
                commandRow(AppCommandId.OPEN_EXAMPLE_PROJECT, "Probar ejemplo", "Crear un proyecto demo incluido.", openExamples),
                commandRow(AppCommandId.OPEN_GUIDE, "Guía rápida", "Consulta los pasos básicos cuando necesites orientación.", openGuide)
        );
        return column;
    }

    private HBox commandRow(AppCommandId commandId, String title, String detail, Runnable action) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("welcome-command-row");

        Node icon = IconView.sideDock(RibbonIconCatalog.iconFor(commandId));
        icon.getStyleClass().add("welcome-command-icon");

        VBox text = new VBox(3);
        text.getStyleClass().add("welcome-command-copy");
        Node actionTitle = action == null
                ? commandLabel(title)
                : ActionButtonFactory.secondary(title, action);
        actionTitle.getStyleClass().add("welcome-command-title");
        Label body = new Label(detail);
        body.setWrapText(true);
        body.getStyleClass().add("welcome-command-detail");
        text.getChildren().addAll(actionTitle, body);

        row.getChildren().addAll(icon, text);
        return row;
    }

    private Label commandLabel(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("welcome-command-title-label");
        return label;
    }

    private VBox centerIntro() {
        VBox center = new VBox(22);
        center.setAlignment(Pos.TOP_LEFT);
        center.getStyleClass().add("welcome-center-column");

        Label prefix = new Label("Bienvenido a");
        prefix.getStyleClass().add("welcome-prefix");
        Label title = new Label("DocuPodcast Studio");
        title.getStyleClass().add("welcome-title");
        Label subtitle = new Label("Abre documentos y empieza escuchando rápido con Voz local simple; luego mejora con Voz IA avanzada, visuales y video MP4 cuando lo necesites.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("welcome-subtitle");

        VBox steps = new VBox(12);
        steps.getStyleClass().add("welcome-step-panel");
        steps.getChildren().addAll(
                step("1", "Escucha rápido", "Configuración inicial prioriza Voz local simple para probar documentos sin una descarga pesada."),
                step("2", "Abre el documento", "DOCX, PDF con texto u OCR local, Markdown o TXT entran como documento protegido."),
                step("3", "Escucha y estudia", "La lectura se controla desde Documento; genera audio desde el fragmento actual, pausa, reanuda y vuelve cuando necesites."),
                step("4", "Exporta", "Genera audio final o video MP4 cuando el proyecto, audio y visuales estén listos.")
        );

        center.getChildren().addAll(prefix, title, subtitle, steps);
        return center;
    }


    private HBox step(String number, String title, String detail) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("welcome-step-row");

        Label n = new Label(number);
        n.getStyleClass().add("welcome-step-number");
        VBox copy = new VBox(3);
        Label t = new Label(title);
        t.getStyleClass().add("welcome-step-title");
        Label d = new Label(detail);
        d.setWrapText(true);
        d.getStyleClass().add("welcome-step-detail");
        copy.getChildren().addAll(t, d);
        row.getChildren().addAll(n, copy);
        return row;
    }

    private VBox recentColumn() {
        VBox column = new VBox(18);
        column.setAlignment(Pos.TOP_LEFT);
        column.getStyleClass().add("welcome-recent-column");
        recentProjectsList = new VBox(9);
        recentProjectsList.getStyleClass().add("welcome-recent-list");
        refreshRecentProjects();
        recentProjects.addListener((ListChangeListener<RecentProjectEntry>) change -> refreshRecentProjects());
        column.getChildren().addAll(
                sectionTitle("Recientes"),
                recentProjectsList,
                promisePanel()
        );
        return column;
    }

    private void refreshRecentProjects() {
        if (recentProjectsList == null) {
            return;
        }
        List<RecentProjectEntry> entries = recentProjects.stream().limit(8).toList();
        if (entries.isEmpty()) {
            recentProjectsList.getChildren().setAll(
                    new EmptyStateView("Historial local", "Sin proyectos recientes", "Cuando abras o crees proyectos, apareceran aqui para volver rapido al trabajo."));
            return;
        }
        recentProjectsList.getChildren().setAll(entries.stream().map(this::recentProjectLink).toList());
    }

    private Node recentProjectLink(RecentProjectEntry entry) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("welcome-recent-link-row");

        Node icon = LucideIconView.of("folder-open");
        icon.getStyleClass().add("welcome-recent-link-icon");

        VBox copy = new VBox(1);
        Hyperlink link = new Hyperlink(entry.displayName());
        link.getStyleClass().add("welcome-recent-link");
        link.setOnAction(event -> openRecentProject.accept(entry.projectFile()));
        StudioFormControls.installTooltip(link, entry.projectFile().toString());

        Label type = new Label("(" + entry.typeLabel() + ")");
        type.getStyleClass().add("welcome-recent-type");
        type.setWrapText(true);

        Label folder = new Label(entry.folderLabel());
        folder.getStyleClass().add("welcome-recent-path");
        folder.setWrapText(true);
        copy.getChildren().addAll(link, type, folder);
        HBox.setHgrow(copy, Priority.ALWAYS);

        row.getChildren().addAll(icon, copy);
        return row;
    }

    private VBox promisePanel() {
        VBox panel = new VBox(9);
        panel.getStyleClass().add("welcome-promise-panel");
        panel.getChildren().addAll(
                new InfoBadge("Documento protegido", "info-badge-neutral"),
                new InfoBadge("Voz local simple para empezar", "info-badge-neutral"),
                new InfoBadge("Voz IA avanzada opcional", "info-badge-neutral"),
                new InfoBadge("Video MP4 con Video local", "info-badge-neutral")
        );
        return panel;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("welcome-section-title");
        return label;
    }

    private Region separator() {
        Region line = new Region();
        line.getStyleClass().add("welcome-vertical-separator");
        return line;
    }
}
