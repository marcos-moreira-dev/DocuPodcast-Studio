# Paquete teatral oficial DocuPodcast

Versión vigente: `schemaVersion: 2`, `grammarVersion: theatre-v2`.

```text
PROYECTO_TEATRO/
├── obra.teatro.md
├── docupodcast-theatre.json
└── assets/
    ├── personajes/  objetos/  fondos/  mapas/
    ├── audio/       frames/   frames_intermedios/
    ├── voces/       referencias/
```

Carpeta y ZIP pasan por el mismo resolvedor, scanner, parser y validador. Un ZIP puede contener los archivos en su raíz o dentro de una única carpeta raíz. Se rechazan traversal, rutas absolutas, entradas duplicadas y enlaces simbólicos.

El manifiesto contiene integridad técnica, no duplica la semántica Markdown:

```json
{
  "schemaVersion": 2,
  "grammarVersion": "theatre-v2",
  "grammar": "obra.teatro.md",
  "packageId": "obra-id",
  "packageVersion": "1.0.0",
  "assets": [{
    "path": "assets/frames/INTERVENCION-7.png",
    "logicalId": "frame:INTERVENCION-7",
    "kind": "INTERVENTION_IMAGE",
    "sha256": "64-hex",
    "size": 1234,
    "interventionId": "INTERVENCION-7"
  }]
}
```

Kinds: `CHARACTER_IMAGE`, `OBJECT_IMAGE`, `BACKDROP`, `SPATIAL_MAP`, `INTERVENTION_IMAGE`, `INTERMEDIATE_FRAME`, `HUMAN_AUDIO`, `VOICE_SAMPLE`, `VIDEO`, `OTHER`.

Ausencia opcional significa propiedad no configurada y no genera warning. Una ruta declarada pero inexistente, hash/tamaño incorrecto o binding inválido bloquea el commit. La importación valida antes de reemplazar el agregado y publica assets desde staging solo al final.

## Instrucciones para IA generadora de paquetes

Usa la estructura exacta, calcula hash y tamaño después de crear cada archivo y declara únicamente archivos reales. No inventes placeholders. Conserva IDs y usa rutas relativas. Consulta `plantilla-gramatica-teatral.md` para la semántica.

