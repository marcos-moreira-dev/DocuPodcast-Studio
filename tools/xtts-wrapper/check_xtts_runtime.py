"""Verificación mínima del runtime local Coqui/XTTS para DocuPodcast Studio."""

from __future__ import annotations

import argparse
import importlib.util
import sys
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verifica Python + Coqui TTS + artefactos XTTS locales")
    parser.add_argument("--model-dir", default="")
    parser.add_argument("--speaker-wav", default="")
    parser.add_argument("--skip-model-check", action="store_true")
    return parser.parse_args()


def fail(message: str) -> int:
    print(f"ERROR: {message}", file=sys.stderr)
    return 1


def main() -> int:
    args = parse_args()
    print(f"Python: {sys.executable}")
    print(f"Versión: {sys.version.split()[0]}")

    if sys.version_info < (3, 10) or sys.version_info >= (3, 12):
        return fail("DocuPodcast espera Python 3.10/3.11 para Coqui XTTS.")

    if importlib.util.find_spec("TTS") is None:
        return fail("No se encontró el paquete TTS. Ejecuta scripts\\20-preparar-python-portable-coqui.bat")
    print("Coqui TTS: paquete encontrado")

    try:
        import transformers  # type: ignore
        from transformers import BeamSearchScorer  # type: ignore  # noqa: F401
        print(f"Transformers: {getattr(transformers, '__version__', 'desconocida')} compatible")
    except Exception as exc:  # pragma: no cover - depende del runtime instalado
        return fail("El runtime Python local tiene una versión incompatible de transformers para Coqui/XTTS. "
                    "Vuelve a ejecutar scripts\\20-preparar-python-portable-coqui.bat para instalar las dependencias fijadas. "
                    f"Detalle: {exc}")

    try:
        from TTS.api import TTS  # type: ignore  # noqa: F401
    except Exception as exc:  # pragma: no cover - depende del runtime instalado
        return fail("El paquete TTS existe, pero no es importable con las dependencias actuales. "
                    "Repara el runtime desde Configuración o ejecuta scripts\\20-preparar-python-portable-coqui.bat. "
                    f"Detalle: {exc}")
    print("Coqui TTS: importable")

    if not args.skip_model_check:
        if not args.model_dir:
            return fail("Falta --model-dir")
        model_dir = Path(args.model_dir)
        if not model_dir.is_dir():
            return fail(f"No existe la carpeta del modelo XTTS: {model_dir}")
        print(f"Modelo XTTS: {model_dir}")

        required_model_files = [
            model_dir / "config.json",
            model_dir / "model.pth",
            model_dir / "vocab.json",
            model_dir / "speakers_xtts.pth",
            model_dir / "dvae.pth",
            model_dir / "mel_stats.pth",
        ]
        missing = [str(path) for path in required_model_files if not path.is_file()]
        if missing:
            return fail("Modelo XTTS local incompleto. Faltan: " + ", ".join(missing))
        print("Modelo XTTS: archivos base presentes (config.json, model.pth, vocab.json, speakers_xtts.pth, dvae.pth, mel_stats.pth)")

        if not args.speaker_wav:
            return fail("Falta --speaker-wav")
        speaker = Path(args.speaker_wav)
        if not speaker.is_file():
            return fail(f"No existe la voz de referencia: {speaker}")
        print(f"Voz de referencia: {speaker}")

    print("Runtime Coqui/XTTS listo para prueba local.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
