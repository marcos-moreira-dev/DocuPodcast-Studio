package com.marcosmoreiradev.docupodcaststudio.presentation.batch;

import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;

import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

/** Connects the persistent Express queue to the existing Documentary Studio production chain. */
public interface DocumentVideoBatchExecutionPort {
    void start(DocumentVideoBatchProject project, Path descriptor, Listener listener);

    void requestPause();

    void cancelCurrent();

    /** Stops the active operation and cancels every non-terminal item without deleting valid derivatives. */
    void cancelAll();

    boolean running();

    /** Engines available to this exact production runtime. */
    default EngineConfiguration engineConfiguration() {
        return EngineConfiguration.defaults();
    }

    /** Persists the voice engine as the shared DocuPodcast selection. */
    default void selectGlobalVoiceEngine(String engineId) throws IOException {
        // Optional for isolated presentation tests and embedders without settings persistence.
    }

    record EngineChoice(String id, String name, String status, boolean available) {
        public EngineChoice {
            id = id == null ? "" : id.strip();
            name = name == null || name.isBlank() ? id : name.strip();
            status = status == null ? "" : status.strip();
        }

        @Override public String toString() {
            return status.isBlank() ? name : name + " · " + status;
        }
    }

    record EngineConfiguration(List<EngineChoice> voiceEngines,
                               List<EngineChoice> aiEngines,
                               String selectedVoiceEngineId,
                               String selectedAiEngineId) {
        public EngineConfiguration {
            voiceEngines = voiceEngines == null ? List.of() : List.copyOf(voiceEngines);
            aiEngines = aiEngines == null ? List.of() : List.copyOf(aiEngines);
            selectedVoiceEngineId = selectedVoiceEngineId == null ? "" : selectedVoiceEngineId.strip();
            selectedAiEngineId = selectedAiEngineId == null ? "" : selectedAiEngineId.strip();
        }

        public static EngineConfiguration defaults() {
            return new EngineConfiguration(
                    List.of(new EngineChoice("", "Usar la configuración general", "", true)),
                    List.of(new EngineChoice("", "Automático (motor compatible)", "", true)), "", "");
        }
    }

    interface Listener {
        default javafx.stage.Window notificationOwner() { return null; }
        void projectChanged(DocumentVideoBatchProject project);

        void finished(DocumentVideoBatchProject project, BatchRunResult result);
    }

    record BatchRunResult(int completed, int failed, int skipped, int cancelled,
                          boolean paused, String message) { }
}
