# Tanda 36 — Lectura cómoda y configuración base

## Propósito

Hacer que la experiencia principal de DocuPodcast Studio se parezca más a un lector narrado para usuario no profesional: el documento debe leerse cómodamente y la configuración técnica debe vivir en una ventana separada, no en la pantalla operativa.

## Cambios

- El texto del documento importado aumenta su tamaño base a 18 px y mejora su interlineado.
- El workspace Documento conserva `Escuchar documento` como acción principal.
- Se agrega una ventana dedicada `Configuración de DocuPodcast Studio`.
- La configuración usa una navegación lateral simple, similar a editores de código, con módulos:
  - Lectura / documento
  - Reproducción / buffer
  - TTS / voz
  - STT
  - Audio
  - Storyboard / video
  - Almacenamiento
  - Diagnóstico / rendimiento
- La sección Reproducción / buffer documenta el contrato inicial de prebuffer: oraciones mínimas antes de empezar y oraciones adelantadas.
- La configuración se abre desde el menú `Configuración`, no desde la toolbar principal, para no competir con la operación de lectura.

## Criterio de producto

La pantalla principal es la zona del cliente: limpia, cómoda y centrada en leer/escuchar. La configuración es la bodega técnica: poderosa, separada y organizada por módulos.

## Límites

Esta tanda no persiste todavía preferencias de usuario ni conecta el prebuffer con reproducción real por chunks. Fija la superficie de configuración y hace el documento más legible por defecto.
