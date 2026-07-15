"""Consolidate a Diffusers sharded T5 safetensors folder without loading tensors into RAM."""

from __future__ import annotations

import json
import os
import struct
import sys
from pathlib import Path


def read_header(path: Path) -> tuple[int, dict]:
    with path.open("rb") as stream:
        raw = stream.read(8)
        if len(raw) != 8:
            raise ValueError(f"Cabecera safetensors incompleta: {path.name}")
        header_length = struct.unpack("<Q", raw)[0]
        header = json.loads(stream.read(header_length).decode("utf-8").rstrip(" "))
        return 8 + header_length, header


def validate_config(source: Path) -> None:
    config = json.loads((source / "config.json").read_text(encoding="utf-8"))
    architectures = config.get("architectures", [])
    if config.get("model_type") != "t5" or "T5EncoderModel" not in architectures:
        raise ValueError("La carpeta no describe un T5EncoderModel oficial.")


def consolidate(source: Path, destination: Path) -> None:
    validate_config(source)
    index_path = source / "model.safetensors.index.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    weight_map: dict[str, str] = index["weight_map"]
    shard_headers: dict[str, tuple[int, dict]] = {}
    for shard_name in sorted(set(weight_map.values())):
        shard = source / shard_name
        if not shard.is_file():
            raise FileNotFoundError(f"Falta shard: {shard_name}")
        shard_headers[shard_name] = read_header(shard)

    tensors: dict[str, dict] = {}
    offset = 0
    ordered_names = list(weight_map.keys())
    for name in ordered_names:
        shard_name = weight_map[name]
        entry = shard_headers[shard_name][1].get(name)
        if not entry:
            raise ValueError(f"Tensor {name} no aparece en {shard_name}")
        start, end = entry["data_offsets"]
        length = end - start
        tensors[name] = {"dtype": entry["dtype"], "shape": entry["shape"], "data_offsets": [offset, offset + length]}
        offset += length

    tensors["__metadata__"] = {"format": "pt", "architecture": "T5EncoderModel", "source": "sharded-diffusers"}
    header = json.dumps(tensors, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    padding = (-len(header)) % 8
    header += b" " * padding
    part = destination.with_name(destination.name + ".part")
    destination.parent.mkdir(parents=True, exist_ok=True)
    if part.exists():
        part.unlink()

    total = len(ordered_names)
    copied = 0
    try:
        with part.open("wb") as output:
            output.write(struct.pack("<Q", len(header)))
            output.write(header)
            for position, name in enumerate(ordered_names, 1):
                shard_name = weight_map[name]
                data_start, shard_header = shard_headers[shard_name]
                start, end = shard_header[name]["data_offsets"]
                remaining = end - start
                with (source / shard_name).open("rb") as shard:
                    shard.seek(data_start + start)
                    while remaining:
                        chunk = shard.read(min(16 * 1024 * 1024, remaining))
                        if not chunk:
                            raise IOError(f"Shard truncado: {shard_name}")
                        output.write(chunk)
                        remaining -= len(chunk)
                        copied += len(chunk)
                if position == total or position % max(1, total // 20) == 0:
                    print(f"Consolidando T5: {position}/{total} tensores ({copied / 1024**3:.2f} GB)", flush=True)
        os.replace(part, destination)
        from safetensors import safe_open
        with safe_open(destination, framework="pt", device="cpu") as handle:
            if len(handle.keys()) != total:
                raise ValueError("La validacion final no encontro todos los tensores.")
        print(f"T5 consolidado y validado: {destination}", flush=True)
    except BaseException:
        if part.exists():
            part.unlink()
        raise


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Uso: merge_safetensors_shards.py <source-folder> <destination.safetensors>")
    consolidate(Path(sys.argv[1]).resolve(), Path(sys.argv[2]).resolve())
