# Memoria — T80C

T80C corrige el test fallido de T80B en Windows: el test ya no crea un `Path` con caracteres inválidos (`:` y `?`), sino que valida la sanitización con `sanitizeProjectName`.

Además se implementa media flexible: MP3/WAV directos y video MP4/MOV/MKV/WEBM solo para extraer audio con FFmpeg. El video queda como `VIDEO_SOURCE`; el audio derivado queda como `AUDIO_CLIP`.
