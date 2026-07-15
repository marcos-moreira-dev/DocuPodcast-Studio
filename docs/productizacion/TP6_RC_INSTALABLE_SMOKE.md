# TP6 — RC instalable con smoke manual y automático

TP6 define el cierre de release candidate instalable/portable.

## Secuencia RC

`scripts/16-release-candidate.bat` ahora encadena:

1. `scripts/13-revalidacion-local-completa.bat`
2. `scripts/29-verificar-runtime-layout.bat`
3. `scripts/30-generar-manifest-terceros.bat`
4. `scripts/14-app-image-completa.bat`
5. `scripts/32-preparar-app-portable-layout.bat`
6. `scripts/33-smoke-rc-instalable.bat`

## Gates

`ReleaseCandidateGate.defaultGates()` documenta los gates mínimos:

- diagnóstico completo verde;
- smoke visual UX T112;
- runtime layout;
- third-party manifest;
- app-image/portable;
- motores reales opt-in.

## Evidencias

- `dist/release-candidate/RELEASE_CANDIDATE_MANIFEST.txt`
- `dist/release-candidate/TP6_RC_SMOKE_REPORT.md`
- `target/runtime-layout/TP3_RUNTIME_LAYOUT_REPORT.md`
- `target/legal/THIRD_PARTY_MANIFEST.md`
