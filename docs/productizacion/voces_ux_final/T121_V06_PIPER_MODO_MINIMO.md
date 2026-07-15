# T121-V06 — Piper modo mínimo

## Objetivo

Reducir Piper a su mínima expresión en la vista Voces. Piper no es protagonista del producto; es modo local simple/intermedio.

## Nombre visible

Usar:

```text
Voz local simple
```

No mostrar excesiva explicación técnica.

## UI para Piper

Si Piper está activo:

```text
Motor activo: Voz local simple
Modelo instalado: <nombre>

[Probar lectura]
[Reemplazar modelo]
[Eliminar modelo importado]
```

Si no hay modelo:

```text
No hay modelo local importado.
[Importar modelo]
```

## Qué NO mostrar

- Grabar muestra.
- Importar muestra humana.
- Tonos de referencia.
- Emociones.
- Catálogo teatral.
- Voz Juanito/María por muestra.
- Wizard avanzado.
- Clonación.

## Acciones

- Importar modelo.
- Reemplazar modelo.
- Eliminar modelo importado.
- Probar lectura simple.

## Reglas

- Eliminar modelo deja a la app sin Piper activo.
- Reemplazar modelo sustituye el actual.
- Si hay un solo modelo, no se crea biblioteca compleja.
- En Documento, si Piper está activo, no se muestran tonos avanzados.

## Tests recomendados

- `PiperMinimalVoiceViewSourceTest`
- `PiperDoesNotShowAdvancedSamplesSourceTest`
- `PiperDoesNotShowToneCatalogSourceTest`
- `PiperReplaceModelUseCaseTest`

## Criterios de aceptación

- Piper queda simple.
- No compite con Voz IA avanzada.
- No muestra capacidades falsas.
