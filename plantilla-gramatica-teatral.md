# Título de la obra

> DocuPodcast Teatro Grammar v2
> grammarVersion: theatre-v2

Esta es la plantilla oficial descargable. El paquete contiene este archivo, `docupodcast-theatre.json` y, únicamente cuando existan, archivos bajo `assets/`.

## Personajes

- personaje: Narrador | id=NARRADOR | aliases=CRONISTA | voz=narrador_neutro | nota=voz externa
- personaje: Concha | id=CONCHA | aliases=OVEJA CANTORA | voz=mujer_35_ecuador
- assets/personajes/CONCHA/frontal.png | escena=SCN-PLAZA | angulo=frontal

## Objetos

- objeto: Sombrero | id=SOMBRERO | nota=utilería portable

## Acto: Acto primero | id=ACT-PRIMERO

### Escena: Plaza | id=SCN-PLAZA

> mapa_espacial=assets/mapas/plaza.png | fondo_escenario=assets/fondos/plaza.png

Concha: Ya no pedimos permiso.
> id=INTERVENCION-1 | hereda=ninguna | presentes=CONCHA@frente_centro,NARRADOR@fondo_izquierda | tono=HEROIC | microexpresion=determinacion_serena | emoji=✊ | plano=CERCA_CENTRO_NIVEL | objetos=SOMBRERO@portado:CONCHA

Narrador: La plaza guarda silencio.
> id=INTERVENCION-2 | hereda=anterior | interaccion=CONCHA | miradas=NARRADOR@CONCHA | eventos=MOVE:NARRADOR@centro_izquierda

## Sintaxis v2 estricta

- IDs: `[A-Z][A-Z0-9_-]{0,63}`. Intervenciones: `INTERVENCION-N`.
- Zonas: `fondo_izquierda`, `fondo_centro`, `fondo_derecha`, `centro_izquierda`, `centro`, `centro_derecha`, `frente_izquierda`, `frente_centro`, `frente_derecha`.
- Herencia: `hereda=anterior`, `hereda=ninguna` o `hereda=INTERVENCION-N` anterior.
- Estado: `presentes=ID@zona`, `ausentes=ID`, `orientaciones=ID@valor`, `miradas=ID@destino`, `variantes=ID@valor`, `vestuarios=ID@valor`.
- Objetos: `objetos=OBJ@zona`, `OBJ@portado:PERSONAJE` o `OBJ@ausente`.
- Eventos separados por `;`: `ENTER:PERSONAJE@zona`, `EXIT:PERSONAJE`, `MOVE:PERSONAJE@zona`, `TAKE:PERSONAJE:OBJ`, `CARRY:PERSONAJE:OBJ`, `DROP:PERSONAJE:OBJ@zona`, `GIVE:PERSONAJE:OBJ:DESTINO`.
- Otras propiedades: `origen`, `destino`, `interaccion`, `tono`, `microexpresion`, `emoji`, `plano`, `voces`, `imagen`, `fondo`.

## INSTRUCCIONES PARA IA GENERADORA DE PAQUETES

1. Crea exactamente `obra.teatro.md` y `docupodcast-theatre.json` schema 2.
2. Usa IDs estables; no uses el nombre visible como vínculo implícito.
3. Usa rutas relativas bajo `assets/`; nunca rutas absolutas, URL, `.` ni `..`.
4. Declara cada archivo existente en el manifiesto con `path`, `logicalId`, `kind`, `sha256` y `size` reales.
5. Si una imagen, frame, audio, mapa o fondo opcional no existe, no lo declares. No inventes rutas ni crees archivos vacíos.
6. No infieras semántica: expresa presencia, zona, interlocutor, objeto y eventos explícitamente.
7. Empaca los archivos en la raíz del ZIP o en una única carpeta raíz. Carpeta y ZIP significan lo mismo.
