# Gramatica de Teatro para DocuPodcast Studio

Especificacion de formato Markdown para definir obras teatrales con personajes,
objetos, escenas, intervenciones dialogadas y metadatos espaciales. Disenada
para ser procesada por `TheatreGrammarMarkdownParser` y consumida por el
sistema de proyeccion teatral del Studio.

## Encabezados

```
# TITULO DE LA OBRA

## Acto: NOMBRE DEL ACTO

### Escena: NOMBRE DE LA ESCENA
> texto_inicio=5 | texto_fin=10 | mapa_espacial=fragmentos/mapa.png | fondo_escenario=fondos/hangar.png
```

- `# ` define el titulo de la obra. Solo se toma la primera ocurrencia.
- `## Acto: ...` define un acto. El nombre puede contener cualquier caracter.
- `### Escena: ...` define una escena. Pertenece al acto anterior inmediato.
- La linea `>` inmediatamente posterior a una escena puede declarar:
  `texto_inicio`, `texto_fin`, `mapa_espacial` y `fondo_escenario`.
- `texto_inicio`/`texto_fin` apuntan a los "Texto N" del DOCX fuente, no a
  lineas del Markdown.
- `fondo_escenario` define el fondo base de la escena si el asset existe en
  el proyecto.

## Perfiles de personajes

```
- Personaje: NOMBRE | nota=DESCRIPCION | foto=ARCHIVO | imagen=ARCHIVO
```

- `NOMBRE` (obligatorio): identificador del personaje. Se usa en las lineas de dialogo.
- `nota=` (opcional): descripcion textual del personaje.
- `foto=` o `imagen=` (opcional): archivo de imagen de referencia.

## Perfiles de objetos

```
- Objeto: NOMBRE | escena=ESCENA | imagen=ARCHIVO | nota=DESCRIPCION
```

Misma sintaxis que personajes. `foto=` e `imagen=` tambien son validos.

## Intervenciones (dialogo)

```
PERSONAJE: Texto del dialogo.
> origen=ubicacion_origen | destino=ubicacion_destino | interaccion=OBJETIVO | plano=CERCA_CENTRO_NIVEL
```

- `PERSONAJE` debe coincidir con el nombre definido en `- Personaje:`.
- `Texto del dialogo` puede contener multiples oraciones.
- La linea `>` es opcional y se coloca inmediatamente despues del dialogo.
  Varias lineas `>` consecutivas se acumulan en la misma intervencion.

### Claves de metadatos espaciales

| Clave | Descripcion | Ejemplo |
|-------|-------------|---------|
| `origen` | Posicion inicial del personaje en el mapa espacial | `origen=fondo centro` |
| `destino` | Posicion final o hacia donde se dirige la accion | `destino=centro derecha` |
| `interaccion` | Identificador del objetivo de la interaccion | `interaccion=TENIENTE` |
| `imagen` | Archivo de imagen para fragmentos visuales | `imagen=fragmentos/fragmento_03_revision.png` |
| `plano` | Tipo de plano/camara teatral desde esta intervencion hacia adelante | `plano=CERCA_CENTRO_NIVEL` |
| `aplicar_plano` | Incluye o excluye el plano del contexto IA del fragmento | `aplicar_plano=false` |
| `fondo` | Fondo/telon heredado desde esta intervencion hacia adelante | `fondo=fondos/montana.png` |
| `quitar_fondo` | Corta la herencia de fondo desde esta intervencion | `quitar_fondo=true` |
| `contexto_ia` | Texto contextual editable para la generacion IA del fragmento | `contexto_ia=Camara fija; mantener continuidad de personajes.` |

Las claves se separan con `|`. El orden no importa.

Tipos de plano embebidos disponibles:

```
CERCA_CENTRO_ALTO, CERCA_CENTRO_BAJO, CERCA_CENTRO_NIVEL
CERCA_DERECHA_ALTO, CERCA_DERECHA_BAJO, CERCA_DERECHA_NIVEL
CERCA_IZQUIERDA_ALTO, CERCA_IZQUIERDA_BAJO, CERCA_IZQUIERDA_NIVEL
PANORAMICA_CENTRO_ALTO, PANORAMICA_CENTRO_BAJO, PANORAMICA_CENTRO_NIVEL
PANORAMICA_DERECHA_ALTO, PANORAMICA_DERECHA_BAJO, PANORAMICA_DERECHA_NIVEL
PANORAMICA_IZQUIERDA_ALTO, PANORAMICA_IZQUIERDA_BAJO, PANORAMICA_IZQUIERDA_NIVEL
```

`plano` y `fondo` son marcadores heredados: aplican desde la intervencion que
los declara hasta que otra intervencion los cambie. `quitar_fondo=true` o
`fondo=SIN_FONDO` deja sin fondo las intervenciones siguientes hasta un nuevo
`fondo=...`. Para portadas, creditos o laminas fuera del teatro, usa
`aplicar_plano=false`.

Posiciones reconocidas por el mapa espacial:

```
fondo derecha, fondo centro, fondo izquierda
centro derecha, centro, centro izquierda
frente derecha, frente centro, frente izquierda
hacia el publico, extra diegetico, diegetico
```

## Notas

```
notas: Texto de nota asociado a la escena o acto actual.
```

Se puede agregar en cualquier momento. Si hay una escena activa, la nota se
asocia a ella. Si no, al acto activo.

## Enlaces multimedia

```
![Descripcion](ruta/al/archivo.png)
```

Cualquier enlace Markdown en el documento se registra como referencia
multimedia. Sirve para asociar imagenes de mapa espacial, referencias
visuales o fragmentos.

## Procesamiento

`TheatreGrammarMarkdownParser` procesa el archivo de forma leniente:
- Las lineas que no coinciden con ningun patron se ignoran silenciosamente.
- Los perfiles pueden aparecer en cualquier orden dentro del documento.
- Las intervenciones se asocian a la escena activa en el momento de su lectura.
- Los metadatos espaciales se acumulan por intervencion.
- Los metadatos de escena se guardan en `ScenePlan` como rango DOCX y mapa.
- Los metadatos de plano, fondo y contexto se guardan por intervencion para
  alimentar la capa teatral, el sidecar semantico y la generacion IA.
- El resultado es un `ImportPlan` con titulo, actos, escenas, personajes,
  objetos, enlaces multimedia y plan de intervenciones.

## Ejemplo completo

```
# Mi obra

- Personaje: NARRADOR | nota=Voz principal
- Personaje: HEROE | nota=Protagonista

- Objeto: ESPADA | nota=Arma legendaria

## Acto: El inicio

### Escena: El bosque
> texto_inicio=5 | texto_fin=10 | mapa_espacial=fragmentos/mapa_bosque.png | fondo_escenario=fondos/bosque.png

NARRADOR: Era un bosque oscuro y tenebroso.
> origen=extra diegetico | destino=extra diegetico | plano=CERCA_CENTRO_NIVEL | contexto_ia=Camara fija, escenario teatral con telon de bosque.

HEROE: Hay alguien ahi?
> origen=centro | destino=centro derecha | interaccion=NARRADOR | plano=CERCA_DERECHA_NIVEL

NARRADOR: Fin de la escena.
> quitar_fondo=true | aplicar_plano=false
```

## Notas para agentes IA

- Usar nombres de personajes consistentes (mayusculas mantenidas).
- Las ubicaciones en `origen`/`destino` deben etiquetar puntos del mapa
  espacial de referencia.
- El importador reporta personaje desconocido, imagen no encontrada, rango
  fuera del DOCX, escena sin limites, posicion no reconocida y plano no
  reconocido.
- Los archivos de imagen referenciados deben existir en el proyecto para
  ser vinculados correctamente.
