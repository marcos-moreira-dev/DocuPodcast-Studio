# T121-V07 — Fallback de tonos faltantes en Documento

Base: T121-V06 con diagnóstico local rojo por imports faltantes en `DocuPodcastShellViewModel`.

## Objetivo

Conectar el sidebar de Documento con el modelo real de tonos por muestra sin exponer nombres técnicos de motores en la interfaz gráfica.

## Implementado

- Se corrige la compilación de V06 agregando los imports faltantes de grabación en `DocuPodcastShellViewModel`.
- `DocumentAudioNarrationPanel` reemplaza el selector legacy de `PerformanceStyle` por `ComboBox<VoiceReferenceTone>`.
- El sidebar de Documento muestra **Tono de referencia** y estado humano del tono resuelto.
- `DocuPodcastShellViewModel.assignVoiceToneToSelectedDocumentRange(...)` resuelve el tono con `ResolveVoiceToneReferenceUseCase`.
- Si la muestra exacta existe, se asigna ese tono.
- Si falta la muestra exacta pero existe neutral, se guarda la asignación usando **fallback neutral** y se informa al usuario.
- Si falta neutral, se bloquea la asignación avanzada y se muestra el motivo.
- `VoiceReferenceTone.layerTargetId()` y `fromLayerTargetId(...)` permiten guardar tonos como targets estables de capa documental.
- `NarrativeLayerTargetResolver` acepta tonos de referencia como targets de `NarrativeLayerKind.EMOTION`.
- `InspectProjectIntegrityUseCase` acepta estilos legacy o tonos nuevos para capas de intención/emoción.
- La UI sigue usando nombres amigables: **Voz IA avanzada**, **Voz local simple** y **Modo de prueba**. No se introducen nombres técnicos de motores en strings visibles.

## Tests

- `VoiceReferenceToneLayerTargetTest`
- `DocumentVoiceToneFallbackT121V07SourceTest`
- Actualización de `DocumentSidebarContextualT106SourceTest`
- Se mantiene `VoiceLocalSimpleT121V06SourceTest` como guardarraíl de nombres técnicos fuera de strings visibles de presentación.

## Validación en entorno ChatGPT

- `javac --release 21` de `domain + application + infrastructure`: OK.
- Compilación focal de tests nuevos/modificados con stubs JUnit: OK.
- Ejecución reflexiva de tests fuente nuevos/modificados: OK.
- Ejecución de guardarraíl de strings visibles de V06: OK.
- Maven completo no se ejecutó por falta de `mvn` en el entorno.
