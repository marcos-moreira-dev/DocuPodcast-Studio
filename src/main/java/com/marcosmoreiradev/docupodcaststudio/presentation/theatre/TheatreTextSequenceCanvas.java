package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.beans.property.StringProperty;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Shared zigzag canvas for theatre text segments Intervencion 1, Intervencion 2 and following. */
final class TheatreTextSequenceCanvas extends Canvas {
    private final String title;
    private final String emptyMessage;
    private final List<IntervencionCatalogo.IntervencionInfo> aliases;
    private final StringProperty selectedIntervencion;
    private final Consumer<String> blockSelection;
    private final Consumer<IntervencionCatalogo.IntervencionInfo> editorIntervencion;
    private final Consumer<IntervencionCatalogo.IntervencionInfo> processIntervencion;
    private final Consumer<IntervencionCatalogo.IntervencionInfo> recordIntervencion;
    private final Consumer<IntervencionCatalogo.IntervencionInfo> contextExportIntervencion;
    private final List<TheatreZigzagLayout.HitBox> hitBoxes = new ArrayList<>();
    private final Tooltip hitTooltip = new Tooltip();

    TheatreTextSequenceCanvas(
            String title,
            String emptyMessage,
            List<IntervencionCatalogo.IntervencionInfo> aliases,
            StringProperty selectedIntervencion,
            Consumer<String> blockSelection,
            Consumer<IntervencionCatalogo.IntervencionInfo> editorIntervencion) {
        this(title, emptyMessage, aliases, selectedIntervencion, blockSelection, editorIntervencion, null, null, null);
    }

    TheatreTextSequenceCanvas(
            String title,
            String emptyMessage,
            List<IntervencionCatalogo.IntervencionInfo> aliases,
            StringProperty selectedIntervencion,
            Consumer<String> blockSelection,
            Consumer<IntervencionCatalogo.IntervencionInfo> editorIntervencion,
            Consumer<IntervencionCatalogo.IntervencionInfo> contextExportIntervencion) {
        this(title, emptyMessage, aliases, selectedIntervencion, blockSelection, editorIntervencion, null,
                null, contextExportIntervencion);
    }

    TheatreTextSequenceCanvas(
            String title,
            String emptyMessage,
            List<IntervencionCatalogo.IntervencionInfo> aliases,
            StringProperty selectedIntervencion,
            Consumer<String> blockSelection,
            Consumer<IntervencionCatalogo.IntervencionInfo> editorIntervencion,
            Consumer<IntervencionCatalogo.IntervencionInfo> processIntervencion,
            Consumer<IntervencionCatalogo.IntervencionInfo> contextExportIntervencion) {
        this(title, emptyMessage, aliases, selectedIntervencion, blockSelection, editorIntervencion,
                processIntervencion, null, contextExportIntervencion);
    }

    TheatreTextSequenceCanvas(
            String title,
            String emptyMessage,
            List<IntervencionCatalogo.IntervencionInfo> aliases,
            StringProperty selectedIntervencion,
            Consumer<String> blockSelection,
            Consumer<IntervencionCatalogo.IntervencionInfo> editorIntervencion,
            Consumer<IntervencionCatalogo.IntervencionInfo> processIntervencion,
            Consumer<IntervencionCatalogo.IntervencionInfo> recordIntervencion,
            Consumer<IntervencionCatalogo.IntervencionInfo> contextExportIntervencion) {
        super(TheatreZigzagLayout.WIDTH,
                TheatreZigzagLayout.heightFor(aliases == null ? 0 : aliases.size()));
        this.title = title == null ? "" : title;
        this.emptyMessage = emptyMessage == null || emptyMessage.isBlank()
                ? "Prepara la lectura para dibujar Intervencion 1, Intervencion 2 y siguientes."
                : emptyMessage;
        this.aliases = aliases == null ? List.of() : List.copyOf(aliases);
        this.selectedIntervencion = selectedIntervencion;
        this.blockSelection = blockSelection == null ? ignored -> { } : blockSelection;
        this.editorIntervencion = editorIntervencion;
        this.processIntervencion = processIntervencion;
        this.recordIntervencion = recordIntervencion;
        this.contextExportIntervencion = contextExportIntervencion;
        getStyleClass().add("theatre-action-canvas");
        hitTooltip.setShowDelay(Duration.millis(180));
        hitTooltip.setWrapText(true);
        hitTooltip.setMaxWidth(340);
        Tooltip.install(this, hitTooltip);
        setOnMouseClicked(this::selectAliasAt);
        setOnMouseMoved(this::updatePointerHint);
        setOnMouseExited(event -> {
            setCursor(Cursor.DEFAULT);
            hitTooltip.setText("");
        });
        if (selectedIntervencion != null) {
            selectedIntervencion.addListener((obs, oldValue, newValue) -> draw());
        }
        draw();
    }

    private void selectAliasAt(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            for (TheatreZigzagLayout.HitBox hit : hitBoxes) {
                if (hit.contains(event.getX(), event.getY())) {
                    aliasFor(hit).ifPresent(alias -> showContextMenu(alias, hit, event));
                    event.consume();
                    return;
                }
            }
        }
        for (TheatreZigzagLayout.HitBox hit : hitBoxes) {
            if (contextExportIntervencion != null && hit.containsDownload(event.getX(), event.getY())) {
                selectHit(hit);
                aliasFor(hit).ifPresent(contextExportIntervencion);
                event.consume();
                return;
            }
            if (hit.containsEdit(event.getX(), event.getY())) {
                selectHit(hit);
                aliasFor(hit).ifPresent(this::runPrimaryAction);
                event.consume();
                return;
            }
            if (hit.contains(event.getX(), event.getY())) {
                selectHit(hit);
                event.consume();
                return;
            }
        }
        TheatreCanvasSelectionSupport.clearOnEmptyPrimaryClick(event, selectedIntervencion, blockSelection);
    }

    private void updatePointerHint(MouseEvent event) {
        String hint = tooltipAt(event.getX(), event.getY());
        hitTooltip.setText(hint);
        setCursor(hint.isBlank() ? Cursor.DEFAULT : Cursor.HAND);
    }

    private String tooltipAt(double x, double y) {
        for (TheatreZigzagLayout.HitBox hit : hitBoxes) {
            String label = aliasLabel(hit);
            if (contextExportIntervencion != null && hit.containsDownload(x, y)) {
                return "Descargar paquete IA de contexto para " + label + ".";
            }
            if (hit.containsEdit(x, y)) {
                return primaryActionTooltip(label);
            }
        }
        return "";
    }

    private String primaryActionTooltip(String label) {
        if (editorIntervencion != null) {
            return "Editar la configuracion de " + label + ".";
        }
        if (processIntervencion != null) {
            return "Procesar " + label + " con el flujo de generacion IA.";
        }
        return "Seleccionar " + label + ".";
    }

    private String aliasLabel(TheatreZigzagLayout.HitBox hit) {
        Optional<IntervencionCatalogo.IntervencionInfo> alias = aliasFor(hit);
        return alias.map(IntervencionCatalogo.IntervencionInfo::displayName)
                .filter(text -> !text.isBlank())
                .orElseGet(() -> TheatreZigzagLayout.displayLabel(hit.alias()));
    }

    private void runPrimaryAction(IntervencionCatalogo.IntervencionInfo alias) {
        if (editorIntervencion != null) {
            editorIntervencion.accept(alias);
        } else if (processIntervencion != null) {
            processIntervencion.accept(alias);
        }
    }

    private void showContextMenu(IntervencionCatalogo.IntervencionInfo alias,
                                 TheatreZigzagLayout.HitBox hit,
                                 MouseEvent event) {
        MenuItem process = new MenuItem("Procesar intervención");
        process.setDisable(processIntervencion == null);
        process.setOnAction(ignored -> {
            selectHit(hit);
            if (processIntervencion != null) {
                processIntervencion.accept(alias);
            }
        });

        MenuItem export = new MenuItem("Exportar paquete IA");
        export.setDisable(contextExportIntervencion == null);
        export.setOnAction(ignored -> {
            selectHit(hit);
            if (contextExportIntervencion != null) {
                contextExportIntervencion.accept(alias);
            }
        });

        MenuItem record = new MenuItem("Grabar audio narraci\u00f3n/efecto sonido");
        record.setDisable(recordIntervencion == null);
        record.setOnAction(ignored -> {
            selectHit(hit);
            if (recordIntervencion != null) {
                recordIntervencion.accept(alias);
            }
        });

        MenuItem viewContext = new MenuItem("Ver contexto");
        viewContext.setOnAction(ignored -> selectHit(hit));

        ContextMenu menu = new ContextMenu(process, record, export, viewContext);
        menu.show(this, event.getScreenX(), event.getScreenY());
    }

    private void selectHit(TheatreZigzagLayout.HitBox hit) {
        if (selectedIntervencion != null) {
            selectedIntervencion.set(hit.alias());
        }
        if (!hit.blockId().isBlank()) {
            blockSelection.accept(hit.blockId());
        }
    }

    private Optional<IntervencionCatalogo.IntervencionInfo> aliasFor(TheatreZigzagLayout.HitBox hit) {
        return aliases.stream()
                .filter(alias -> alias.alias().equals(hit.alias()))
                .findFirst();
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        hitBoxes.clear();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#F8FAFE"));
        gc.fillRoundRect(0, 0, width, height, 10, 10);
        gc.setStroke(Color.web("#D8E0EA"));
        gc.strokeRoundRect(0.5, 0.5, width - 1, height - 1, 10, 10);
        gc.setFill(Color.web("#334155"));
        gc.setFont(javafx.scene.text.Font.font("System", 11));
        gc.fillText(title, 14, 18);
        if (aliases.isEmpty()) {
            gc.setFill(Color.web("#64748B"));
            gc.fillText(emptyMessage, 14, 76);
            return;
        }
        List<TheatreZigzagLayout.StagePoint> points = TheatreZigzagLayout.pointsFor(aliases.size(), width);
        for (int i = 0; i < points.size() - 1; i++) {
            TheatreZigzagLayout.drawArrow(gc, points.get(i), points.get(i + 1));
        }
        for (int i = 0; i < points.size(); i++) {
            IntervencionCatalogo.IntervencionInfo alias = aliases.get(i);
            TheatreZigzagLayout.StagePoint point = points.get(i);
            boolean selected = selectedIntervencion != null && alias.alias().equals(selectedIntervencion.get());
            gc.setFill(Color.web(selected ? "#DCFCE7" : "#FFFFFF"));
            gc.setStroke(Color.web(selected ? "#16A34A" : "#8FB2EA"));
            gc.setLineWidth(selected ? 2.5 : 1.5);
            gc.fillRoundRect(point.x(), point.y(), point.width(), point.height(), 8, 8);
            gc.strokeRoundRect(point.x(), point.y(), point.width(), point.height(), 8, 8);
            gc.setFill(Color.web(selected ? "#166534" : "#1E3A8A"));
            gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
            gc.fillText(TheatreZigzagLayout.displayLabel(alias.alias()), point.x() + 12, point.y() + 21);
            String cue = TheatreZigzagLayout.speakerLabel(alias.preview());
            if (!cue.isBlank()) {
                gc.setFill(Color.web(selected ? "#15803D" : "#475569"));
                gc.setFont(javafx.scene.text.Font.font("System", 9.5));
                gc.fillText(TheatreZigzagLayout.ellipsize(cue, 15), point.x() + 12, point.y() + 42);
            }
            double editX = point.x() + point.width() - 22;
            double editY = point.y() + 6;
            double downloadX = editX;
            double downloadY = editY + 23;
            TheatreZigzagLayout.drawEditActionButton(gc, editX, editY);
            if (contextExportIntervencion != null) {
                TheatreZigzagLayout.drawDownloadActionButton(gc, downloadX, downloadY);
            }
            hitBoxes.add(new TheatreZigzagLayout.HitBox(alias.alias(), alias.blockId(), point.x(), point.y(), point.width(), point.height(),
                    editX, editY, TheatreZigzagLayout.ACTION_BUTTON_SIZE, TheatreZigzagLayout.ACTION_BUTTON_SIZE,
                    contextExportIntervencion == null ? -100 : downloadX,
                    contextExportIntervencion == null ? -100 : downloadY,
                    contextExportIntervencion == null ? 0 : TheatreZigzagLayout.ACTION_BUTTON_SIZE,
                    contextExportIntervencion == null ? 0 : TheatreZigzagLayout.ACTION_BUTTON_SIZE));
        }
    }
}
