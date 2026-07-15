package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEngineArtifactInspectionReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectLocalTheatreImageEngineArtifactsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupportPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxLicenseAcceptanceStore;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxModelBundle;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationMemoryProfile;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/** Product settings card for the managed local theatre image engine. */
final class ImageEngineSettingsCard {
    private final ImageEngineSettingsOperations operations;

    ImageEngineSettingsCard(ImageEngineSettingsOperations operations) {
        this.operations = operations;
    }

    Node create(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        VBox card = new VBox(8);
        card.getStyleClass().add("settings-engine-status-card");

        Label title = new Label("Imagen IA teatral local");
        title.getStyleClass().add("settings-engine-status-title");

        Label description = new Label("Prepara un motor local autocontenido para generar imagenes por intervencion. El flujo base usa referencias/adaptadores; LoRA queda como opcion avanzada para identidad o estilo entrenado. Los modelos pesados solo se descargan con confirmacion especifica.");
        description.setWrapText(true);
        description.getStyleClass().add("settings-engine-status-message");

        Label status = new Label(initialStatus(form, services, applicationRoot));
        status.setWrapText(true);
        status.getStyleClass().add("settings-engine-status-action");

        GridPane summary = summaryGrid(form);
        VBox catalog = modelCatalog(form, services, applicationRoot, status);

        Button verify = ActionButtonFactory.secondary("Verificar todo",
                () -> operations.verify(form, services, applicationRoot, status));
        Button start = ActionButtonFactory.secondary("Iniciar motor",
                () -> operations.startEngine(form, services, applicationRoot, status));
        Button stop = ActionButtonFactory.secondary("Detener motor",
                () -> operations.stopEngine(form, services, status));
        Button smoke = ActionButtonFactory.secondary("Probar generacion",
                () -> operations.runSmoke(form, services, applicationRoot, status));
        disableIfNoServices(services, verify, start, stop, smoke);

        HBox engineActions = actionRow(verify, start, stop, smoke);

        TitledPane advanced = advancedDiagnostics(form);
        advanced.setExpanded(false);

        card.getChildren().addAll(title, description, status, summary, engineActions, catalog, advanced);
        return card;
    }

    static boolean ready(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        if (services == null) {
            return false;
        }
        try {
            return services.inspectLocalTheatreImageEngine()
                    .inspect(form.toSettings(), applicationRoot)
                    .ready();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String initialStatus(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot) {
        if (services == null) {
            return "Pendiente de verificacion. Abre Configuracion con servicios activos para preparar Imagen IA teatral.";
        }
        try {
            ImageEngineReadinessReport report = services.inspectLocalTheatreImageEngine()
                    .inspect(form.toSettings(), applicationRoot);
            return SettingsDialog.friendlyEngineText(report.statusLabel() + " Siguiente accion: " + report.nextAction());
        } catch (RuntimeException ex) {
            return "Pendiente de verificacion. Usa Verificar para revisar runtime, paquete y motor local.";
        }
    }

    private GridPane summaryGrid(SettingsFormModel form) {
        configureImageProfileCombo(form);
        configureMemoryProfileCombo(form);
        GridPane grid = new GridPane();
        grid.getStyleClass().add("settings-edit-grid");
        grid.setHgap(12);
        grid.setVgap(8);
        Label model = summaryText();
        Label size = summaryText();
        Label access = summaryText();
        Label consumption = summaryText();
        Label intermediates = summaryText();
        Label workflow = summaryText();
        Label execution = summaryText();
        Label memory = summaryText();
        Runnable refreshProfile = () -> {
            ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(form.imagePreset.getValue());
            ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forPresetId(form.imagePreset.getValue());
            ImageGenerationMemoryProfile memoryProfile = ImageGenerationMemoryProfile.from(form.imageMemoryProfile.getValue(),
                    form.imageLowVram.isSelected());
            model.setText(profile.checkpointName().isBlank() ? "Definido por paquete importado" : profile.checkpointName());
            size.setText(humanBytes(profile.approximateBytes()));
            access.setText(accessLabel(profile));
            consumption.setText(memoryProfile.displayName());
            intermediates.setText(profile.supportsInterpolation() ? "Soportados por workflow" : "No disponibles en este perfil");
            workflow.setText(support.workflowName());
            execution.setText(support.builtInWorkflowAvailable()
                    ? "Disponible en flujo integrado"
                    : "Requiere importacion y conexion explicita");
            memory.setText(memoryProfile.displayName());
        };
        form.imagePreset.valueProperty().addListener((obs, oldValue, newValue) -> {
            ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(newValue);
            if (!profile.checkpointName().isBlank()) {
                form.imageModelName.setText(profile.checkpointName());
            }
            form.imageMemoryProfile.setValue(profile.highEnd()
                    ? ImageGenerationMemoryProfile.VRAM_RAM_OFFLOAD.name()
                    : ImageGenerationMemoryProfile.SAFE_LOW_VRAM.name());
            form.imageLowVram.setSelected(ImageGenerationMemoryProfile.from(form.imageMemoryProfile.getValue(), false).legacyLowVram());
            refreshProfile.run();
        });
        form.imageMemoryProfile.valueProperty().addListener((obs, oldValue, newValue) -> {
            form.imageLowVram.setSelected(ImageGenerationMemoryProfile.from(newValue, form.imageLowVram.isSelected()).legacyLowVram());
            refreshProfile.run();
        });
        addSummaryNodeRow(grid, 0, "Perfil", form.imagePreset);
        addSummaryRow(grid, 1, "Modelo", model);
        addSummaryRow(grid, 2, "Tamano", size);
        addSummaryRow(grid, 3, "Descarga", access);
        addSummaryRow(grid, 4, "Consumo", consumption);
        addSummaryRow(grid, 5, "Intermedios", intermediates);
        addSummaryRow(grid, 6, "Workflow", workflow);
        addSummaryRow(grid, 7, "Ejecucion", execution);
        addSummaryNodeRow(grid, 8, "Memoria", form.imageMemoryProfile);
        refreshProfile.run();
        return grid;
    }

    private static void addSummaryRow(GridPane grid, int row, String key, Label text) {
        Label label = new Label(key);
        label.getStyleClass().add("settings-key");
        grid.add(label, 0, row);
        grid.add(text, 1, row);
        GridPane.setHgrow(text, Priority.ALWAYS);
    }

    private static void addSummaryNodeRow(GridPane grid, int row, String key, Node node) {
        Label label = new Label(key);
        label.getStyleClass().add("settings-key");
        grid.add(label, 0, row);
        grid.add(node, 1, row);
        GridPane.setHgrow(node, Priority.ALWAYS);
    }

    private static Label summaryText() {
        Label text = new Label();
        text.setWrapText(true);
        text.getStyleClass().add("settings-engine-status-message");
        return text;
    }

    private static void configureImageProfileCombo(SettingsFormModel form) {
        form.imagePreset.setEditable(false);
        form.imagePreset.setPrefWidth(320);
        form.imagePreset.setConverter(new StringConverter<>() {
            @Override public String toString(String value) {
                return ImageModelPackageProfile.fromPreset(value).displayName();
            }

            @Override public String fromString(String value) {
                return value;
            }
        });
        form.imagePreset.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ImageModelPackageProfile.fromPreset(item).displayName());
            }
        });
        form.imagePreset.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ImageModelPackageProfile.fromPreset(item).displayName());
            }
        });
    }

    private static void configureMemoryProfileCombo(SettingsFormModel form) {
        form.imageMemoryProfile.setEditable(false);
        form.imageMemoryProfile.setPrefWidth(260);
        form.imageMemoryProfile.setConverter(new StringConverter<>() {
            @Override public String toString(String value) {
                return ImageGenerationMemoryProfile.from(value, true).displayName();
            }

            @Override public String fromString(String value) {
                return value;
            }
        });
        form.imageMemoryProfile.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ImageGenerationMemoryProfile.from(item, true).displayName());
            }
        });
        form.imageMemoryProfile.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ImageGenerationMemoryProfile.from(item, true).displayName());
            }
        });
    }


    private static String accessLabel(ImageModelPackageProfile profile) {
        return switch (profile.accessPolicy()) {
            case PUBLIC -> "Disponible con confirmacion";
            case GATED_OR_TOKEN -> "Puede requerir acceso del proveedor";
            case MANUAL_IMPORT -> "Importacion manual";
        };
    }

    private VBox modelCatalog(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        VBox box = new VBox(8);
        box.getStyleClass().add("settings-image-resource-list");
        Label title = new Label("Catalogo de modelos y componentes");
        title.getStyleClass().add("settings-engine-status-title");
        box.getChildren().add(title);

        addRuntimeRow(box, form, services, applicationRoot, status);
        for (ImageModelPackageProfile profile : ImageModelPackageProfile.values()) {
            if (profile == ImageModelPackageProfile.HIGH_QUALITY_FLUX) {
                addFluxProfileRow(box, form, services, applicationRoot, status);
            } else {
                addProfileRow(box, profile, form, services, applicationRoot, status);
            }
        }
        addEnhancementComponentRows(box, form, services, applicationRoot, status);
        addReferenceRow(box, form, services, applicationRoot, status);
        return box;
    }

    private void addRuntimeRow(VBox box, SettingsFormModel form, SettingsApplicationServices services,
                               Path applicationRoot, Label status) {
        Button prepare = ActionButtonFactory.primary("Crear carpetas",
                () -> operations.confirmAndPrepare(form, services, applicationRoot, status));
        Button importRuntime = ActionButtonFactory.secondary("Importar runtime",
                () -> operations.importRuntimeFolder(form, services, applicationRoot, status));
        Button verify = ActionButtonFactory.secondary("Verificar",
                () -> operations.verify(form, services, applicationRoot, status));
        disableIfNoServices(services, prepare, importRuntime, verify);
        box.getChildren().add(resourceRow("Runtime local",
                "https://github.com/comfyanonymous/ComfyUI\n"
                        + "tools/image con start-image-engine.bat, run.bat, ComfyUI.bat o ComfyUI/main.py con Python local.",
                "Segun runtime",
                runtimeStatus(applicationRoot),
                actionRow(prepare, importRuntime, verify)));
    }

    private void addProfileRow(VBox box, ImageModelPackageProfile profile, SettingsFormModel form,
                               SettingsApplicationServices services, Path applicationRoot, Label status) {
        ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forPresetId(profile.presetId());
        String provider = profile.downloadUrl().isBlank()
                ? "Importacion local. " + profile.description() + "\nWorkflow: " + support.workflowName()
                : profile.providerUrl() + "\nDescarga: " + profile.downloadUrl() + "\nWorkflow: " + support.workflowName();
        if (!profile.manualSetupInstructions().isBlank()) {
            provider += "\n" + profile.manualSetupInstructions();
        }
        String size = profile.highEnd()
                ? humanBytes(profile.approximateBytes()) + "\nEquipo potente"
                : humanBytes(profile.approximateBytes());
        HBox actions = new HBox(6);
        actions.getStyleClass().add("settings-image-resource-actions");
        if (!profile.downloadUrl().isBlank()) {
            Button download = ActionButtonFactory.secondary(profileInstalled(profile, applicationRoot) ? "Reinstalar modelo" : "Descargar modelo",
                    () -> {
                        selectProfile(form, profile);
                        operations.confirmAndDownloadPackage(form, services, applicationRoot, status,
                                profileInstalled(profile, applicationRoot));
                    });
            actions.getChildren().add(download);
        }
        Button importPackage = ActionButtonFactory.secondary("Importar modelo local",
                () -> {
                    selectProfile(form, profile);
                    operations.importPackageFolder(form, services, applicationRoot, status);
                });
        Button verify = ActionButtonFactory.secondary("Verificar",
                () -> {
                    selectProfile(form, profile);
                    operations.verify(form, services, applicationRoot, status);
                });
        actions.getChildren().addAll(importPackage, verify);
        disableIfNoServices(services, actions.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toArray(Button[]::new));
        box.getChildren().add(resourceRow(profile.displayName() + "\n" + profile.installType().displayName(),
                provider,
                size,
                profileStatus(profile, applicationRoot),
                actions));
    }

    private void addFluxProfileRow(VBox box, SettingsFormModel form, SettingsApplicationServices services,
                                   Path applicationRoot, Label status) {
        FluxLicenseAcceptanceStore licenses = new FluxLicenseAcceptanceStore();
        VBox row = new VBox(10);
        row.getStyleClass().add("settings-image-resource-row");
        Label title = new Label("Alta calidad / Flux");
        title.getStyleClass().add("settings-engine-status-title");
        Label explanation = new Label("FLUX.1-dev es gated: inicia sesion en Hugging Face, acepta sus terminos y descarga manualmente los componentes. DocuPodcast no automatiza la autenticacion.");
        explanation.setWrapText(true);
        explanation.getStyleClass().add("settings-engine-status-message");

        VBox urls = new VBox(5,
                urlRow("Terminos y repositorio", FluxLicenseAcceptanceStore.LICENSE_URL),
                urlRow("Modelo", "https://huggingface.co/black-forest-labs/FLUX.1-dev/resolve/main/flux1-dev.safetensors"),
                urlRow("VAE", "https://huggingface.co/black-forest-labs/FLUX.1-dev/resolve/main/ae.safetensors"),
                urlRow("T5 oficial", "https://huggingface.co/black-forest-labs/FLUX.1-dev/tree/main/text_encoder_2"),
                urlRow("CLIP-L oficial", "https://huggingface.co/black-forest-labs/FLUX.1-dev/tree/main/text_encoder"),
                urlRow("Encoders ComfyUI", "https://huggingface.co/comfyanonymous/flux_text_encoders/tree/main"),
                urlRow("Guia ComfyUI", "https://docs.comfy.org/tutorials/flux/flux-1-text-to-image"));

        CheckBox accepted = new CheckBox("Confirmo que inicie sesion y acepte la licencia FLUX.1-dev");
        accepted.setSelected(licenses.accepted(applicationRoot));
        accepted.selectedProperty().addListener((obs, oldValue, selected) -> {
            try {
                if (selected) {
                    licenses.accept(applicationRoot);
                    status.setText("Aceptacion local FLUX registrada. Ya puedes importar los componentes descargados.");
                } else {
                    licenses.revoke(applicationRoot);
                    status.setText("Aceptacion local FLUX eliminada.");
                }
            } catch (IOException ex) {
                accepted.setSelected(oldValue);
                status.setText("No se pudo guardar la aceptacion FLUX: " + ex.getMessage());
            }
        });

        Button openTerms = ActionButtonFactory.secondary("Abrir terminos", () -> openUrl(FluxLicenseAcceptanceStore.LICENSE_URL));
        Button copyModel = ActionButtonFactory.secondary("Copiar URL modelo", () -> copyText(ImageModelPackageProfile.HIGH_QUALITY_FLUX.downloadUrl()));
        Button importFile = ActionButtonFactory.secondary("Importar archivo", () -> {
            selectProfile(form, ImageModelPackageProfile.HIGH_QUALITY_FLUX);
            operations.importFluxFile(services, applicationRoot, status);
        });
        Button importFolder = ActionButtonFactory.secondary("Importar carpeta", () -> {
            selectProfile(form, ImageModelPackageProfile.HIGH_QUALITY_FLUX);
            operations.importFluxFolder(services, applicationRoot, status);
        });
        Button verify = ActionButtonFactory.secondary("Verificar", () -> {
            selectProfile(form, ImageModelPackageProfile.HIGH_QUALITY_FLUX);
            operations.verify(form, services, applicationRoot, status);
        });
        disableIfNoServices(services, verify);
        Label componentState = new Label(fluxStatus(applicationRoot));
        componentState.setWrapText(true);
        componentState.getStyleClass().add("settings-engine-status-action");
        row.getChildren().addAll(title, explanation, urls, accepted, componentState,
                actionRow(openTerms, copyModel, importFile, importFolder, verify));
        box.getChildren().add(row);
    }

    private static HBox urlRow(String labelText, String url) {
        Label label = new Label(labelText);
        label.setMinWidth(150);
        label.getStyleClass().add("settings-key");
        TextField field = new TextField(url);
        field.setEditable(false);
        field.setFocusTraversable(true);
        HBox.setHgrow(field, Priority.ALWAYS);
        Button copy = ActionButtonFactory.secondary("Copiar", () -> copyText(url));
        Button open = ActionButtonFactory.secondary("Abrir", () -> openUrl(url));
        return new HBox(6, label, field, copy, open);
    }

    private static void copyText(String text) {
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(text == null ? "" : text);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
    }

    private static void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            copyText(url);
        }
    }

    private static String fluxStatus(Path applicationRoot) {
        FluxModelBundle bundle = FluxModelBundle.inspect(applicationRoot);
        boolean accepted = new FluxLicenseAcceptanceStore().accepted(applicationRoot);
        String components = bundle.ready() ? "Modelo, VAE, CLIP-L y T5 listos."
                : "Faltan: " + String.join(", ", bundle.missingComponents()) + ".";
        return (accepted ? "Licencia confirmada. " : "Licencia no confirmada. ") + components;
    }

    private void addEnhancementComponentRows(VBox box, SettingsFormModel form,
                                            SettingsApplicationServices services, Path applicationRoot, Label status) {
        addConditioningComponentRow(box, "Identidad SD 1.5 / IP-Adapter Plus",
                "https://github.com/cubiq/ComfyUI_IPAdapter_plus\n"
                        + "Custom node: tools/image/ComfyUI/custom_nodes/ComfyUI_IPAdapter_plus\n"
                        + "CLIP Vision: models/image/clip_vision\n"
                        + "Workflow API: models/image/workflows/workflow-sd15-ipadapter-reference-api.json",
                conditioningStatus(applicationRoot, "ipadapter"), form, services, applicationRoot, status);
        addConditioningComponentRow(box, "Identidad Flux / PuLID-FLUX",
                "https://github.com/ToTheBeginning/PuLID\n"
                        + "Custom node: tools/image/ComfyUI/custom_nodes/ComfyUI-PuLID-Flux\n"
                        + "Workflow API: models/image/workflows/workflow-flux-pulid-reference-api.json",
                conditioningStatus(applicationRoot, "pulid"), form, services, applicationRoot, status);
        addConditioningComponentRow(box, "Boceto estructural / ControlNet",
                "https://github.com/Fannovel16/comfyui_controlnet_aux\n"
                        + "El boceto guia composicion con scribble/canny; no define estilo.\n"
                        + "Modelos: models/image/controlnet",
                conditioningStatus(applicationRoot, "controlnet"), form, services, applicationRoot, status);
        addComponentRow(box, "ControlNet Tile",
                "https://github.com/lllyasviel/ControlNet-v1-1-nightly\nmodels/image/controlnet",
                "Segun modelo", "models/image/controlnet", form, services, applicationRoot, status);
        addComponentRow(box, "Real-ESRGAN",
                "https://github.com/xinntao/Real-ESRGAN\nmodels/image/upscalers/realesrgan",
                "Segun modelo", "models/image/upscalers/realesrgan", form, services, applicationRoot, status);
        addComponentRow(box, "SwinIR",
                "https://github.com/JingyunLiang/SwinIR\nmodels/image/upscalers/swinir",
                "Segun modelo", "models/image/upscalers/swinir", form, services, applicationRoot, status);
        addComponentRow(box, "SUPIR",
                "https://github.com/Fanghua-Yu/SUPIR\nmodels/image/upscalers/supir",
                "Segun modelo", "models/image/upscalers/supir", form, services, applicationRoot, status);
        addComponentRow(box, "Workflows profesionales",
                "models/image/workflows\nFlux, ControlNet Tile, outpainting y tiled upscale.",
                "Segun workflow", "models/image/workflows", form, services, applicationRoot, status);
        addComponentRow(box, "LoRA de consistencia",
                "models/image/loras\nImporta LoRA por personaje, vestuario o estilo.",
                "Segun LoRA", "models/image/loras", form, services, applicationRoot, status);
    }

    private void addConditioningComponentRow(VBox box, String name, String provider, String state,
                                             SettingsFormModel form, SettingsApplicationServices services,
                                             Path applicationRoot, Label status) {
        Button importPackage = ActionButtonFactory.secondary("Importar componente local",
                () -> operations.importPackageFolder(form, services, applicationRoot, status));
        Button verify = ActionButtonFactory.secondary("Verificar",
                () -> operations.verify(form, services, applicationRoot, status));
        disableIfNoServices(services, importPackage, verify);
        box.getChildren().add(resourceRow(name, provider, "Segun paquete", state,
                actionRow(importPackage, verify)));
    }

    private void addComponentRow(VBox box, String name, String provider, String size,
                                String relativeFolder, SettingsFormModel form, SettingsApplicationServices services,
                                Path applicationRoot, Label status) {
        Button importPackage = ActionButtonFactory.secondary("Importar componente local",
                () -> operations.importPackageFolder(form, services, applicationRoot, status));
        Button verify = ActionButtonFactory.secondary("Verificar",
                () -> operations.verify(form, services, applicationRoot, status));
        disableIfNoServices(services, importPackage, verify);
        box.getChildren().add(resourceRow(name, provider, size, componentStatus(applicationRoot, relativeFolder),
                actionRow(importPackage, verify)));
    }

    private void addReferenceRow(VBox box, SettingsFormModel form, SettingsApplicationServices services,
                                 Path applicationRoot, Label status) {
        Button importPackage = ActionButtonFactory.secondary("Importar workflow/adaptadores",
                () -> operations.importPackageFolder(form, services, applicationRoot, status));
        Button verify = ActionButtonFactory.secondary("Verificar",
                () -> operations.verify(form, services, applicationRoot, status));
        disableIfNoServices(services, importPackage, verify);
        box.getChildren().add(resourceRow("Adaptadores, workflow y LoRA avanzada",
                "models/image/adapters\nmodels/image/workflows\nmodels/image/loras",
                "Segun paquete",
                referenceStatus(applicationRoot),
                actionRow(importPackage, verify)));
    }

    private static VBox resourceRow(String component, String provider, String size, String state, Node actions) {
        VBox row = new VBox(8);
        row.getStyleClass().add("settings-image-resource-row");
        GridPane meta = new GridPane();
        meta.getStyleClass().add("settings-image-resource-meta");
        meta.setHgap(12);
        meta.setVgap(6);
        VBox componentCell = metaCell("Componente", component);
        VBox providerCell = metaCell("Proveedor", provider);
        VBox sizeCell = metaCell("Tamano", size);
        VBox stateCell = metaCell("Estado", state);
        meta.add(componentCell, 0, 0);
        meta.add(providerCell, 1, 0);
        meta.add(sizeCell, 2, 0);
        meta.add(stateCell, 3, 0);
        GridPane.setHgrow(providerCell, Priority.ALWAYS);
        row.getChildren().addAll(meta, actions);
        return row;
    }

    private static VBox metaCell(String key, String value) {
        Label label = new Label(key);
        label.getStyleClass().add("settings-image-resource-key");
        Label text = new Label(value == null ? "" : value);
        text.setWrapText(true);
        text.getStyleClass().add("settings-image-resource-value");
        VBox cell = new VBox(2, label, text);
        cell.getStyleClass().add("settings-image-resource-cell");
        return cell;
    }

    private static HBox actionRow(Button... buttons) {
        HBox actions = new HBox(6, buttons);
        actions.getStyleClass().add("settings-image-resource-actions");
        return actions;
    }

    private static void disableIfNoServices(SettingsApplicationServices services, Button... buttons) {
        if (services != null) {
            return;
        }
        for (Button button : buttons) {
            if (button != null) {
                button.setDisable(true);
            }
        }
    }

    private static void addHeader(GridPane grid, int column, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-key");
        grid.add(label, column, 0);
    }

    private static void addText(GridPane grid, int row, int column, String value) {
        Label label = summaryText();
        label.setText(value == null ? "" : value);
        label.setMaxWidth(column == 1 ? 520 : 220);
        grid.add(label, column, row);
        GridPane.setHgrow(label, column == 1 ? Priority.ALWAYS : Priority.NEVER);
    }

    private static void selectProfile(SettingsFormModel form, ImageModelPackageProfile profile) {
        form.imagePreset.setValue(profile.presetId());
        if (!profile.checkpointName().isBlank()) {
            form.imageModelName.setText(profile.checkpointName());
        }
        form.imageMemoryProfile.setValue(profile.highEnd()
                ? ImageGenerationMemoryProfile.VRAM_RAM_OFFLOAD.name()
                : ImageGenerationMemoryProfile.SAFE_LOW_VRAM.name());
        form.imageLowVram.setSelected(ImageGenerationMemoryProfile.from(form.imageMemoryProfile.getValue(), false).legacyLowVram());
    }

    private static String runtimeStatus(Path applicationRoot) {
        Path runtime = root(applicationRoot).resolve("tools/image");
        if (!Files.isDirectory(runtime)) {
            return "pendiente";
        }
        if (hasLauncher(runtime)) {
            return "instalado";
        }
        return "carpetas preparadas; falta lanzador";
    }

    private static String profileStatus(ImageModelPackageProfile profile, Path applicationRoot) {
        if (profile == ImageModelPackageProfile.HIGH_QUALITY_FLUX) {
            String missing = missingFluxComponents(applicationRoot);
            return missing.isBlank() ? "componentes Flux importados" : "faltan: " + missing;
        }
        if (profile.accessPolicy() == com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageModelPackageAccessPolicy.GATED_OR_TOKEN
                && !profileInstalled(profile, applicationRoot)) {
            return "requiere acceso o importacion";
        }
        if (profile.downloadUrl().isBlank()) {
            return "manual";
        }
        return profileInstalled(profile, applicationRoot) ? "descargado" : "pendiente";
    }

    private static boolean profileInstalled(ImageModelPackageProfile profile, Path applicationRoot) {
        if (profile.checkpointName().isBlank()) {
            return false;
        }
        if (profile == ImageModelPackageProfile.HIGH_QUALITY_FLUX) {
            return missingFluxComponents(applicationRoot).isBlank();
        }
        return Files.isRegularFile(root(applicationRoot).resolve("models/image").resolve(profile.checkpointName()));
    }

    private static String missingFluxComponents(Path applicationRoot) {
        ArrayList<String> missing = new ArrayList<>(FluxModelBundle.inspect(applicationRoot).missingComponents());
        if (!new FluxLicenseAcceptanceStore().accepted(applicationRoot)) {
            missing.add("confirmacion de licencia");
        }
        return String.join(", ", missing);
    }

    private static String referenceStatus(Path applicationRoot) {
        ImageEngineArtifactInspectionReport report = new InspectLocalTheatreImageEngineArtifactsUseCase()
                .inspect(OperationalSettings.defaults(), root(applicationRoot));
        String workflow = switch (report.workflowStatus()) {
            case READY -> "workflow real";
            case PLACEHOLDER -> "workflow placeholder";
            case INVALID -> "workflow invalido";
            default -> "workflow pendiente";
        };
        String consistency = report.advancedConsistencyReady()
                ? "consistencia avanzada lista"
                : "consistencia avanzada pendiente";
        return workflow + "; " + consistency;
    }

    private static String componentStatus(Path applicationRoot, String relativeFolder) {
        Path folder = root(applicationRoot).resolve(relativeFolder);
        if (!Files.isDirectory(folder)) {
            return "pendiente";
        }
        return hasNonPlaceholderArtifact(folder) ? "instalado" : "placeholder o vacio";
    }

    private static String conditioningStatus(Path applicationRoot, String kind) {
        Path base = root(applicationRoot);
        Path customNodes = base.resolve("tools/image/ComfyUI/custom_nodes");
        boolean node = containsPathToken(customNodes, kind);
        if ("ipadapter".equals(kind)) {
            boolean vision = containsPathToken(base.resolve("models/image/clip_vision"), "clip");
            boolean workflow = Files.isRegularFile(base.resolve(
                    "models/image/workflows/workflow-sd15-ipadapter-reference-api.json"));
            return readiness(node, vision, workflow, "nodo IP-Adapter", "CLIP Vision", "workflow API");
        }
        if ("pulid".equals(kind)) {
            boolean workflow = Files.isRegularFile(base.resolve(
                    "models/image/workflows/workflow-flux-pulid-reference-api.json"));
            return readiness(node, true, workflow, "nodo PuLID", "modelos", "workflow API");
        }
        boolean model = containsPathToken(base.resolve("models/image/controlnet"), "");
        return readiness(node, model, true, "nodo ControlNet", "modelo ControlNet", "workflow");
    }

    private static String readiness(boolean first, boolean second, boolean third,
                                    String firstName, String secondName, String thirdName) {
        ArrayList<String> missing = new ArrayList<>();
        if (!first) missing.add(firstName);
        if (!second) missing.add(secondName);
        if (!third) missing.add(thirdName);
        return missing.isEmpty() ? "listo" : "faltan: " + String.join(", ", missing);
    }

    private static boolean containsPathToken(Path root, String token) {
        if (!Files.isDirectory(root)) return false;
        String expected = token == null ? "" : token.toLowerCase(Locale.ROOT);
        if (expected.isBlank()) return hasNonPlaceholderArtifact(root);
        try (Stream<Path> paths = Files.walk(root, 4)) {
            return paths.anyMatch(path -> {
                String value = path.toString().toLowerCase(Locale.ROOT);
                if (!value.contains(expected)) return false;
                return Files.isDirectory(path) || (Files.isRegularFile(path) && !value.contains("placeholder"));
            });
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean hasNonPlaceholderArtifact(Path folder) {
        try (Stream<Path> paths = Files.walk(folder, 4)) {
            return paths.filter(Files::isRegularFile).anyMatch(path -> {
                String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.contains("placeholder") || name.equals("readme.md") || name.equals("readme.txt")) {
                    return false;
                }
                try {
                    return Files.size(path) > 1024L;
                } catch (IOException ex) {
                    return false;
                }
            });
        } catch (IOException ex) {
            return false;
        }
    }

    private static Path root(Path applicationRoot) {
        return applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
    }

    private static boolean hasLauncher(Path runtime) {
        return Files.isRegularFile(runtime.resolve("start-image-engine.bat"))
                || Files.isRegularFile(runtime.resolve("start.bat"))
                || Files.isRegularFile(runtime.resolve("run.bat"))
                || Files.isRegularFile(runtime.resolve("ComfyUI.bat"))
                || (Files.isRegularFile(runtime.resolve("main.py")) && hasLocalPython(runtime))
                || (Files.isRegularFile(runtime.resolve("ComfyUI/main.py")) && hasLocalPython(runtime));
    }

    private static boolean hasLocalPython(Path runtime) {
        return Files.isRegularFile(runtime.resolve("python_embeded/python.exe"))
                || Files.isRegularFile(runtime.resolve("python_embedded/python.exe"))
                || Files.isRegularFile(runtime.resolve("venv/Scripts/python.exe"))
                || Files.isRegularFile(runtime.resolve(".venv/Scripts/python.exe"));
    }

    private static String humanBytes(long bytes) {
        if (bytes <= 0) {
            return "Segun paquete";
        }
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        if (gb >= 1.0) {
            return String.format(Locale.ROOT, "%.2f GB", gb);
        }
        double mb = bytes / 1024.0 / 1024.0;
        return String.format(Locale.ROOT, "%.0f MB", mb);
    }

    private TitledPane advancedDiagnostics(SettingsFormModel form) {
        VBox box = new VBox(8);
        box.getStyleClass().add("settings-engine-status-list");
        box.getChildren().addAll(
                SettingsDialog.downloadUrlControl("Endpoint local", form.imageBaseUrl,
                        "Diagnostico avanzado. En uso normal la app inicia o detecta el motor local automaticamente."),
                SettingsDialog.downloadUrlControl("Checkpoint", form.imageModelName,
                        "Nombre del modelo dentro de models/image/checkpoints."),
                SettingsDialog.downloadUrlControl("Adaptadores y LoRA", form.imageAdaptersDirectory,
                        "Carpeta local para referencias de cara, vestuario y consistencia. LoRA vive en models/image/loras como opcion avanzada."),
                SettingsDialog.downloadUrlControl("Timeout", form.imageTimeoutSeconds,
                        "Tiempo maximo para pruebas y generaciones locales."),
                SettingsDialog.downloadUrlControl("Intentos por imagen", form.imageMaxAttempts,
                        "Intentos totales por candidato o frame cuando el motor local falla temporalmente."));
        return new TitledPane("Avanzado / diagnostico", box);
    }
}
