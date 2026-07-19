package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import java.util.List;

/**
 * Guided catalog of local voice/media engines supported by the product.
 *
 * <p>The catalog provides short user-facing guidance without exposing file extensions, paths or command-line details.</p>
 */
public final class EngineSetupCatalog {
    private EngineSetupCatalog() {
    }

    public static List<EngineSetupOption> recommendedOptions() {
        return List.of(advancedVoice(), localSimpleVoice(), localTheatreImage());
    }

    public static EngineSetupOption advancedVoice() {
        return new EngineSetupOption(
                "advanced-voice",
                "Voz IA avanzada — calidad alta",
                "Prioritario",
                "Motor potente para narración más humana, personajes y voces autorizadas de referencia.",
                "Obras de teatro, documentos largos narrados con voz más natural y voces/personajes diferenciados.",
                "Carpeta local del motor avanzado. No se guarda dentro del DOCX.",
                "Usar asistente: localizar modelo, importar carpeta descargada o descargar desde catálogo verificable cuando esté disponible.",
                "Velocidad global, voz de referencia, idioma, muestras por intención; emoción audible solo si el motor la respeta.",
                "No depender de un link único. Si falla la descarga, permitir importación manual y verificación por checksum; la línea de comandos queda oculta para el usuario normal.",
                List.of("Descargar modelo", "Importar modelo manualmente", "Probar voz", "Abrir carpeta de modelos")
        );
    }

    public static EngineSetupOption localSimpleVoice() {
        return new EngineSetupOption(
                "local-simple-voice",
                "Voz local simple — liviana",
                "Modo intermedio local",
                "Motor intermedio y liviano para lectura rápida en equipos modestos.",
                "Lectura general, modo local intermedio y modo ahorro de recursos cuando la Voz IA avanzada sea demasiado pesada.",
                "Carpeta local de voces simples.",
                "Elegir una voz instalada, importar una voz descargada o preparar la voz local desde la app.",
                "Velocidad y volumen; no habilita clonación, emociones expresivas ni voces por muestra humana.",
                "Mantener importación manual para no atar el producto a una fuente externa permanente.",
                List.of("Seleccionar voz", "Importar voz local simple", "Probar lectura", "Abrir carpeta de voces")
        );
    }

    public static EngineSetupOption localTheatreImage() {
        return new EngineSetupOption(
                "local-theatre-image",
                "Generación visual local",
                "Generacion visual",
                "Motor local para generar imagenes por intervencion usando contexto visual de personajes, objetos y mapa teatral.",
                "Obras de teatro importadas por manifiesto MD, demos teatrales y proyectos con paquetes de contexto por intervencion.",
                "Runtime en tools/image y modelos en models/image.",
                "Preparar motor local, importar o descargar paquete verificable, seleccionar dispositivo y ejecutar una prueba corta.",
                "Preset Prueba 4GB, resolucion, pasos, semilla, alcance por acto/escena/intervencion y modo de frames.",
                "Debe funcionar offline despues de descargar o importar el paquete. Si falla la descarga, permitir importacion manual y verificacion por checksum.",
                List.of("Preparar motor de imagen", "Importar paquete local", "Probar generacion", "Abrir carpeta de modelos")
        );
    }

}
