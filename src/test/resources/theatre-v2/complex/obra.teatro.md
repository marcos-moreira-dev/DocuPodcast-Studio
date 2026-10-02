# La plaza determinística

> DocuPodcast Teatro Grammar v2
> grammarVersion: theatre-v2

## Personajes
- personaje: Narrador | id=NARRADOR | voz=voz_narrador
- personaje: Concha | id=CONCHA | aliases=OVEJA | voz=voz_concha
- personaje: Eloy | id=ELOY | voz=voz_eloy
- personaje: Guardia | id=GUARDIA | voz=voz_guardia
- personaje: Coro | id=CORO | voz=voz_coro
- assets/personajes/CONCHA/frontal.png | escena=SCN-PLAZA | angulo=frontal

## Objetos
- objeto: Sombrero | id=SOMBRERO
- objeto: Carta | id=CARTA
- assets/objetos/SOMBRERO/frontal.png | escena=SCN-PLAZA | angulo=frontal

## Acto: Acto primero
### Escena: Plaza
> fondo_escenario=assets/fondos/plaza.png | mapa_espacial=assets/mapas/plaza.png
Concha: Ya no pedimos permiso.
> id=INTERVENCION-1 | hereda=ninguna | presentes=CONCHA@frente_centro,ELOY@centro,GUARDIA@fondo_derecha | objetos=SOMBRERO@portado:CONCHA,CARTA@centro_izquierda | tono=HEROIC | microexpresion=determinacion | emoji=✊ | plano=CERCA_CENTRO_NIVEL | imagen=assets/frames/INTERVENCION-1.png
Eloy: Entonces avancemos.
> id=INTERVENCION-2 | hereda=anterior | interaccion=CONCHA | presentes=ELOY@frente_centro | miradas=ELOY@CONCHA | eventos=GIVE:CONCHA:SOMBRERO:ELOY
Guardia: Alto allí.
> id=INTERVENCION-3 | hereda=anterior | presentes=GUARDIA@frente_derecha | orientaciones=GUARDIA@izquierda | vestuarios=GUARDIA@uniforme_gala | eventos=MOVE:GUARDIA@frente_derecha

### Escena: Salón
Narrador: El salón recibe a todos.
> id=INTERVENCION-4 | hereda=ninguna | presentes=NARRADOR@fondo_izquierda,CONCHA@centro_izquierda,ELOY@centro_derecha | ausentes=GUARDIA | tono=SOLEMN
Coro: La patria despierta.
> id=INTERVENCION-5 | hereda=anterior | presentes=CORO@fondo_centro | voces=CONCHA,ELOY,CORO | variantes=CONCHA@tarqui | microexpresion=esperanza | emoji=🌅

### Escena: Camino
Concha: La carta seguirá con nosotros.
> id=INTERVENCION-6 | hereda=ninguna | presentes=CONCHA@frente_izquierda,ELOY@frente_derecha | objetos=CARTA@portado:CONCHA | eventos=TAKE:CONCHA:CARTA
Eloy: Hasta el final.
> id=INTERVENCION-7 | hereda=anterior | interaccion=CONCHA | eventos=DROP:CONCHA:CARTA@centro
