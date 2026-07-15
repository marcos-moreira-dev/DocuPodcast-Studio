package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Official command catalog used by menu bar, future ribbon, workspace and sidebars. */
public final class AppCommandRegistry {
    private final Map<AppCommandId, AppCommandDescriptor> commands;

    public AppCommandRegistry(List<AppCommandDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors");
        EnumMap<AppCommandId, AppCommandDescriptor> map = new EnumMap<>(AppCommandId.class);
        for (AppCommandDescriptor descriptor : descriptors) {
            AppCommandDescriptor previous = map.put(descriptor.id(), descriptor);
            if (previous != null) {
                throw new IllegalArgumentException("Comando duplicado: " + descriptor.id());
            }
        }
        commands = Collections.unmodifiableMap(map);
    }

    public static AppCommandRegistry official() {
        List<AppCommandDescriptor> descriptors = new ArrayList<>();
        descriptors.add(command(AppCommandId.SHOW_WELCOME, "Inicio", "Inicio", "Volver a la pantalla inicial.", AppCommandOwner.VIEW, AppCommandSurface.TOOLBAR, true, surfaces(AppCommandSurface.TOOLBAR, AppCommandSurface.MENU_BAR, AppCommandSurface.WELCOME, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.OPEN_DOCUMENT_READER, "Documento / lector", "Documento", "Volver al lector principal del documento.", AppCommandOwner.VIEW, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.OPEN_THEATRE_SCRIPT, "Mostrar módulos estructurales", "Mostrar módulos estructurales", "Abrir la gestion manual de estructura teatral.", AppCommandOwner.VIEW, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.OPEN_THEATRE_IMAGE_GENERATION, "Gestionar frames de la obra", "Gestionar frames de la obra", "Abrir gestion de frames teatrales con motor local de imagen IA.", AppCommandOwner.VIEW, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.OPEN_NARRATIVE_VISUAL_PRODUCTION, "Produccion visual narrativa", "Visual narrativa", "Abrir produccion visual de Video narrativo por fragmento.", AppCommandOwner.VIEW, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.IMPORT_THEATRE_GRAMMAR, "Importar obra a partir de gramatica", "Importar gramatica", "Importar un Markdown con la gramatica teatral DocuPodcast para crear la obra, actos y escenas.", AppCommandOwner.SOURCE_DOCUMENT, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.EXPORT_THEATRE_GRAMMAR_TEMPLATE, "Exportar plantilla de gramatica teatral", "Plantilla gramatica", "Exportar una plantilla Markdown para que GPT ayude a configurar obra, fragmentos, fotos, personajes y objetos.", AppCommandOwner.EXPORT, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.IMPORT_NARRATIVE_VIDEO_GRAMMAR, "Importar guion narrativo", "Importar narrativa", "Importar un Markdown con la gramatica de Video narrativo.", AppCommandOwner.SOURCE_DOCUMENT, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE, "Exportar plantilla narrativa", "Plantilla narrativa", "Exportar una plantilla Markdown para Video narrativo.", AppCommandOwner.EXPORT, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.EXIT_APPLICATION, "Salir", "Salir", "Cerrar DocuPodcast Studio.", AppCommandOwner.VIEW, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.CLEAR_SELECTION, "Limpiar selección", "Limpiar", "Quitar la selección actual del documento.", AppCommandOwner.SELECTION, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.LEFT_SIDEBAR, AppCommandSurface.KEYBOARD_SHORTCUT)));
        descriptors.add(command(AppCommandId.NEW_PROJECT, "Nuevo proyecto", "Nuevo", "Crear un proyecto DocuPodcast.", AppCommandOwner.PROJECT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.WELCOME, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.OPEN_PROJECT, "Abrir proyecto", "Abrir", "Abrir un proyecto DocuPodcast existente.", AppCommandOwner.PROJECT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.WELCOME, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.SAVE_PROJECT, "Guardar", "Guardar", "Guardar el proyecto actual.", AppCommandOwner.PROJECT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.KEYBOARD_SHORTCUT, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.SAVE_PROJECT_AS, "Guardar como", "Guardar como", "Guardar el proyecto en otra ubicación.", AppCommandOwner.PROJECT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.CLOSE_PROJECT, "Cerrar proyecto", "Cerrar", "Cerrar el proyecto actual.", AppCommandOwner.PROJECT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.OPEN_PROJECT_FOLDER, "Abrir carpeta del proyecto", "Carpeta", "Abrir la carpeta contenedora del proyecto.", AppCommandOwner.PROJECT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.SETTINGS)));


        descriptors.add(command(AppCommandId.OPEN_EXAMPLE_PROJECT, "Abrir ejemplo", "Ejemplos", "Crear un proyecto demo desde ejemplos incluidos.", AppCommandOwner.PROJECT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.WELCOME)));

        descriptors.add(command(AppCommandId.OPEN_SOURCE_DOCUMENT, "Abrir fuente documental", "Abrir fuente", "Abrir la fuente documental de entrada.", AppCommandOwner.SOURCE_DOCUMENT, AppCommandSurface.WELCOME, true, surfaces(AppCommandSurface.WELCOME, AppCommandSurface.MENU_BAR, AppCommandSurface.RIBBON, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.REFRESH_SOURCE_DOCUMENT, "Refrescar desde fuente documental", "Refrescar", "Releer la fuente documental desde disco.", AppCommandOwner.SOURCE_DOCUMENT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.WORKSPACE)));
        descriptors.add(command(AppCommandId.OPEN_SOURCE_DOCUMENT_LOCATION, "Abrir ubicación de la fuente", "Ubicación fuente", "Abrir la carpeta donde vive la fuente documental activa.", AppCommandOwner.SOURCE_DOCUMENT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.PREPARE_DOCUMENT_READING, "Preparar lectura", "Preparar lectura", "Preparar la lectura del documento antes de generar audio.", AppCommandOwner.SOURCE_DOCUMENT, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.MENU_BAR, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.PREPARE_TECHNICAL_PROBLEM, "Preparar problema tecnico", "Problema tecnico", "Abrir el panel de problema tecnico para seleccionar fragmentos y resolver ejercicios.", AppCommandOwner.SOURCE_DOCUMENT, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.MENU_BAR, AppCommandSurface.TOOLBAR)));

        descriptors.add(command(AppCommandId.LISTEN_DOCUMENT, "Escuchar documento", "Escuchar", "Iniciar o continuar la lectura del documento.", AppCommandOwner.READING, AppCommandSurface.WORKSPACE_PLAYBAR, true, surfaces(AppCommandSurface.WORKSPACE_PLAYBAR, AppCommandSurface.MENU_BAR, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.PLAY_SELECTION, "Reproducir oración seleccionada", "Reproducir desde aquí", "Reproducir la oración o unidad seleccionada.", AppCommandOwner.READING, AppCommandSurface.WORKSPACE_PLAYBAR, true, surfaces(AppCommandSurface.WORKSPACE_PLAYBAR, AppCommandSurface.MENU_BAR, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.PAUSE_PLAYBACK, "Pausar", "Pausar", "Pausar la reproducción actual.", AppCommandOwner.READING, AppCommandSurface.WORKSPACE_PLAYBAR, true, surfaces(AppCommandSurface.WORKSPACE_PLAYBAR, AppCommandSurface.MENU_BAR, AppCommandSurface.KEYBOARD_SHORTCUT)));
        descriptors.add(command(AppCommandId.RESUME_PLAYBACK, "Reanudar", "Reanudar", "Reanudar la reproducción pausada.", AppCommandOwner.READING, AppCommandSurface.WORKSPACE_PLAYBAR, true, surfaces(AppCommandSurface.WORKSPACE_PLAYBAR, AppCommandSurface.MENU_BAR, AppCommandSurface.KEYBOARD_SHORTCUT)));
        descriptors.add(command(AppCommandId.STOP_PLAYBACK, "Detener", "Detener", "Detener la reproducción actual.", AppCommandOwner.READING, AppCommandSurface.WORKSPACE_PLAYBAR, true, surfaces(AppCommandSurface.WORKSPACE_PLAYBAR, AppCommandSurface.MENU_BAR, AppCommandSurface.KEYBOARD_SHORTCUT)));

        descriptors.add(command(AppCommandId.ASSIGN_AI_VOICE_TO_SELECTION, "Voz IA", "Voz IA", "Asignar voz IA a la selección.", AppCommandOwner.SELECTION, AppCommandSurface.LEFT_SIDEBAR, true, surfaces(AppCommandSurface.LEFT_SIDEBAR, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.ASSIGN_HUMAN_RECORDING_TO_SELECTION, "Grabar voz", "Grabar", "Preparar grabación de voz para la selección.", AppCommandOwner.SELECTION, AppCommandSurface.LEFT_SIDEBAR, true, surfaces(AppCommandSurface.LEFT_SIDEBAR, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.IMPORT_VOICE_SAMPLE, "Importar voz", "Importar voz", "Importar una muestra de voz con permiso del usuario.", AppCommandOwner.VOICE, AppCommandSurface.SETTINGS, true, surfaces(AppCommandSurface.SETTINGS, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.OPEN_VOICE_LIBRARY, "Biblioteca de voces", "Voces", "Abrir biblioteca y gestión de voces.", AppCommandOwner.VOICE, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.RIBBON, AppCommandSurface.TOOLBAR)));

        descriptors.add(command(AppCommandId.IMPORT_AUDIO_FOR_SELECTION, "Elegir audio", "Elegir audio", "Asignar un clip de audio del computador a la selección.", AppCommandOwner.MEDIA, AppCommandSurface.LEFT_SIDEBAR, true, surfaces(AppCommandSurface.LEFT_SIDEBAR)));
        descriptors.add(command(AppCommandId.EXTRACT_VIDEO_AUDIO_FOR_SELECTION, "Extraer audio de video", "Audio de video", "Extraer audio de un video y asignarlo a la selección.", AppCommandOwner.MEDIA, AppCommandSurface.LEFT_SIDEBAR, true, surfaces(AppCommandSurface.LEFT_SIDEBAR)));
        descriptors.add(command(AppCommandId.IMPORT_IMAGE_FOR_SELECTION, "Elegir imagen", "Elegir imagen", "Asignar una imagen a la selección.", AppCommandOwner.MEDIA, AppCommandSurface.RIGHT_RAIL, true, surfaces(AppCommandSurface.RIGHT_RAIL)));
        descriptors.add(command(AppCommandId.IMPORT_BRIDGE_IMAGE_FOR_SELECTION, "Elegir imagen puente", "Imagen puente", "Asignar una imagen puente opcional al fragmento seleccionado.", AppCommandOwner.MEDIA, AppCommandSurface.RIGHT_RAIL, true, surfaces(AppCommandSurface.RIGHT_RAIL)));
        descriptors.add(command(AppCommandId.ASSOCIATE_IMAGE_TO_SELECTION, "Asociar imagen", "Asociar", "Asociar la última imagen importada al fragmento seleccionado.", AppCommandOwner.MEDIA, AppCommandSurface.RIGHT_RAIL, true, surfaces(AppCommandSurface.RIGHT_RAIL, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.CREATE_STORYBOARD, "Crear secuencia visual", "Visuales", "Crear una secuencia visual simple desde imágenes asociadas al documento.", AppCommandOwner.MEDIA, AppCommandSurface.TOOLBAR, true, surfaces(AppCommandSurface.TOOLBAR, AppCommandSurface.MENU_BAR)));
        descriptors.add(hidden(AppCommandId.OPEN_STORYBOARD, "Panel visual", "Visuales", "Comando heredado: la secuencia visual vive en Teatro > Guión, no como workspace.", AppCommandOwner.MEDIA, AppCommandSurface.MENU_BAR, surfaces(AppCommandSurface.MENU_BAR)));

        descriptors.add(command(AppCommandId.GENERATE_AUDIO, "Generar audio", "Generar", "Generar audio con el motor configurado.", AppCommandOwner.PROCESS, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.TOOLBAR, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.CANCEL_AUDIO_JOB, "Cancelar generación", "Cancelar", "Cancelar la generación de audio activa.", AppCommandOwner.PROCESS, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.TOOLBAR)));
        descriptors.add(hidden(AppCommandId.OPEN_AUDIO_JOBS, "Procesos de audio", "Procesos", "Comando heredado: los jobs se mostrarán como overlay de progreso, no como workspace.", AppCommandOwner.PROCESS, AppCommandSurface.MENU_BAR, surfaces(AppCommandSurface.MENU_BAR)));

        descriptors.add(command(AppCommandId.EXPORT_PROJECT_BUNDLE, "Exportar paquete de soporte", "Paquete soporte", "Crear un paquete técnico auditable para soporte avanzado.", AppCommandOwner.DIAGNOSTIC, AppCommandSurface.SETTINGS, true, surfaces(AppCommandSurface.SETTINGS, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.EXPORT_PODCAST_WAV, "Exportar audio", "Audio", "Exportar el audio disponible del proyecto como WAV, MP3 o AAC.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO, "Exportar video documental texto+audio", "Texto+audio", "Exportar un MP4 simple de Estudio documental con texto del fragmento y audio generado.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.EXPORT_DIAGNOSTIC_REPORT, "Exportar reporte de soporte", "Reporte soporte", "Exportar un reporte técnico para soporte avanzado.", AppCommandOwner.DIAGNOSTIC, AppCommandSurface.SETTINGS, true, surfaces(AppCommandSurface.SETTINGS, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE, "Exportar video", "Video", "Exportar un archivo MP4 final desde la secuencia visual del documento.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.EXPORT_THEATRE_WORK, "Exportar obra", "Obra", "Exportar un video teatral con texto, imagen y mapa espacial cuando exista.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW, "Exportar video de mapa teatral", "Video mapa", "Exportar un MP4 teatral con mapa espacial y fragmentos sincronizados.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.EXPORT_THEATRE_PORTION, "Exportar porcion de obra", "Porcion obra", "Exportar un acto o escena como video de fragmentos o video mapa teatral.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.OPEN_EXPORT_CENTER, "Centro de exportaciones", "Centro de exportaciones", "Abrir el centro de exportaciones creativas del proyecto actual.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.OPEN_EXPORTS_FOLDER, "Abrir carpeta de exportaciones", "Exportaciones", "Abrir o crear la carpeta exports del proyecto.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));

        descriptors.add(command(AppCommandId.TOGGLE_FULLSCREEN, "Pantalla completa", "Pantalla completa", "Alternar pantalla completa.", AppCommandOwner.VIEW, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.TOGGLE_RIBBON_COLLAPSED, "Minimizar o expandir cinta", "Cinta", "Mostrar u ocultar el contenido de la cinta de opciones.", AppCommandOwner.VIEW, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.TOGGLE_RIGHT_RAIL, "Mostrar u ocultar panel visual", "Panel visual", "Mostrar u ocultar el panel visual del Documento.", AppCommandOwner.VIEW, AppCommandSurface.RIGHT_RAIL, true, surfaces(AppCommandSurface.RIGHT_RAIL, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.TOGGLE_DOCUMENT_PLAYBAR_DOCK, "Desplazar playbar", "Desplazar playbar", "Alternar la barra de lectura entre el documento y el rail izquierdo.", AppCommandOwner.VIEW, AppCommandSurface.RIBBON, true, surfaces(AppCommandSurface.RIBBON, AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.OPEN_SETTINGS, "Configuración", "Configurar", "Abrir configuración de motores y preferencias.", AppCommandOwner.SETTINGS, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.RIBBON, AppCommandSurface.SETTINGS)));
        descriptors.add(command(AppCommandId.OPEN_GUIDE, "Guía de uso", "Guía", "Abrir guía de uso.", AppCommandOwner.HELP, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.TOOLBAR, AppCommandSurface.RIBBON)));
        descriptors.add(command(AppCommandId.OPEN_ABOUT, "Acerca de DocuPodcast Studio", "Acerca de", "Abrir información del producto.", AppCommandOwner.HELP, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        descriptors.add(command(AppCommandId.OPEN_WORD_GUIDE, "Ayuda Word", "Ayuda Word", "Abrir ayuda para documentos Word.", AppCommandOwner.HELP, AppCommandSurface.TOOLBAR, true, surfaces(AppCommandSurface.TOOLBAR)));
        descriptors.add(command(AppCommandId.INSPECT_PROJECT_INTEGRITY, "Validar integridad del proyecto", "Integridad", "Revisar integridad de proyecto y assets.", AppCommandOwner.DIAGNOSTIC, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.SETTINGS)));
        descriptors.add(command(AppCommandId.INSPECT_EXPORT_READINESS, "Ver estado de exportación", "Estado exportación", "Revisar disponibilidad de exportaciones.", AppCommandOwner.DIAGNOSTIC, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR, AppCommandSurface.SETTINGS)));
        descriptors.add(command(AppCommandId.EXPORT_AI_RESOURCES, "Exportar recursos IA", "Recursos IA", "Exportar recursos Markdown/IA del proyecto.", AppCommandOwner.EXPORT, AppCommandSurface.MENU_BAR, true, surfaces(AppCommandSurface.MENU_BAR)));
        return new AppCommandRegistry(descriptors);
    }

    public AppCommandDescriptor descriptor(AppCommandId id) {
        AppCommandDescriptor descriptor = commands.get(Objects.requireNonNull(id, "id"));
        if (descriptor == null) {
            throw new IllegalArgumentException("Comando no registrado: " + id);
        }
        return descriptor;
    }

    public List<AppCommandDescriptor> all() {
        return List.copyOf(commands.values());
    }

    public List<AppCommandDescriptor> visibleOn(AppCommandSurface surface) {
        Objects.requireNonNull(surface, "surface");
        return commands.values().stream()
                .filter(AppCommandDescriptor::visibleByDefault)
                .filter(command -> command.allowedOn(surface))
                .toList();
    }

    public List<AppCommandDescriptor> byOwner(AppCommandOwner owner) {
        Objects.requireNonNull(owner, "owner");
        return commands.values().stream()
                .filter(command -> command.owner() == owner)
                .toList();
    }

    public boolean contains(AppCommandId id) {
        return commands.containsKey(Objects.requireNonNull(id, "id"));
    }

    private static AppCommandDescriptor command(AppCommandId id, String label, String shortLabel, String description,
                                                AppCommandOwner owner, AppCommandSurface primarySurface,
                                                boolean implemented, Set<AppCommandSurface> surfaces) {
        return new AppCommandDescriptor(id, label, shortLabel, description, owner, primarySurface, surfaces, implemented, implemented);
    }

    private static AppCommandDescriptor hidden(AppCommandId id, String label, String shortLabel, String description,
                                               AppCommandOwner owner, AppCommandSurface primarySurface,
                                               Set<AppCommandSurface> surfaces) {
        return new AppCommandDescriptor(id, label, shortLabel, description, owner, primarySurface, surfaces, false, false);
    }

    private static Set<AppCommandSurface> surfaces(AppCommandSurface first, AppCommandSurface... rest) {
        EnumSet<AppCommandSurface> set = EnumSet.of(first, rest);
        return Set.copyOf(set);
    }

    public Map<AppCommandOwner, Long> visibleCountByOwner() {
        return commands.values().stream()
                .filter(AppCommandDescriptor::visibleByDefault)
                .collect(Collectors.groupingBy(AppCommandDescriptor::owner, () -> new EnumMap<>(AppCommandOwner.class), Collectors.counting()));
    }
}
