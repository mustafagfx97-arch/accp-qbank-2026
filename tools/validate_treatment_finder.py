#!/usr/bin/env python3
"""Validate the source-backed inputs used by the offline therapy decision aid."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


REQUIRED_SYNDROME_FIELDS = {
    "Likely pathogens",
    "Empiric regimen",
    "Key modifier / action",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--taxonomy", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    dataset = json.loads(args.dataset.read_text(encoding="utf-8"))
    taxonomy = json.loads(args.taxonomy.read_text(encoding="utf-8"))
    entries = {entry["id"]: entry for entry in dataset["entries"]}

    syndrome_ids = [
        entry_id
        for group in taxonomy["infectionGroups"]
        for entry_id in group["entryIds"]
    ]
    organism_ids = list(
        dict.fromkeys(
            entry_id
            for family in taxonomy["organismFamilies"]
            for entry_id in family["organismEntryIds"]
        )
    )
    tissue_sites = taxonomy["tissueSites"]

    assert len(syndrome_ids) == 78, "Expected 78 curated syndrome choices"
    assert len(set(syndrome_ids)) == len(syndrome_ids), "Syndrome choices must be unique"
    assert len(organism_ids) >= 70, "Organism picker is unexpectedly small"
    assert len(tissue_sites) == 10, "Expected 10 tissue-site choices"

    missing_ids = [entry_id for entry_id in syndrome_ids + organism_ids if entry_id not in entries]
    assert not missing_ids, f"Missing decision-support entries: {missing_ids[:5]}"

    incomplete_syndromes: list[dict[str, object]] = []
    for entry_id in syndrome_ids:
        entry = entries[entry_id]
        fields = {field["label"]: field["value"].strip() for field in entry.get("fields", [])}
        missing = sorted(label for label in REQUIRED_SYNDROME_FIELDS if not fields.get(label))
        if missing:
            incomplete_syndromes.append({"id": entry_id, "missing": missing})
    assert not incomplete_syndromes, f"Incomplete syndrome rows: {incomplete_syndromes[:3]}"

    tissue_reference_ids = [
        entry_id
        for site in tissue_sites
        for entry_id in site["summaryEntryIds"] + site["supportEntryIds"]
    ]
    missing_tissue_refs = [entry_id for entry_id in tissue_reference_ids if entry_id not in entries]
    assert not missing_tissue_refs, f"Missing tissue evidence: {missing_tissue_refs}"

    report = {
        "status": "passed",
        "design": "deterministic-offline-source-crosswalk",
        "directRegimens": len(syndrome_ids),
        "organismChoices": len(organism_ids),
        "tissueSites": len(tissue_sites),
        "spectrumRows": len(taxonomy["spectrumEntryIds"]),
        "penetrationRows": len(taxonomy["penetrationEntryIds"]),
        "guardrails": [
            "no generated regimens",
            "site-only and organism-only queries abstain from a single regimen",
            "severe/shock warning",
            "beta-lactam allergy warning",
            "renal/RRT monograph links",
            "hepatic monograph links",
            "AST/MIC and local-protocol reminder",
        ],
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
