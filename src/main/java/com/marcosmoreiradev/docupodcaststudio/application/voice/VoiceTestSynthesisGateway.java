package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.io.IOException;

/** Port used by the voice workspace to synthesize a short real voice test. */
public interface VoiceTestSynthesisGateway {
    VoiceTestSynthesisResult synthesize(VoiceTestSynthesisRequest request) throws IOException;

    static VoiceTestSynthesisGateway unavailable() {
        return request -> VoiceTestSynthesisResult.blocked(
                "No hay un motor de voz real conectado para generar esta prueba. Abre Configuración y verifica Voz IA avanzada o Voz local simple.",
                true);
    }
}
