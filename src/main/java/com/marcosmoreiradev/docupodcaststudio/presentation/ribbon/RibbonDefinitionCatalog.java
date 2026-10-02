package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;

import java.util.List;

/** Official tab/group catalog for the top ribbon. RibbonView renders this catalog; it no longer owns product structure. */
public final class RibbonDefinitionCatalog {
    private RibbonDefinitionCatalog() {
    }

    public static List<RibbonTabDefinition> officialTabs() {
        return officialTabs(ProjectMode.defaultMode());
    }

    public static List<RibbonTabDefinition> officialTabs(ProjectMode mode) {
        ProjectMode resolved = mode == null ? ProjectMode.defaultMode() : mode;
        return List.of(
                tab("inicio", "Inicio",
                        group("Fuente documental",
                                cmd(AppCommandId.OPEN_SOURCE_DOCUMENT, true)),
                        group("Proyecto",
                                cmd(AppCommandId.NEW_PROJECT, false),
                                cmd(AppCommandId.OPEN_PROJECT, false),
                                cmd(AppCommandId.SAVE_PROJECT, false)),
                        group("Soporte",
                                cmd(AppCommandId.OPEN_SETTINGS, false),
                                cmd(AppCommandId.OPEN_GUIDE, false))),
                tab("lectura", "Lectura",
                        group("Preparar lectura",
                                cmd(AppCommandId.PREPARE_DOCUMENT_READING, false),
                                cmd(AppCommandId.GENERATE_AUDIO, true),
                                cmd(AppCommandId.CANCEL_AUDIO_JOB, false))),
                tab("vista", "Vista",
                        group("Vistas principales",
                                cmd(AppCommandId.SHOW_WELCOME, true),
                                cmd(AppCommandId.OPEN_DOCUMENT_READER, true),
                                cmd(AppCommandId.OPEN_VOICE_LIBRARY, true)),
                        group("Ventana",
                                cmd(AppCommandId.TOGGLE_FULLSCREEN, false))),
                modeTab(resolved),
                tab("exportar", "Exportar",
                        group("Centro de exportaciones",
                                cmd(AppCommandId.OPEN_EXPORT_CENTER, true)))
        );
    }

    private static RibbonTabDefinition modeTab(ProjectMode mode) {
        return switch (mode) {
            case DOCUMENTARY_STUDIO -> tab("estudio", "Estudio",
                    group("Problemas",
                            cmd(AppCommandId.PREPARE_TECHNICAL_PROBLEM, true)),
                    group("Configuracion rapida",
                            cmd(AppCommandId.TOGGLE_DOCUMENT_PLAYBAR_DOCK, false)));
            case NARRATIVE_VIDEO -> tab("video-narrativo", "Video narrativo",
                    group("Produccion visual",
                            cmd(AppCommandId.OPEN_NARRATIVE_VISUAL_PRODUCTION, true)),
                    group("Gramatica narrativa",
                            cmd(AppCommandId.IMPORT_NARRATIVE_VIDEO_GRAMMAR, true),
                            cmd(AppCommandId.EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE, false)));
            case THEATRE_PRODUCTION -> tab("teatro", "Teatro",
                    group("Gestion manual de la obra",
                            cmd(AppCommandId.OPEN_THEATRE_SCRIPT, true)),
                    group("Gramatica",
                            cmd(AppCommandId.IMPORT_THEATRE_GRAMMAR, true),
                            cmd(AppCommandId.EXPORT_THEATRE_GRAMMAR_TEMPLATE, false)),
                    group("Carpeta de obra",
                            cmd(AppCommandId.REFRESH_THEATRE_PACKAGE, true)),
                    group("Gestion avanzada de obra",
                            cmd(AppCommandId.OPEN_THEATRE_IMAGE_GENERATION, true)));
        };
    }

    private static RibbonTabDefinition tab(String id, String title, RibbonGroupDefinition... groups) {
        return new RibbonTabDefinition(id, title, List.of(groups));
    }

    private static RibbonGroupDefinition group(String title, RibbonCommandDefinition... commands) {
        return new RibbonGroupDefinition(title, List.of(commands));
    }

    private static RibbonCommandDefinition cmd(AppCommandId commandId, boolean primary) {
        return new RibbonCommandDefinition(commandId, primary);
    }
}
