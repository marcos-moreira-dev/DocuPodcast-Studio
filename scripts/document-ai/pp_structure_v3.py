"""Offline PP-StructureV3 bridge used by DocuPodcast Studio.

The runtime and every model directory must already exist. This bridge never
allows Paddle to choose or download a model implicitly.
"""
from __future__ import annotations

import argparse
import inspect
import json
import os
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--models-root", required=True)
    parser.add_argument("--operation", choices=("layout", "table", "math"), required=True)
    parser.add_argument("--device", choices=("auto", "cpu", "gpu"), default="auto")
    args = parser.parse_args()

    source = Path(args.input).resolve(strict=True)
    output = Path(args.output).resolve()
    models_root = Path(args.models_root).resolve(strict=True)
    manifest_path = models_root / "manifest.json"
    if not manifest_path.is_file():
        raise RuntimeError("Falta models/manifest.json del paquete PP-StructureV3 administrado.")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    configured = manifest.get("constructor", {})
    if not isinstance(configured, dict):
        raise RuntimeError("El manifest PP-StructureV3 no contiene constructor.")

    required = ["layout_detection_model_dir", "text_detection_model_dir",
                "text_recognition_model_dir"]
    table_models = [
        "table_classification_model_dir",
        "wired_table_structure_recognition_model_dir",
        "wireless_table_structure_recognition_model_dir",
        "wired_table_cells_detection_model_dir",
        "wireless_table_cells_detection_model_dir",
        "table_orientation_classify_model_dir",
    ]
    if args.operation == "table":
        required.extend(table_models)
    if args.operation == "math":
        required.append("formula_recognition_model_dir")
    resolved: dict[str, object] = {}
    for key, value in configured.items():
        if not isinstance(value, str) or not value.strip():
            continue
        candidate = (models_root / value).resolve()
        if not candidate.is_dir() or models_root not in candidate.parents:
            raise RuntimeError(f"Modelo local inválido para {key}: {candidate}")
        resolved[key] = str(candidate)
    missing = [key for key in required if key not in resolved]
    if missing:
        raise RuntimeError("Faltan modelos locales: " + ", ".join(missing))

    os.environ["PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK"] = "True"
    os.environ["PADDLEOCR_HOME"] = str(models_root)
    if args.device == "cpu":
        os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    # Disable every known implicit model source before importing PaddleX.
    os.environ["PADDLE_PDX_MODEL_SOURCE"] = "local"
    from paddleocr import PPStructureV3  # imported only after download guards
    import paddle

    compiled_with_cuda = bool(paddle.device.is_compiled_with_cuda())
    if args.device == "gpu" and not compiled_with_cuda:
        raise RuntimeError("DEVICE_MISMATCH: el perfil GPU no dispone de Paddle CUDA.")
    effective_device = (
        "gpu" if args.device == "gpu"
        or (args.device == "auto" and compiled_with_cuda) else "cpu"
    )

    accepted = inspect.signature(PPStructureV3.__init__).parameters
    constructor = {key: value for key, value in resolved.items() if key in accepted}
    feature_flags = {
        "use_doc_orientation_classify":
            "doc_orientation_classify_model_dir" in resolved,
        "use_doc_unwarping": "doc_unwarping_model_dir" in resolved,
        "use_textline_orientation":
            "textline_orientation_model_dir" in resolved,
        "use_table_recognition": args.operation == "table",
        "use_formula_recognition": args.operation == "math",
        "use_chart_recognition": False,
        "use_seal_recognition": False,
    }
    constructor.update({
        key: value for key, value in feature_flags.items() if key in accepted
    })
    if "device" in accepted:
        constructor["device"] = effective_device
    elif "use_gpu" in accepted:
        constructor["use_gpu"] = effective_device == "gpu"
    pipeline = PPStructureV3(**constructor)
    results = []
    for result in pipeline.predict(input=str(source)):
        payload = getattr(result, "json", result)
        if callable(payload):
            payload = payload()
        if isinstance(payload, str):
            payload = json.loads(payload)
        results.append(payload)
    output.parent.mkdir(parents=True, exist_ok=True)
    temp = output.with_suffix(output.suffix + ".tmp")
    temp.write_text(json.dumps({
        "schemaVersion": 1,
        "operation": args.operation,
        "requestedDevice": args.device,
        "device": effective_device,
        "source": source.name,
        "results": results,
    }, ensure_ascii=False), encoding="utf-8")
    os.replace(temp, output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
