<!-- VOZ_CATALOGO: VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO, VOC-PRESET-HOMBRE-60-VETERANO-ECUADOR, VOC-PRESET-HOMBRE-20-IDEALISTA-ECUADOR -->
<!-- TONO_CATALOGO: NEUTRAL, CALM, SERIOUS, DRAMATIC, HAPPY, ENTHUSIASTIC -->

# El vuelo del Tornillo Dorado

Manifiesto teatral del demo Aviadores. El archivo `source.docx` es la fuente
documental; este Markdown solo organiza la obra para teatro, video, voces,
recursos visuales, ubicaciones y acciones.

- Personaje: NARRADOR | voz=VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO | tono=CALM | nota=Voz externa opcional para abrir, cerrar y comentar las escenas.
- personajes/narrador/narrador_01_frontal.png | angulo=Frontal

- Personaje: CAPITAN BIGOTE | voz=VOC-PRESET-HOMBRE-60-VETERANO-ECUADOR | tono=SERIOUS | nota=Piloto veterano, ceremonioso y terco.
- personajes/capitan_bigote/capitan_bigote_01_frontal.png | escena=El hangar | angulo=Frontal
- personajes/capitan_bigote/capitan_bigote_02_lateral_izquierdo.png | escena=El hangar | angulo=Lateral izquierdo
- personajes/capitan_bigote/capitan_bigote_03_lateral_derecho.png | escena=En el aire | angulo=Lateral derecho
- personajes/capitan_bigote/capitan_bigote_04_posterior.png | escena=El aterrizaje | angulo=Posterior
- personajes/capitan_bigote/capitan_bigote_05_trasero.png | escena=El aterrizaje | angulo=Trasero

- Personaje: TENIENTE TORNILLO | voz=VOC-PRESET-HOMBRE-20-IDEALISTA-ECUADOR | tono=ENTHUSIASTIC | nota=Copiloto inventor, optimista y literal.
- personajes/teniente_tornillo/teniente_tornillo_01_frontal.png | escena=El hangar | angulo=Frontal
- personajes/teniente_tornillo/teniente_tornillo_02_lateral_izquierdo.png | escena=El hangar | angulo=Lateral izquierdo
- personajes/teniente_tornillo/teniente_tornillo_03_lateral_derecho.png | escena=En el aire | angulo=Lateral derecho
- personajes/teniente_tornillo/teniente_tornillo_04_posterior.png | escena=El aterrizaje | angulo=Posterior
- personajes/teniente_tornillo/teniente_tornillo_05_trasero.png | escena=El aterrizaje | angulo=Trasero

- Objeto: Avion Tornillo Dorado | escena=El hangar | imagen=utileria/avion_tornillo_dorado_01.png | nota=Biplano antiguo y centro visual de la obra.
- Objeto: Bidon de combustible | escena=El hangar | imagen=utileria/obj_bidon_combustible_01.png | nota=Utileria del hangar para revision antes del despegue.
- Objeto: Tornillos sobrantes | escena=El hangar | imagen=utileria/obj_tornillos_sobrantes_01.png | nota=Objeto comico recurrente.
- Objeto: Mapa de ruta | escena=En el aire | imagen=utileria/obj_mapa_01.png | nota=Mapa que orienta la escena de vuelo.
- Objeto: Compas | escena=En el aire | imagen=utileria/obj_compas_01.png | nota=Instrumento de navegacion interpretado de forma absurda.
- Objeto: Paloma | escena=En el aire | imagen=utileria/obj_paloma_01.png | nota=Referencia visual breve.
- Objeto: Fardo de heno | escena=El aterrizaje | imagen=utileria/obj_fardo_heno_01.png | nota=Utileria del aterrizaje rural.
- Objeto: Vaca | escena=El aterrizaje | imagen=utileria/animal_vaca_01.png | nota=Referencia visual rural.
- Objeto: Sombrero del capitan | escena=El aterrizaje | imagen=utileria/obj_sombrero_capitan_01.png | nota=Objeto final del capitan.

## Acto: El vuelo

### Escena: El hangar
> texto_inicio=5 | texto_fin=10 | mapa_espacial=mapas/mapa-espacial.png

notas: El hangar presenta a los aviadores y establece el tono mecanico-comico.

NARRADOR: Ahora veremos una escena en el viejo hangar del Llano del Medio, donde un avion parece listo para despegar y tambien para pedir una segunda opinion.
> origen=extra diegetico | destino=extra diegetico | imagen=fragmentos/fragmento_02_narrador_aerodromo.png | tono=CALM | plano=CERCA_CENTRO_NIVEL | contexto_ia=Plano teatral frontal del hangar; mantener camara fija salvo cambio explicito.

CAPITAN BIGOTE: Teniente, revise el combustible antes del despegue.
> origen=centro derecha | destino=centro izquierda | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_03_revision_capitan.png | tono=SERIOUS

TENIENTE TORNILLO: Combustible hay, mi capitan. Tambien encontre estos tornillos sobrantes en el ala izquierda.
> origen=centro izquierda | destino=centro derecha | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_06_tornillos_sobrantes.png | tono=ENTHUSIASTIC

CAPITAN BIGOTE: No son sobrantes, teniente. Son tornillos de confianza. Cada uno ha sido probado en condiciones extremas.
> origen=centro derecha | destino=centro izquierda | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_07_tornillos_confianza.png | tono=SERIOUS

TENIENTE TORNILLO: Pero si estan oxidados, mi capitan. Y uno tiene una pequena flor pegada.
> origen=centro izquierda | destino=centro derecha | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_04_dignidad_mantenimiento.png | tono=ENTHUSIASTIC

CAPITAN BIGOTE: Eso es camuflaje natural. Sirve para confundir a las aves.
> origen=centro derecha | destino=centro izquierda | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_05_omision_administrativa.png | tono=DRAMATIC

### Escena: En el aire
> texto_inicio=12 | texto_fin=20 | mapa_espacial=mapas/mapa-espacial.png

notas: Cabina en vuelo. Las acciones alternan entre mapa, instrumentos y dialogo de mando.

TENIENTE TORNILLO: Mi capitan, el mapa parece estar al reves. El norte apunta hacia abajo.
> origen=centro derecha | destino=centro izquierda | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_09_mapa_norte_abajo.png | tono=ENTHUSIASTIC | plano=PANORAMICA_CENTRO_NIVEL | contexto_ia=Cabina en vuelo como escena teatral; camara estable y continuidad de personajes.

CAPITAN BIGOTE: Eso no es problema. Significa que vamos con tanta elegancia que hasta el norte quiere mirarnos.
> origen=centro izquierda | destino=centro derecha | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_10_elegancia_hacia_abajo.png | tono=SERIOUS

TENIENTE TORNILLO: El compas apunta hacia donde almorzamos.
> origen=centro derecha | destino=centro izquierda | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_11_compas_almuerzo.png | tono=ENTHUSIASTIC

CAPITAN BIGOTE: Entonces vamos bien. Ese restaurante tenia una energia... direccional.
> origen=centro izquierda | destino=centro derecha | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_12_destino_emocional.png | tono=SERIOUS

NARRADOR: Una paloma paso junto a la cabina, miro a los tripulantes y acelero para adelantarlos.
> origen=extra diegetico | destino=extra diegetico | imagen=fragmentos/fragmento_13_paloma_juzga.png | tono=CALM

TENIENTE TORNILLO: Veo el campo de aterrizaje, mi capitan. Esta justo donde el mapa dice que hay una laguna.
> origen=centro derecha | destino=frente izquierda | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_14_veo_campo_aterrizaje.png | tono=ENTHUSIASTIC

CAPITAN BIGOTE: Perfecto. Entonces la laguna se ha secado. Prepare el protocolo de aterrizaje.
> origen=centro izquierda | destino=centro derecha | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_15_preparar_protocolo.png | tono=DRAMATIC

TENIENTE TORNILLO: Cual era, mi capitan? La de cerrar los ojos o la de soltar lastre?
> origen=frente derecha | destino=frente izquierda | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_16_cual_era.png | tono=ENTHUSIASTIC

CAPITAN BIGOTE: Ambas. Es mejor improvisar con responsabilidad.
> origen=frente izquierda | destino=frente derecha | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_17_improvisar_con_responsabilidad.png | tono=DRAMATIC

### Escena: El aterrizaje
> texto_inicio=22 | texto_fin=27 | mapa_espacial=mapas/mapa-espacial.png

notas: La escena aterriza en un espacio rural con cierre de comedia.

TENIENTE TORNILLO: Aterrizamos, mi capitan. Todos los que estan enteros, quiero decir.
> origen=frente izquierda | destino=frente derecha | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_19_aterrizamos_enteros.png | tono=ENTHUSIASTIC | plano=CERCA_CENTRO_NIVEL | contexto_ia=Escenario rural de aterrizaje; mantener encuadre teatral sin salto de camara.

CAPITAN BIGOTE: Enteros nosotros. El sombrero no tuvo tanta suerte. Pero la dignidad se lleva por dentro.
> origen=frente derecha | destino=frente izquierda | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_20_sombrero_deserto.png | tono=SERIOUS

NARRADOR: El publico rural, compuesto por una vaca y tres fardos de heno, aplaudio con entusiasmo.
> origen=extra diegetico | destino=extra diegetico | imagen=fragmentos/fragmento_21_publico_rural_aplaude.png | tono=CALM

TENIENTE TORNILLO: Entonces, fue un exito, mi capitan?
> origen=frente izquierda | destino=frente derecha | interaccion=CAPITAN BIGOTE | imagen=fragmentos/fragmento_22_fue_un_exito.png | tono=ENTHUSIASTIC

CAPITAN BIGOTE: Teniente, si uno baja del avion por la puerta y no por el techo, ya es un exito.
> origen=frente derecha | destino=frente izquierda | interaccion=TENIENTE TORNILLO | imagen=fragmentos/fragmento_23_bajar_por_la_puerta.png | tono=DRAMATIC

NARRADOR: Y asi termino el vuelo del Tornillo Dorado. No hubo heridos graves, solo unas cuantas vacas perplejas y un sombrero menos.
> origen=extra diegetico | destino=extra diegetico | imagen=fragmentos/fragmento_24_cierre_tornillo_dorado.png | tono=CALM
