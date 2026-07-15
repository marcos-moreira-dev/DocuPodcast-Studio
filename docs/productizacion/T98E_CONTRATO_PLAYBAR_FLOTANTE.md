# T98E — Contrato del panel flotante de escucha

El panel flotante de escucha es la superficie primaria para abrir un documento y escucharlo rápido. Debe sentirse como control superpuesto moderno, no como bloque técnico incrustado.

## Ubicación

Aproximadamente 20–30 px debajo del Ribbon. Debajo del panel empieza el área de lectura/hoja.

Debe flotar visualmente sobre el workspace, sin bloquear la lectura más de lo necesario.

## Apariencia

- Fondo blanco semitransparente o blanquinoso.
- Efecto de desenfoque/difuminado del fondo si es posible.
- Bordes suaves.
- Sombra ligera.
- Botones limpios.
- Iconos vectoriales con tooltips.

## Botones

Debe contener una acción principal visible:

```text
Escuchar documento
```

Y controles con iconos:

```text
Pausar
Reanudar
Detener
Reproducir oración seleccionada
```

Los iconos deben tener tooltips claros. No usar emojis.

## Automatización de precondiciones

Si falta proyecto:

```text
Escuchar documento
→ mostrar confirmación: “Para escuchar este documento se creará un proyecto DocuPodcast.”
→ Aceptar / Cancelar
→ elegir carpeta
→ crear proyecto contenedor
→ continuar
```

Si falta audio:

- generar/preparar el audio que haga falta;
- continuar con lo posible;
- usar StatusBar para avisos no bloqueantes.

Si falta motor:

- mostrar message box accionable;
- ofrecer abrir Configuración o preparar motor;
- no fallar con error técnico crudo.

## Mensajes

Evitar jerga técnica visible:

No mostrar:

- manifest;
- job;
- stdout;
- stderr;
- gateway;
- command template.

Usar:

- Preparando audio;
- Generando lectura;
- Falta configurar motor de voz;
- Audio listo;
- Reproduciendo oración.
