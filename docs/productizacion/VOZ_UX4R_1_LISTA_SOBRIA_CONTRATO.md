# VOZ-UX4R-1 — Lista sobria de voces y contrato vivo

## Objetivo

Reanudar la línea delicada de Vista Voces sin perder decisiones previas del usuario. Esta tanda no toca la descarga de Voz IA avanzada; primero estabiliza el contrato visual y funcional de la lista de voces creadas.

## Decisiones preservadas

- La interfaz normal no debe mostrar nombres técnicos como Coqui, XTTS o Piper.
- Los nombres visibles siguen siendo **Voz IA avanzada**, **Voz local simple** y **Modo de prueba**.
- Voces es un workspace secundario real: crea, prueba, importa, graba, elimina y revisa voces/muestras.
- Documento es donde se asigna una voz a una oración/fragmento.
- Neutral es necesaria para que una voz avanzada sea usable.
- Los demás tonos son **Tonos**, no se presentan como “opcionales” en la UI normal.
- Voz local simple sigue siendo mínima: texto breve de prueba, lectura neutral, sin muestras humanas ni tonos.
- La descarga/preparación de Voz IA avanzada queda para una tanda posterior con diagnóstico real de descarga, verificación, selección y prueba generada.

## Cambios de esta tanda

- La lista lateral pasa a hablar de **Voces creadas**.
- Cada voz muestra estado humano:
  - `Voz base lista`
  - `Neutral lista`
  - `Tonos configurados`
  - `Falta neutral`
  - `Incompleta`
- La celda de lista se extrae a `VoiceListItemView` para evitar celdas improvisadas y reutilizarla luego en otras listas sobrias.
- Se elimina lenguaje visible de “Opcional” para tonos no neutrales: ahora se muestran como `Tono · <nombre>`.
- CSS específico mantiene una lista sobria, no tabla Excel ni CRUD antiguo.

## Tandas de Voces que siguen

1. **VOZ-UX4R-2 — Selector de motor activo dentro de Voces**: ComboBox real de Voz IA avanzada / Voz local simple / Modo de prueba, con persistencia en settings y refresco del workspace.
2. **VOZ-UX4R-3 — Voz local simple completa**: textbox editable, probar lectura simple, estado de modelo, sin tonos ni muestras humanas.
3. **VOZ-UX4R-4 — Crear/renombrar/eliminar voz avanzada**: alta/baja de voces reales y eliminación segura de muestras gestionadas.
4. **VOZ-UX4R-5 — Tonos por voz**: neutral necesaria, tonos registrados visibles, combos de Documento filtrados por tonos existentes.
5. **VOZ-UX4R-6 — Prueba avanzada/simple**: separar reproducir muestra importada/grabada de generar prueba con esa referencia.
6. **VOZ-TTS5 — Documento usa voz y tono reales**: generación de chunks con voz/tono asignados por capa.
7. **MOTOR-SMOKE4R — Voz IA avanzada real**: descarga/verificación/selección/prueba WAV real y diagnóstico de fallos.

## Guardarraíl

`VoiceUx4R1HumanListSourceTest` valida que la lista usa `VoiceListItemView`, estados humanos, copy sin “Opcional” para tonos, y CSS dedicado.

## Alineación posterior — VOZ-UX4R-DOC1

Después de VOZ-UX4R-1, la definición final de Vista Voces se simplifica a una microaplicación administrativa con tres módulos:

1. **Inicio** — lista voces registradas con filas sobrias, estado y emociones registradas. Acciones rápidas: editar y eliminar.
2. **Configurar motor** — selector de motor, selector CPU/GPU real para todos los motores, estado real y prueba con textbox.
3. **Gestionar voces** — crear/editar/eliminar voces, importar/grabar/reemplazar muestras por emoción, reproducir muestras y exportar audios.

Reglas nuevas que prevalecen sobre planes anteriores:

- No usar tarjetas futuristas ni estética web/SaaS; la vista debe parecer administrativa y sobria.
- Una voz es una entidad única (`Pepito`); las emociones son muestras asociadas, no voces separadas.
- Deben soportarse muchas emociones, no solo tres.
- Neutral es obligatoria para que una voz aparezca en Documento.
- En Gestionar voces se debe poder seleccionar voz + emoción y reemplazar esa emoción importando o grabando de nuevo.
- Al eliminar una voz debe aparecer un message box avisando que también se eliminarán los archivos de audio/muestras relacionados a esa voz.
- El selector de dispositivo CPU/GPU aplica a todos los motores, con detección real y sin placeholders.
- Documento solo muestra voces con Neutral y emociones realmente registradas para la voz elegida.

Documento rector: `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`.
