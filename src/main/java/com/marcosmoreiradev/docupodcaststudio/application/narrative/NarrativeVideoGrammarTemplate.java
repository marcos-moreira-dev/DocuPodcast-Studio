package com.marcosmoreiradev.docupodcaststudio.application.narrative;

/** Downloadable Markdown contract for Video narrativo. */
public final class NarrativeVideoGrammarTemplate {
    private NarrativeVideoGrammarTemplate() {
    }

    public static String markdown() {
        return """
                # Mi video narrativo

                > DocuPodcast Video Narrativo Grammar v1

                ## Metadatos
                - idioma: es
                - formato sugerido: 16:9
                - estilo visual general: ilustracion cinematografica sobria
                - narrador/voz sugerida: Narrador neutro
                - notas de exportacion: MP4 horizontal, texto opcional sobre el video.

                ## Fragmento 1
                Escribe aqui el texto narrado del primer fragmento.
                > tono=MYSTERIOUS
                > visual=Imagen principal del fragmento.
                > corte_fuerte=no
                > imagen_puente=Imagen opcional para conectar con el siguiente fragmento.
                > notas=Notas de produccion opcionales.

                ## Fragmento 2
                Escribe aqui el texto narrado del segundo fragmento.
                > tono=CALM
                > visual=Imagen principal del cierre.
                > corte_fuerte=si
                > imagen_puente=
                > notas=
                """;
    }
}
