# FIRST-USE-ONBOARDING1 — primera experiencia orientada a escuchar rápido

## Objetivo

La primera experiencia de DocuPodcast debe permitir escuchar un documento lo antes posible. La configuración inicial ya no debe empujar primero a una descarga pesada de Voz IA avanzada; debe preparar **Voz local simple** como camino rápido y dejar Voz IA avanzada y Video local como mejoras posteriores.

## Cambios

- Inicio explica que Configuración inicial prepara Voz local simple para escuchar rápido.
- Configuración inicial revisa/prepara Voz local simple primero.
- Voz IA avanzada queda como mejora de calidad para voces por muestra y se prepara cuando el usuario la necesite.
- Video local queda como requisito de exportación MP4, no como bloqueo para escuchar.
- La guía integrada deja de hablar de paquete renderizable en el flujo normal y usa **video MP4 final**.

## Contrato de producto

1. Un usuario nuevo puede preparar voz rápida sin entender motores ni rutas.
2. Si Voz IA avanzada falta o tarda, no bloquea la prueba inicial del documento.
3. El flujo normal habla de Voz local simple, Voz IA avanzada, Audio final y Video MP4.
4. Los paquetes técnicos quedan para soporte avanzado, no para la guía básica.

## Validación esperada

- `scripts\99-diagnostico-completo.bat` verde.
- Inicio conserva `Configuración inicial` y `Abrir documento`.
- Configuración inicial prepara Voz local simple.
- La guía integrada no promete paquete renderizable como salida normal.
