# Smoke manual — Release Candidate DocuPodcast Studio

Usar después de ejecutar `scripts\13-revalidacion-local-completa.bat` y antes de considerar válido un release candidate.

## 1. Inicio y shell

- [ ] La aplicación abre con ventana nativa visible: minimizar, maximizar y cerrar.
- [ ] La pantalla de inicio muestra acciones en lenguaje de producto, no jerga técnica.
- [ ] La toolbar es desplazable horizontalmente y no corta textos principales.

## 2. Word/DOCX

- [ ] `Abrir Word/DOCX` permite seleccionar un `.docx`.
- [ ] El documento se muestra con bloques detectados.
- [ ] El diagnóstico documental muestra advertencias comprensibles.
- [ ] El perfil de lectura puede previsualizar y aplicar cambios.

## 3. Guion narrable

- [ ] El flujo de guion narrable queda validado desde documento importado hasta segmentos revisables.
- [ ] `Crear guion` genera segmentos desde el documento.
- [ ] El workspace Guion muestra segmentos y permite seleccionar uno.
- [ ] Los botones de voz IA, grabar voz, audio a texto y reproducir selección están visibles como preparación de roadmap.

## 4. Audio

- [ ] `Generar audio` produce un job con progreso y ETA.
- [ ] El motor mock funciona si no hay `DOCUPODCAST_TTS_COMMAND`.
- [ ] Si se configura TTS real, el diagnóstico de proceso registra eventos.
- [ ] Se puede ver historial de jobs persistidos.

## 5. Storyboard vivo

- [ ] Se puede crear storyboard desde guion.
- [ ] Se puede importar una imagen.
- [ ] Se puede asociar la imagen a un segmento.
- [ ] La escena asociada aparece en el workspace Storyboard.

## 6. Playback sincronizado

- [ ] El playback usa segmentos del guion y audio manifest.
- [ ] Se puede pausar/reanudar/detener.
- [ ] Al hacer clic en un segmento, la reproducción puede iniciarse desde esa selección.

## 7. Exportaciones

- [ ] Exportar guion Markdown.
- [ ] Exportar podcast WAV si existe job completado.
- [ ] Exportar reporte diagnóstico.
- [ ] Exportar paquete completo con `input/`, `editable/`, `output/`, `jobs/` y `reports/`.

## 8. Guía y recursos IA

- [ ] La guía integrada abre.
- [ ] Existe tema de Word/DOCX antes de Markdown/IA.
- [ ] Recursos IA se exportan con índice.
- [ ] Plantillas con placeholders no están marcadas como importables.
- [ ] Ningún recurso oficial se marca importable mientras no exista parser/importador real.

## Resultado

- [ ] Aprobado.
- [ ] Aprobado con observaciones.
- [ ] Rechazado.

## TP6 — Smoke RC instalable/portable

Antes de distribuir una RC, ejecutar:

1. `scripts\99-diagnostico-completo.bat`
2. `scripts\29-verificar-runtime-layout.bat`
3. `scripts\30-generar-manifest-terceros.bat`
4. `scripts\16-release-candidate.bat`
5. Abrir `dist\portable\DocuPodcastStudio` y repetir el smoke visual T112 con un ejemplo T113.

Evidencia esperada: `dist\release-candidate\TP6_RC_SMOKE_REPORT.md` y `RELEASE_CANDIDATE_MANIFEST.txt`.
