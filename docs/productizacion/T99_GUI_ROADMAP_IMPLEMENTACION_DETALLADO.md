# Roadmap detallado de implementación GUI — T99 a T112

Este documento conserva el plan completo de implementación de la interfaz gráfica posterior al contrato T98. Su objetivo es evitar perder decisiones si se interrumpe el chat o cambia el contexto.

## Principios globales

1. Si aparece, funciona.
2. Si no funciona, no aparece.
3. No usar “avanzado” como sinónimo de incompleto.
4. No reintroducir Whisper/STT en la UI del producto.
5. Audio del computador es clip genérico elegido por el usuario.
6. El documento fuente se llama Fuente documental.
7. El proyecto `.docupodcast` se diferencia de la Fuente documental.
8. El Ribbon no reemplaza el Sidebar ni la Playbar.
9. El Sidebar izquierdo configura la oración seleccionada.
10. El Rail derecho solo muestra/navega imágenes/storyboard.
11. La Playbar flotante es dueña de escuchar/pausar/reanudar/detener en uso cotidiano.
12. El control de tamaño de lectura vive abajo a la derecha junto a la StatusBar.
13. Los pasos previos obvios deben automatizarse con confirmación humana.
14. No se hardcodean botones si existe componente transversal.
15. Toda acción visible debe invocar `AppCommandId`.

## Orden recomendado

```text
T99A  — Deshuesadero de vistas y navegación vieja
T99B  — Deshuesadero de acciones duplicadas
T99C  — Deshuesadero visual mínimo
T100  — MenuBar final
T101  — Ribbon base
T102  — StatusBar + ReadingZoomControl
T103  — Vista Inicio propagandística
T104  — Workspace Documento limpio
T105  — Playbar flotante
T106  — Sidebar izquierdo contextual
T107  — Rail derecho retráctil
T108  — Workspace Voces
T109  — Overlay de procesos largos
T110  — Configuración, guía y diálogos
T111  — Iconografía y CSS final
T112  — Smoke visual y checklist UX
```

Cada tanda debe actualizar README, AI_HANDOFF, VALIDATION y el registro de tandas cuando corresponda.
