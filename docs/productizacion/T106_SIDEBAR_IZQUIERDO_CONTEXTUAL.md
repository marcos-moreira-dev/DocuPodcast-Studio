# T106 — Sidebar izquierdo contextual

## Objetivo

Implementar el inspector y configurador de la oración seleccionada.

## Función

El Sidebar izquierdo no es menú general. Es contexto de la oración seleccionada.

## Contenido

```text
Fragmento seleccionado
Audio / narración
Imagen
Capas
```

## Audio / narración

Origen:

```text
Voz IA
Audio del computador
```

Si Voz IA:

```text
combo de voces ya configuradas
emoción / estilo
velocidad de esa oración
generar / regenerar
```

Si Audio del computador:

```text
elegir archivo de audio
extraer audio desde video
quitar audio
```

## Imagen

```text
elegir imagen
quitar imagen
abrir carpeta de la imagen
```

## Reglas

- El combobox de voz solo muestra voces ya registradas.
- Crear/grabar/importar voces nuevas pertenece al workspace Voces.
- Audio del computador es clip genérico del usuario.
- Abrir carpeta de imagen pertenece al sidebar, no al rail.

## Criterio de aceptación

- Configura la oración seleccionada.
- No duplica el Ribbon.
- No usa labels truncados.
