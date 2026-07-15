# Memoria — Tanda 10B

Se implementó importación de muestras de voz como assets portables `VOICE_SAMPLE`.

El objetivo fue dejar la base técnica para usar voz propia o autorizada sin incluir todavía archivos estáticos pesados ni grabación real. Los audios subidos por el usuario sirven como muestras manuales, pero no se incrustan en el repositorio.

Piezas clave:

```text
VoiceSampleImportRequest
VoiceSampleImportResult
VoiceSampleRepository
ImportVoiceSampleUseCase
LocalVoiceSampleFileRepository
```

La UI expone:

```text
Menú Voz → Importar muestra para Mi voz…
Toolbar → Importar muestra
Workspace Voces → Importar muestra para Mi voz…
```

La muestra queda en:

```text
voices/samples/
```

Se registra en el catálogo de assets y se enlaza a `VoiceProfile.sampleAssetId`.

Reglas preservadas:

```text
- no rutas absolutas;
- no binarios en JSON;
- consentimiento para voces autorizadas/importadas;
- repositorio fuente sin audios personales.
```
