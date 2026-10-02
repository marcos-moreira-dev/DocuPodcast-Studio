package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarMarkdownParser;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TheatreDeterministicStateTest {
    @Test void v2RejectsUnknownProperties() {
        assertThrows(IllegalArgumentException.class, () -> TheatreGrammarMarkdownParser.parse("""
                # Obra
                > DocuPodcast Teatro Grammar v2
                > grammarVersion: theatre-v2
                ## Acto: Uno
                ### Escena: Una
                Narrador: Hola.
                > id=INTERVENCION-1 | propiedad_inventada=valor
                """));
    }

    @Test void v2MaterializesStableIdsAndInheritedState() {
        var plan = TheatreGrammarMarkdownParser.parse("""
                # Obra
                > DocuPodcast Teatro Grammar v2
                > grammarVersion: theatre-v2
                ## Personajes
                - personaje: Concha | id=CONCHA | alias=OVEJA | voz=voz_concha
                - personaje: Eloy | id=ELOY | voz=voz_eloy
                ## Objetos
                - objeto: Sombrero | id=SOMBRERO
                ## Acto: Primero
                ### Escena: Plaza
                Concha: Hola.
                > id=INTERVENCION-1 | hereda=ninguna | presentes=CONCHA@centro_izquierda,ELOY@centro | objetos=SOMBRERO@portado:CONCHA | microexpresion=sonrisa_contenida | emoji=😏
                Eloy: Avanzo.
                > id=INTERVENCION-2 | hereda=anterior | presentes=ELOY@frente_centro | eventos=GIVE:CONCHA:SOMBRERO:ELOY
                """);
        var layer = new TheatreImportUseCase().execute(plan, null, VoiceLibrary.defaults(), Map.of()).layer();
        assertEquals("CONCHA", layer.characters().get(0).id());
        var snapshot = new ResolveTheatreInterventionSnapshotUseCase().execute(layer, "INTERVENCION-2");
        assertEquals("centro_izquierda", snapshot.characters().get("CONCHA").position());
        assertEquals("frente_centro", snapshot.characters().get("ELOY").position());
        assertEquals("ELOY", snapshot.objects().get("SOMBRERO").holderCharacterId());
        assertEquals("😏", snapshot.emoji());
        assertTrue(snapshot.invalidReferences().isEmpty());
    }

    @Test void sceneChangeResetsImplicitPreviousState() {
        TheatreProjectLayer layer = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1,"B1"), TheatreProjectLayer.Intervencion.ofSequence(2,"B2")),
                List.of(new TheatreProjectLayer.CharacterProfile("A","A",List.of(),"")), List.of(), List.of(), List.of(),
                List.of(), List.of(new TheatreProjectLayer.TheatreAct("ACT","Act","")),
                List.of(new TheatreProjectLayer.Scene("S1","S1","","ACT"), new TheatreProjectLayer.Scene("S2","S2","","ACT")),
                List.of(), List.of(),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1","S1","A","","","",Map.of()),
                        new TheatreProjectLayer.TextActionPlacement("INTERVENCION-2","S2","A","","","",Map.of())),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new TheatreInterventionState("INTERVENCION-1", TheatreInterventionState.InheritanceMode.RESET,"",
                                List.of(new TheatreInterventionState.CharacterState("A", TheatreInterventionState.Presence.PRESENT,"centro","","","","")),List.of(),List.of(),"",""),
                        new TheatreInterventionState("INTERVENCION-2", TheatreInterventionState.InheritanceMode.PREVIOUS,"",List.of(),List.of(),List.of(),"","")));
        assertFalse(new ResolveTheatreInterventionSnapshotUseCase().execute(layer,"INTERVENCION-2").characters().containsKey("A"));
    }
}
