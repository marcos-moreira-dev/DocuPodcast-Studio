from __future__ import annotations

import argparse
import json
import unicodedata
from collections import Counter
from pathlib import Path


CORE_POSITIONS = {
    "fondo derecha", "fondo centro", "fondo izquierda",
    "centro derecha", "centro", "centro izquierda",
    "frente derecha", "frente centro", "frente izquierda",
    "hacia el publico", "extra diegetico", "diegetico",
}
NON_STAGE_POSITIONS = {"fuera escena", "fuera de escena", "no presente", "offstage"}


def normalize(value: object) -> str:
    text = "" if value is None else str(value).strip().lower()
    text = "".join(
        character for character in unicodedata.normalize("NFD", text)
        if unicodedata.category(character) != "Mn"
    )
    return " ".join(text.replace("_", " ").replace("-", " ").split())


def markdown_placements(path: Path) -> dict[str, dict[str, object]]:
    result: dict[str, dict[str, object]] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.startswith("> id=INTERVENCION-"):
            continue
        fields: dict[str, str] = {}
        for part in line[2:].split("|"):
            key, separator, value = part.strip().partition("=")
            if separator:
                fields[key.strip()] = value.strip()
        intervention_id = fields.get("id", "")
        locations: dict[str, str] = {}
        for participant in fields.get("presentes", "").split(","):
            name, separator, location = participant.strip().partition("@")
            if separator and name.strip():
                locations[name.strip()] = location.strip()
        result[intervention_id] = {
            "origin": fields.get("origen", ""),
            "destination": fields.get("destino", ""),
            "interactionTarget": fields.get("interaccion", ""),
            "characterLocations": locations,
        }
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description="Audit all theatre spatial positions in MD and JSON.")
    parser.add_argument("project_root", type=Path)
    args = parser.parse_args()
    root = args.project_root.resolve()
    project = json.loads((root / "obra.docupodcast.json").read_text(encoding="utf-8"))
    json_items = {
        item["intervencionId"]: item
        for item in project["theatre"]["textActionPlacements"]
    }
    md_items = markdown_placements(root / "source" / "obra.teatro.md")

    mismatches: list[str] = []
    unknown: list[str] = []
    counts: Counter[str] = Counter()
    for intervention_id, item in json_items.items():
        md = md_items.get(intervention_id)
        if md is None:
            mismatches.append(f"{intervention_id}: missing from Markdown")
            continue
        for field in ("origin", "destination"):
            json_value = normalize(item.get(field, ""))
            md_value = normalize(md.get(field, ""))
            if json_value != md_value:
                mismatches.append(
                    f"{intervention_id}: {field} JSON={item.get(field, '')!r} MD={md.get(field, '')!r}"
                )
            if json_value:
                counts[json_value] += 1
                if json_value not in CORE_POSITIONS | NON_STAGE_POSITIONS | {"publico", "lateral narrador"}:
                    unknown.append(f"{intervention_id}: {field}={item.get(field, '')!r}")

        json_locations = {
            name: normalize(location)
            for name, location in item.get("characterLocations", {}).items()
        }
        md_locations = {
            name: normalize(location)
            for name, location in md.get("characterLocations", {}).items()
        }
        if json_locations != md_locations:
            mismatches.append(f"{intervention_id}: characterLocations/presentes differ")
        for name, location in json_locations.items():
            counts[location] += 1
            if location not in CORE_POSITIONS | NON_STAGE_POSITIONS | {"lateral narrador"}:
                unknown.append(f"{intervention_id}: {name}@{location}")

    for intervention_id in sorted(set(md_items) - set(json_items)):
        mismatches.append(f"{intervention_id}: missing from JSON")

    print(f"JSON placements: {len(json_items)}")
    print(f"Markdown placements: {len(md_items)}")
    print(f"Spatial mismatches: {len(mismatches)}")
    print(f"Unknown positions: {len(unknown)}")
    print("Position distribution:")
    for position, count in counts.most_common():
        print(f"  {position or '[empty]'}: {count}")
    if mismatches:
        print("Mismatches:")
        for message in mismatches:
            print(f"  {message}")
    if unknown:
        print("Unknown positions:")
        for message in unknown:
            print(f"  {message}")
    raise SystemExit(1 if mismatches or unknown else 0)


if __name__ == "__main__":
    main()
