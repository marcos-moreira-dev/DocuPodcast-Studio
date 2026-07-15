# Integridad y reparación del proyecto — T77

## Contrato

DocuPodcast Studio debe poder inspeccionar un proyecto como carpeta autocontenida y responder con un estado no binario:

- `OK`: no hay hallazgos relevantes.
- `CON_ADVERTENCIAS`: el proyecto es utilizable, pero conviene revisar o completar algo.
- `REQUIERE_REPARACION`: hay referencias rotas o inconsistencias que pueden afectar lectura, reproducción, exportación o reapertura confiable.

## Alcance del reporte

El reporte cubre:

- raíz del proyecto;
- assets y rutas relativas;
- checksums SHA-256;
- documento narrable, guion interno y storyboard materializados;
- capas narrativas con targets reales;
- audio jobs persistidos y WAVs faltantes;
- storyboard contra segmentos e imágenes;
- paquete de video simple si el usuario ya generó archivos de render.

## Reparación sugerida

T77 no ejecuta reparaciones automáticas agresivas. Primero crea hallazgos accionables con sugerencias: reimportar asset, regenerar audio, rehidratar documento, reconstruir storyboard o eliminar/reasignar una capa obsoleta.

Esto prepara una futura vista de diagnóstico o asistente de reparación sin contaminar la pantalla principal de lectura.
