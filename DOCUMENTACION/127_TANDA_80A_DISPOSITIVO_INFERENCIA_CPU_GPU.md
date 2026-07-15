# Tanda 80A — Dispositivo de inferencia CPU/GPU y rendimiento

Se agrega al cerebro un contrato explícito para CPU/GPU: política `AUTO`, `CPU_ONLY`, `PREFER_GPU` o `SPECIFIC_DEVICE`; diagnóstico ligero de entorno; persistencia en `operational-settings.properties`; placeholders para TTS externo; política de STT; encoder de video simple (`AUTO`, `CPU_X264`, `NVIDIA_NVENC`, `INTEL_QSV`, `AMD_AMF`) y manifest de render con política/encoder/fallback.

La decisión UX se mantiene: Documento no muestra tecnicismos de inferencia. Configuración es la bodega técnica.
