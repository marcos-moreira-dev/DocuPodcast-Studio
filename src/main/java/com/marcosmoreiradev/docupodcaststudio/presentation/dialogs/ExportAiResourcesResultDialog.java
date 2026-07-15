package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceExportResult;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import javafx.stage.Window;

/** Success dialog for AI resources export. */
public final class ExportAiResourcesResultDialog {
    private final ExceptionAlertPresenter presenter = new ExceptionAlertPresenter();

    public void show(Window owner, AiResourceExportResult result) {
        presenter.show(UserNotification.success(
                "Recursos IA exportados correctamente",
                "Archivos exportados: " + result.exportedFiles()
                        + System.lineSeparator() + "Indice: " + result.indexPath()
        ), owner);
    }
}
