package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF2: presentation orchestration follows real use cases and keeps a recoverable Markdown log. */
final class PresentationOrchestrationRf2SourceTest {
    @Test
    void rf2BitacoraIsTheRecoverableContractForThisRefactor() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        assertTrue(bitacora.contains("Bitácora RF2"));
        assertTrue(bitacora.contains("Contrato operativo por vista"));
        assertTrue(bitacora.contains("Paso RF2-01"));
        assertTrue(bitacora.contains("Tests corridos"));
        assertTrue(bitacora.contains("Pendiente siguiente"));

        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF2"));
        assertTrue(registro.contains("PlaybackTransportCoordinator"));
    }

    @Test
    void mainViewsHaveOperationalContractsAndLegacyWorkspacesStayInternal() throws IOException {
        String descriptors = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceDescriptorCatalog.java");
        assertTrue(descriptors.contains("abrir fuente, proyecto, ejemplo o configuración inicial"));
        assertTrue(descriptors.contains("leer, seleccionar, escuchar, generar fragmentos de audio y asignar voz/audio sin herramientas teatrales"));
        assertTrue(descriptors.contains("imágenes por fragmento, personajes, mapa textual, mapa espacial y acciones"));
        assertTrue(descriptors.contains("gestionar voces, muestras, tonos/emociones y motor/dispositivo"));
        assertTrue(descriptors.contains("dependencias, settings, readiness, diagnóstico y herramientas locales"));

        String surfacePolicy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceSurfacePolicy.java");
        assertTrue(surfacePolicy.contains("PRODUCT_SURFACES"));
        assertTrue(surfacePolicy.contains("LEGACY_INTERNAL_SURFACES"));
        assertTrue(surfacePolicy.contains("WorkspaceKind.SCRIPT_EDITOR"));
        assertTrue(surfacePolicy.contains("WorkspaceKind.AUDIO_JOBS"));
        assertTrue(surfacePolicy.contains("WorkspaceKind.STORYBOARD"));

        String commands = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        assertTrue(commands.contains("Exportar audio"));
        assertTrue(commands.contains("Exportar video"));
        assertTrue(commands.contains("Ver estado de exportación"));
    }

    @Test
    void playbackTransportIsOutsideTheShellViewModel() throws IOException {
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java");
        assertTrue(coordinator.contains("SegmentAudioPlayer"));
        assertTrue(coordinator.contains("PlaybackContinuationController"));
        assertTrue(coordinator.contains("PlaybackSequentialQueueDriver"));
        assertTrue(coordinator.contains("PlaybackRuntimeQueue"));
        assertTrue(coordinator.contains("PlaybackCueDeadlineSequencer"));
        assertTrue(coordinator.contains("PlaybackDiagnosticRecorder"));

        String shell = shell();
        assertTrue(shell.contains("PlaybackTransportCoordinator"));
        assertFalse(shell.contains("segmentAudioPlayer().play("));
        assertFalse(shell.contains("segmentAudioPlayer().pause("));
        assertFalse(shell.contains("segmentAudioPlayer().resume("));
        assertFalse(shell.contains("segmentAudioPlayer().stop("));
        assertFalse(shell.contains("private final PlaybackContinuationController playbackContinuation"));
        assertFalse(shell.contains("private final PlaybackSequentialQueueDriver playbackSequentialQueue"));
        assertFalse(shell.contains("private final PlaybackRuntimeQueue playbackRuntimeQueue"));
    }

    @Test
    void shellViewModelStaysUnderRf2LineBudget() throws IOException {
        long lines = shell().lines().count();
        assertTrue(lines <= 2600, "DocuPodcastShellViewModel debe quedar en <= 2600 líneas; actual=" + lines);
    }

    private static String shell() throws IOException {
        return read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
