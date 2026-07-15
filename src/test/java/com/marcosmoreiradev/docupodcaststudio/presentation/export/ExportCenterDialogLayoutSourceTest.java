package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportCenterDialogLayoutSourceTest {
    @Test
    void exportCenterKeepsLabelsVisibleAndDialogWidthBounded() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportCenterDialog.java"));
        String imports = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));
        String css = Files.readString(Path.of("src/main/resources/css/components/export-center.css"));

        assertTrue(source.contains("EXPORT_CENTER_DIALOG_PREF_WIDTH = 960.0"));
        assertTrue(source.contains("TARGET_LIST_MIN_WIDTH = 300.0"));
        assertTrue(source.contains("TARGET_LIST_MAX_WIDTH = 520.0"));
        assertTrue(source.contains("DETAIL_COLUMN_PREF_WIDTH = 620.0"));
        assertTrue(source.contains("ROW_LABEL_WIDTH = 88.0"));
        assertTrue(source.contains("export-center-dialog"));
        assertTrue(source.contains("export-center-target-list"));
        assertTrue(source.contains("targetCell(item)"));
        assertTrue(source.contains("setText(null)"));
        assertTrue(source.contains("setGraphic(empty || item == null ? null : targetCell(item))"));
        assertTrue(source.contains("rowLabel(\"Estado\")"));
        assertTrue(source.contains("renderDetailSections(detailTitle, evidenceTitle, evidence, limitationsTitle, limitations, selected.detail())"));
        assertTrue(source.contains("DetailSections sections = DetailSections.from(detail)"));
        assertTrue(source.contains("Consumer<Window> readinessAction"));
        assertTrue(source.contains("readinessNode.addEventFilter(ActionEvent.ACTION"));
        assertTrue(source.contains("event.consume()"));
        assertTrue(source.contains("readinessAction.accept(dialogWindow(dialog, owner), selectionFrom("));
        assertTrue(source.contains("private static Window dialogWindow(Dialog<?> dialog, Window fallback)"));
        assertTrue(source.contains("ExportCenterContext.defaults()"));
        assertTrue(source.contains("styleDialogButton(exportButton, \"ui-action-button-primary\")"));
        assertTrue(source.contains("styleDialogButton((Button) dialog.getDialogPane().lookupButton(cancel), \"ui-action-button-secondary\")"));
        assertTrue(source.contains("dialog.getDialogPane().setPrefWidth(EXPORT_CENTER_DIALOG_PREF_WIDTH)"));
        assertTrue(source.contains("dialog.getDialogPane().setMinWidth(EXPORT_CENTER_DIALOG_MIN_WIDTH)"));
        assertTrue(source.contains("adaptiveTargetListWidth(newValue.doubleValue())"));
        assertTrue(source.contains("Math.max(TARGET_LIST_MIN_WIDTH, contentWidth * 0.32)"));
        assertTrue(source.contains("list.setMaxHeight(Double.MAX_VALUE)"));
        assertTrue(source.contains("title.setWrapText(true)"));
        assertTrue(source.contains("status.setWrapText(true)"));
        assertTrue(source.contains("HBox.setHgrow(list, Priority.SOMETIMES)"));
        assertTrue(source.contains("label.setMinWidth(ROW_LABEL_WIDTH)"));
        assertTrue(source.contains("label.setPrefWidth(ROW_LABEL_WIDTH)"));
        assertTrue(source.contains("label.setMaxWidth(ROW_LABEL_WIDTH)"));
        assertTrue(imports.contains("components/export-center.css"));
        assertTrue(css.contains(".export-center-dialog"));
        assertTrue(css.contains(".export-center-target-list .list-cell:selected"));
        assertTrue(css.contains(".export-center-target-list .scroll-bar:horizontal"));
        assertTrue(css.contains("-docu-accent"));
        assertTrue(css.contains(".export-center-detail-pane"));
        assertTrue(css.contains(".export-center-note-icon"));
    }
}
