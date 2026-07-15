package com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Markdown contract exported from the Theatre ribbon for AI-assisted setup. */
public final class TheatreGrammarTemplate {
    private TheatreGrammarTemplate() {
    }

    public static String markdown() {
        String toneCatalog = Arrays.stream(VoiceReferenceTone.values())
                .map(tone -> tone.name() + " (" + tone.displayName() + ")")
                .collect(Collectors.joining(", "));
        return """
                <!-- TONO_CATALOGO: %s -->

                # Titulo de la obra

                > DocuPodcast Teatro Grammar v1
                > Escribe cualquier obra en Markdown. Una IA puede completar este archivo y DocuPodcast puede importarlo.
                > Usa `tono=` con uno de los codigos de TONO_CATALOGO cuando quieras fijar emocion/interpretacion.
                > Usa `interaccion=` con uno o varios destinos separados por coma: otro personaje, Publico o el mismo personaje.
                > Usa `plano=` con un tipo de plano teatral embebido; se hereda desde esa intervencion hacia adelante.
                > Usa `aplicar_plano=false` cuando el fragmento no deba usar la guia de plano como contexto IA.
                > Usa `fondo=` para asignar un telon/fondo desde la intervencion y `quitar_fondo=true` para cortar la herencia.
                > Usa `contexto_ia=` para guardar un texto contextual editable por intervencion.
                > Usa `voces=CAPITAN BIGOTE, TENIENTE TORNILLO` para marcar una intervencion teatral de voces simultaneas.

                ## Metadatos
                - idioma: es
                - mapa espacial: https://ejemplo.com/mapa-espacial.png
                - referencia visual general: https://ejemplo.com/obra.jpg

                ## Personajes
                - personaje: NARRADOR | nota=voz externa opcional | foto=https://ejemplo.com/narrador.png
                - personaje: CAPITAN BIGOTE | alias=BIGOTE | nota=piloto veterano | foto=https://ejemplo.com/capitan.png

                ## Objetos
                - objeto: avion antiguo | nota=biplano central de la escena | foto=https://ejemplo.com/avion.png
                - objeto: mapa de ruta | nota=papel que guia el vuelo | foto=https://ejemplo.com/mapa.png

                ## Acto: Acto 1
                notas: Presenta el mundo, el conflicto y las reglas visuales.

                ### Escena: El hangar
                > fondo_escenario=assets/fondos/hangar-amanecer.png
                notas: Amanecer. Avion antiguo al centro. Herramientas dispersas.

                ACOTACION: El hangar espera al amanecer.
                NARRADOR: En el viejo aerodromo, dos aviadores preparan el primer vuelo publico.
                > tono=SOLEMN | plano=CERCA_CENTRO_NIVEL | contexto_ia=Toma frontal del escenario; mantener camara fija y luz de amanecer.
                CAPITAN BIGOTE: Teniente, revise el combustible, el viento y la dignidad de esta maquina.
                > origen=centro derecha | interaccion=TENIENTE TORNILLO, Publico | tono=SERIOUS | plano=CERCA_DERECHA_NIVEL | fondo=assets/fondos/hangar-amanecer.png
                TENIENTE TORNILLO: Combustible hay. Viento hay.
                > origen=centro izquierda | interaccion=CAPITAN BIGOTE | tono=CALM
                TODOS: Que sorpresa.
                > voces=CAPITAN BIGOTE, TENIENTE TORNILLO | tono=EXCITED
                ![fragmento visual: hangar](https://ejemplo.com/hangar.png)

                ### Escena: Pista exterior
                notas: El avion sale del hangar. Viento visible.

                ACOTACION: La puerta del hangar se abre.
                CAPITAN BIGOTE: Hoy volaremos hacia el Tornillo Dorado.
                > origen=frente centro | interaccion=CAPITAN BIGOTE | tono=HEROIC | plano=PANORAMICA_CENTRO_NIVEL | quitar_fondo=true
                """.formatted(toneCatalog);
    }
}
