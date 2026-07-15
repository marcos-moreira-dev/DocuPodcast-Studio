package com.marcosmoreiradev.docupodcaststudio.presentation.toolbar;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.WorkspaceCapabilityCommandMapper;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceCapability;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceDescriptorCatalog;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceSurfacePolicy;

import java.util.List;
import java.util.Objects;

/** Provides contextual toolbar actions from the official workspace capability catalog. */
public final class WorkspaceToolbarActionProvider {
    private final WorkspaceDescriptorCatalog catalog;
    private final WorkspaceCapabilityCommandMapper commandMapper = new WorkspaceCapabilityCommandMapper();
    private final WorkspaceSurfacePolicy surfacePolicy = new WorkspaceSurfacePolicy();

    public WorkspaceToolbarActionProvider(WorkspaceDescriptorCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public static WorkspaceToolbarActionProvider official() {
        return new WorkspaceToolbarActionProvider(WorkspaceDescriptorCatalog.official());
    }

    public List<WorkspaceToolbarAction> actionsFor(WorkspaceKind requested) {
        WorkspaceKind kind = surfacePolicy.restoreStartupWorkspace(catalog.descriptor(requested).kind());
        return switch (kind) {
            case DOCUMENT_READER -> documentActions();
            case VOICE_LIBRARY -> voiceActions();
            case WELCOME_HOME -> welcomeActions();
            default -> welcomeActions();
        };
    }

    private List<WorkspaceToolbarAction> welcomeActions() {
        return List.of(
                action("Inicio", "Abrir fuente", WorkspaceCapability.IMPORT_WORD, true),
                action("Inicio", "Guía", WorkspaceCapability.OPEN_GUIDE, false));
    }

    private List<WorkspaceToolbarAction> documentActions() {
        return List.of(
                action("Lectura", "Escuchar documento", WorkspaceCapability.LISTEN_DOCUMENT, true),
                action("Lectura", "Preparar lectura", WorkspaceCapability.CREATE_SCRIPT, false),
                action("Voces", "Voces", WorkspaceCapability.OPEN_VOICE_LIBRARY, false));
    }

    private List<WorkspaceToolbarAction> voiceActions() {
        return List.of(
                action("Voces", "Importar voz", WorkspaceCapability.IMPORT_VOICE_SAMPLE, true),
                action("Voces", "Grabar voz", WorkspaceCapability.PREPARE_HUMAN_VOICE, false),
                action("Voces", "Guía de voces", WorkspaceCapability.OPEN_GUIDE, false));
    }

    private WorkspaceToolbarAction action(String group, String label, WorkspaceCapability capability, boolean primary) {
        return new WorkspaceToolbarAction(group, label, capability, commandMapper.commandFor(capability), primary);
    }
}
