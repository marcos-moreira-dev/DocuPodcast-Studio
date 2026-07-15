"""Minimal local XTTS wrapper for DocuPodcast Studio."""

from __future__ import annotations

import argparse
import inspect
import sys
from pathlib import Path
from typing import Any


REQUIRED_MODEL_FILES = ("model.pth", "config.json", "vocab.json")
LEGACY_MARKERS = (
    "recursos locales ia avanzada",
    "componentes locales ia avanzada-wrapper",
    "/model.pth/model.pth",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Synthesize one DocuPodcast segment with local XTTS")
    parser.add_argument("--text-file", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--speaker-wav", required=True)
    parser.add_argument("--language", default="es")
    parser.add_argument("--model-dir", default="")
    parser.add_argument("--device", default="")
    return parser.parse_args()


def log(message: str) -> None:
    print(f"DOCUPODCAST_XTTS: {message}", flush=True)


def normalized_text(path: Path | str) -> str:
    return str(path).replace("\\", "/").lower()


def is_legacy_path(path: Path | str) -> bool:
    text = normalized_text(path)
    return any(marker in text for marker in LEGACY_MARKERS)


def has_required_model_files(model_root: Path) -> bool:
    return all((model_root / name).is_file() for name in REQUIRED_MODEL_FILES)


def portable_model_root_from_wrapper() -> Path:
    return Path(__file__).resolve().parents[2] / "models" / "tts" / "xtts"


def normalize_model_root(raw: Path) -> Path:
    """Accept the XTTS model folder or a mistakenly selected model.pth file."""
    raw_text = str(raw).strip().strip('"').strip("'").rstrip("\\/")
    model_root = Path(raw_text).expanduser()
    if model_root.name.lower() == "model.pth":
        log("model-dir-apunta-a-model-pth; evitar model.pth/model.pth; usando_carpeta_padre")
        log("model_dir_termina_en_model_pth; usando_carpeta_padre")
        if model_root.parent.name.lower() == "model.pth":
            return normalize_model_root(model_root.parent)
        return model_root.parent
    return model_root


def resolve_model_root(raw_model_dir: str) -> Path:
    if not raw_model_dir.strip():
        raise SystemExit("Model directory is required. DocuPodcast does not download XTTS models automatically.")

    requested = normalize_model_root(Path(raw_model_dir))
    portable = portable_model_root_from_wrapper()
    if is_legacy_path(raw_model_dir) or is_legacy_path(requested):
        if has_required_model_files(portable):
            log("legacy_model_dir_detected; usando_modelo_portable")
            return portable
        raise SystemExit(
            "No existe modelo XTTS portable valido en "
            + str(portable)
            + ". Debe contener model.pth, config.json y vocab.json."
        )

    if has_required_model_files(requested):
        return requested

    if requested != portable and has_required_model_files(portable):
        log("model_dir_incompleto; usando_modelo_portable")
        return portable

    missing = [str(requested / name) for name in REQUIRED_MODEL_FILES if not (requested / name).is_file()]
    raise SystemExit(
        "No existe modelo XTTS portable valido en "
        + str(requested)
        + ". Faltan: "
        + ", ".join(missing)
    )


def gpu_index(requested: str) -> int:
    for separator in ("-", ":"):
        if separator in requested:
            suffix = requested.rsplit(separator, 1)[-1]
            if suffix.isdigit():
                return int(suffix)
    return 0


def import_torch_for_device() -> Any:
    try:
        import torch  # type: ignore

        return torch
    except Exception as exc:  # pragma: no cover - environment dependent
        raise SystemExit(
            "No se pudo importar PyTorch dentro del Python local para resolver el dispositivo solicitado. "
            f"Detalle: {exc}"
        )


def resolve_cuda_device(requested: str, index: int) -> str:
    torch = import_torch_for_device()
    if not torch.cuda.is_available():
        raise SystemExit(
            f"Dispositivo manual {requested} solicitado, pero PyTorch CUDA no esta disponible "
            "en el Python local de DocuPodcast."
        )
    count = int(torch.cuda.device_count())
    if index < 0 or index >= count:
        raise SystemExit(
            f"Dispositivo manual {requested} solicitado, pero PyTorch CUDA reporta {count} GPU(s)."
        )
    name = torch.cuda.get_device_name(index)
    log(f"device_backend=cuda device_name={name}")
    return f"cuda:{index}"


def resolve_xpu_device(requested: str, index: int) -> str | None:
    torch = import_torch_for_device()
    xpu = getattr(torch, "xpu", None)
    if xpu is None or not callable(getattr(xpu, "is_available", None)) or not xpu.is_available():
        return None
    count_fn = getattr(xpu, "device_count", None)
    count = int(count_fn()) if callable(count_fn) else 1
    if index < 0 or index >= count:
        raise SystemExit(
            f"Dispositivo manual {requested} solicitado, pero torch.xpu reporta {count} GPU(s)."
        )
    log("device_backend=torch.xpu")
    return f"xpu:{index}"


def resolve_directml_device(requested: str, index: int) -> Any | None:
    try:
        import torch_directml  # type: ignore
    except Exception:
        return None
    device_count = getattr(torch_directml, "device_count", None)
    count = int(device_count()) if callable(device_count) else 1
    if index < 0 or index >= count:
        raise SystemExit(
            f"Dispositivo manual {requested} solicitado, pero torch-directml reporta {count} GPU(s)."
        )
    log("device_backend=torch_directml")
    return torch_directml.device(index)


def resolve_intel_device(requested: str, index: int) -> Any:
    xpu_device = resolve_xpu_device(requested, index)
    if xpu_device is not None:
        return xpu_device
    directml_device = resolve_directml_device(requested, index)
    if directml_device is not None:
        return directml_device
    raise SystemExit(
        f"Dispositivo manual {requested} solicitado, pero el Python local no tiene backend Intel "
        "usable para XTTS (torch.xpu o torch-directml)."
    )


def resolve_amd_device(requested: str, index: int) -> Any:
    torch = import_torch_for_device()
    if getattr(torch.version, "hip", None) and torch.cuda.is_available():
        return resolve_cuda_device(requested, index)
    directml_device = resolve_directml_device(requested, index)
    if directml_device is not None:
        return directml_device
    raise SystemExit(
        f"Dispositivo manual {requested} solicitado, pero el Python local no tiene backend AMD "
        "usable para XTTS (ROCm/HIP o torch-directml)."
    )


def resolve_device(raw: str) -> Any:
    requested = (raw or "").strip().lower()
    log(f"device_requested={requested or 'auto'}")
    if requested in ("", "auto"):
        return "cpu"
    if requested == "cpu":
        return "cpu"
    if requested.startswith("cuda"):
        return resolve_cuda_device(requested, gpu_index(requested))
    if requested in ("gpu", "nvidia") or requested.startswith("gpu-nvidia"):
        return resolve_cuda_device(requested, gpu_index(requested))
    if requested.startswith("gpu-intel") or requested == "intel":
        return resolve_intel_device(requested, gpu_index(requested))
    if requested.startswith("gpu-amd") or requested == "amd":
        return resolve_amd_device(requested, gpu_index(requested))
    if requested.startswith("gpu-unknown"):
        raise SystemExit(
            f"Dispositivo manual {requested} solicitado, pero no hay backend conocido para esa GPU."
        )
    return requested


def configure_local_torch_deserialization() -> None:
    """Keep Coqui XTTS compatible with Torch 2.6+ inside the portable venv.

    XTTS checkpoints bundled for local use include trusted Coqui config objects.
    Newer Torch versions default to weights_only loading and reject those objects.
    The wrapper only loads the validated local model folder, so it can explicitly
    opt out for this process without changing the user's global Python.
    """
    try:
        import torch  # type: ignore
    except Exception as exc:  # pragma: no cover - reported by the later TTS import too
        log(f"torch_import_failed={exc}")
        return

    try:
        from TTS.tts.configs.xtts_config import XttsAudioConfig, XttsConfig  # type: ignore
        from TTS.tts.models.xtts import XttsArgs  # type: ignore

        add_safe_globals = getattr(getattr(torch, "serialization", None), "add_safe_globals", None)
        if callable(add_safe_globals):
            add_safe_globals([XttsConfig, XttsAudioConfig, XttsArgs])
            log("torch_safe_globals=xtts")
    except Exception as exc:  # pragma: no cover - weights_only=False remains the main compatibility path
        log(f"torch_safe_globals_skipped={exc}")

    if getattr(torch.load, "_docupodcast_weights_only_compat", False):
        return
    if "weights_only" not in inspect.signature(torch.load).parameters:
        return

    original_load = torch.load

    def docupodcast_torch_load(*args: Any, **kwargs: Any) -> Any:
        kwargs.setdefault("weights_only", False)
        return original_load(*args, **kwargs)

    setattr(docupodcast_torch_load, "_docupodcast_weights_only_compat", True)
    torch.load = docupodcast_torch_load  # type: ignore[assignment]
    log("torch_load_weights_only=false")


def configure_local_audio_loader() -> None:
    """Fallback for torchaudio builds that require torchcodec for WAV loading."""
    try:
        import torch  # type: ignore
        import torchaudio  # type: ignore
    except Exception as exc:  # pragma: no cover - the later TTS import reports this too
        log(f"torchaudio_import_failed={exc}")
        return

    if getattr(torchaudio.load, "_docupodcast_audio_loader_compat", False):
        return

    original_load = torchaudio.load

    def load_with_soundfile(uri: Any) -> tuple[Any, int]:
        try:
            import soundfile as sf  # type: ignore
        except Exception as error:
            raise ImportError(
                "No se pudo cargar audio de referencia porque faltan torchcodec y soundfile "
                "en el Python portable de DocuPodcast."
            ) from error

        data, sample_rate = sf.read(str(uri), dtype="float32", always_2d=True)
        audio = torch.from_numpy(data.T.copy())
        return audio, int(sample_rate)

    def docupodcast_torchaudio_load(uri: Any, *args: Any, **kwargs: Any) -> tuple[Any, int]:
        try:
            return original_load(uri, *args, **kwargs)
        except ImportError as exc:
            detail = str(exc).lower()
            if "torchcodec" not in detail:
                raise
            log("torchaudio_load_fallback=soundfile")
            return load_with_soundfile(uri)

    setattr(docupodcast_torchaudio_load, "_docupodcast_audio_loader_compat", True)
    torchaudio.load = docupodcast_torchaudio_load  # type: ignore[assignment]
    log("torchaudio_load=portable_fallback")


def main() -> int:
    args = parse_args()
    text_path = Path(args.text_file)
    output_path = Path(args.output)
    speaker_path = Path(args.speaker_wav)
    log("inicio")
    log(f"python={sys.executable}")

    if not text_path.is_file():
        raise SystemExit(f"Text file does not exist: {text_path}")
    if not speaker_path.is_file():
        raise SystemExit(f"Speaker WAV does not exist: {speaker_path}")

    model_root = resolve_model_root(args.model_dir)
    model_path = model_root / "model.pth"
    config_path = model_root / "config.json"
    vocab_path = model_root / "vocab.json"
    log(f"model_dir={model_root}")
    log(f"model_path={model_path}")
    log(f"config_path={config_path}")
    log(f"vocab_path={vocab_path}")

    text = text_path.read_text(encoding="utf-8-sig").strip().lstrip("\ufeff")
    if not text:
        raise SystemExit("Text file is empty")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    log(f"texto_cargado chars={len(text)}")

    try:
        log("importando_paquete_tts")
        configure_local_torch_deserialization()
        configure_local_audio_loader()
        from TTS.api import TTS  # type: ignore
    except Exception as exc:  # pragma: no cover - environment dependent
        detail = str(exc)
        if "BeamSearchScorer" in detail or "transformers" in detail:
            raise SystemExit(
                "Runtime de Voz IA avanzada incompatible: transformers no expone BeamSearchScorer. "
                "Vuelve a ejecutar scripts\\20-preparar-python-portable-coqui.bat. Detalle: " + detail
            )
        raise SystemExit(
            "Coqui TTS no es importable dentro del Python local de DocuPodcast. "
            "Repara el runtime desde Configuracion o ejecuta scripts\\20-preparar-python-portable-coqui.bat. "
            "Detalle: " + detail
        )

    device = resolve_device(args.device)
    log(f"voz_referencia={speaker_path}")
    log(f"device={device}")
    log("cargando_modelo")
    # Coqui XTTS names this argument model_path, but XTTS consumes it as checkpoint_dir.
    # Passing model.pth here makes XTTS append another model.pth internally.
    tts = TTS(model_path=str(model_root), config_path=str(config_path), progress_bar=False)

    if device:
        log(f"asignando_device={device}")
        tts.to(device)

    log("sintetizando")
    tts.tts_to_file(
        text=text,
        file_path=str(output_path),
        speaker_wav=str(speaker_path),
        language=args.language or "es",
    )
    if not output_path.is_file() or output_path.stat().st_size <= 44:
        raise SystemExit(f"No se genero WAV valido: {output_path}")
    log(f"wav_generado bytes={output_path.stat().st_size}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
