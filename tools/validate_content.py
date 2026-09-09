#!/usr/bin/env python3
"""Fail-fast integrity checks for the generated encyclopedia dataset."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    data = json.loads(args.dataset.read_text(encoding="utf-8"))
    report = json.loads(args.report.read_text(encoding="utf-8"))
    drugs = data["drugs"]
    entries = data["entries"]
    kinds = Counter(entry["kind"] for entry in entries)

    require(data["schemaVersion"] == 1, "Unexpected schema version")
    require(len(data["sources"]) == 8, "Expected eight non-duplicate source editions")
    require(sum(source["pages"] for source in data["sources"]) == 381, "Source page total changed")
    require(len(data.get("excludedDuplicates", [])) == 1, "Combined duplicate audit record missing")
    require(data["excludedDuplicates"][0]["pages"] == 140, "Combined master page count changed")
    require(len(drugs) >= 105, "Too few structured antibiotic cards")
    require(len(entries) >= 1600, "Too few searchable records")
    require(len({drug["name"].casefold() for drug in drugs}) == len(drugs), "Duplicate drug name")

    for kind, floor in {
        "antibiotic": 250,
        "bacteria": 100,
        "infection": 160,
        "distribution": 30,
        "culture": 500,
        "quick": 120,
    }.items():
        require(kinds[kind] >= floor, f"Too few {kind} entries: {kinds[kind]}")

    mandatory_fields = (
        "spectrumUse", "adultDose", "renalRrt", "administrationPkPd", "toxicityMonitoring"
    )
    for drug in drugs:
        for field in mandatory_fields:
            require(bool(drug[field].strip()), f"{drug['name']} missing {field}")
        require(len(drug["sourceRefs"]) >= 1, f"{drug['name']} missing source reference")
        require(len(drug["name"]) <= 80, f"Suspicious merged drug name: {drug['name']}")
        require(not drug["name"].lower().startswith("regi"), "Regimen table parsed as a drug")

    by_name = {drug["name"].casefold(): drug for drug in drugs}
    expected = {
        "meropenem": ("1 g IV q8h", "Renal"),
        "ceftriaxone": ("1-2 g IV q24h", "renal"),
        "piperacillin-tazobactam": ("4.5 g IV q6h", "CRRT"),
        "vancomycin iv": ("AUC", "AUC"),
        "rifampin": ("600 mg", "No routine adjustment"),
    }
    for name, (dose_fragment, renal_fragment) in expected.items():
        require(name in by_name, f"Missing key drug: {name}")
        drug = by_name[name]
        require(dose_fragment.casefold() in drug["adultDose"].casefold(), f"Dose mismatch for {name}")
        require(renal_fragment.casefold() in drug["renalRrt"].casefold(), f"Renal field mismatch for {name}")

    require(sum(bool(drug["hepaticAdjustment"]) for drug in drugs) >= 50, "Hepatic notes under-extracted")
    require(sum(bool(drug["distributionSummary"]) for drug in drugs) >= 75, "Distribution links under-extracted")

    corpus = "\n".join(
        [drug["name"] for drug in drugs]
        + [f"{entry['title']}\n{entry['text']}" for entry in entries]
    ).casefold()
    for term in (
        "pneumonia", "meningitis", "osteomyelitis", "urinary tract", "intra-abdominal",
        "staphylococcus aureus", "pseudomonas", "escherichia coli", "enterococcus",
        "cns", "lung", "bone", "prostate", "urine", "bile", "intracellular",
        "mic", "antimicrobial susceptibility", "blood culture",
    ):
        require(term in corpus, f"Search corpus missing expected term: {term}")

    require(report["datasetSha256"] == file_sha256(args.dataset), "Dataset SHA does not match report")
    require(report["drugCount"] == len(drugs), "Drug count report mismatch")
    require(report["entryCount"] == len(entries), "Entry count report mismatch")

    suspicious = [
        drug["name"]
        for drug in drugs
        if re.search(r"\b(?:eligibility|components and duration|standard ds-tb)\b", drug["name"], re.I)
    ]
    require(not suspicious, f"Suspicious drug names: {suspicious}")

    print(
        json.dumps(
            {
                "status": "PASS",
                "sources": len(data["sources"]),
                "pages": sum(source["pages"] for source in data["sources"]),
                "drugs": len(drugs),
                "drugsWithHepaticNotes": sum(bool(drug["hepaticAdjustment"]) for drug in drugs),
                "drugsWithDistribution": sum(bool(drug["distributionSummary"]) for drug in drugs),
                "entries": len(entries),
                "entriesByKind": dict(sorted(kinds.items())),
                "sha256": file_sha256(args.dataset),
            },
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
