package com.marcosmoreiradev.docupodcaststudio.presentation.dialogs;

import com.marcosmoreiradev.docupodcaststudio.presentation.notification.ExceptionAlertPresenter;
import com.marcosmoreiradev.docupodcaststudio.presentation.notification.UserNotification;
import javafx.stage.Window;

/** Warning shown when a project-changing operation is attempted while audio is running. */
public final class ActiveAudioJobDialog {
    private final ExceptionAlertPresenter presenter = new ExceptionAlertPresenter();

    public void show(Window owner) {
        presenter.show(UserNotification.warning(
                "Hay un job de audio en ejecución.",
                "Cancela o espera a que termine antes de cerrar o cambiar de proyecto."
        ), owner);
    }
}
