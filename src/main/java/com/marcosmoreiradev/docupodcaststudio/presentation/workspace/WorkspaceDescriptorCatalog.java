package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

/** Official workspace descriptor catalog for the current desktop product surface. */
public final class WorkspaceDescriptorCatalog {
    private final Map<WorkspaceKind, WorkspaceDescriptor> descriptors;

    private WorkspaceDescriptorCatalog(Map<WorkspaceKind, WorkspaceDescriptor> descriptors) {
        this.descriptors = Collections.unmodifiableMap(new EnumMap<>(descriptors));
    }

    public static WorkspaceDescriptorCatalog official() {
        EnumMap<WorkspaceKind, WorkspaceDescriptor> descriptors = new EnumMap<>(WorkspaceKind.class);
        put(descriptors, WorkspaceKind.WELCOME_HOME, "Inicio", "Inicio",
                "Portada funcional para abrir fuente, proyecto, ejemplo o configuración inicial.", true, true,
                WorkspaceCapability.OPEN_GUIDE);
        put(descriptors, WorkspaceKind.DOCUMENT_READER, "Documento", "Documento",
                "Raíz operativa para leer, seleccionar, escuchar, generar fragmentos de audio y asignar voz/audio sin herramientas teatrales.", true, true,
                WorkspaceCapability.LISTEN_DOCUMENT,
                WorkspaceCapability.CREATE_SCRIPT,
                WorkspaceCapability.OPEN_WORD_GUIDE);
        put(descriptors, WorkspaceKind.THEATRE_SCRIPT, "Guión teatral", "Guión",
                "Documento con capa teatral: imágenes por fragmento, personajes, mapa textual, mapa espacial y acciones.", true, true,
                WorkspaceCapability.LISTEN_DOCUMENT,
                WorkspaceCapability.CREATE_SCRIPT,
                WorkspaceCapability.OPEN_WORD_GUIDE);
        put(descriptors, WorkspaceKind.VOICE_LIBRARY, "Voces", "Voces",
                "Microaplicación administrativa para gestionar voces, muestras, tonos/emociones y motor/dispositivo.", true, true,
                WorkspaceCapability.IMPORT_VOICE_SAMPLE,
                WorkspaceCapability.PREPARE_AI_VOICE,
                WorkspaceCapability.PREPARE_HUMAN_VOICE,
                WorkspaceCapability.OPEN_GUIDE);
        put(descriptors, WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION, "Produccion visual narrativa", "Visual narrativa",
                "Superficie de Video narrativo para revisar imagen principal, puente y prompts por fragmento.", true, true,
                WorkspaceCapability.OPEN_GUIDE);
        put(descriptors, WorkspaceKind.THEATRE_IMAGE_GENERATION, "Gestionar frames de la obra", "Gestionar frames de la obra",
                "Microaplicacion teatral para preparar contexto visual, motor local y frames por intervencion.", true, true,
                WorkspaceCapability.OPEN_GUIDE);
        put(descriptors, WorkspaceKind.SCRIPT_EDITOR, "Preparación interna heredada", "Interno",
                "Superficie interna heredada: la preparación de lectura no es workspace de producto; Documento es la raíz del flujo.", false, false);
        put(descriptors, WorkspaceKind.AUDIO_JOBS, "Procesos internos heredados", "Interno",
                "Superficie interna heredada: los jobs de audio deben mostrarse como overlay/panel de progreso, no como workspace operativo.", false, false);
        put(descriptors, WorkspaceKind.STORYBOARD, "Secuencia visual interna heredada", "Interno",
                "Superficie interna heredada: la secuencia visual vive en Teatro > Guión, no como vista principal.", false, false);
        put(descriptors, WorkspaceKind.OBSERVABILITY, "Observabilidad", "Observabilidad",
                "Logs y métricas de generación; reservado para maduración posterior.", false, false);
        put(descriptors, WorkspaceKind.SETTINGS, "Configuración", "Configuración",
                "Modal dedicado a dependencias, settings, readiness, diagnóstico y herramientas locales; no es workspace.", false, false);
        return new WorkspaceDescriptorCatalog(descriptors);
    }

    public WorkspaceDescriptor descriptor(WorkspaceKind kind) {
        WorkspaceKind resolved = kind == null ? WorkspaceKind.WELCOME_HOME : kind;
        WorkspaceDescriptor descriptor = descriptors.get(resolved);
        if (descriptor == null) {
            return descriptors.get(WorkspaceKind.WELCOME_HOME);
        }
        return descriptor;
    }

    public Map<WorkspaceKind, WorkspaceDescriptor> descriptors() {
        return descriptors;
    }

    public boolean isKnown(WorkspaceKind kind) {
        return kind != null && descriptors.containsKey(kind);
    }

    private static void put(Map<WorkspaceKind, WorkspaceDescriptor> descriptors,
                            WorkspaceKind kind,
                            String title,
                            String navigationLabel,
                            String description,
                            boolean implemented,
                            boolean primaryNavigation,
                            WorkspaceCapability... capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        EnumSet<WorkspaceCapability> capabilitySet = capabilities.length == 0
                ? EnumSet.noneOf(WorkspaceCapability.class)
                : EnumSet.of(capabilities[0], capabilities);
        descriptors.put(kind, new WorkspaceDescriptor(kind, title, navigationLabel, description,
                implemented, primaryNavigation, capabilitySet));
    }
}
