# VOICE-UX-POLISH1A — limpieza operativa de Vista Voces

Estado: implementada sobre `VOICE-PLAN-REFERENCIAS1` y `MOTOR-GPU-SMOKE1-HF2` verde.

## Propósito

Esta tanda limpia la Vista Voces sin introducir todavía el wizard de grabación. La vista debe servir para administrar voces y muestras de referencia, no para decorar la pantalla ni para duplicar estados que ya viven en Configuración.

## Cambios de UX aplicados

- Inicio conserva el encabezado `Voces disponibles`, el título `Inicio` y el microcopy: `Voces administra voces y muestras. Documento asigna voces a fragmentos.`
- Se elimina el botón `Ver estado de voces`, porque no aportaba una operación clara al flujo principal.
- Se elimina el bloque `Resumen de operación`; la vista ya muestra motor/estado donde corresponde.
- La lista `Voces creadas` queda como elemento operativo principal del inicio.
- Las filas de voz muestran nombre, estado y tipo humano (`Voz simple`, `Voz avanzada`, `Modo de prueba`).
- Las emociones/tonos aparecen como etiquetas solo cuando esa voz tiene muestras registradas.
- No se muestran etiquetas de tonos faltantes.

## Configurar motor

- Se mantiene el encabezado `Motor activo`, el título `Configurar motor` y los ComboBox de motor/dispositivo.
- Se corrige el microcopy de estado de motor para que tenga color visible y no se funda con el fondo.
- Se retira la región `Estado y validación` del flujo principal.
- `Abrir configuración completa` usa botón primario, porque abre una acción importante y compartida.
- El estado `Sin prueba generada` queda como texto de estado, no como botón visual.

## Tonos y ComboBox

Se agrega `VoiceToneLabelPolicy` para fijar que los ComboBox de tonos/emociones muestren solo el nombre humano simple:

- `Neutral`
- `Feliz`
- `Enojada`
- `Triste`
- `Sorprendida`

No deben volver labels como:

- `Tonos recomendados · Feliz`
- `Catálogo teatral extendido · Dramática`
- códigos `STY-*`
- frases explicativas dentro del valor seleccionado.

## Contrato operativo de voces

Las muestras de `María · Neutral`, `María · Enojada`, etc. son referencias que Voz IA avanzada usará para sintetizar texto nuevo. No son clips fijos para repetir siempre igual.

Documento conserva dos caminos separados:

1. Voz IA generada desde una voz/tono registrados.
2. Audio del computador elegido o extraído por el usuario.

## Fuera de alcance

Esta tanda no implementa todavía:

- subvista `Nueva voz`;
- grabación desde micrófono;
- detener/reproducir/eliminar grabación;
- guardar voz solo con Neutral;
- sincronización transversal posterior al registro.

Eso queda para `VOICE-REGISTRATION-WIZARD1` y `VOICE-LIBRARY-SYNC1`.

## Guardarraíles

Se agregan y actualizan source tests para proteger:

- que no vuelva `Ver estado de voces`;
- que no vuelva `Resumen de operación`;
- que no vuelva `homeOperationalSummary`;
- que los ComboBox usen nombres simples de tonos;
- que la lista de voces muestre solo tonos registrados;
- que el estado de prueba/motor no tenga aspecto de botón ni color ilegible.
