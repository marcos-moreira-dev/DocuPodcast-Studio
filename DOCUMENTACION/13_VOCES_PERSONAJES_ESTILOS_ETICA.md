# 13 — Voces, personajes, estilos y ética

DocuPodcast debe contemplar voces prediseñadas, voz propia y voces autorizadas.

## Tipos de voz

```text
PREDEFINED
OWN
AUTHORIZED
IMPORTED
```

## VoiceProfile

```text
id
name
type
engine
language
sampleAssetId
modelRef
qualityPreset
consentInfo
notes
```

## CharacterProfile

Un personaje no es lo mismo que una voz. Un personaje puede apuntar a una voz y a un estilo base.

```text
id
name
voiceProfileId
defaultStyleId
visualColor
notes
```

## Estilos

```text
NEUTRAL
SERIOUS
HAPPY
SAD
ANGRY
SURPRISED
DRAMATIC
SOFT
```

Pero la UI debe depender de capacidades reales del motor.

## Ética

La app no debe promover clonar voces sin permiso. Debe explicar voces propias/autorizadas. Una voz de una amiga o amigo debe tener autorización.

## Motor

XTTS u otro motor puede usar muestras de voz; el control emocional puede no ser explícito. Por eso los estilos se modelan como intención de interpretación, no promesa garantizada.
