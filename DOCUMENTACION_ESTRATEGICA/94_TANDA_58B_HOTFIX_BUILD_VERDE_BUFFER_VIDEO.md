# Estrategia — Tanda 58B: hotfix build verde buffer/video

## Propósito estratégico

Restablecer base verde antes de tocar diseño visual o arquitectura. La Tanda 58B no intenta cerrar producto: solo alinea dos contratos rotos detectados por validación local.

## Contratos protegidos

### Streaming/prebuffer

El mensaje visible de espera debe estar centralizado en `PlaybackBufferPolicy`, para que dominio, configuración, shell y tests no diverjan.

### Video simple

Hasta que exista render MP4 real completo, la exportación debe describirse como paquete renderizable/auditable. El lenguaje visible no debe prometer que el MP4 final ya se genera.

## Secuencia posterior recomendada

```text
T58B — hotfix build verde
T58C — smoke exploratorio mínimo
T59 — criterios de scaffolding
T59A — componentes GUI transversales
T59B — limpieza UX Documento
T60 — coordinadores/refactor
T61 — round-trip real
T62 — configuración operativa
T63 — smoke real
T64 — RC
```
