"""Persistent, line-delimited local XTTS worker for one admitted voice batch.

The Java host owns the process lifetime. The model stays resident only while
the host keeps the heavy-model lease and the worker accepts one chunk at a
time, which gives the scheduler a safe yield boundary between chunks.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from synthesize_xtts import (
    INFERENCE_PROFILE,
    configure_local_audio_loader,
    configure_local_torch_deserialization,
    resolve_device,
    resolve_model_root,
    inference_options,
)

PREFIX = "DOCUPODCAST_XTTS_WORKER:"

# The worker protocol is UTF-8. On Windows, a GUI-launched Python process may
# otherwise retain a legacy console encoding and TTS can fail merely while
# printing an OCR sentence containing mathematical Unicode.
if hasattr(sys.stdin, "reconfigure"):
    sys.stdin.reconfigure(encoding="utf-8")
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="backslashreplace")


def emit(payload: dict[str, object]) -> None:
    print(PREFIX + json.dumps(payload, ensure_ascii=False), flush=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--device", default="")
    args = parser.parse_args()

    try:
        configure_local_torch_deserialization()
        configure_local_audio_loader()
        from TTS.api import TTS  # type: ignore

        model_root = resolve_model_root(args.model_dir)
        device = resolve_device(args.device)
        tts = TTS(
            model_path=str(model_root),
            config_path=str(model_root / "config.json"),
            progress_bar=False,
        )
        if device:
            tts.to(device)
        emit({
            "event": "ready",
            "device": device or "auto",
            "inferenceProfile": INFERENCE_PROFILE,
        })
    except Exception as exc:
        emit({"event": "fatal", "message": str(exc)})
        return 2

    for raw_line in sys.stdin:
        try:
            command = json.loads(raw_line)
            if command.get("command") == "shutdown":
                emit({"event": "stopped"})
                return 0
            if command.get("command") != "synthesize":
                raise ValueError("Comando XTTS desconocido.")
            segment_id = str(command["segmentId"])
            text = Path(command["textFile"]).read_text(
                encoding="utf-8-sig"
            ).strip().lstrip("\ufeff")
            output = Path(command["outputFile"])
            speaker = Path(command["speakerWav"])
            language = str(command.get("language") or "es")
            if not text:
                raise ValueError("El texto del chunk está vacío.")
            if not speaker.is_file():
                raise FileNotFoundError(f"Falta la muestra de voz: {speaker}")
            output.parent.mkdir(parents=True, exist_ok=True)
            output.unlink(missing_ok=True)
            tts.tts_to_file(
                text=text,
                file_path=str(output),
                speaker_wav=str(speaker),
                language=language,
                **inference_options(),
            )
            if not output.is_file() or output.stat().st_size <= 44:
                raise RuntimeError("XTTS no produjo un WAV válido.")
            emit({
                "event": "completed",
                "segmentId": segment_id,
                "bytes": output.stat().st_size,
            })
        except Exception as exc:
            emit({
                "event": "error",
                "segmentId": str(
                    command.get("segmentId", "") if "command" in locals() else ""
                ),
                "message": str(exc),
            })
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
