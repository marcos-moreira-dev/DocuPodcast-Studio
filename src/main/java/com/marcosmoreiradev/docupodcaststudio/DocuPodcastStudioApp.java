package com.marcosmoreiradev.docupodcaststudio;

import javafx.application.Application;
import javafx.stage.Stage;

/** Guarded compatibility entry point. The product starts exclusively in {@code studio-launcher}. */
public final class DocuPodcastStudioApp extends Application {
    @Override public void start(Stage stage) {
        throw new IllegalStateException(launchGuardMessage());
    }

    public static String launchGuardMessage() {
        return "Esta entrada no compone motores de voz. Inicia DocuPodcast Studio con "
                + "scripts\\01-ejecutar-app.bat o mediante studio-launcher.";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
