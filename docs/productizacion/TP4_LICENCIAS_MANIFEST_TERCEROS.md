# TP4 — Licencias y manifest de terceros

TP4 introduce un inventario auditable para preparar la distribución de DocuPodcast Studio sin asumir derechos de redistribución que no han sido confirmados.

## Contrato

- FFmpeg vive en `tools/ffmpeg/bin`, pero su redistribución depende del build concreto y sus términos GPL/LGPL.
- Piper vive en `tools/piper`; el binario y las voces pueden tener condiciones distintas.
- Coqui/XTTS, modelos y voces de referencia se tratan como user-provided o preparación local hasta revisión legal.
- Python portable para XTTS requiere conservar avisos de Python y dependencias si se empaqueta.
- JavaFX y dependencias Maven deben quedar inventariadas en el paquete final.
- Los ejemplos internos de T113 son assets propios del proyecto y pueden viajar como demos.

## Código

Se agregan:

- `ThirdPartyComponentKind`
- `ThirdPartyComponent`
- `ThirdPartyLicenseManifest`
- `BuildThirdPartyLicenseManifestUseCase`

## Script

`scripts/30-generar-manifest-terceros.bat` genera:

- `target/legal/THIRD_PARTY_MANIFEST.md`
- `dist/legal/THIRD_PARTY_MANIFEST.md`

El manifest es evidencia operativa, no permiso legal automático.


Nota de guardarraíl: no asumir derechos de redistribución sin evidencia legal concreta.
