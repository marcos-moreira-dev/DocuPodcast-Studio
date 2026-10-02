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

                > DocuPodcast Teatro Grammar v2
                > grammarVersion: theatre-v2
                > Esta es la semantica legible del paquete oficial. Las rutas siempre son relativas y solo se declaran si el archivo existe.
                > Zonas: fondo_izquierda, fondo_centro, fondo_derecha, centro_izquierda, centro, centro_derecha, frente_izquierda, frente_centro, frente_derecha.
                > `hereda=anterior`, `hereda=ninguna` o `hereda=INTERVENCION-N`. El cambio de escena corta la herencia anterior por defecto.

                ## Personajes
                - personaje: Narrador | id=NARRADOR | aliases=CRONISTA | voz=narrador_neutro | nota=voz externa opcional
                - personaje: Capitan Bigote | id=CAPITAN_BIGOTE | aliases=BIGOTE | voz=hombre_45_ecuador | nota=piloto veterano
                - assets/personajes/CAPITAN_BIGOTE/frontal.png | escena=SCN-HANGAR | angulo=frontal

                ## Objetos
                - objeto: Avion antiguo | id=AVION | nota=biplano central
                - objeto: Mapa de ruta | id=MAPA_RUTA | nota=papel que guia el vuelo

                ## Acto: Acto 1 | id=ACT-1
                notas: Presenta el mundo, el conflicto y las reglas visuales.

                ### Escena: El hangar | id=SCN-HANGAR
                > mapa_espacial=assets/mapas/hangar.png | fondo_escenario=assets/fondos/hangar-amanecer.png
                notas: Amanecer. Avion antiguo al centro. Herramientas dispersas.

                Narrador: En el viejo aerodromo comienza la historia.
                > id=INTERVENCION-1 | hereda=ninguna | presentes=NARRADOR@frente_centro,CAPITAN_BIGOTE@centro_derecha | tono=SOLEMN | plano=CERCA_CENTRO_NIVEL | microexpresion=serenidad | emoji=🎙️
                Capitan Bigote: Revisemos la maquina.
                > id=INTERVENCION-2 | hereda=anterior | interaccion=NARRADOR | presentes=CAPITAN_BIGOTE@frente_centro | orientaciones=CAPITAN_BIGOTE@izquierda | miradas=CAPITAN_BIGOTE@NARRADOR | objetos=MAPA_RUTA@portado:CAPITAN_BIGOTE | eventos=MOVE:CAPITAN_BIGOTE@frente_centro;TAKE:CAPITAN_BIGOTE:MAPA_RUTA
                CAPITAN BIGOTE: Teniente, revise el combustible.
                > id=INTERVENCION-3 | hereda=anterior | interaccion=TENIENTE TORNILLO, Publico | tono=SERIOUS | plano=CERCA_DERECHA_NIVEL | aplicar_plano=false | fondo=assets/fondos/hangar-amanecer.png | contexto_ia=Toma frontal controlada.
                TENIENTE TORNILLO: Combustible hay.
                > id=INTERVENCION-4 | hereda=anterior | quitar_fondo=true | tono=CALM

                ## INSTRUCCIONES PARA IA GENERADORA DE PAQUETES
                Crea `obra.teatro.md`, `docupodcast-theatre.json` schemaVersion 2 y solamente los archivos declarados bajo `assets/`.
                Cada asset declarado necesita path, logicalId, kind, sha256 y size correctos. Nunca uses rutas absolutas ni `..`.
                Si una imagen, frame o audio opcional no existe, NO lo declares, NO inventes una ruta y NO crees un archivo vacio.
                Conserva IDs de personajes, objetos e intervenciones; los nombres visibles pueden cambiar sin cambiar los IDs.
                Empaca esos elementos directamente o dentro de una unica carpeta raiz. Carpeta y ZIP tienen la misma semantica.
                """.formatted(toneCatalog);
    }
}
