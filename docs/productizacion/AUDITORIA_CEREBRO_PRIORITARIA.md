# Auditoría prioritaria del cerebro — DocuPodcast Studio

## Objetivo

Definir cómo auditar el equivalente al backend de la aplicación antes de aplicar un rediseño visual profundo. Esta auditoría no busca cambiar la cara final todavía; busca saber si el cerebro puede sostener el producto.

## Preguntas obligatorias por flujo

Para cada flujo se debe responder:

1. ¿Cuál es el estado inicial válido?
2. ¿Qué caso de uso inicia el cambio?
3. ¿Qué modelo de dominio cambia?
4. ¿Qué repositorio o gateway participa?
5. ¿Qué se persiste?
6. ¿Qué se puede reabrir?
7. ¿Qué ocurre si falla a mitad?
8. ¿Qué debe ver el usuario normal?
9. ¿Qué debe ir a diagnóstico avanzado?
10. ¿Qué test lo protege?

## Flujos prioritarios

| Flujo | Estado requerido | Riesgo que debe auditarse |
|---|---|---|
| Abrir DOCX/PDF/Markdown/TXT | Documento fuente importado/visible en modo solo lectura. | Confusión entre fuente inmutable y artefacto editable. |
| Escuchar documento | Documento narrable, proyección interna de narración, audio y playback coordinados. | El ViewModel orquesta demasiado y Guion puede parecer paso obligatorio. |
| Seleccionar rango/oración | Ancla documental estable en Documento narrable. | Offset documento/proyección interna inconsistente. |
| Asignar voz/audio | Capa primaria válida. | Solapamientos o placeholders persistidos. |
| Asignar imagen | Asset real o placeholder explícito. | Rail no muestra múltiples usos de misma imagen. |
| Guardar/reabrir | Proyecto portable íntegro. | Pérdida de capas, metadatos, jobs o checksums; sobrescritura accidental de fuente. |
| Generar audio | Jobs persistibles y recuperables. | Estado técnico visible o no recuperable. |
| Playback con buffer | Espera y continuidad entendibles. | Pausa silenciosa o estado incoherente. |
| STT/Whisper | Modelo/ruta configurados. | Dependencia de consola o settings no persistidos. |
| Video simple | Paquete o MP4 claramente distinguido. | Promesa falsa de MP4 final. |

## Métricas de salud del cerebro

| Métrica | Umbral deseado |
|---|---|
| Tamaño de ViewModel shell | Debe bajar de forma progresiva mediante coordinadores. |
| Cambios por navegación | No deben marcar dirty de contenido. |
| Acciones permitidas | Deben pasar por capability policies. |
| Persistencia | Todo flujo de usuario debe tener round-trip o limitación explícita. |
| Jobs | Deben poder inspeccionarse, cancelar/reanudar cuando aplique y dejar log. |
| Configuración | Debe guardarse y poder probarse desde UI guiada. |
| Tests | Deben combinar unitarios, integración ligera, source guards y smoke manual. |

## Resultado esperado de la auditoría

La auditoría debe producir:

- mapa de responsabilidades actuales;
- lista de clases que concentran demasiadas decisiones;
- lista de coordinadores a extraer;
- flujos con round-trip incompleto;
- acciones visibles que no coinciden con capacidades reales;
- tests faltantes;
- plan de refactor por tandas pequeñas.

## Regla de no regresión

Ningún refactor del cerebro debe cambiar comportamiento visible sin declararlo. Primero se extraen responsabilidades, luego se rediseña la cara.


## Criterio agregado por T59C

La auditoría del cerebro debe verificar que los documentos fuente Word/DOCX, PDF, Markdown/MD y TXT entren como solo lectura. Cualquier edición debe caer sobre artefactos del proyecto DocuPodcast o sobre exportaciones nuevas, nunca sobre el archivo fuente original.


## Criterio agregado por T60

La auditoría debe partir de una raíz única: **Documento narrable**. Guion, segmentos y manifest son proyecciones o artefactos internos/avanzados. Si una acción visible obliga al usuario normal a pensar en Guion antes de escuchar, debe marcarse como deuda de lenguaje, navegación o arquitectura.


## Ajuste T60B — refresco de documento fuente

La auditoría del cerebro debe incluir la operación **Refrescar contenido**. Todos los documentos fuente son solo lectura, pero el usuario puede modificarlos fuera de DocuPodcast. La app debe poder comparar el archivo actual contra el snapshot importado, mostrar impacto y marcar audio/capas/storyboard como vigentes, obsoletos o en revisión sin sobrescribir la fuente.
