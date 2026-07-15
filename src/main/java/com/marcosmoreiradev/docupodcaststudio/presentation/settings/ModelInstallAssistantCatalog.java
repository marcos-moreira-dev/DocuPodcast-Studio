package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import java.util.List;

/**
 * Guided installation plans for local model files.
 *
 * <p>The assistant describes how the UI guides a normal user through preparing, importing, verifying and testing voice components without exposing internal file details.</p>
 *
 * <p>Guardrail interno: Verificar carpeta local con InspectLocalModelFolderUseCase, sin mostrar el nombre tecnico al usuario final.</p>
 */
public final class ModelInstallAssistantCatalog {
    private ModelInstallAssistantCatalog() {
    }

    public static List<ModelInstallPlan> recommendedPlans() {
        return List.of(advancedVoice(), localSimpleVoice(), localTheatreImage());
    }

    public static ModelInstallPlan advancedVoice() {
        return new ModelInstallPlan(
                "model-advanced-voice",
                "advanced-voice",
                "Voz IA avanzada — calidad alta",
                "Prioritario",
                "Carpeta local del motor avanzado",
                "Descargar desde catálogo verificable cuando esté disponible; si falla, importar carpeta ya descargada.",
                "Importar modelo manualmente desde una carpeta local o unidad externa; nunca exigir línea de comandos al usuario normal.",
                "Verificar la instalación local, confirmar integridad y hacer una prueba corta de voz antes de habilitarlo.",
                "Elegir motor potente, seleccionar o importar modelo, grabar muestra autorizada, probar una oración y guardar como predeterminado.",
                "Los ajustes técnicos quedan fuera de la pantalla principal y se reservan para diagnóstico avanzado. No depender de un enlace único ni de una URL fija.",
                List.of(
                        new ModelInstallStep("Elegir modelo", "Mostrar opciones recomendadas para español y narración humana."),
                        new ModelInstallStep("Descargar o importar", "Usar descarga asistida o importación manual si el proveedor externo cambia."),
                        new ModelInstallStep("Verificar", "Validar integridad y carpeta local dentro del programa."),
                        new ModelInstallStep("Probar voz", "Generar una oración breve con voz de referencia autorizada antes de usarlo en Documento.")
                ),
                List.of("Descargar desde catálogo verificable", "Importar modelo manualmente", "Verificar checksum", "Probar voz")
        );
    }

    public static ModelInstallPlan localSimpleVoice() {
        return new ModelInstallPlan(
                "model-local-simple-voice",
                "local-simple-voice",
                "Voz local simple — lectura liviana",
                "Intermedio",
                "Carpeta local de voces simples",
                "Instalar voces locales desde la app o importarlas manualmente por carpeta.",
                "Importar voz local cuando la descarga no esté disponible; mantener el modo intermedio sin romper el lector narrado.",
                "Verificar la voz local, confirmar integridad y probar una frase de lectura general.",
                "Usar Voz local simple cuando Voz IA avanzada sea demasiado pesada o el equipo necesite bajo consumo.",
                "La configuración técnica queda oculta salvo para diagnóstico avanzado. No depender de un enlace único.",
                List.of(
                        new ModelInstallStep("Seleccionar voz", "Elegir una voz liviana para lectura general."),
                        new ModelInstallStep("Importar o descargar", "Agregar archivos de voz a la carpeta local de modelos."),
                        new ModelInstallStep("Verificar", "Comprobar que la voz exista y pueda sintetizar una frase simple."),
                        new ModelInstallStep("Usar como modo intermedio", "Marcarlo como motor intermedio/liviano cuando Voz IA avanzada sea demasiado pesada o no esté disponible.")
                ),
                List.of("Seleccionar voz", "Importar voz local simple", "Verificar voz", "Probar lectura")
        );
    }

    public static ModelInstallPlan localTheatreImage() {
        return new ModelInstallPlan(
                "model-local-theatre-image",
                "local-theatre-image",
                "Imagen IA teatral local",
                "Visual",
                "Carpeta local de motor y modelos de imagen",
                "Descargar desde catalogo verificable cuando este disponible; si falla, importar paquete local completo.",
                "Importar runtime y modelos desde una carpeta local o unidad externa; despues de verificar, la app puede trabajar offline.",
                "Verificar runtime local, modelo base, adaptadores de referencia y prueba corta antes de habilitar generacion por intervencion.",
                "Usar Contextos para elegir una intervencion, preparar referencias visuales y generar candidatos revisables sin reemplazo automatico.",
                "El preset Prueba 4GB usa SD 1.5 fp16/pruned, 512x512, batch 1 y modo bajo consumo para validar flujo, no calidad final HD.",
                List.of(
                        new ModelInstallStep("Elegir preset", "Seleccionar Prueba 4GB o un workflow personalizado cuando el equipo lo soporte."),
                        new ModelInstallStep("Descargar o importar", "Preparar runtime local y modelos dentro de tools/image y models/image."),
                        new ModelInstallStep("Verificar", "Confirmar archivos minimos, checksum y conexion local antes de generar."),
                        new ModelInstallStep("Probar imagen", "Generar una intervencion corta y guardar el candidato para revision.")
                ),
                List.of("Preparar motor de imagen", "Importar paquete local", "Verificar checksum", "Probar generacion")
        );
    }

}
