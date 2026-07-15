# 33 — Contratos de datos, borrador

## NarrationSegment

```json
{
  "id": "SEG-001",
  "title": "Introducción",
  "text": "Bienvenidos...",
  "sourceBlockIds": ["BLK-001"],
  "characterId": "CHR-001",
  "voiceProfileId": "VOC-001",
  "styleId": "STY-NEUTRAL",
  "status": "PENDING"
}
```

## StoryboardBinding

```json
{
  "id": "STB-001",
  "segmentId": "SEG-001",
  "imageAssetId": "IMG-001",
  "displayMode": "FIT_CONTAIN",
  "caption": "Imagen de apoyo"
}
```

## AudioClipReference

```json
{
  "id": "AUD-001",
  "segmentId": "SEG-001",
  "assetId": "AUDIO-ASSET-001",
  "durationSeconds": 31.2,
  "jobId": "JOB-001"
}
```

## PlaybackCue

```json
{
  "segmentId": "SEG-001",
  "audioClipId": "AUD-001",
  "imageAssetId": "IMG-001",
  "startSeconds": 0.0,
  "endSeconds": 31.2
}
```

Estos contratos son borradores para orientar implementación; no son todavía formato congelado.
