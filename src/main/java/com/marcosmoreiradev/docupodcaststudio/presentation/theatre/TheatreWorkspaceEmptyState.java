package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.Optional;

/** User-facing empty-state copy for the theatre script workspace. */
final class TheatreWorkspaceEmptyState {
    private TheatreWorkspaceEmptyState() {
    }

    static String noActsMessage(ReadableDocument document, NarrationScriptDocument script) {
        if (!hasDocumentBlocks(document)) {
            return "Sin fuente primaria: puedes crear actos, pero importa una fuente teatral para poblar intervenciones.";
        }
        if (!hasTheatreInterventions(document, script)) {
            return "No hay intervenciones teatrales detectables. Usa dialogos PERSONAJE: texto o acotaciones para alimentar Teatro.";
        }
        return "Crea el primer acto para ordenar escenas e intervenciones detectadas.";
    }

    static String noScenesMessage(TheatreProjectLayer.TheatreAct act) {
        String target = act == null || act.displayName().isBlank() ? "este acto" : act.displayName();
        return "Agrega una escena en " + target + " para mapear texto inicial/final, personajes, objetos y posiciones.";
    }

    static String interventionSequenceMessage(ReadableDocument document, NarrationScriptDocument script) {
        if (!hasDocumentBlocks(document)) {
            return "Sin fuente primaria: importa una fuente teatral para ver Intervencion 1, Intervencion 2 y siguientes.";
        }
        if (!hasTheatreInterventions(document, script)) {
            return "No se detectaron intervenciones teatrales. Usa dialogos PERSONAJE: texto o acotaciones.";
        }
        return "Marca texto inicial y final si quieres acotar esta escena.";
    }

    static Optional<String> preparationNotice(ReadableDocument document, NarrationScriptDocument script) {
        if (!hasDocumentBlocks(document) || !hasTheatreInterventions(document, script) || hasPreparedScript(script)) {
            return Optional.empty();
        }
        return Optional.of("Lectura no preparada: puedes ubicar intervenciones desde la fuente, pero audio, paquetes IA y exportacion usaran la lectura cuando exista.");
    }

    private static boolean hasDocumentBlocks(ReadableDocument document) {
        return document != null && !document.blocks().isEmpty();
    }

    private static boolean hasPreparedScript(NarrationScriptDocument script) {
        return script != null && !script.empty();
    }

    private static boolean hasTheatreInterventions(ReadableDocument document, NarrationScriptDocument script) {
        return hasDocumentBlocks(document) && !IntervencionCatalogo.intervenciones(document, script).isEmpty();
    }
}
