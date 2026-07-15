# T121-V04 — Wizard de registro de voz avanzada

## Objetivo

Crear un wizard claro para registrar voces avanzadas con muestras por tono. Debe ser simple para uso común, pero suficientemente potente para teatro/elencos.

## Nombre visible

Usar:

```text
Voz IA avanzada
```

No mostrar:

```text
Coqui
XTTS
speaker_wav
```

## Paso 1 — Nombre de voz

Campos:

- Nombre de voz.
- Uso opcional/notas.

Ejemplo:

```text
Nombre de voz: María
```


## Frases guía por tono

Cada tono del catálogo debe tener una frase guía por defecto. La frase no se genera aleatoriamente en runtime; queda definida por la aplicación para que el usuario entienda cómo actuar la muestra. La frase debe tener sentido con el tono para facilitar la grabación.

Regla de UX:

```text
Cuando el usuario elige grabar una muestra de tono, la app muestra la frase sugerida de ese tono.
El usuario lee la frase, graba y luego puede detener/cancelar/guardar la grabación.
```

Regla de implementación:

- Cada `VoiceReferenceTone` debe exponer una `suggestedRecordingPrompt`.
- La frase puede ser reemplazada por el usuario en una fase futura, pero para V1 se entrega una frase fija por tono.
- La UI debe mostrar la frase en una tarjeta amplia y legible antes de iniciar grabación.
- La grabación se asocia al tono seleccionado.
- Si el usuario cancela, no se reemplaza la muestra anterior.
- Si el usuario detiene y confirma, se guarda como muestra de ese tono.

### Frases sugeridas iniciales

| Tono | Frase guía sugerida |
|---|---|
| Neutral | Hoy leeré este texto con claridad, calma y una voz natural para que cada palabra se entienda bien. |
| Feliz | Qué alegría estar aquí; siento que este momento trae una luz nueva y una sonrisa imposible de esconder. |
| Triste | A veces el silencio pesa más que las palabras, y aun así intento seguir hablando con el corazón sereno. |
| Enojada | No puedo aceptar que esto siga ocurriendo; ya he esperado demasiado y necesito que me escuchen ahora. |
| Seria | Este asunto requiere atención, precisión y responsabilidad; cada detalle debe revisarse antes de decidir. |
| Calmada | Respira con tranquilidad; todo puede ordenarse paso a paso si mantenemos la mente clara y la voz serena. |
| Preocupada | Me inquieta lo que pueda pasar si no actuamos a tiempo, aunque todavía quiero creer que hay solución. |
| Entusiasmada | Esto puede convertirse en algo enorme; siento que estamos a punto de descubrir una oportunidad increíble. |
| Aburrida | Otra vez la misma historia, las mismas palabras y la misma espera interminable que parece no cambiar nada. |
| Alegre | Me encanta ver cómo todo empieza a tomar forma; hay una energía bonita en este lugar y se nota. |
| Eufórica | ¡No puedo creerlo! Esto es mucho mejor de lo que imaginaba y siento que voy a explotar de emoción. |
| Melancólica | Recuerdo aquellos días con una mezcla de ternura y distancia, como si el tiempo los hubiera cubierto suavemente. |
| Nerviosa | No sé si estoy lista, pero voy a intentarlo; mis manos tiemblan un poco y mi voz quiere adelantarse. |
| Asustada | Escuché un ruido detrás de la puerta y, por un momento, sentí que el aire se quedaba completamente quieto. |
| Sorprendida | ¿De verdad ocurrió eso? No esperaba esta noticia y todavía estoy tratando de entender lo que significa. |
| Dudosa | Tal vez sea una buena idea, pero hay algo que no termina de convencerme y necesito pensarlo mejor. |
| Cansada | He caminado demasiado por hoy; mi voz se vuelve lenta y solo quiero descansar un momento en silencio. |
| Suplicante | Por favor, escúchame un instante más; no te pido mucho, solo una oportunidad para explicar lo que siento. |
| Autoritaria | Escuchen con atención: desde este momento cada persona cumplirá su parte sin excusas ni retrasos innecesarios. |
| Irónica | Claro, porque seguramente todo se arregla solo si fingimos que nada de esto era importante. |
| Sarcástica | Maravilloso, justo lo que necesitábamos: otro problema presentado como si fuera una brillante solución. |
| Misteriosa | Hay cosas que no deben decirse en voz alta, especialmente cuando la noche parece escuchar detrás de las paredes. |
| Solemne | Hoy pronunciamos estas palabras con respeto, conscientes del peso que tienen para quienes estuvieron antes que nosotros. |
| Heroica | Aunque el camino sea difícil, avanzaremos con valor, porque alguien debe dar el primer paso por los demás. |
| Dramática | Si esta es la última vez que hablo, que al menos mis palabras queden grabadas en tu memoria. |
| Tensa | Nadie se movió; todos esperaban una respuesta mientras el reloj parecía sonar más fuerte que nunca. |
| Apurada | Tenemos que salir ahora mismo; no hay tiempo para discutir detalles, recoge lo necesario y ven conmigo. |
| Confundida | Espera, no entiendo qué acaba de pasar; hace un momento todo parecía claro y ahora nada encaja. |
| Tierna | Ven aquí, no tengas miedo; a veces una voz suave puede hacer que el mundo parezca menos grande. |
| Fría | No confundas mi silencio con duda; simplemente ya tomé una decisión y no pienso repetirla. |
| Burlona | ¿Eso era todo? Pensé que venías con una gran respuesta, no con esa explicación tan conveniente. |
| Desconfiada | Dices que puedo confiar en ti, pero tus palabras no coinciden con lo que vi hace un momento. |
| Arrepentida | Si pudiera volver atrás, elegiría mejor mis palabras y no dejaría que el orgullo hablara por mí. |
| Vulnerable | No me resulta fácil decir esto, pero necesito admitir que tengo miedo y que no puedo hacerlo sola. |
| Esperanzada | Quizás todavía haya una salida; a veces basta una pequeña señal para volver a creer. |
| Resignada | Ya entendí que no todo puede cambiarse; haré lo que me toca y seguiré adelante sin pelear más. |
| Amenazante | Te conviene pensar muy bien tu próxima palabra, porque esta vez no voy a pasar por alto lo que hiciste. |
| Desafiante | Si creen que voy a rendirme tan fácilmente, todavía no han entendido quién soy ni por qué sigo aquí. |
| Insinuante no explícita | Habla más bajo; algunas verdades se dicen mejor con calma, presencia y una intención cuidadosamente medida. |

Nota: las frases deben mantenerse aptas para uso educativo. La etiqueta `Insinuante no explícita` se maneja de forma no explícita y teatral, sin contenido gráfico.


## Paso 2 — Muestra neutral obligatoria

Texto:

```text
Graba o importa una muestra natural. Esta será la referencia principal para esta voz.
```

Acciones:

- Grabar muestra neutral.
- Importar muestra neutral.
- Reproducir muestra.
- Reemplazar.
- Descargar.

Debe existir una frase guía suficientemente larga para grabación.

## Paso 3 — Tonos recomendados

Mostrar tonos base:

- Feliz
- Triste
- Enojada
- Seria
- Calmada
- Preocupada
- Entusiasmada
- Aburrida

Cada fila:

- Grabar.
- Importar.
- Omitir.
- Reproducir si existe.

## Paso 4 — Catálogo teatral extendido

Sección expandible:

```text
Mostrar catálogo teatral extendido
```

Incluye:

- Alegre
- Eufórica
- Melancólica
- Nerviosa
- Asustada
- Sorprendida
- Dudosa
- Cansada
- Suplicante
- Autoritaria
- Irónica
- Sarcástica
- Misteriosa
- Solemne
- Heroica
- Dramática
- Tensa
- Apurada
- Confundida
- Tierna
- Fría
- Burlona
- Desconfiada
- Arrepentida
- Vulnerable
- Esperanzada
- Resignada
- Amenazante
- Desafiante
- Insinuante no explícita

## Paso 5 — Prueba generada

Componentes:

- TextBox editable.
- Placeholder/frase por defecto.
- Selector de tono de referencia.
- Botón `Generar prueba con esta voz`.
- Botón `Reproducir última prueba`.

## Paso 6 — Guardar

Resumen:

- nombre;
- cantidad de muestras;
- neutral presente;
- tonos opcionales agregados;
- ubicación de almacenamiento.

## Nota honesta

Mostrar:

```text
Los tonos son muestras de referencia. Ayudan a orientar la lectura generada, pero el resultado puede variar según el motor de voz.
```

## Tests recomendados

- `VoiceRegistrationWizardSourceTest`
- `VoiceWizardRequiresNeutralSampleSourceTest`
- `VoiceWizardIncludesExtendedToneCatalogSourceTest`
- `VoiceWizardNoCoquiVisibleSourceTest`

## Criterios de aceptación

- El wizard guía al usuario.
- Neutral es obligatorio.
- Tonos extendidos son opcionales.
- No se promete emoción garantizada.
- No aparece `Coqui` en UI.


## Ajuste agregado: flujo de grabación por tono

Al presionar `Grabar` en cualquier tono:

1. La app muestra la frase guía del tono.
2. El usuario lee esa frase.
3. La app permite `Cancelar`, `Detener grabación` y `Guardar muestra`.
4. Si cancela, no se modifica la muestra existente.
5. Si guarda, la muestra queda asociada al tono correspondiente.
6. Luego puede reproducir, reemplazar, descargar o eliminar esa muestra.

Este flujo aplica a la muestra neutral y a todos los tonos del catálogo teatral extendido.


## Scaffolding implementado en T121-V04

- `BuildVoiceRegistrationWizardPlanUseCase` construye el plan del wizard.
- `VoiceRegistrationWizardPlan` contiene textos visibles, neutral, tonos recomendados y tonos teatrales.
- `VoiceToneRecordingPrompt` enlaza cada tono con su frase guía.
- `BuildVoiceToneRecordingPlanUseCase` prepara una grabación concreta por tono.
- `VoiceToneRecordingPlan` declara etiquetas de Cancelar, Detener grabación y Guardar muestra, y el contrato `cancelKeepsPreviousSample`.
