# Entrada teatral y autoconfiguración

DocuPodcast distingue tres artefactos:

1. **Gramática teatral Markdown**: declara parlamentos, reparto, voces, actos, escenas y sus referencias. Se abre mediante **Teatro → Importar gramática**.
2. **Carpeta vinculada de recursos**: tiene `docupodcast-theatre.json` y un contrato de sincronización propio. Véase [formato de carpeta](theatre-package-format.md). No sustituye al guion.
3. **Proyecto guardado**: `.docupodcast.json`, instantánea documental, guion de narración y recursos copiados. Es el resultado editable, no un Markdown genérico.

Un repositorio de dirección humana, sus instrucciones de onboarding, documentos históricos y variantes artísticas **no son automáticamente un proyecto DocuPodcast**. Su adaptación debe declarar las decisiones escénicas en el contrato de entrada. No se deduce autoridad de un nombre de carpeta ni se ejecutan instrucciones presentes en esos documentos.

## Ejemplo mínimo recomendado

```markdown
# Obra de ejemplo
> DocuPodcast Teatro Grammar v2
> grammarVersion: theatre-v2

## Personajes
- personaje: ANA | id=CHR-ANA | voz=VOC-NARRATOR
- assets/personajes/ana/frontal.png | angulo=frontal
- personaje: LUIS | id=CHR-LUIS | voz=VOC-NARRATOR
- assets/personajes/luis/frontal.png | angulo=frontal

## Objetos
- objeto: LLAVE | id=OBJ-LLAVE | nota=Llave del despacho
- assets/objetos/llave.png

## Acto: Primero | id=ACT-PRIMERO
### Escena: Despacho | id=SCN-DESPACHO
> fondo_escenario=assets/fondos/despacho.png

ANA: Encontré la llave. ¿Abrimos la puerta?
> id=INTERVENCION-1 | origen=centro | interaccion=LUIS | tono=SERIOUS
LUIS: Adelante.
> id=INTERVENCION-2 | origen=derecha | imagen=assets/personajes/luis/frontal.png
```

Cada parlamento ocupa una línea. La línea `>` inmediatamente posterior contiene su metadata; la metadata de escena se declara inmediatamente después del encabezado. Las imágenes de un perfil se enumeran justo después de su declaración, sin una línea vacía intermedia. Las rutas de esas listas no admiten espacios: use nombres de archivo portables.

Se recomienda declarar IDs únicos e inmutables. En v1, si no se declara ID de intervención, se obtiene de su orden; insertar un parlamento puede cambiar los siguientes IDs. Los IDs explícitos de intervención admitidos son `INTERVENCION-<número>` o el número solo. La gramática v1 y los planes históricos conservan compatibilidad.

## Qué configura la importación

| Declaración | Resultado |
|---|---|
| Actos y escenas | Estructura, notas y límites inicial/final de cada escena |
| Personajes y alias | Fichas e identidad del hablante |
| `voz` | Asignación a voz existente por ID, nombre exacto o alias del preset oficial |
| Imágenes de personajes/objetos | Recursos del proyecto con identificadores persistentes y vista/escena asociadas |
| `mapa_espacial`, `fondo_escenario`, `fondo` | Mapa, fondo de escena o fondo de intervención |
| `origen`, `destino`, `interaccion` | Ubicación y relaciones de la intervención |
| `tono`, `plano`, `aplicar_plano`, `contexto_ia` | Emoción, cámara y contexto estructurado |
| `voces` | Participantes del coro por nombre/ID de personaje |
| Estado v2: `presentes`, `ausentes`, `objetos`, `eventos`, `hereda` | Estado escénico y continuidad declarada |
| Estado v2: `orientaciones`, `miradas`, `variantes`, `vestuarios` | Propiedades de personajes para la representación escénica |

La importación configura referencias y asignaciones; no sintetiza voces, mezclas corales ni vídeo por sí sola. Una voz que no exista en la biblioteca se informa como no resuelta. No se inventa un modelo de voz ni se descarga automáticamente.

## Configuración complementaria de voces

Opcionalmente, junto al Markdown se admite `config/voces.csv` con esta cabecera:

```csv
personaje_id,voz_alias_o_mezcla,modo,sintetizar
ANA,hombre_maduro_narrativo,SINGLE,true
LUIS,hombre_20_idealista_ecuador_dialogo,SINGLE,true
CORO,hombre_maduro_narrativo;hombre_20_idealista_ecuador_dialogo,MIX,true
```

Todos esos personajes deben estar declarados en el Markdown. `SINGLE` completa una voz no declarada; ante conflicto prevalece el Markdown y se informa. `MIX` convierte los alias de voz en los personajes asociados y configura sus intervenciones corales si no tienen ya `voces` explícitas. Cada alias del coro debe corresponder a un personaje con esa voz.

`sintetizar=false` no crea una asignación de voz desde el CSV. **No es una instrucción para eliminar parlamentos ni una implementación de acotaciones silenciosas.** Otros CSV/JSON de producción no se interpretan por sus nombres: necesitan un contrato o adaptador definido. Esto evita convertir documentos de onboarding en configuración accidental.

## Invariantes verificadas

- Un parlamento explícito produce un bloque y un segmento narrable, aunque contenga varias oraciones o dos puntos.
- El nombre del personaje y las instrucciones técnicas no se añaden a su texto hablado.
- La vinculación intervención ↔ segmento ↔ bloque usa el ID declarado, no la posición de un párrafo del Markdown genérico.
- Los comentarios, encabezados, fichas, rutas y bloques de código no se convierten en parlamentos.
- Las imágenes se resuelven respecto al Markdown original, se deduplican por ruta real y se copian al proyecto. Una referencia inexistente se informa sin perder los diálogos.
- Rutas absolutas, escapes `../` y enlaces que salgan de la raíz de la obra se rechazan.
- Guardar, reabrir y reconstruir la lectura conserva texto, IDs, imágenes, voces y límites de escena.
- Markdown documental y gramática de vídeo narrativo conservan su ruta independiente.

## Necesidades de montaje que requieren decisiones adicionales

La referencia de producción humana puede incluir duración de silencios, cues de iluminación, sonido instrumental, coreografía, seguridad de montaje, cambios de mano y autoridad entre versiones. La existencia de esos textos no significa que el motor los ejecute.

La continuidad de objetos, portadores, entradas/salidas y vestuario dispone de estado v2, pero debe declararse allí. Los cues de luz/sonido con reloj propio y las acotaciones silenciosas necesitan ampliar o verificar su contrato de ejecución antes de prometer equivalencia con una puesta en escena humana. Las notas de seguridad y dirección se conservan como documentación, sin convertirlas en órdenes de render.

## Validación

`TheatreGrammarImportIntegrationTest` prueba importación, recursos, alias oficiales, coros, IDs explícitos no consecutivos, persistencia y reconstrucción de lectura. La prueba de producción local se activa con `-Dtheatre.audit.source=<Markdown>` y escribe una copia fuera de la obra con `-Dtheatre.audit.output=<proyecto>`. No incorpora contenido de una obra específica al código ni a los fixtures del repositorio.
