"""Batch local XTTS wrapper for DocuPodcast Studio document jobs."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

from synthesize_xtts import (
    INFERENCE_PROFILE,
    configure_local_audio_loader,
    configure_local_torch_deserialization,
    resolve_device,
    resolve_model_root,
    inference_options,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Synthesize several DocuPodcast segments with one local XTTS load")
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--language", default="es")
    parser.add_argument("--device", default="")
    return parser.parse_args()


def log(message: str) -> None:
    print(f"DOCUPODCAST_XTTS: {message}", flush=True)


def batch_log(message: str) -> None:
    print(f"DOCUPODCAST_XTTS_BATCH: {message}", flush=True)


def load_manifest(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        raise SystemExit(f"Batch manifest does not exist: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception as exc:
        raise SystemExit(f"Batch manifest is not valid JSON: {exc}") from exc
    raw_items = payload.get("segments") if isinstance(payload, dict) else None
    if not isinstance(raw_items, list) or not raw_items:
        raise SystemExit("Batch manifest must contain a non-empty 'segments' array.")
    items: list[dict[str, Any]] = []
    for index, raw in enumerate(raw_items, start=1):
        if not isinstance(raw, dict):
            raise SystemExit(f"Batch manifest segment #{index} is not an object.")
        segment_id = str(raw.get("segmentId") or "").strip()
        text_file = str(raw.get("textFile") or "").strip()
        output_file = str(raw.get("outputFile") or "").strip()
        speaker_wav = str(raw.get("speakerWav") or "").strip()
        if not segment_id or not text_file or not output_file or not speaker_wav:
            raise SystemExit(f"Batch manifest segment #{index} is missing segmentId/textFile/outputFile/speakerWav.")
        items.append(
            {
                "segmentId": segment_id,
                "textFile": text_file,
                "outputFile": output_file,
                "speakerWav": speaker_wav,
                "language": str(raw.get("language") or "").strip(),
            }
        )
    return items


def main() -> int:
    args = parse_args()
    manifest_path = Path(args.manifest)
    log("inicio_batch")
    log(f"python={sys.executable}")
    items = load_manifest(manifest_path)
    batch_log(f"segments_total={len(items)}")

    model_root = resolve_model_root(args.model_dir)
    config_path = model_root / "config.json"
    log(f"model_dir={model_root}")
    log(f"model_path={model_root / 'model.pth'}")
    log(f"config_path={config_path}")
    log(f"vocab_path={model_root / 'vocab.json'}")

    for item in items:
        text_path = Path(item["textFile"])
        speaker_path = Path(item["speakerWav"])
        if not text_path.is_file():
            raise SystemExit(f"Text file does not exist for {item['segmentId']}: {text_path}")
        if not speaker_path.is_file():
            raise SystemExit(f"Speaker WAV does not exist for {item['segmentId']}: {speaker_path}")

    try:
        log("importando_paquete_tts")
        configure_local_torch_deserialization()
        configure_local_audio_loader()
        from TTS.api import TTS  # type: ignore
    except Exception as exc:
        raise SystemExit(
            "Coqui TTS no es importable dentro del Python local de DocuPodcast. "
            "Repara el runtime desde Configuracion. Detalle: " + str(exc)
        ) from exc

    device = resolve_device(args.device)
    log(f"device={device}")
    log("cargando_modelo")
    log(f"inference_profile={INFERENCE_PROFILE}")
    tts = TTS(model_path=str(model_root), config_path=str(config_path), progress_bar=False)
    if device:
        log(f"asignando_device={device}")
        tts.to(device)

    for item in items:
        segment_id = item["segmentId"]
        text_path = Path(item["textFile"])
        output_path = Path(item["outputFile"])
        speaker_path = Path(item["speakerWav"])
        language = item["language"] or args.language or "es"
        text = text_path.read_text(encoding="utf-8-sig").strip().lstrip("\ufeff")
        if not text:
            raise SystemExit(f"Text file is empty for {segment_id}: {text_path}")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        if output_path.exists():
            output_path.unlink()
        log(f"texto_cargado segment={segment_id} chars={len(text)}")
        batch_log(f"segment_start={segment_id}")
        log("sintetizando")
        tts.tts_to_file(
            text=text,
            file_path=str(output_path),
            speaker_wav=str(speaker_path),
            language=language,
            **inference_options(),
        )
        if not output_path.is_file() or output_path.stat().st_size <= 44:
            raise SystemExit(f"No se genero WAV valido para {segment_id}: {output_path}")
        bytes_written = output_path.stat().st_size
        log(f"wav_generado segment={segment_id} bytes={bytes_written}")
        batch_log(f"segment_done={segment_id} bytes={bytes_written}")
    batch_log("complete=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
