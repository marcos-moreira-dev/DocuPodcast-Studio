# Tanda 6 - Voces Transversales

## Alcance
Esta tanda consolida las voces como capacidad compartida para Estudio, Narrativa y Teatro sin cambiar schema, persistencia ni `TheatreProjectLayer`.

La regla operativa queda asi: `VoiceLibrary`, `VoiceProfile`, `VoiceCapabilityPolicy`, `AudioEngineDescriptor` y `BuildVoiceAssignmentOptionsUseCase` gobiernan las opciones de voz. Teatro mantiene solo alias personaje-voz mediante `VoiceRoleAlias`.

## Contrato comun
- `VoiceAssignmentOption` es la opcion UI/aplicacion para asignar voz a un destino.
- Cada opcion expone `voiceId`, `displayName`, `availability`, `selectable`, `current`, `reservedByOtherTarget`, `status` y `detail`.
- `BuildVoiceAssignmentOptionsUseCase` construye opciones desde la biblioteca activa, el motor actual, las voces reservadas por otros destinos y la voz actual.
- Una voz actual sigue visible aunque el motor activo no pueda sintetizarla; si no es seleccionable, el UI no debe aceptarla como nueva confirmacion.
- Una voz reservada por otro personaje queda bloqueada salvo que sea la voz actual del mismo destino.
- `VoiceCapabilityPolicy` sigue siendo la fuente para readiness/capacidad de motor; los paneles no deben instanciar reglas paralelas.

## Flujo por modalidad
- Estudio/Documento: el panel de audio conserva su selector de voz, pero obtiene la lista desde `BuildVoiceAssignmentOptionsUseCase` y la politica desde `VoiceApplicationServices`.
- Narrativa: los planes de render mantienen las asignaciones de segmentos y la biblioteca comun; no hay schema nuevo.
- Teatro: `TheatreCharactersPanel` muestra opciones transversales, bloquea voces usadas por otros personajes y persiste solamente `VoiceRoleAlias`.
- Exportacion/readiness: sigue leyendo las asignaciones desde la proyeccion de audio; el cierre de variantes de exportacion queda para Tanda 9.

## Decisiones
- No se introduce motor teatral propio.
- No se cambia `.docupodcast` ni `TheatreProjectLayer`.
- No se rediseña el ribbon.
- Documento no cambia su UX: sigue mostrando `VoiceProfile`, pero su origen de datos pasa por el contrato comun.
- Teatro puede mostrar opciones incompletas o bloqueadas para que el usuario entienda el estado real, pero no permite aceptarlas.

## Resultado de pruebas
- `mvn -q "-Dtest=VoiceCapabilityPolicyTest,BuildVoiceAssignmentOptionsUseCaseTest,BuildAudioVoiceProductionProjectionUseCaseTest,BuildNarrationRenderPlanUseCaseTest,TheatreCharactersModuleSourceTest,DocumentAudioNarrationPanelTest,VoiceTransversalTanda6SourceTest" test`: PASA.
- `mvn -q test`: PASA.

Durante la validacion completa aparecio una falla de guardas de microcopy porque el panel documental tenia literales tecnicos de motor en presentation. Se corrigio moviendo el reconocimiento de etiquetas humanas a `VoiceCapabilityPolicy` y dejando `DocumentAudioNarrationPanel` con nombres operativos no tecnicos.

## Archivos de implementacion
- `application.voice`: `VoiceAssignmentOption` y `BuildVoiceAssignmentOptionsUseCase`.
- `application.services`: `VoiceApplicationServices` expone `buildVoiceAssignmentOptions`.
- `bootstrap`: `ApplicationServicesFactory` registra el nuevo caso de uso con la misma `VoiceCapabilityPolicy` compartida por la familia de voz.
- `presentation.theatre`: `TheatreCharactersPanel` usa opciones transversales y bloquea opciones no seleccionables.
- `presentation.document`: `DocumentAudioNarrationPanel` consume servicios de voz desde `ApplicationServices` y deja de instanciar politica local.
