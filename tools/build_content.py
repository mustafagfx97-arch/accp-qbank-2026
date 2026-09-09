#!/usr/bin/env python3
"""Build the offline Antibiotic Encyclopedia dataset from the source PDFs.

The pipeline deliberately keeps two representations:

1. Source-faithful page text so no searchable clinical statement is lost.
2. Structured table rows for mobile cards and faceted search.

The source PDFs are not copied into the Android repository. Every generated
record retains its source filename and physical PDF page number.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter, defaultdict
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any, Iterable

import pdfplumber


@dataclass(frozen=True)
class SourceSpec:
    id: str
    filename: str
    title: str
    role: str
    quick: bool = False


# Complete_Antibiotic_Master_Reference_2026.pdf is intentionally excluded: its
# three clinical volumes are represented below by their standalone editions,
# which preserve clearer page references. This avoids ingesting the same 138
# pages twice.
SOURCES = (
    SourceSpec(
        "drug_handbook",
        "Focused_Concise_Antibiotic_Reference_2026.pdf",
        "Focused & Concise Antibiotic Reference 2026",
        "Core drug pharmacology, dosing, renal/RRT and safety",
    ),
    SourceSpec(
        "spectrum_distribution",
        "Clinical_Antibiotic_Spectrum_and_Tissue_Distribution_Handbook_2026.pdf",
        "Clinical Antibiotic Spectrum & Tissue Distribution Handbook 2026",
        "Organisms, spectrum, tissue penetration and site selection",
    ),
    SourceSpec(
        "empiric_therapy",
        "Comprehensive_Empiric_Antibiotic_Therapy_Handbook_2026.pdf",
        "Comprehensive Empiric Antibiotic Therapy Handbook 2026",
        "Empiric therapy organized by infection syndrome",
    ),
    SourceSpec(
        "bacteria_atlas",
        "Bacteria_Centered_Antibiotic_Atlas_2026.pdf",
        "Bacteria-Centered Antibiotic Atlas 2026",
        "Organism-directed therapy and resistance rescue",
    ),
    SourceSpec(
        "culture_master",
        "Clinical_Culture_and_Diagnostic_Microbiology_Master_Reference_2026.pdf",
        "Clinical Culture & Diagnostic Microbiology Master Reference 2026",
        "Culture validity, specimens, AST/MIC and diagnostic stewardship",
    ),
    SourceSpec(
        "culture_encyclopedia",
        "Clinical_Microbiology_Culture_Encyclopedia_2026.pdf",
        "Clinical Microbiology Culture Encyclopedia 2026",
        "Expanded culture interpretation and case-based diagnostics",
    ),
    SourceSpec(
        "spectrum_quick",
        "Antibiotic_Spectrum_and_Distribution_Compact_Ward_Guide_2026.pdf",
        "Antibiotic Spectrum & Distribution Ward Pocket Guide 2026",
        "Fast ward review of spectrum and penetration",
        quick=True,
    ),
    SourceSpec(
        "empiric_quick",
        "Empiric_Antibiotic_Therapy_Compact_Ward_Guide_2026.pdf",
        "Empiric Antibiotic Therapy Compact Ward Guide 2026",
        "Fast syndrome-based empiric selection",
        quick=True,
    ),
)

EXCLUDED_DUPLICATE = "Complete_Antibiotic_Master_Reference_2026.pdf"


HEADER_TERMS = {
    "drug", "agent", "agent/group", "class", "organism", "pathogen",
    "syndrome", "infection", "infection syndrome", "clinical syndrome",
    "specimen", "site", "clinical situation", "condition", "scenario",
    "step", "rule", "factor", "parameter", "test", "result", "finding",
    "mechanism", "resistance mechanism / organism", "question", "category",
    "major group", "setting", "population", "source", "collection issue",
    "decision point", "phase", "target", "sample", "body site", "regimen",
}

FOOTER_PATTERNS = (
    re.compile(r"^page\s+\d+$", re.I),
    re.compile(r"^\d+$"),
    re.compile(r"^evidence reviewed through", re.I),
    re.compile(r"^educational reference\s*[-|]", re.I),
    re.compile(r"^advanced clinical pharmacy reference\s*[|]", re.I),
    re.compile(r"^evidence-based reference for clinical pharmacists", re.I),
    re.compile(r"^use local antibiogram", re.I),
)

REPEATED_HEADER_FRAGMENTS = (
    "comprehensive antibiotic pharmacology handbook - 2026",
    "clinical antibiotic spectrum & tissue distribution | ward edition 2026",
    "empiric antibiotic therapy handbook 2026 | ward and icu reference",
    "bacteria-centered antibiotic atlas | board-level clinical pharmacy | 2026",
    "clinical culture & diagnostic microbiology master reference 2026",
    "clinical microbiology culture encyclopedia 2026",
    "antibiotic spectrum & distribution | ward pocket guide 2026",
    "empiric antibiotic therapy - compact ward guide 2026",
)

HEPATIC_TERMS = re.compile(
    r"\b(hepat(?:ic|itis|otoxicity)|liver|lft|biliar(?:y|ies)|bile|cholestat(?:ic|is)|cirrhosis)\b",
    re.I,
)


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def clean_inline(value: str | None) -> str:
    if not value:
        return ""
    value = value.replace("\u00ad", "")
    value = re.sub(r"(?<=\w)-\s+(?=[a-z])", "-", value)
    value = re.sub(r"\s+", " ", value)
    return value.strip(" \t|;")


def is_footer(line: str) -> bool:
    line = clean_inline(line)
    if not line:
        return False
    low = line.lower()
    if any(fragment in low for fragment in REPEATED_HEADER_FRAGMENTS):
        return True
    return any(pattern.search(line) for pattern in FOOTER_PATTERNS)


def clean_cell(value: str | None) -> str:
    if not value:
        return ""
    lines = []
    for raw in value.splitlines():
        line = clean_inline(raw)
        if line and not is_footer(line):
            lines.append(line)
    return clean_inline(" ".join(lines))


def clean_page_text(value: str | None) -> str:
    if not value:
        return ""
    lines: list[str] = []
    for raw in value.splitlines():
        line = clean_inline(raw)
        if not line or is_footer(line):
            continue
        lines.append(line)
    # Keep line boundaries: they make source-faithful reading considerably more
    # legible on mobile than one giant paragraph.
    return "\n".join(lines)


def color_luminance(color: Any) -> float:
    if color is None:
        return 1.0
    if isinstance(color, (int, float)):
        return float(color)
    if isinstance(color, (list, tuple)) and color:
        values = [float(v) for v in color[:3]]
        if len(values) == 1:
            return values[0]
        while len(values) < 3:
            values.append(values[-1])
        return 0.2126 * values[0] + 0.7152 * values[1] + 0.0722 * values[2]
    return 1.0


def dedupe_rects(page: pdfplumber.page.Page) -> list[dict[str, Any]]:
    unique: dict[tuple[float, float, float, float], dict[str, Any]] = {}
    for rect in page.rects:
        key = tuple(round(float(rect[k]), 1) for k in ("x0", "x1", "top", "bottom"))
        unique[key] = rect
    return list(unique.values())


def colored_row_groups(page: pdfplumber.page.Page) -> list[dict[str, Any]]:
    by_y: dict[tuple[float, float], list[dict[str, Any]]] = defaultdict(list)
    for rect in dedupe_rects(page):
        top, bottom = round(float(rect["top"]), 1), round(float(rect["bottom"]), 1)
        if bottom - top < 3:
            continue
        by_y[(top, bottom)].append(rect)

    groups: list[dict[str, Any]] = []
    for (top, bottom), rects in by_y.items():
        rects.sort(key=lambda item: float(item["x0"]))
        # Deduplicate accidental repeated cells and require a wide multi-column row.
        cells: list[tuple[float, float]] = []
        for rect in rects:
            pair = (round(float(rect["x0"]), 1), round(float(rect["x1"]), 1))
            if pair not in cells:
                cells.append(pair)
        if not (2 <= len(cells) <= 12):
            continue
        if cells[-1][1] - cells[0][0] < page.width * 0.55:
            continue
        xs = [cells[0][0]] + [cell[1] for cell in cells]
        signature = tuple(round(x / page.width, 3) for x in xs)
        colors = [rect.get("non_stroking_color") for rect in rects]
        luminance = sum(color_luminance(c) for c in colors) / max(1, len(colors))
        groups.append(
            {
                "top": top,
                "bottom": bottom,
                "xs": xs,
                "signature": signature,
                "dark": luminance < 0.58,
            }
        )
    groups.sort(key=lambda group: (group["top"], group["bottom"]))
    return groups


def extract_cells(
    page: pdfplumber.page.Page,
    xs: list[float],
    top: float,
    bottom: float,
) -> list[str]:
    result: list[str] = []
    safe_top = max(0.0, top + 0.35)
    safe_bottom = min(float(page.height), bottom - 0.35)
    if safe_bottom <= safe_top:
        return [""] * (len(xs) - 1)
    for left, right in zip(xs, xs[1:]):
        crop = page.crop((left + 0.35, safe_top, right - 0.35, safe_bottom))
        result.append(clean_cell(crop.extract_text(x_tolerance=2, y_tolerance=2)))
    return result


def looks_like_header(cells: list[str], dark: bool) -> bool:
    if not dark or sum(bool(cell) for cell in cells) < 2:
        return False
    first = clean_inline(cells[0]).lower()
    if first in HEADER_TERMS:
        return True
    combined = " | ".join(cells).lower()
    signals = (
        "adult dose", "renal / rrt", "typical syndrome", "clinical interpretation",
        "recommended empiric", "preferred therapy", "major gaps", "gram-positive",
        "specimen type", "collection method", "common pathogens", "ward action",
        "blood", "cns", "lung", "bone", "prostate", "urine", "bile",
    )
    return sum(signal in combined for signal in signals) >= 2


def page_heading(page: pdfplumber.page.Page, before_top: float | None = None) -> str:
    limit = float(before_top if before_top is not None else page.height - 25)
    words = page.extract_words(extra_attrs=["size"])
    lines: dict[float, list[dict[str, Any]]] = defaultdict(list)
    for word in words:
        top = float(word["top"])
        if top < 18 or top >= limit or top > page.height - 25:
            continue
        lines[round(top, 1)].append(word)
    candidates: list[tuple[float, float, str]] = []
    for top, items in lines.items():
        items.sort(key=lambda item: float(item["x0"]))
        text = clean_inline(" ".join(str(item["text"]) for item in items))
        if not text or is_footer(text) or len(text) > 180:
            continue
        low = text.lower()
        first_token = low.split()[0].rstrip(":") if low.split() else ""
        column_signals = sum(
            signal in low
            for signal in (
                "adult dose", "renal / rrt", "clinical use", "eligibility / use",
                "components and duration", "typical syndromes", "major gaps",
            )
        )
        if first_token in HEADER_TERMS or column_signals >= 2:
            continue
        size = max(float(item.get("size") or 0) for item in items)
        if size >= 9.5:
            candidates.append((size, -top, text))
    if not candidates:
        return "Reference page"
    # Prefer the largest heading; break equal-size ties by the earliest line.
    candidates.sort(reverse=True)
    return candidates[0][2]


def classify_table(headers: list[str], source: SourceSpec, context: str) -> str:
    normalized = [clean_inline(header).lower() for header in headers]
    joined = " | ".join(normalized)
    first = normalized[0] if normalized else ""
    if first in {"drug", "agent", "agent/group"} and (
        "dose" in joined or "spectrum" in joined or "clinical use" in joined
    ):
        return "antibiotic"
    if all(term in joined for term in ("cns", "lung")) and any(
        term in joined for term in ("bone", "urine", "bile", "prostate", "blood")
    ):
        return "distribution"
    if first in {"organism", "pathogen"} or "organism" in first:
        return "bacteria"
    if any(term in first for term in ("syndrome", "infection", "site")) and any(
        term in joined for term in ("therapy", "regimen", "empiric", "pathogen", "agent")
    ):
        return "infection"
    if first == "regimen" and any(term in joined for term in ("eligibility", "duration", "components")):
        return "infection"
    if source.id.startswith("culture_") or any(
        term in joined for term in ("specimen", "culture", "collection", "mic", "ast")
    ):
        return "culture"
    if source.quick:
        return "quick"
    low_context = context.lower()
    if "organism" in low_context or "bacterial atlas" in low_context:
        return "bacteria"
    if "empiric" in low_context or source.id == "empiric_therapy":
        return "infection"
    return "reference"


def next_non_table_boundary(
    page: pdfplumber.page.Page,
    after: float,
    before: float,
) -> float:
    candidates: list[float] = []
    for rect in dedupe_rects(page):
        top = float(rect["top"])
        width = float(rect["x1"]) - float(rect["x0"])
        if after + 1 < top < before and width >= page.width * 0.7:
            candidates.append(top)
    return min(candidates) if candidates else before


def extract_tables(page: pdfplumber.page.Page, source: SourceSpec, page_no: int) -> list[dict[str, Any]]:
    groups = colored_row_groups(page)
    for group in groups:
        group["cells"] = extract_cells(page, group["xs"], group["top"], group["bottom"])
        group["is_header"] = looks_like_header(group["cells"], group["dark"])

    tables: list[dict[str, Any]] = []
    headers = [group for group in groups if group["is_header"]]
    for header_index, header in enumerate(headers):
        next_header_top = float(page.height - 23)
        for later in headers[header_index + 1 :]:
            if later["top"] > header["top"]:
                next_header_top = later["top"]
                break

        same_signature = [
            group
            for group in groups
            if group["signature"] == header["signature"]
            and group["top"] > header["top"]
            and group["top"] >= header["bottom"] - 0.6
            and group["top"] < next_header_top
            and not group["is_header"]
        ]
        same_signature.sort(key=lambda group: group["top"])

        # A large vertical break means a new visual block, not an alternating
        # white table row. Keep only the contiguous group nearest the header.
        contiguous: list[dict[str, Any]] = []
        cursor = float(header["bottom"])
        for group in same_signature:
            if group["top"] - cursor > 125:
                break
            contiguous.append(group)
            cursor = float(group["bottom"])

        last_bottom = float(contiguous[-1]["bottom"] if contiguous else header["bottom"])
        table_end = next_non_table_boundary(page, last_bottom, next_header_top)
        table_end = min(table_end, float(page.height - 23))

        intervals: list[tuple[float, float]] = []
        cursor = float(header["bottom"])
        for group in contiguous:
            if group["top"] - cursor > 2:
                intervals.append((cursor, float(group["top"])))
            intervals.append((float(group["top"]), float(group["bottom"])))
            cursor = float(group["bottom"])
        if table_end - cursor > 2:
            intervals.append((cursor, table_end))

        rows: list[list[str]] = []
        for top, bottom in intervals:
            cells = extract_cells(page, header["xs"], top, bottom)
            if not any(cells):
                continue
            # Discard obvious prose blocks accidentally encountered after a table.
            if not cells[0] and sum(bool(cell) for cell in cells) < 2:
                continue
            rows.append(cells)

        if not rows:
            continue
        context = page_heading(page, before_top=float(header["top"]))
        headers_clean = [clean_inline(cell) or f"Column {i + 1}" for i, cell in enumerate(header["cells"])]
        tables.append(
            {
                "id": f"{source.id}-p{page_no}-t{len(tables) + 1}",
                "sourceId": source.id,
                "sourceTitle": source.title,
                "page": page_no,
                "context": context,
                "kind": classify_table(headers_clean, source, context),
                "headers": headers_clean,
                "rows": rows,
            }
        )
    return tables


def field_value(headers: list[str], cells: list[str], terms: Iterable[str]) -> str:
    terms_low = [term.lower() for term in terms]
    for header, cell in zip(headers, cells):
        low = header.lower()
        if any(term in low for term in terms_low):
            return clean_inline(cell)
    return ""


def split_aliases(name: str) -> list[str]:
    bits = re.split(r"\s*(?:/|\+|\bor\b)\s*", name, flags=re.I)
    aliases = {clean_inline(name)}
    aliases.update(clean_inline(bit) for bit in bits if len(clean_inline(bit)) >= 3)
    # Common display punctuation should not prevent exact search.
    aliases.add(clean_inline(name.replace("- ", "-").replace(" -", "-")))
    simplified = re.sub(
        r"\s+(?:aqueous|iv|po/rectal|oral|topical/nasal|topical|for tb)$",
        "",
        name,
        flags=re.I,
    )
    simplified = re.sub(r"\s*\([^)]*\)\s*$", "", simplified)
    if len(clean_inline(simplified)) >= 3:
        aliases.add(clean_inline(simplified))
    return sorted(
        (alias for alias in aliases if alias),
        key=lambda value: (value != name, value.lower()),
    )


def make_entry(table: dict[str, Any], cells: list[str], row_no: int) -> dict[str, Any]:
    headers = table["headers"]
    fields = [
        {"label": header, "value": clean_inline(value)}
        for header, value in zip(headers, cells)
        if clean_inline(value)
    ]
    first = clean_inline(cells[0]) if cells else ""
    title = first or table["context"]
    body = "\n".join(f"{field['label']}: {field['value']}" for field in fields)
    return {
        "id": f"{table['id']}-r{row_no}",
        "kind": table["kind"],
        "title": title,
        "subtitle": table["context"],
        "fields": fields,
        "text": body,
        "sourceId": table["sourceId"],
        "sourceTitle": table["sourceTitle"],
        "page": table["page"],
        "quick": table["sourceId"].endswith("_quick"),
    }


def merge_continuation(previous: dict[str, Any], current: dict[str, Any]) -> None:
    existing = {field["label"]: field for field in previous["fields"]}
    for field in current["fields"]:
        if field["label"] in existing:
            old = existing[field["label"]]["value"]
            new = field["value"]
            if new and new not in old:
                existing[field["label"]]["value"] = clean_inline(f"{old} {new}")
        else:
            previous["fields"].append(field)
    previous["text"] = "\n".join(
        f"{field['label']}: {field['value']}" for field in previous["fields"]
    )
    refs = previous.setdefault("additionalSourcePages", [])
    if current["page"] not in refs:
        refs.append(current["page"])


def build_drugs(entries: list[dict[str, Any]]) -> list[dict[str, Any]]:
    drugs: list[dict[str, Any]] = []
    by_key: dict[str, dict[str, Any]] = {}
    for entry in entries:
        if entry["sourceId"] != "drug_handbook" or entry["kind"] != "antibiotic":
            continue
        headers = [field["label"] for field in entry["fields"]]
        values = [field["value"] for field in entry["fields"]]
        name = field_value(headers, values, ("drug", "agent")) or entry["title"]
        name = clean_inline(name)
        if not name or name.lower() in HEADER_TERMS or len(name) > 100:
            continue
        key = re.sub(r"[^a-z0-9]+", "", name.lower())
        record = {
            "id": f"drug-{len(drugs) + 1:03d}",
            "name": name,
            "aliases": split_aliases(name),
            "family": entry["subtitle"],
            "spectrumUse": field_value(headers, values, ("spectrum", "clinical use", "role")),
            "adultDose": field_value(headers, values, ("adult dose", "dose/regimen", "core dose")),
            "renalRrt": field_value(headers, values, ("renal", "rrt", "kidney")),
            "hepaticAdjustment": "",
            "administrationPkPd": field_value(headers, values, ("administration", "pk/pd", "optimization")),
            "toxicityMonitoring": field_value(headers, values, ("toxicity", "monitoring", "safety")),
            "distributionSummary": "",
            "relatedEntryIds": [],
            "sourceRefs": [{"sourceId": entry["sourceId"], "page": entry["page"]}],
        }
        hepatic_sentences: list[str] = []
        for value in values:
            for sentence in re.split(r"(?<=[.;])\s+", value):
                if HEPATIC_TERMS.search(sentence):
                    sentence = clean_inline(sentence)
                    if sentence and sentence not in hepatic_sentences:
                        hepatic_sentences.append(sentence)
        record["hepaticAdjustment"] = " ".join(hepatic_sentences)
        if key in by_key:
            existing = by_key[key]
            for field in (
                "spectrumUse", "adultDose", "renalRrt", "hepaticAdjustment",
                "administrationPkPd", "toxicityMonitoring",
            ):
                addition = record[field]
                if addition and addition not in existing[field]:
                    existing[field] = clean_inline(f"{existing[field]} {addition}")
            existing["sourceRefs"].extend(record["sourceRefs"])
        else:
            by_key[key] = record
            drugs.append(record)
    return drugs


def link_drugs(drugs: list[dict[str, Any]], entries: list[dict[str, Any]]) -> None:
    relevant_kinds = {"distribution", "bacteria", "infection", "antibiotic", "quick"}
    for drug in drugs:
        aliases = [alias.lower() for alias in drug["aliases"] if len(alias) >= 4]
        family = re.sub(r"^\d+\.\s*", "", drug["family"]).lower()
        family_terms = [
            clean_inline(term)
            for term in re.split(r"\s+(?:and|&)\s+", family)
            if len(clean_inline(term)) >= 6
        ]
        matches: list[dict[str, Any]] = []
        for entry in entries:
            if entry["kind"] not in relevant_kinds:
                continue
            haystack = f"{entry['title']}\n{entry['text']}".lower()
            exact_alias = any(
                re.search(rf"(?<![a-z0-9]){re.escape(alias)}(?![a-z0-9])", haystack)
                for alias in aliases
            )
            class_distribution = (
                entry["sourceId"] == "spectrum_distribution"
                and entry["kind"] == "distribution"
                and any(term in haystack for term in family_terms)
            )
            if exact_alias or class_distribution:
                matches.append(entry)
        # Keep the relationship index bounded while preserving the most useful
        # site/distribution evidence first.
        matches.sort(
            key=lambda entry: (
                {"distribution": 0, "antibiotic": 1, "infection": 2, "bacteria": 3, "quick": 4}.get(entry["kind"], 5),
                entry["sourceId"],
                entry["page"],
            )
        )
        drug["relatedEntryIds"] = [entry["id"] for entry in matches[:80]]
        distribution = [
            entry
            for entry in matches
            if entry["sourceId"] == "spectrum_distribution"
            and entry["kind"] in {"distribution", "antibiotic"}
        ]
        if distribution:
            drug["distributionSummary"] = "\n\n".join(entry["text"] for entry in distribution[:5])

        hepatic_notes: list[str] = []
        for entry in matches:
            # Whole-page records contain many unrelated drugs. Restrict hepatic
            # enrichment to a structured row that matched this drug or class.
            if not entry.get("fields"):
                continue
            for field in entry["fields"]:
                value = clean_inline(field["value"])
                if HEPATIC_TERMS.search(value):
                    note = f"{field['label']}: {value}"
                    if note not in hepatic_notes:
                        hepatic_notes.append(note)
        if hepatic_notes:
            existing = clean_inline(drug["hepaticAdjustment"])
            drug["hepaticAdjustment"] = clean_inline(
                f"{existing} {' '.join(hepatic_notes[:8])}"
            )


def extract_source(source: SourceSpec, path: Path) -> tuple[dict[str, Any], list[dict[str, Any]], list[dict[str, Any]]]:
    pages_out: list[dict[str, Any]] = []
    tables_out: list[dict[str, Any]] = []
    last_heading = source.title
    with pdfplumber.open(path) as pdf:
        for page_no, page in enumerate(pdf.pages, 1):
            raw_text = page.extract_text(layout=True, x_tolerance=2, y_tolerance=2)
            text = clean_page_text(raw_text)
            detected_heading = page_heading(page)
            is_section_heading = bool(
                re.match(r"^(?:\d+(?:\.\d+)*\.?\s+|part\s+[ivx0-9]+\b)", detected_heading, re.I)
            )
            if detected_heading == "Reference page":
                heading = last_heading
            else:
                heading = detected_heading
                if is_section_heading or page_no <= 2:
                    last_heading = detected_heading
            pages_out.append(
                {
                    "id": f"{source.id}-p{page_no}",
                    "kind": "quick" if source.quick else (
                        "culture" if source.id.startswith("culture_") else "reference"
                    ),
                    "title": heading,
                    "subtitle": source.title,
                    "text": text,
                    "sourceId": source.id,
                    "sourceTitle": source.title,
                    "page": page_no,
                    "quick": source.quick,
                }
            )
            page_tables = extract_tables(page, source, page_no)
            for table in page_tables:
                table_context_is_section = bool(
                    re.match(r"^(?:\d+(?:\.\d+)*\.?\s+|part\s+[ivx0-9]+\b)", table["context"], re.I)
                )
                if source.id == "drug_handbook" and not table_context_is_section:
                    table["context"] = last_heading
                elif table["context"] == "Reference page":
                    table["context"] = heading
            tables_out.extend(page_tables)
    source_meta = {
        **asdict(source),
        "pages": len(pages_out),
        "sha256": sha256(path),
        "bytes": path.stat().st_size,
    }
    return source_meta, pages_out, tables_out


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    missing = [spec.filename for spec in SOURCES if not (args.source_dir / spec.filename).is_file()]
    if missing:
        raise SystemExit(f"Missing source PDFs: {', '.join(missing)}")

    sources_meta: list[dict[str, Any]] = []
    pages: list[dict[str, Any]] = []
    tables: list[dict[str, Any]] = []
    for spec in SOURCES:
        meta, source_pages, source_tables = extract_source(spec, args.source_dir / spec.filename)
        sources_meta.append(meta)
        pages.extend(source_pages)
        tables.extend(source_tables)

    entries: list[dict[str, Any]] = []
    previous_by_source_kind: dict[tuple[str, str], dict[str, Any]] = {}
    for table in tables:
        for row_no, cells in enumerate(table["rows"], 1):
            entry = make_entry(table, cells, row_no)
            key = (entry["sourceId"], entry["kind"])
            if not clean_inline(cells[0]) and key in previous_by_source_kind:
                merge_continuation(previous_by_source_kind[key], entry)
                continue
            entries.append(entry)
            previous_by_source_kind[key] = entry

    # Page entries guarantee complete source coverage. Structured table rows
    # are placed first in search results by the Android app.
    entries.extend(pages)
    drugs = build_drugs(entries)
    link_drugs(drugs, entries)

    dataset = {
        "schemaVersion": 1,
        "title": "Clinical Antibiotic Encyclopedia 2026",
        "contentLanguage": "en",
        "sourceEdition": "Updated through July 2026",
        "safetyNotice": (
            "Educational reference. Verify current product labeling, local antibiogram, "
            "AST/MIC, allergy history, pregnancy status, renal replacement modality and "
            "patient-specific adult, pediatric or neonatal dosing before prescribing."
        ),
        "sources": sources_meta,
        "excludedDuplicates": [
            {
                "filename": EXCLUDED_DUPLICATE,
                "reason": (
                    "Combined edition of the standalone drug, spectrum/distribution and "
                    "empiric-therapy volumes; excluded to prevent duplicate search results."
                ),
                "pages": 140,
                "bytes": (args.source_dir / EXCLUDED_DUPLICATE).stat().st_size,
                "sha256": sha256(args.source_dir / EXCLUDED_DUPLICATE),
            }
        ] if (args.source_dir / EXCLUDED_DUPLICATE).is_file() else [],
        "drugs": drugs,
        "entries": entries,
    }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(dataset, ensure_ascii=False, separators=(",", ":")) + "\n"
    args.output.write_text(payload, encoding="utf-8")

    kind_counts = Counter(entry["kind"] for entry in entries)
    report = {
        "schemaVersion": dataset["schemaVersion"],
        "sourceCount": len(sources_meta),
        "sourcePages": sum(source["pages"] for source in sources_meta),
        "tableCount": len(tables),
        "structuredEntryCount": len(entries) - len(pages),
        "pageEntryCount": len(pages),
        "entryCount": len(entries),
        "drugCount": len(drugs),
        "entriesByKind": dict(sorted(kind_counts.items())),
        "datasetBytes": args.output.stat().st_size,
        "datasetSha256": sha256(args.output),
        "sources": sources_meta,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
