package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import java.util.List;

/** Chooses the active side-dock module when the available modules change. */
public final class SideDockStatePolicy {
    public SideDockModuleId choose(SideDockModuleId previous, List<SideDockModule> modules) {
        if (modules.isEmpty()) return null;
        if (previous != null && modules.stream().anyMatch(module -> module.id() == previous)) {
            return previous;
        }
        for (SideDockModuleId preferred : List.of(
                SideDockModuleId.THEATRE_FRAGMENT_IMAGES,
                SideDockModuleId.THEATRE_CHARACTERS,
                SideDockModuleId.THEATRE_TEXTUAL_MAP,
                SideDockModuleId.THEATRE_SPATIAL_MAP,
                SideDockModuleId.THEATRE_ACTIONS,
                SideDockModuleId.THEATRE_OBJECTS,
                SideDockModuleId.DOCUMENT_CONTEXT_DETAILS,
                SideDockModuleId.DOCUMENT_INDEX,
                SideDockModuleId.DOCUMENT_AUDIO_NARRATION,
                SideDockModuleId.DOCUMENT_STRUCTURE,
                SideDockModuleId.OPERATIONAL_HELP)) {
            for (SideDockModule module : modules) {
                if (module.id().name().equals(preferred.name())) {
                    return module.id();
                }
            }
        }
        return modules.get(0).id();
    }
}
