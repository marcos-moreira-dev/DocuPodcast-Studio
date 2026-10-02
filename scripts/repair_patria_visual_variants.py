"""Repair project-specific visual variants and final-scene backdrops.

The script is intentionally idempotent so it can be rerun after DocuPodcast closes
if an already-open session writes an older in-memory copy of the project.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
from datetime import datetime
from pathlib import Path


ACTIVE_PACKAGE = "c-mo-ser-la-patria-7e57fff73f7af385"
CHARACTER_VARIANTS = {
    "CHR-CONCHA": {
        "variant_id": "CHR-CONCHA-SOLDADO-TARQUI",
        "base_name": "CONCHA",
        "variant_name": "CONCHA_SOLDADO_TARQUI",
        "threshold": "INTERVENCION-400",
        "base_asset": "THEATRE-IMG-003",
        "variant_asset": "THEATRE-IMG-089",
        "voice_profile": "VOC-PRESET-HOMBRE-35-ASPIRACIONAL-EMOTIVO-PERSONAJE",
    },
    "CHR-ELOY": {
        "variant_id": "CHR-ELOY-SOLDADO-TARQUI",
        "base_name": "ELOY",
        "variant_name": "ELOY_SOLDADO_TARQUI",
        "threshold": "INTERVENCION-408",
        "base_asset": "THEATRE-IMG-002",
        "variant_asset": "THEATRE-IMG-090",
        "voice_profile": "VOC-PRESET-HOMBRE-20-IDEALISTA-ECUADOR",
    },
}
FINAL_SCENES = {
    "SCN-CUADRO-IXB-MEMORIAL-Y-CANCIN",
    "SCN-CUADRO-IXC-GAGS-DE-FLORINDOS",
    "SCN-CUADRO-IXD-EPLOGO-DEL-NARRADOR",
}
EMPTY_THEATRE_ASSET_ID = "THEATRE-TEATRO-VACIO-FRENTE"
EMPTY_THEATRE_BACKDROP_ID = "BDR-TEATRO-VACIO-FRENTE-FINAL"
EMPTY_THEATRE_RELATIVE = f"assets/theatre/{ACTIVE_PACKAGE}/assets/fondos/TEATRO-VACIO-FRENTE.png"
PACKAGE_EMPTY_THEATRE_RELATIVE = "assets/fondos/TEATRO-VACIO-FRENTE.png"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def intervention_number(value: str) -> int | None:
    match = re.fullmatch(r"INTERVENCION-(\d+)", value or "")
    return int(match.group(1)) if match else None


def expected_character_asset(character_id: str, intervention_id: str) -> str | None:
    number = intervention_number(intervention_id)
    if number is None:
        return None
    if character_id in {"CHR-CONCHA", "CHR-CONCHA-SOLDADO-TARQUI"}:
        return "THEATRE-IMG-089" if number >= 400 else "THEATRE-IMG-003"
    if character_id in {"CHR-ELOY", "CHR-ELOY-SOLDADO-TARQUI"}:
        return "THEATRE-IMG-090" if number >= 408 else "THEATRE-IMG-002"
    return None


def rewrite_variant_token(value: str, active: set[str]) -> str:
    rewritten = value
    for base_id, variant in CHARACTER_VARIANTS.items():
        base_name = variant["base_name"]
        variant_name = variant["variant_name"]
        desired = variant_name if base_id in active else base_name
        other = base_name if desired == variant_name else variant_name
        rewritten = re.sub(rf"(?<![A-Z0-9_]){re.escape(other)}(?![A-Z0-9_])", desired, rewritten)
    return rewritten


def normalize_markdown_variants(lines: list[str]) -> list[str]:
    """Give the two uniformed variants their own canonical speaker identities."""
    variant_names = {item["variant_name"] for item in CHARACTER_VARIANTS.values()}
    lines = [
        line for line in lines
        if not (line.startswith("- personaje: ") and any(
            line.startswith(f"- personaje: {name} |") for name in variant_names
        ))
    ]
    enriched: list[str] = []
    for line in lines:
        enriched.append(line)
        for base_id, variant in CHARACTER_VARIANTS.items():
            if line.startswith(f"- personaje: {variant['base_name']} |"):
                enriched.append(
                    f"- personaje: {variant['variant_name']} | id={variant['variant_id']} | "
                    f"aliases={variant['variant_name']} | voz={variant['voice_profile']} | "
                    f"nota=Variante uniformada de {variant['base_name']}, activa desde "
                    f"{variant['threshold']}; conserva la misma voz del personaje base."
                )

    active: set[str] = set()
    output = list(enriched)
    for index, line in enumerate(output):
        metadata = re.search(r"\bid=(INTERVENCION-\d+)\b", line)
        if not metadata:
            continue
        intervention_id = metadata.group(1)
        for base_id, variant in CHARACTER_VARIANTS.items():
            if intervention_id == variant["threshold"]:
                active.add(base_id)
        if "interaccion=" in line:
            output[index] = rewrite_variant_token(line, active)
        speaker_index = index - 1
        while speaker_index >= 0 and not output[speaker_index].strip():
            speaker_index -= 1
        if speaker_index < 0:
            continue
        speaker_match = re.match(r"^([A-ZÁÉÍÓÚÑ0-9_]+):", output[speaker_index])
        if not speaker_match:
            continue
        speaker = speaker_match.group(1)
        for base_id, variant in CHARACTER_VARIANTS.items():
            if speaker not in {variant["base_name"], variant["variant_name"]}:
                continue
            desired = variant["variant_name"] if base_id in active else variant["base_name"]
            output[speaker_index] = desired + output[speaker_index][len(speaker):]
            break
    return output


def rewrite_markdown(path: Path) -> dict[str, tuple[str, str]]:
    lines = normalize_markdown_variants(path.read_text(encoding="utf-8").splitlines())
    current_speaker = ""
    current_scene = ""
    assignments: dict[str, tuple[str, str]] = {}
    output: list[str] = []
    for line in lines:
        if line.startswith("- personaje: CONCHA |"):
            line = re.sub(r"\| nota=.*$", "| nota=Concha base hasta que la Anciana le coloca el sombrero; desde INTERVENCION-400 continúa como CONCHA_SOLDADO_TARQUI.", line)
        elif line.startswith("- personaje: ELOY |"):
            line = re.sub(r"\| nota=.*$", "| nota=Eloy base durante la entrega del uniforme; desde INTERVENCION-408, después de ponerse la última prenda, continúa como ELOY_SOLDADO_TARQUI.", line)
        scene_match = re.match(r"^### Escena:.*\| id=([^ |]+)", line)
        if scene_match:
            current_scene = scene_match.group(1)
        speaker_match = re.match(r"^([A-ZÁÉÍÓÚÑ0-9_]+):", line)
        if speaker_match:
            current_speaker = speaker_match.group(1)
        if line.startswith("> mapa_espacial=") and current_scene in FINAL_SCENES:
            if "fondo_escenario=" in line:
                line = re.sub(r"fondo_escenario=[^ |]+", f"fondo_escenario={PACKAGE_EMPTY_THEATRE_RELATIVE}", line)
            else:
                line += f" | fondo_escenario={PACKAGE_EMPTY_THEATRE_RELATIVE}"
        metadata = re.search(r"\bid=(INTERVENCION-\d+)\b", line)
        if metadata:
            intervention_id = metadata.group(1)
            character_id = {
                "CONCHA": "CHR-CONCHA",
                "ELOY": "CHR-ELOY",
                "CONCHA_SOLDADO_TARQUI": "CHR-CONCHA-SOLDADO-TARQUI",
                "ELOY_SOLDADO_TARQUI": "CHR-ELOY-SOLDADO-TARQUI",
            }.get(current_speaker)
            asset_id = expected_character_asset(character_id or "", intervention_id)
            if asset_id:
                image_path = "assets/frames/IMG-089.png" if asset_id.endswith("089") else (
                    "assets/frames/IMG-090.png" if asset_id.endswith("090") else (
                        "assets/personajes/IMG-003.png" if asset_id.endswith("003") else "assets/personajes/IMG-002.png"
                    )
                )
                if "imagen=" in line:
                    line = re.sub(r"imagen=[^ |]+", f"imagen={image_path}", line)
                else:
                    line += f" | imagen={image_path}"
                assignments[intervention_id] = (character_id, asset_id)
            if current_scene in FINAL_SCENES and current_speaker != "ACOTACIÓN":
                if "fondo=" in line:
                    line = re.sub(r"fondo=[^ |]+", f"fondo={PACKAGE_EMPTY_THEATRE_RELATIVE}", line)
                else:
                    line += f" | fondo={PACKAGE_EMPTY_THEATRE_RELATIVE}"
        output.append(line)
    path.write_text("\n".join(output) + "\n", encoding="utf-8")
    return assignments


def upsert_asset(items: list[dict], asset: dict) -> None:
    for index, current in enumerate(items):
        if current.get("id") == asset["id"]:
            items[index] = asset
            return
    items.append(asset)


def upsert_by_key(items: list[dict], key: str, value: str, item: dict) -> None:
    for index, current in enumerate(items):
        if current.get(key) == value:
            items[index] = item
            return
    items.append(item)


def repair_project(project_root: Path, empty_theatre_source: Path, backup: bool) -> None:
    project_file = project_root / "obra.docupodcast.json"
    package_root = project_root / "assets" / "theatre" / ACTIVE_PACKAGE
    manifest_file = package_root / "docupodcast-theatre.json"
    source_files = [project_root / "source" / "obra.teatro.md", package_root / "obra.teatro.md"]
    required = [project_file, manifest_file, *source_files, empty_theatre_source]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise FileNotFoundError("Missing required files: " + ", ".join(missing))

    if backup:
        stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        backup_root = project_root / "backups" / f"visual-variants-{stamp}"
        backup_root.mkdir(parents=True, exist_ok=False)
        shutil.copy2(project_file, backup_root / project_file.name)
        shutil.copy2(manifest_file, backup_root / manifest_file.name)
        for source in source_files:
            prefix = "canonical-" if source.parent.name == "source" else "package-"
            shutil.copy2(source, backup_root / f"{prefix}{source.name}")

    backdrop_file = package_root / PACKAGE_EMPTY_THEATRE_RELATIVE
    backdrop_file.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(empty_theatre_source, backdrop_file)
    backdrop_hash = sha256(backdrop_file)
    backdrop_size = backdrop_file.stat().st_size

    canonical_assignments = rewrite_markdown(source_files[0])
    package_assignments = rewrite_markdown(source_files[1])
    if canonical_assignments != package_assignments:
        raise ValueError("Canonical and package theatre sources produced different variant assignments")

    manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
    manifest_assets = manifest["assets"]
    bindings_by_asset: dict[str, list[str]] = {}
    for intervention_id, (_, asset_id) in canonical_assignments.items():
        logical_id = asset_id.removeprefix("THEATRE-")
        bindings_by_asset.setdefault(logical_id, []).append(intervention_id)
    for logical_id in ("IMG-002", "IMG-003", "IMG-089", "IMG-090"):
        asset = next(item for item in manifest_assets if item.get("logicalId") == logical_id)
        ids = sorted(bindings_by_asset.get(logical_id, []), key=lambda value: intervention_number(value) or 0)
        asset["bindings"] = [{"kind": "INTERVENTION_IMAGE", "interventionId": value} for value in ids[1:]]
        if ids:
            asset["interventionId"] = ids[0]
        else:
            asset.pop("interventionId", None)
    concha_soldier = next(item for item in manifest_assets if item.get("logicalId") == "IMG-089")
    concha_soldier.update(characterId="CHR-CONCHA-SOLDADO-TARQUI", view="soldado_tarqui_frontal")
    eloy_soldier = next(item for item in manifest_assets if item.get("logicalId") == "IMG-090")
    eloy_soldier.update(characterId="CHR-ELOY-SOLDADO-TARQUI", view="soldado_tarqui_frontal")
    backdrop_manifest = {
        "path": PACKAGE_EMPTY_THEATRE_RELATIVE,
        "logicalId": "TEATRO-VACIO-FRENTE",
        "kind": "BACKDROP",
        "sha256": backdrop_hash,
        "size": backdrop_size,
        "backdropId": EMPTY_THEATRE_BACKDROP_ID,
        "displayName": "Teatro vacío frontal",
        "scope": "SCENE",
        "scopeId": sorted(FINAL_SCENES)[0],
        "bindings": [{"kind": "BACKDROP", "sceneId": scene} for scene in sorted(FINAL_SCENES)[1:]],
    }
    upsert_by_key(manifest_assets, "logicalId", "TEATRO-VACIO-FRENTE", backdrop_manifest)
    manifest_file.write_text(json.dumps(manifest, ensure_ascii=False, indent=4) + "\n", encoding="utf-8")

    project = json.loads(project_file.read_text(encoding="utf-8"))
    theatre = project["theatre"]
    sequence_by_intervention = {
        item["id"]: item["sequenceIndex"] for item in theatre["intervenciones"]
    }
    threshold_sequence = {
        base_id: sequence_by_intervention[variant["threshold"]]
        for base_id, variant in CHARACTER_VARIANTS.items()
    }

    def variant_active(base_id: str, intervention_id: str) -> bool:
        return sequence_by_intervention.get(intervention_id, -1) >= threshold_sequence[base_id]

    def migrate_character_id(character_id: str, intervention_id: str) -> str:
        for base_id, variant in CHARACTER_VARIANTS.items():
            if character_id not in {base_id, variant["variant_id"]}:
                continue
            return variant["variant_id"] if variant_active(base_id, intervention_id) else base_id
        return character_id

    for base_id, variant in CHARACTER_VARIANTS.items():
        base_character = next(item for item in theatre["characters"] if item.get("id") == base_id)
        base_character["notes"] = (
            f"Identidad base antes de {variant['threshold']}; después continúa como "
            f"{variant['variant_name']}."
        )
        upsert_by_key(theatre["characters"], "id", variant["variant_id"], {
            "id": variant["variant_id"],
            "displayName": variant["variant_name"],
            "aliases": [variant["variant_name"]],
            "notes": (
                f"Variante uniformada de {variant['base_name']}, activa desde "
                f"{variant['threshold']}; conserva identidad vocal."
            ),
        })
        upsert_by_key(theatre["voiceRoleAliases"], "characterId", variant["variant_id"], {
            "id": f"VOICE-ROLE-{variant['variant_id']}",
            "displayName": variant["variant_name"],
            "voiceProfileId": variant["voice_profile"],
            "characterId": variant["variant_id"],
            "notes": f"Misma voz que {variant['base_name']}; variante visual de uniforme.",
        })

    upsert_by_key(theatre["characterImages"], "id", "CHARIMG-CONCHA-SOLDADO-TARQUI", {
        "id": "CHARIMG-CONCHA-SOLDADO-TARQUI",
        "characterId": "CHR-CONCHA-SOLDADO-TARQUI",
        "sceneId": "",
        "view": "soldado_tarqui_frontal",
        "assetId": "THEATRE-IMG-089",
        "notes": "Concha con sombrero de Tarqui desde INTERVENCION-400, justo después de que la Anciana se lo coloca.",
    })
    upsert_by_key(theatre["characterImages"], "id", "CHARIMG-ELOY-SOLDADO-TARQUI", {
        "id": "CHARIMG-ELOY-SOLDADO-TARQUI",
        "characterId": "CHR-ELOY-SOLDADO-TARQUI",
        "sceneId": "",
        "view": "soldado_tarqui_frontal",
        "assetId": "THEATRE-IMG-090",
        "notes": "Eloy con uniforme de Tarqui desde INTERVENCION-408, después de ponerse la última prenda.",
    })

    placements = {item["intervencionId"]: item for item in theatre["textActionPlacements"]}
    for intervention_id, placement in placements.items():
        placement["characterId"] = migrate_character_id(
            placement.get("characterId", ""), intervention_id
        )
        active_ids = {
            base_id for base_id in CHARACTER_VARIANTS
            if variant_active(base_id, intervention_id)
        }
        placement["interactionTarget"] = rewrite_variant_token(
            placement.get("interactionTarget", ""), active_ids
        )
        locations = placement.get("characterLocations", {})
        for base_id, variant in CHARACTER_VARIANTS.items():
            base_name = variant["base_name"]
            variant_name = variant["variant_name"]
            desired = variant_name if base_id in active_ids else base_name
            other = base_name if desired == variant_name else variant_name
            if other in locations:
                locations[desired] = locations.pop(other)

    for position in theatre["positions"]:
        intervention_id = position.get("alias", "")
        position["characterId"] = migrate_character_id(
            position.get("characterId", ""), intervention_id
        )
    for action in theatre["actions"]:
        intervention_id = action.get("fromAlias", "")
        action["characterId"] = migrate_character_id(
            action.get("characterId", ""), intervention_id
        )
        active_ids = {
            base_id for base_id in CHARACTER_VARIANTS
            if variant_active(base_id, intervention_id)
        }
        action["description"] = rewrite_variant_token(
            action.get("description", ""), active_ids
        )
    expected_assets: dict[str, str] = {}
    for intervention_id, placement in placements.items():
        expected = expected_character_asset(placement.get("characterId", ""), intervention_id)
        if expected:
            expected_assets[intervention_id] = expected
    for visual in theatre["intervencionesVisuales"]:
        expected = expected_assets.get(visual.get("intervencionId", ""))
        if expected:
            visual["assetId"] = expected
            visual["notes"] = "Variante visual autoconfigurada por identidad y punto de cambio del proyecto."
    for layer in project["narrativeLayers"]["assignments"]:
        if layer.get("kind") != "IMAGE":
            continue
        segment_id = layer.get("segmentId", "")
        intervention_id = segment_id.removeprefix("SEG-")
        expected = expected_assets.get(intervention_id)
        if expected:
            layer["targetId"] = expected
            layer["notes"] = "Variante visual autoconfigurada por identidad y punto de cambio del proyecto."
    root_assets = project["assets"]["items"]
    upsert_asset(root_assets, {
        "id": EMPTY_THEATRE_ASSET_ID,
        "kind": "IMAGE",
        "displayName": "Teatro vacío frontal.png",
        "relativePath": EMPTY_THEATRE_RELATIVE,
        "mimeType": "image/png",
        "purpose": "BACKDROP",
        "checksum": backdrop_hash,
        "notes": "Plano general frontal usado en Memorial y canción, Gags de Florindos y Epílogo del Narrador.",
    })
    for asset in root_assets:
        if asset.get("id") == "THEATRE-IMG-089":
            concha_file = package_root / "assets" / "frames" / "IMG-089.png"
            asset["displayName"] = "Concha soldado Tarqui.png"
            asset["relativePath"] = f"assets/theatre/{ACTIVE_PACKAGE}/assets/frames/IMG-089.png"
            asset["checksum"] = sha256(concha_file)
            asset["notes"] = "Imagen de CHR-CONCHA-SOLDADO-TARQUI, activa desde INTERVENCION-400."
        elif asset.get("id") == "THEATRE-IMG-090":
            asset["displayName"] = "Eloy soldado Tarqui.png"
            asset["notes"] = "Imagen de CHR-ELOY-SOLDADO-TARQUI, activa desde INTERVENCION-408."

    backdrop = {
        "id": EMPTY_THEATRE_BACKDROP_ID,
        "displayName": "Teatro vacío frontal",
        "assetId": EMPTY_THEATRE_ASSET_ID,
        "notes": "Fondo final del proyecto, plano general visto de frente.",
    }
    upsert_by_key(theatre["stageBackdrops"], "id", EMPTY_THEATRE_BACKDROP_ID, backdrop)
    theatre["stageBackdropAssignments"] = [
        item for item in theatre["stageBackdropAssignments"]
        if not (item.get("scope") == "SCENE" and item.get("scopeId") in FINAL_SCENES)
        and not (item.get("scope") == "INTERVENTION" and placements.get(item.get("scopeId", ""), {}).get("sceneId") in FINAL_SCENES)
    ]
    for scene in sorted(FINAL_SCENES):
        theatre["stageBackdropAssignments"].append({
            "scope": "SCENE", "scopeId": scene, "backdropId": EMPTY_THEATRE_BACKDROP_ID,
            "notes": "Teatro vacío frontal configurado para el tramo final.",
        })
    for intervention_id, placement in placements.items():
        if placement.get("sceneId") in FINAL_SCENES:
            theatre["stageBackdropAssignments"].append({
                "scope": "INTERVENTION", "scopeId": intervention_id, "backdropId": EMPTY_THEATRE_BACKDROP_ID,
                "notes": "Fondo heredado del tramo final: teatro vacío frontal.",
            })
    project_file.write_text(json.dumps(project, ensure_ascii=False, indent=4) + "\n", encoding="utf-8")

    print(f"Updated {project_file}")
    print(f"Visual assignments: {len(expected_assets)}; final backdrop assignments: "
          f"{sum(1 for item in theatre['stageBackdropAssignments'] if item.get('backdropId') == EMPTY_THEATRE_BACKDROP_ID)}")
    print(f"Empty theatre SHA-256: {backdrop_hash}; size: {backdrop_size}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("project_root", type=Path)
    parser.add_argument("empty_theatre_source", type=Path)
    parser.add_argument("--no-backup", action="store_true")
    args = parser.parse_args()
    repair_project(args.project_root, args.empty_theatre_source, not args.no_backup)


if __name__ == "__main__":
    main()
