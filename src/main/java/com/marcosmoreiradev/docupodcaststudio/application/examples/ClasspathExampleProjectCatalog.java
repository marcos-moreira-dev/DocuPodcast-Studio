package com.marcosmoreiradev.docupodcaststudio.application.examples;

import java.util.List;

/** Static catalog for T113 examples bundled in src/main/resources/examples. */
public final class ClasspathExampleProjectCatalog implements ExampleProjectCatalog {
    private final List<ExampleProjectDescriptor> examples = List.of(
            new ExampleProjectDescriptor(
                    "instinto-creativo",
                    "Instinto creativo",
                    "Narrativa con imagen embebida",
                    "Documento narrativo corto para probar lectura comoda, imagen fuente y flujo de proyecto.",
                    "Instinto Creativo Demo",
                    "/examples/instinto-creativo/source.docx",
                    "instinto-creativo.docx",
                    List.of("DOCX narrativo", "imagen embebida", "lectura por fragmentos"),
                    List.of()
            ),
            new ExampleProjectDescriptor(
                    "caso-contable-cafe-luna",
                    "Caso contable: Cafe Luna Azul",
                    "Documento de negocio con tabla",
                    "Caso hipotetico de cafeteria para probar tablas como bloques visuales fuente y lectura de negocio.",
                    "Cafe Luna Azul Demo",
                    "/examples/caso-contable-cafe-luna/source.docx",
                    "caso-contable-cafe-luna.docx",
                    List.of("tabla contable", "documento de negocio", "bloque visual fuente"),
                    List.of()
            ),
            new ExampleProjectDescriptor(
                    "aviadores-comicos",
                    "El vuelo del Tornillo Dorado",
                    "Demo teatral desde manifiesto MD",
                    "Mini obra de aviadores. Copia teatro.md, el Word y los assets; luego genera la obra teatral desde el manifiesto.",
                    "Aviadores Comicos Demo",
                    "/examples/aviadores-comicos/source.docx",
                    "el-vuelo-del-tornillo-dorado.docx",
                    List.of("demo teatral", "teatro.md", "voces/personajes", "mapa espacial"),
                    List.of(
                            new ExampleAssetDescriptor("Presentacion 000", "/examples/aviadores-comicos/assets/fragmentos/fragmento_000_presentacion_personajes.png", "fragmentos/fragmento_000_presentacion_personajes.png", "Visual de presentación para portada del demo."),
                            new ExampleAssetDescriptor("Presentacion 00", "/examples/aviadores-comicos/assets/fragmentos/fragmento_00_presentacion_personajes.png", "fragmentos/fragmento_00_presentacion_personajes.png", "Visual de presentación para portada del demo."),
                            new ExampleAssetDescriptor("Fragmento 01", "/examples/aviadores-comicos/assets/fragmentos/fragmento_01_escena1_hangar_presentacion.png", "fragmentos/fragmento_01_escena1_hangar_presentacion.png", "Visual sugerido para el fragmento 01."),
                            new ExampleAssetDescriptor("Fragmento 02", "/examples/aviadores-comicos/assets/fragmentos/fragmento_02_narrador_aerodromo.png", "fragmentos/fragmento_02_narrador_aerodromo.png", "Visual sugerido para el fragmento 02."),
                            new ExampleAssetDescriptor("Fragmento 03", "/examples/aviadores-comicos/assets/fragmentos/fragmento_03_revision_capitan.png", "fragmentos/fragmento_03_revision_capitan.png", "Visual sugerido para el fragmento 03."),
                            new ExampleAssetDescriptor("Fragmento 04", "/examples/aviadores-comicos/assets/fragmentos/fragmento_04_dignidad_mantenimiento.png", "fragmentos/fragmento_04_dignidad_mantenimiento.png", "Visual sugerido para el fragmento 04."),
                            new ExampleAssetDescriptor("Fragmento 05", "/examples/aviadores-comicos/assets/fragmentos/fragmento_05_omision_administrativa.png", "fragmentos/fragmento_05_omision_administrativa.png", "Visual sugerido para el fragmento 05."),
                            new ExampleAssetDescriptor("Fragmento 06", "/examples/aviadores-comicos/assets/fragmentos/fragmento_06_tornillos_sobrantes.png", "fragmentos/fragmento_06_tornillos_sobrantes.png", "Visual sugerido para el fragmento 06."),
                            new ExampleAssetDescriptor("Fragmento 07", "/examples/aviadores-comicos/assets/fragmentos/fragmento_07_tornillos_confianza.png", "fragmentos/fragmento_07_tornillos_confianza.png", "Visual sugerido para el fragmento 07."),
                            new ExampleAssetDescriptor("Fragmento 08", "/examples/aviadores-comicos/assets/fragmentos/fragmento_08_escena2_vuelo.png", "fragmentos/fragmento_08_escena2_vuelo.png", "Visual sugerido para el fragmento 08."),
                            new ExampleAssetDescriptor("Fragmento 09", "/examples/aviadores-comicos/assets/fragmentos/fragmento_09_mapa_norte_abajo.png", "fragmentos/fragmento_09_mapa_norte_abajo.png", "Visual sugerido para el fragmento 09."),
                            new ExampleAssetDescriptor("Fragmento 10", "/examples/aviadores-comicos/assets/fragmentos/fragmento_10_elegancia_hacia_abajo.png", "fragmentos/fragmento_10_elegancia_hacia_abajo.png", "Visual sugerido para el fragmento 10."),
                            new ExampleAssetDescriptor("Fragmento 11", "/examples/aviadores-comicos/assets/fragmentos/fragmento_11_compas_almuerzo.png", "fragmentos/fragmento_11_compas_almuerzo.png", "Visual sugerido para el fragmento 11."),
                            new ExampleAssetDescriptor("Fragmento 12", "/examples/aviadores-comicos/assets/fragmentos/fragmento_12_destino_emocional.png", "fragmentos/fragmento_12_destino_emocional.png", "Visual sugerido para el fragmento 12."),
                            new ExampleAssetDescriptor("Fragmento 13", "/examples/aviadores-comicos/assets/fragmentos/fragmento_13_paloma_juzga.png", "fragmentos/fragmento_13_paloma_juzga.png", "Visual sugerido para el fragmento 13."),
                            new ExampleAssetDescriptor("Fragmento 14", "/examples/aviadores-comicos/assets/fragmentos/fragmento_14_veo_campo_aterrizaje.png", "fragmentos/fragmento_14_veo_campo_aterrizaje.png", "Visual sugerido para el fragmento 14."),
                            new ExampleAssetDescriptor("Fragmento 15", "/examples/aviadores-comicos/assets/fragmentos/fragmento_15_preparar_protocolo.png", "fragmentos/fragmento_15_preparar_protocolo.png", "Visual sugerido para el fragmento 15."),
                            new ExampleAssetDescriptor("Fragmento 16", "/examples/aviadores-comicos/assets/fragmentos/fragmento_16_cual_era.png", "fragmentos/fragmento_16_cual_era.png", "Visual sugerido para el fragmento 16."),
                            new ExampleAssetDescriptor("Fragmento 17", "/examples/aviadores-comicos/assets/fragmentos/fragmento_17_improvisar_con_responsabilidad.png", "fragmentos/fragmento_17_improvisar_con_responsabilidad.png", "Visual sugerido para el fragmento 17."),
                            new ExampleAssetDescriptor("Fragmento 18", "/examples/aviadores-comicos/assets/fragmentos/fragmento_18_aterrizaje_heno.png", "fragmentos/fragmento_18_aterrizaje_heno.png", "Visual sugerido para el fragmento 18."),
                            new ExampleAssetDescriptor("Fragmento 19", "/examples/aviadores-comicos/assets/fragmentos/fragmento_19_aterrizamos_enteros.png", "fragmentos/fragmento_19_aterrizamos_enteros.png", "Visual sugerido para el fragmento 19."),
                            new ExampleAssetDescriptor("Fragmento 20", "/examples/aviadores-comicos/assets/fragmentos/fragmento_20_sombrero_deserto.png", "fragmentos/fragmento_20_sombrero_deserto.png", "Visual sugerido para el fragmento 20."),
                            new ExampleAssetDescriptor("Fragmento 21", "/examples/aviadores-comicos/assets/fragmentos/fragmento_21_publico_rural_aplaude.png", "fragmentos/fragmento_21_publico_rural_aplaude.png", "Visual sugerido para el fragmento 21."),
                            new ExampleAssetDescriptor("Fragmento 22", "/examples/aviadores-comicos/assets/fragmentos/fragmento_22_fue_un_exito.png", "fragmentos/fragmento_22_fue_un_exito.png", "Visual sugerido para el fragmento 22."),
                            new ExampleAssetDescriptor("Fragmento 23", "/examples/aviadores-comicos/assets/fragmentos/fragmento_23_bajar_por_la_puerta.png", "fragmentos/fragmento_23_bajar_por_la_puerta.png", "Visual sugerido para el fragmento 23."),
                            new ExampleAssetDescriptor("Fragmento 24", "/examples/aviadores-comicos/assets/fragmentos/fragmento_24_cierre_tornillo_dorado.png", "fragmentos/fragmento_24_cierre_tornillo_dorado.png", "Visual sugerido para el fragmento 24."),
                            new ExampleAssetDescriptor("Mapa espacial teatral", "/examples/aviadores-comicos/assets/mapas/mapa-espacial.png", "mapas/mapa-espacial.png", "Plano superior del escenario para el mapa espacial."),
                            new ExampleAssetDescriptor("Teatro vacio", "/examples/aviadores-comicos/assets/mapas/teatro-vacio.png", "mapas/teatro-vacio.png", "Referencia del teatro vacio para paquetes IA."),
                            new ExampleAssetDescriptor("Presentacion de personajes", "/examples/aviadores-comicos/assets/imagen_01_presentacion_personajes.png", "imagen_01_presentacion_personajes.png", "Portada visual de personajes del demo teatral."),
                            new ExampleAssetDescriptor("Narrador frontal", "/examples/aviadores-comicos/assets/personajes/narrador/narrador_01_frontal.png", "personajes/narrador/narrador_01_frontal.png", "Foto frontal para el Narrador."),
                            new ExampleAssetDescriptor("Capitan Bigote frontal", "/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_01_frontal.png", "personajes/capitan_bigote/capitan_bigote_01_frontal.png", "Foto frontal para vestuario de Capitan Bigote."),
                            new ExampleAssetDescriptor("Capitan Bigote lateral izquierdo", "/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_02_lateral_izquierdo.png", "personajes/capitan_bigote/capitan_bigote_02_lateral_izquierdo.png", "Foto lateral izquierda para vestuario de Capitan Bigote."),
                            new ExampleAssetDescriptor("Capitan Bigote lateral derecho", "/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_03_lateral_derecho.png", "personajes/capitan_bigote/capitan_bigote_03_lateral_derecho.png", "Foto lateral derecha para vestuario de Capitan Bigote."),
                            new ExampleAssetDescriptor("Capitan Bigote posterior", "/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_04_posterior.png", "personajes/capitan_bigote/capitan_bigote_04_posterior.png", "Foto posterior para vestuario de Capitan Bigote."),
                            new ExampleAssetDescriptor("Capitan Bigote trasero", "/examples/aviadores-comicos/assets/personajes/capitan_bigote/capitan_bigote_05_trasero.png", "personajes/capitan_bigote/capitan_bigote_05_trasero.png", "Foto trasera alternativa para vestuario de Capitan Bigote."),
                            new ExampleAssetDescriptor("Teniente Tornillo frontal", "/examples/aviadores-comicos/assets/personajes/teniente_tornillo/teniente_tornillo_01_frontal.png", "personajes/teniente_tornillo/teniente_tornillo_01_frontal.png", "Foto frontal para vestuario de Teniente Tornillo."),
                            new ExampleAssetDescriptor("Teniente Tornillo lateral izquierdo", "/examples/aviadores-comicos/assets/personajes/teniente_tornillo/teniente_tornillo_02_lateral_izquierdo.png", "personajes/teniente_tornillo/teniente_tornillo_02_lateral_izquierdo.png", "Foto lateral izquierda para vestuario de Teniente Tornillo."),
                            new ExampleAssetDescriptor("Teniente Tornillo lateral derecho", "/examples/aviadores-comicos/assets/personajes/teniente_tornillo/teniente_tornillo_03_lateral_derecho.png", "personajes/teniente_tornillo/teniente_tornillo_03_lateral_derecho.png", "Foto lateral derecha para vestuario de Teniente Tornillo."),
                            new ExampleAssetDescriptor("Teniente Tornillo posterior", "/examples/aviadores-comicos/assets/personajes/teniente_tornillo/teniente_tornillo_04_posterior.png", "personajes/teniente_tornillo/teniente_tornillo_04_posterior.png", "Foto posterior para vestuario de Teniente Tornillo."),
                            new ExampleAssetDescriptor("Teniente Tornillo trasero", "/examples/aviadores-comicos/assets/personajes/teniente_tornillo/teniente_tornillo_05_trasero.png", "personajes/teniente_tornillo/teniente_tornillo_05_trasero.png", "Foto trasera alternativa para vestuario de Teniente Tornillo."),
                            new ExampleAssetDescriptor("Avion Tornillo Dorado", "/examples/aviadores-comicos/assets/utileria/avion_tornillo_dorado_01.png", "utileria/avion_tornillo_dorado_01.png", "Objeto principal de utileria del demo."),
                            new ExampleAssetDescriptor("Bidon de combustible", "/examples/aviadores-comicos/assets/utileria/obj_bidon_combustible_01.png", "utileria/obj_bidon_combustible_01.png", "Objeto de utileria para la escena del hangar."),
                            new ExampleAssetDescriptor("Tornillos sobrantes", "/examples/aviadores-comicos/assets/utileria/obj_tornillos_sobrantes_01.png", "utileria/obj_tornillos_sobrantes_01.png", "Objeto comico recurrente del demo."),
                            new ExampleAssetDescriptor("Mapa de ruta", "/examples/aviadores-comicos/assets/utileria/obj_mapa_01.png", "utileria/obj_mapa_01.png", "Objeto de navegacion del demo."),
                            new ExampleAssetDescriptor("Compas", "/examples/aviadores-comicos/assets/utileria/obj_compas_01.png", "utileria/obj_compas_01.png", "Instrumento de navegacion del demo."),
                            new ExampleAssetDescriptor("Paloma", "/examples/aviadores-comicos/assets/utileria/obj_paloma_01.png", "utileria/obj_paloma_01.png", "Referencia visual de la paloma del vuelo."),
                            new ExampleAssetDescriptor("Fardo de heno", "/examples/aviadores-comicos/assets/utileria/obj_fardo_heno_01.png", "utileria/obj_fardo_heno_01.png", "Utileria de aterrizaje rural."),
                            new ExampleAssetDescriptor("Vaca", "/examples/aviadores-comicos/assets/utileria/animal_vaca_01.png", "utileria/animal_vaca_01.png", "Referencia visual de la vaca testigo."),
                            new ExampleAssetDescriptor("Sombrero del capitan", "/examples/aviadores-comicos/assets/utileria/obj_sombrero_capitan_01.png", "utileria/obj_sombrero_capitan_01.png", "Objeto final del Capitan Bigote.")
                    ),
                    List.of(
                            new ExampleVisualBindingDescriptor("fragmento_000_presentacion_personajes.png", 1, "El vuelo del Tornillo Dorado", "Visual de presentación 000."),
                            new ExampleVisualBindingDescriptor("fragmento_00_presentacion_personajes.png", 2, "Demo visual cómica con assets", "Visual de presentación 00."),
                            new ExampleVisualBindingDescriptor("imagen_01_presentacion_personajes.png", 3, "Personajes. Capitan Bigote", "Visual de presentación de personajes."),
                            new ExampleVisualBindingDescriptor("fragmento_01_escena1_hangar_presentacion.png", 4, "Escena 1: El hangar.", "Visual de Escena 1: El hangar."),
                            new ExampleVisualBindingDescriptor("fragmento_02_narrador_aerodromo.png", 5, "NARRADOR: En el viejo aerodromo", "Visual del narrador en el aeródromo."),
                            new ExampleVisualBindingDescriptor("fragmento_03_revision_capitan.png", 6, "CAPITAN BIGOTE: Teniente, revise", "Visual de la revisión del Capitán Bigote."),
                            new ExampleVisualBindingDescriptor("fragmento_04_dignidad_mantenimiento.png", 7, "TENIENTE TORNILLO: Combustible hay.", "Visual de la dignidad en mantenimiento."),
                            new ExampleVisualBindingDescriptor("fragmento_05_omision_administrativa.png", 8, "CAPITAN BIGOTE: Excelente.", "Visual de la omisión administrativa."),
                            new ExampleVisualBindingDescriptor("fragmento_06_tornillos_sobrantes.png", 9, "TENIENTE TORNILLO: Tambien encontre", "Visual de los tornillos sobrantes."),
                            new ExampleVisualBindingDescriptor("fragmento_07_tornillos_confianza.png", 10, "CAPITAN BIGOTE: No son sobrantes.", "Visual de los tornillos de confianza."),
                            new ExampleVisualBindingDescriptor("fragmento_08_escena2_vuelo.png", 11, "Escena 2: En el aire.", "Visual de Escena 2: En el aire."),
                            new ExampleVisualBindingDescriptor("fragmento_09_mapa_norte_abajo.png", 12, "TENIENTE TORNILLO: Mi capitan, el mapa", "Visual del mapa con el norte abajo."),
                            new ExampleVisualBindingDescriptor("fragmento_10_elegancia_hacia_abajo.png", 13, "CAPITAN BIGOTE: Eso no es problema.", "Visual de volar con elegancia hacia abajo."),
                            new ExampleVisualBindingDescriptor("fragmento_11_compas_almuerzo.png", 14, "TENIENTE TORNILLO: El compas apunta", "Visual del compás apuntando al almuerzo."),
                            new ExampleVisualBindingDescriptor("fragmento_12_destino_emocional.png", 15, "CAPITAN BIGOTE: Entonces vamos bien.", "Visual del destino emocional."),
                            new ExampleVisualBindingDescriptor("fragmento_13_paloma_juzga.png", 16, "NARRADOR: Una paloma paso", "Visual de la paloma que no juzga."),
                            new ExampleVisualBindingDescriptor("fragmento_14_veo_campo_aterrizaje.png", 17, "TENIENTE TORNILLO: Veo el campo", "Visual del campo de aterrizaje."),
                            new ExampleVisualBindingDescriptor("fragmento_15_preparar_protocolo.png", 18, "CAPITAN BIGOTE: Perfecto.", "Visual del protocolo de aterrizaje."),
                            new ExampleVisualBindingDescriptor("fragmento_16_cual_era.png", 19, "TENIENTE TORNILLO: Cual era?", "Visual de la duda sobre el protocolo."),
                            new ExampleVisualBindingDescriptor("fragmento_17_improvisar_con_responsabilidad.png", 20, "CAPITAN BIGOTE: Cerrar los ojos", "Visual de improvisar con responsabilidad."),
                            new ExampleVisualBindingDescriptor("fragmento_18_aterrizaje_heno.png", 21, "Escena 3: El aterrizaje.", "Visual de Escena 3: El aterrizaje."),
                            new ExampleVisualBindingDescriptor("fragmento_19_aterrizamos_enteros.png", 22, "TENIENTE TORNILLO: Aterrizamos, mi capitan.", "Visual del aterrizaje entero."),
                            new ExampleVisualBindingDescriptor("fragmento_20_sombrero_deserto.png", 23, "CAPITAN BIGOTE: Enteros nosotros.", "Visual del sombrero desertor."),
                            new ExampleVisualBindingDescriptor("fragmento_21_publico_rural_aplaude.png", 24, "NARRADOR: El publico rural aplaudio", "Visual del público rural."),
                            new ExampleVisualBindingDescriptor("fragmento_22_fue_un_exito.png", 25, "TENIENTE TORNILLO: Entonces, fue un exito?", "Visual de la pregunta por el éxito."),
                            new ExampleVisualBindingDescriptor("fragmento_23_bajar_por_la_puerta.png", 26, "CAPITAN BIGOTE: Teniente, si uno baja", "Visual de bajar por la puerta."),
                            new ExampleVisualBindingDescriptor("fragmento_24_cierre_tornillo_dorado.png", 27, "NARRADOR: Y asi termino el vuelo", "Visual del cierre del Tornillo Dorado.")
                    ),
                    "/examples/aviadores-comicos/PROYECTO_DEMO.md"
            )
    );

    @Override
    public List<ExampleProjectDescriptor> listExamples() {
        return examples;
    }
}
