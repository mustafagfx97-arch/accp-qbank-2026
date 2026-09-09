#!/usr/bin/env python3
"""Build the curated clinical navigation layer from the verified dataset.

This does not re-extract or rewrite the PDF-derived encyclopedia. It maps the
existing drug cards and source-backed entries into four predictable browsing
axes: antibiotic families, organism families, infection systems and tissue
distribution.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any, Iterable


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def localized(
    item_id: str,
    title_ar: str,
    title_en: str,
    description_ar: str,
    description_en: str,
) -> dict[str, str]:
    return {
        "id": item_id,
        "titleAr": title_ar,
        "titleEn": title_en,
        "descriptionAr": description_ar,
        "descriptionEn": description_en,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    dataset = json.loads(args.dataset.read_text(encoding="utf-8"))
    entries = dataset["entries"]
    entries_by_id = {entry["id"]: entry for entry in entries}
    drug_names = {drug["name"] for drug in dataset["drugs"]}

    def find_entry(
        source_id: str,
        *,
        title: str | None = None,
        page: int | None = None,
        fields: bool | None = None,
    ) -> str:
        matches = [
            entry
            for entry in entries
            if entry["sourceId"] == source_id
            and (title is None or entry["title"] == title)
            and (page is None or entry["page"] == page)
            and (fields is None or bool(entry.get("fields")) == fields)
        ]
        require(
            len(matches) == 1,
            f"Expected one entry for source={source_id!r}, title={title!r}, "
            f"page={page!r}, fields={fields!r}; found {len(matches)}",
        )
        return matches[0]["id"]

    def find_titles(source_id: str, titles: Iterable[str]) -> list[str]:
        return [find_entry(source_id, title=title, fields=True) for title in titles]

    def find_pages(source_id: str, pages: Iterable[int]) -> list[str]:
        return [find_entry(source_id, page=page, fields=False) for page in pages]

    drug_families: list[dict[str, Any]] = [
        {
            **localized(
                "beta_lactams",
                "عائلة البيتا-لاكتام",
                "Beta-lactams",
                "مرتبة حسب البنية والدور السريري: بنسلينات، سيفالوسبورينات، كاربابينيمات، مونوباكتام وعوامل حديثة للمقاومة.",
                "Organized by structure and clinical role: penicillins, cephalosporins, carbapenems, monobactams and modern resistance agents.",
            ),
            "subfamilies": [
                {
                    **localized("penicillins", "البنسلينات", "Penicillins", "", ""),
                    "drugNames": [
                        "Penicillin G aqueous", "Penicillin V", "Benzathine penicillin G",
                        "Nafcillin / Oxacillin", "Dicloxacillin / Flucloxacillin",
                        "Ampicillin", "Amoxicillin", "Ampicillin-sulbactam",
                        "Amoxicillin-clavulanate", "Piperacillin-tazobactam", "Pivmecillinam",
                    ],
                },
                {
                    **localized("cephalosporins", "السيفالوسبورينات والسيفامايسينات", "Cephalosporins & cephamycins", "", ""),
                    "drugNames": [
                        "Cefazolin", "Cephalexin", "Cefadroxil", "Cefuroxime", "Cefoxitin",
                        "Cefotetan", "Ceftriaxone", "Cefotaxime", "Ceftazidime",
                        "Cefixime / Cefpodoxime", "Cefepime", "Ceftaroline", "Ceftobiprole",
                    ],
                },
                {
                    **localized("carbapenems", "الكاربابينيمات ومثبطاتها", "Carbapenems & protected carbapenems", "", ""),
                    "drugNames": [
                        "Meropenem", "Imipenem-cilastatin", "Ertapenem", "Doripenem",
                        "Meropenem-vaborbactam", "Imipenem-cilastatin-relebactam",
                    ],
                },
                {
                    **localized("monobactams_penems", "المونوباكتام والبنيمات الفموية", "Monobactams & oral penems", "", ""),
                    "drugNames": [
                        "Aztreonam", "Aztreonam-avibactam",
                        "Sulopenem etzadroxil-probenecid", "Tebipenem pivoxil",
                    ],
                },
                {
                    **localized("modern_mdr_beta_lactams", "عوامل بيتا-لاكتام الحديثة للمقاومة", "Modern MDR beta-lactam strategies", "", ""),
                    "drugNames": [
                        "Ceftolozane-tazobactam", "Cefiderocol", "Cefepime-enmetazobactam",
                        "Cefepime-zidebactam", "Ceftazidime-avibactam", "Sulbactam-durlobactam",
                    ],
                },
            ],
        },
        {
            **localized(
                "gram_positive_agents",
                "عوامل موجّهة لموجبات الغرام",
                "Gram-positive focused agents",
                "الغليكوببتيدات والليبوغليكوببتيدات، الدابتومايسين، الأوكسازوليدينونات وعوامل الإنقاذ المختارة.",
                "Glycopeptides and lipoglycopeptides, daptomycin, oxazolidinones and selected salvage agents.",
            ),
            "subfamilies": [
                {
                    **localized("glycopeptides", "الغليكوببتيدات والليبوغليكوببتيدات", "Glycopeptides & lipoglycopeptides", "", ""),
                    "drugNames": [
                        "Vancomycin IV", "Vancomycin PO/rectal", "Teicoplanin", "Telavancin",
                        "Dalbavancin", "Oritavancin",
                    ],
                },
                {
                    **localized("lipopeptides", "الليبوببتيدات", "Lipopeptides", "", ""),
                    "drugNames": ["Daptomycin"],
                },
                {
                    **localized("oxazolidinones", "الأوكسازوليدينونات", "Oxazolidinones", "", ""),
                    "drugNames": ["Linezolid", "Tedizolid"],
                },
                {
                    **localized("other_gram_positive", "عوامل أخرى لموجبات الغرام", "Other Gram-positive agents", "", ""),
                    "drugNames": ["Quinupristin-dalfopristin", "Fusidic acid"],
                },
            ],
        },
        {
            **localized(
                "aminoglycosides",
                "الأمينوغليكوزيدات",
                "Aminoglycosides",
                "عوامل قاتلة تعتمد على التركيز ضد الجراثيم الهوائية مع ضرورة ضبط الجرعة والمراقبة.",
                "Concentration-dependent agents for aerobic pathogens with dose optimization and monitoring.",
            ),
            "subfamilies": [
                {
                    **localized("systemic_aminoglycosides", "العوامل الجهازية", "Systemic agents", "", ""),
                    "drugNames": ["Gentamicin", "Tobramycin", "Amikacin", "Plazomicin", "Streptomycin"],
                },
                {
                    **localized("luminal_aminoglycosides", "العوامل اللمعية", "Luminal agents", "", ""),
                    "drugNames": ["Paromomycin"],
                },
            ],
        },
        {
            **localized(
                "macrolides_lincosamides_pleuromutilins",
                "الماكروليدات واللينكوساميدات والبلوروموتيلينات",
                "Macrolides, lincosamides & pleuromutilins",
                "مرتبة إلى ماكروليدات، كليندامايسين، بلوروموتيلينات وعلاج لمعي للمطثية العسيرة.",
                "Separated into macrolides, clindamycin, pleuromutilins and luminal C. difficile therapy.",
            ),
            "subfamilies": [
                {
                    **localized("macrolides", "الماكروليدات", "Macrolides", "", ""),
                    "drugNames": ["Azithromycin", "Clarithromycin", "Erythromycin"],
                },
                {
                    **localized("lincosamides", "اللينكوساميدات", "Lincosamides", "", ""),
                    "drugNames": ["Clindamycin"],
                },
                {
                    **localized("pleuromutilins", "البلوروموتيلينات", "Pleuromutilins", "", ""),
                    "drugNames": ["Lefamulin", "Retapamulin topical"],
                },
                {
                    **localized("luminal_cdi", "علاج لمعي للمطثية العسيرة", "Luminal C. difficile agent", "", ""),
                    "drugNames": ["Fidaxomicin"],
                },
            ],
        },
        {
            **localized(
                "tetracyclines",
                "التتراسيكلينات ومشتقاتها",
                "Tetracyclines & derivatives",
                "التتراسيكلينات التقليدية والمشتقات الأحدث ذات الأدوار النسيجية والمقاومة المختلفة.",
                "Traditional tetracyclines and newer derivatives with distinct tissue and resistance roles.",
            ),
            "subfamilies": [
                {
                    **localized("classic_tetracyclines", "التتراسيكلينات التقليدية", "Traditional tetracyclines", "", ""),
                    "drugNames": ["Doxycycline", "Minocycline", "Tetracycline"],
                },
                {
                    **localized("new_tetracyclines", "مشتقات التتراسيكلين الحديثة", "Newer tetracycline derivatives", "", ""),
                    "drugNames": ["Tigecycline", "Eravacycline", "Omadacycline", "Sarecycline"],
                },
            ],
        },
        {
            **localized(
                "topoisomerase_agents",
                "الفلوروكينولونات ومثبطات التوبويزوميراز",
                "Fluoroquinolones & topoisomerase inhibitors",
                "تمييز عوامل التنفس، الزائفة، البول والبروستات عن العوامل الحديثة محددة الاستطباب.",
                "Separates respiratory, antipseudomonal, urinary/prostate and newer indication-specific agents.",
            ),
            "subfamilies": [
                {
                    **localized("fluoroquinolones", "الفلوروكينولونات", "Fluoroquinolones", "", ""),
                    "drugNames": ["Ciprofloxacin", "Levofloxacin", "Moxifloxacin", "Delafloxacin", "Ofloxacin"],
                },
                {
                    **localized("novel_topoisomerase", "مثبطات توبويزوميراز حديثة", "Novel topoisomerase inhibitors", "", ""),
                    "drugNames": ["Gepotidacin", "Zoliflodacin"],
                },
            ],
        },
        {
            **localized(
                "folate_anaerobe_urinary",
                "عوامل الفولات واللاهوائيات والمسالك البولية",
                "Folate, anaerobic & urinary agents",
                "فصل واضح بين TMP-SMX والنيتروإيميدازولات والعوامل الخاصة بالمثانة أو البول.",
                "A clear split between TMP-SMX, nitroimidazoles and bladder/urinary-specific agents.",
            ),
            "subfamilies": [
                {
                    **localized("folate_antagonists", "مضادات الفولات", "Folate antagonists", "", ""),
                    "drugNames": ["TMP-SMX"],
                },
                {
                    **localized("nitroimidazoles", "النيتروإيميدازولات - لاهوائيات", "Nitroimidazoles - anaerobes", "", ""),
                    "drugNames": ["Metronidazole", "Tinidazole", "Secnidazole"],
                },
                {
                    **localized("urinary_agents", "عوامل بولية ولمعية", "Urinary & luminal agents", "", ""),
                    "drugNames": [
                        "Nitrofurantoin monohydrate/macrocrystals", "Fosfomycin tromethamine oral",
                        "Fosfomycin IV", "Methenamine hippurate",
                    ],
                },
            ],
        },
        {
            **localized(
                "rifamycins_membrane_misc",
                "الريفامايسينات وعوامل الغشاء والعوامل الموضعية",
                "Rifamycins, membrane & topical agents",
                "ريفامايسينات، بوليميكسينات، كلورامفينيكول، عوامل موضعية وعوامل خاصة.",
                "Rifamycins, polymyxins, chloramphenicol, topical agents and selected special agents.",
            ),
            "subfamilies": [
                {
                    **localized("rifamycins", "الريفامايسينات", "Rifamycins", "", ""),
                    "drugNames": ["Rifampin", "Rifabutin", "Rifapentine", "Rifaximin"],
                },
                {
                    **localized("polymyxins", "البوليميكسينات", "Polymyxins", "", ""),
                    "drugNames": ["Colistimethate sodium (colistin)", "Polymyxin B"],
                },
                {
                    **localized("phenicols", "الفينيكولات", "Phenicols", "", ""),
                    "drugNames": ["Chloramphenicol"],
                },
                {
                    **localized("topical_other", "عوامل موضعية وخاصة", "Topical & special agents", "", ""),
                    "drugNames": ["Mupirocin topical/nasal", "Bacitracin topical", "Spectinomycin"],
                },
            ],
        },
        {
            **localized(
                "tuberculosis_agents",
                "أدوية السل",
                "Tuberculosis agents",
                "عوامل الخط الأول والعوامل الحديثة والإضافية للسل المقاوم، وتُقرأ ضمن نظم متعددة الأدوية.",
                "First-line, newer and companion drug-resistant TB agents, interpreted within multidrug regimens.",
            ),
            "subfamilies": [
                {
                    **localized("first_line_tb", "عوامل أساسية", "Core agents", "", ""),
                    "drugNames": ["Isoniazid", "Pyrazinamide", "Ethambutol"],
                },
                {
                    **localized("new_dr_tb", "عوامل حديثة للسل المقاوم", "Newer drug-resistant TB agents", "", ""),
                    "drugNames": ["Bedaquiline", "Pretomanid", "Linezolid for TB", "Moxifloxacin for TB", "Delamanid"],
                },
                {
                    **localized("companion_tb", "عوامل مرافقة واحتياطية", "Companion & reserve agents", "", ""),
                    "drugNames": [
                        "Clofazimine for TB", "Cycloserine / Terizidone",
                        "Ethionamide / Prothionamide", "PAS",
                    ],
                },
            ],
        },
    ]

    spectrum_entry_ids = [
        entry["id"]
        for entry in entries
        if entry["sourceId"] == "spectrum_distribution"
        and entry["page"] in {10, 11}
        and entry.get("fields")
        and any(field["label"] == "Class" for field in entry["fields"])
    ]

    def organism_group(
        item_id: str,
        title_ar: str,
        title_en: str,
        description_ar: str,
        description_en: str,
        overview_title: str,
        organism_titles: list[str],
        coverage_field: str,
        therapy_ids: list[str] | None = None,
        deep_dive_pages: list[int] | None = None,
    ) -> dict[str, Any]:
        return {
            **localized(item_id, title_ar, title_en, description_ar, description_en),
            "overviewEntryId": find_entry(
                "spectrum_distribution", title=overview_title, page=4, fields=True
            ),
            "organismEntryIds": find_titles("spectrum_distribution", organism_titles),
            "coverageField": coverage_field,
            "therapyEntryIds": therapy_ids or [],
            "deepDiveEntryIds": find_pages("bacteria_atlas", deep_dive_pages or []),
        }

    organism_families = [
        organism_group(
            "gram_positive_cocci", "المكورات موجبة الغرام", "Gram-positive cocci",
            "المكورات العنقودية والعقدية والمكورات المعوية مع الفصل بين أنماط المقاومة المهمة.",
            "Staphylococci, streptococci and enterococci separated by clinically decisive resistance phenotypes.",
            "Gram-positive cocci",
            [
                "Staphylococcus aureus - MSSA", "Staphylococcus aureus - MRSA",
                "S. epidermidis and other CoNS", "S. lugdunensis", "S. saprophyticus",
                "Streptococcus pyogenes - GAS", "Streptococcus agalactiae - GBS",
                "Streptococcus pneumoniae", "Viridans streptococci",
                "Streptococcus anginosus group", "Streptococcus gallolyticus",
                "Enterococcus faecalis", "Enterococcus faecium",
                "E. gallinarum / E. casseliflavus", "Aerococcus urinae",
                "Leuconostoc / Pediococcus",
            ],
            "Gram-positive",
            deep_dive_pages=list(range(5, 20)),
        ),
        organism_group(
            "gram_positive_bacilli", "العصيات موجبة الغرام والمتفرعة", "Gram-positive bacilli & branching organisms",
            "ليستيريا، كورينيباكتيريوم، باسيلس، المطثيات، أكتينومايسس ونوكارديا مع الفجوات العلاجية المهمة.",
            "Listeria, Corynebacterium, Bacillus, clostridia, Actinomyces and Nocardia with their key treatment gaps.",
            "Gram-positive bacilli",
            [
                "Listeria monocytogenes", "Corynebacterium diphtheriae",
                "C. jeikeium / C. striatum", "Cutibacterium acnes",
                "Erysipelothrix rhusiopathiae", "Bacillus anthracis", "Bacillus cereus",
                "Clostridium perfringens", "Clostridium septicum", "Clostridioides difficile",
                "Actinomyces spp.", "Nocardia spp.", "Rhodococcus equi",
            ],
            "Gram-positive",
            therapy_ids=find_titles(
                "drug_handbook", ["Systemic anthrax", "Actinomycosis", "Respiratory diphtheria"]
            ),
            deep_dive_pages=list(range(20, 26)) + [61, 62],
        ),
        organism_group(
            "gram_negative_cocci", "المكورات والعصيات القصيرة سالبة الغرام", "Gram-negative cocci & coccobacilli",
            "النيسيريا، المستدمية، الموراكسيلة، جراثيم العضات ومجموعة HACEK.",
            "Neisseria, Haemophilus, Moraxella, bite-associated organisms and HACEK.",
            "Gram-negative cocci/coccobacilli",
            [
                "Neisseria meningitidis", "Neisseria gonorrhoeae", "Haemophilus influenzae",
                "Moraxella catarrhalis", "Pasteurella multocida", "Capnocytophaga canimorsus",
                "Bordetella pertussis", "Francisella tularensis", "Brucella spp.",
                "HACEK group", "Eikenella corrodens", "Kingella kingae",
            ],
            "Gram-negative",
            deep_dive_pages=list(range(41, 49)),
        ),
        organism_group(
            "enterobacterales", "المعويات", "Enterobacterales",
            "ترتيب بحسب النوع ثم النمط: حسّاس، ESBL، AmpC، CRE وآلية الكاربابينيماز.",
            "Organized by species and phenotype: susceptible, ESBL, AmpC, CRE and carbapenemase mechanism.",
            "Enterobacterales",
            [
                "Escherichia coli", "Klebsiella pneumoniae", "Klebsiella oxytoca",
                "Enterobacter cloacae complex", "Klebsiella aerogenes", "Citrobacter freundii",
                "Citrobacter koseri", "Serratia marcescens", "Proteus mirabilis",
                "Morganella morganii", "Providencia stuartii/rettgeri",
                "Salmonella Typhi/Paratyphi", "Non-typhoidal Salmonella", "Shigella spp.",
                "Yersinia enterocolitica", "Cronobacter sakazakii",
            ],
            "Gram-negative",
            deep_dive_pages=list(range(26, 35)) + list(range(53, 57)),
        ),
        organism_group(
            "non_fermenters", "الجراثيم غير المخمّرة وعالية الخطورة", "Non-fermenters & high-consequence Gram-negatives",
            "الزائفة، الأسينيتوباكتر، ستينوتروفوموناس، بوركولديريا وغيرها؛ لا تُفترض حساسية مشتركة بينها.",
            "Pseudomonas, Acinetobacter, Stenotrophomonas, Burkholderia and related organisms; susceptibility is not interchangeable.",
            "Non-fermenters",
            [
                "Pseudomonas aeruginosa", "Acinetobacter baumannii complex",
                "Stenotrophomonas maltophilia", "Burkholderia cepacia complex",
                "Burkholderia pseudomallei", "Achromobacter xylosoxidans",
                "Elizabethkingia anophelis/meningoseptica", "Vibrio vulnificus", "Aeromonas spp.",
            ],
            "Gram-negative",
            deep_dive_pages=list(range(35, 41)) + [63, 64],
        ),
        organism_group(
            "anaerobes", "الجراثيم اللاهوائية", "Anaerobes",
            "قسم مخصص يربط اللاهوائيات بمواضع الخراج والنخر والأسنان والحوض والبطن، ثم يعرض الأدوية الفعالة والفجوات.",
            "A dedicated map linking anaerobes to abscess, necrosis, dental, pelvic and abdominal sources, then showing active options and gaps.",
            "Anaerobes",
            [
                "Bacteroides fragilis group", "Prevotella / Porphyromonas",
                "Fusobacterium necrophorum", "Anaerobic G+ cocci",
                "Clostridium perfringens", "Clostridium septicum",
                "Clostridioides difficile", "Actinomyces spp.",
            ],
            "Anaerobes",
            therapy_ids=[
                find_entry("spectrum_distribution", title="Pseudomonas + anaerobes", fields=True),
                find_entry("spectrum_distribution", title="Anaerobes but no Pseudomonas", fields=True),
                find_entry("spectrum_distribution", title="Metronidazole", page=16, fields=True),
                find_entry("spectrum_quick", title="Clindamycin", page=7, fields=True),
                find_entry("spectrum_quick", title="Metronidazole", page=7, fields=True),
                find_entry("spectrum_quick", title="Intra-abdominal/abscess", fields=True),
            ],
            deep_dive_pages=list(range(22, 26)) + list(range(49, 53)),
        ),
        organism_group(
            "atypical_intracellular", "اللانمطية وداخل الخلايا", "Atypical & intracellular organisms",
            "جراثيم تحتاج وصولاً داخل الخلايا أو لا تملك جداراً خلوياً؛ البيتا-لاكتام وحده غير كافٍ.",
            "Organisms requiring intracellular exposure or lacking a cell wall; beta-lactam monotherapy is inadequate.",
            "Atypical/intracellular",
            [
                "Legionella pneumophila", "Mycoplasma pneumoniae", "Mycoplasma genitalium",
                "Ureaplasma spp.", "Chlamydia trachomatis",
                "Rickettsia / Ehrlichia / Anaplasma", "Coxiella burnetii",
            ],
            "Atypicals",
            therapy_ids=find_titles(
                "drug_handbook",
                ["Pertussis", "Rickettsial disease", "Acute Q fever", "Chronic Q fever endocarditis", "Bartonella endocarditis"],
            ),
            deep_dive_pages=list(range(57, 61)),
        ),
        organism_group(
            "spirochetes", "الملتويات", "Spirochetes",
            "الزهري والبوريليا مع نظم علاج مرتبطة بالمظهر السريري وإصابة الجهاز العصبي أو القلب.",
            "Syphilis and Borrelia regimens linked to manifestation and CNS or cardiac involvement.",
            "Spirochetes/Mycobacteria",
            ["Treponema pallidum", "Borrelia spp."],
            "",
            therapy_ids=find_titles(
                "drug_handbook",
                ["Early syphilis", "Late latent syphilis", "Neurosyphilis/ocular/otosyphilis", "Lyme erythema migrans", "Severe leptospirosis"],
            ),
        ),
        organism_group(
            "mycobacteria", "المتفطرات", "Mycobacteria",
            "السل والمتفطرات غير السلية ضمن استراتيجيات متعددة الأدوية وتحديد النوع والمقاومة.",
            "Tuberculosis and NTM interpreted through species-level, multidrug and resistance-specific strategies.",
            "Spirochetes/Mycobacteria",
            ["Mycobacterium tuberculosis complex", "NTM: MAC, M. kansasii, M. abscessus complex"],
            "",
            therapy_ids=find_titles(
                "drug_handbook",
                ["Standard DS-TB", "Adult/adolescent 4-month", "Pediatric nonsevere 4-month", "3HP LTBI", "4R LTBI", "BPaLM", "BPaL"],
            ),
        ),
    ]

    infection_specs = [
        ("respiratory", "التهابات الجهاز التنفسي", "Respiratory tract", "5. RESPIRATORY TRACT INFECTIONS"),
        ("cns", "التهابات الجهاز العصبي المركزي", "Central nervous system", "6. CENTRAL NERVOUS SYSTEM INFECTIONS"),
        ("blood_endovascular", "الإنتان والدم والأوعية", "Sepsis, bloodstream & endovascular", "7. SEPSIS, BLOODSTREAM AND ENDOVASCULAR INFECTIONS"),
        ("urinary_genital", "المسالك البولية والتناسلية", "Urinary & genital tract", "8. URINARY AND GENITAL TRACT INFECTIONS"),
        ("abdominal_hepatobiliary", "البطن والكبد والمرارة والمطثية العسيرة", "Intra-abdominal, hepatobiliary & C. difficile", "9. INTRA-ABDOMINAL, HEPATOBILIARY AND C. DIFFICILE INFECTIONS"),
        ("skin_wound", "الجلد والأنسجة الرخوة والجروح", "Skin, soft tissue & wounds", "10. SKIN, SOFT-TISSUE AND WOUND INFECTIONS"),
        ("bone_joint", "العظم والمفصل والأجهزة", "Bone, joint & hardware", "11. BONE, JOINT AND HARDWARE INFECTIONS"),
        ("ent_dental", "الأنف والأذن والحنجرة والأسنان", "ENT & dental", "12. ENT AND DENTAL INFECTIONS"),
        ("ob_gyn", "النسائية والتوليد", "Obstetric & gynecologic", "13. OBSTETRIC AND GYNECOLOGIC INFECTIONS"),
        ("immunocompromised", "المريض ناقص المناعة", "Immunocompromised host", "14. IMMUNOCOMPROMISED HOST"),
        ("nicu_pediatric", "حديثو الولادة والأطفال", "NICU & pediatrics", "15. NICU AND PEDIATRIC QUICK REFERENCE"),
    ]
    infection_groups: list[dict[str, Any]] = []
    for item_id, title_ar, title_en, subtitle in infection_specs:
        group_entries = [
            entry["id"]
            for entry in entries
            if entry["sourceId"] == "empiric_quick"
            and entry.get("fields")
            and entry["subtitle"] == subtitle
        ]
        infection_groups.append(
            {
                **localized(
                    item_id,
                    title_ar,
                    title_en,
                    "الأعراض المحتملة، الممرضات المتوقعة، النظام التجريبي، والتعديل أو الإجراء الحاسم.",
                    "Likely pathogens, empiric regimen and the key modifier or immediate action.",
                ),
                "entryIds": group_entries,
            }
        )

    penetration_entry_ids = [
        entry["id"]
        for entry in entries
        if entry["sourceId"] == "spectrum_distribution"
        and entry["kind"] == "distribution"
        and entry["page"] in {22, 23}
        and entry["title"] != "Nitrofurantoin"
    ]

    def tissue_site(
        item_id: str,
        title_ar: str,
        title_en: str,
        description_ar: str,
        description_en: str,
        matrix_field: str,
        summary_title: str | None = None,
        support: list[tuple[str, str, int | None]] | None = None,
    ) -> dict[str, Any]:
        summary_ids = (
            [find_entry("drug_handbook", title=summary_title, fields=True)]
            if summary_title
            else []
        )
        support_ids = [
            find_entry(source, title=title, page=page, fields=True)
            for source, title, page in (support or [])
        ]
        return {
            **localized(item_id, title_ar, title_en, description_ar, description_en),
            "matrixField": matrix_field,
            "summaryEntryIds": summary_ids,
            "supportEntryIds": support_ids,
        }

    tissue_sites = [
        tissue_site("blood", "مجرى الدم والشغاف", "Bloodstream & endocardium", "التعرّض المصلي الملائم مع الانتباه إلى الأدوية غير المناسبة لتجرثم الدم الأولي.", "Serum exposure with explicit avoidance of agents unsuitable for primary bacteremia.", "Blood", "Bloodstream"),
        tissue_site("cns", "الجهاز العصبي المركزي", "Central nervous system", "اختراق السحايا والدماغ يتغير مع الالتهاب والجرعة وطبيعة الدواء.", "CNS exposure varies with inflammation, dose and drug properties.", "CNS", "CNS"),
        tissue_site("lung", "الرئة والحويصلات", "Lung & alveoli", "يفصل بين وصول الدواء للنسيج وبين تعطله في الحويصلات أو ضعف العلاج الأحادي.", "Separates tissue delivery from alveolar inactivation and unreliable monotherapy.", "Lung", "Lung alveoli"),
        tissue_site("bone", "العظم والمفصل", "Bone & joint", "الاختراق النسيجي مع دور الجراحة والأجهزة والبيوفيلم ومدة العلاج.", "Tissue exposure interpreted with surgery, hardware, biofilm and treatment duration.", "Bone", "Bone/joint", [("spectrum_quick", "Bone/joint", None)]),
        tissue_site("urinary", "المثانة والكلية والبول", "Bladder, kidney & urine", "تمييز دواء المثانة عن علاج النسيج الكلوي أو تجرثم الدم البولي.", "Distinguishes bladder-lumen drugs from renal parenchymal or urosepsis therapy.", "Urine", "Urine/bladder", [("spectrum_quick", "Bladder", None), ("spectrum_quick", "Kidney/urosepsis", None)]),
        tissue_site("prostate", "البروستات", "Prostate", "العوامل ذات التركيز البروستاتي المفيد وما يجب تجنبه.", "Agents with useful prostatic exposure and major avoidances.", "Prostate", "Prostate", [("spectrum_quick", "Prostate", None)]),
        tissue_site("bile", "الصفراء والطرق الصفراوية", "Bile & biliary tract", "الاختراق الصفراوي لا يعوض تصريف الانسداد.", "Biliary penetration cannot replace drainage of obstruction.", "Bile", "Bile", [("spectrum_quick", "Bile/cholangitis", None)]),
        tissue_site("abscess", "الخراج والبطن", "Abscess & intra-abdominal space", "وصول اللاهوائيات والهوائيات مع أولوية تصريف الخراج والسيطرة على المصدر.", "Aerobic and anaerobic exposure with drainage and source control as priorities.", "", "Abscess", [("spectrum_quick", "Intra-abdominal/abscess", None)]),
        tissue_site("skin", "الجلد والأنسجة الرخوة", "Skin & soft tissue", "الاختراق يتأثر بالتروية والوذمة والنخر، والجراحة حاسمة عند الحاجة.", "Penetration is limited by perfusion, edema and necrosis; surgery remains decisive when indicated.", "", None, [("spectrum_distribution", "Skin/subcutaneous tissue", 28), ("spectrum_quick", "Skin/soft tissue", None)]),
        tissue_site(
            "intracellular", "داخل الخلايا", "Intracellular distribution",
            "تصنيف العوامل الأعلى وصولاً داخل الخلايا مقابل العوامل ضعيفة الدخول.",
            "Ranks agents with strong intracellular activity against weak-entry families.",
            "Intracellular", None,
            [
                ("spectrum_distribution", "Highest clinical intracellular activity", 28),
                ("spectrum_distribution", "Useful additional agents", 28),
                ("spectrum_distribution", "Weak intracellular entry", 28),
                ("spectrum_distribution", "No cell wall", 28),
            ],
        ),
    ]

    taxonomy = {
        "schemaVersion": 1,
        "title": "Clinical Antibiotic Navigation Taxonomy 2026",
        "drugFamilies": drug_families,
        "organismFamilies": organism_families,
        "infectionGroups": infection_groups,
        "tissueSites": tissue_sites,
        "spectrumEntryIds": spectrum_entry_ids,
        "penetrationEntryIds": penetration_entry_ids,
    }

    mapped_drugs = [
        name
        for family in drug_families
        for subfamily in family["subfamilies"]
        for name in subfamily["drugNames"]
    ]
    require(len(mapped_drugs) == len(set(mapped_drugs)), "A drug is mapped to multiple subfamilies")
    require(set(mapped_drugs) == drug_names, f"Drug taxonomy mismatch: missing={sorted(drug_names - set(mapped_drugs))}, extra={sorted(set(mapped_drugs) - drug_names)}")

    referenced_ids: list[str] = list(spectrum_entry_ids) + list(penetration_entry_ids)
    for group in organism_families:
        referenced_ids.extend([group["overviewEntryId"]])
        referenced_ids.extend(group["organismEntryIds"])
        referenced_ids.extend(group["therapyEntryIds"])
        referenced_ids.extend(group["deepDiveEntryIds"])
    for group in infection_groups:
        require(group["entryIds"], f"Empty infection group: {group['id']}")
        referenced_ids.extend(group["entryIds"])
    for site in tissue_sites:
        require(site["summaryEntryIds"] or site["supportEntryIds"], f"Empty tissue site: {site['id']}")
        referenced_ids.extend(site["summaryEntryIds"])
        referenced_ids.extend(site["supportEntryIds"])
    require(all(item_id in entries_by_id for item_id in referenced_ids), "Taxonomy references a missing entry")

    anaerobes = next(group for group in organism_families if group["id"] == "anaerobes")
    require(len(anaerobes["organismEntryIds"]) >= 8, "Anaerobe organism map is incomplete")
    require(len(anaerobes["therapyEntryIds"]) >= 6, "Anaerobe therapy map is incomplete")
    require(len(spectrum_entry_ids) >= 23, "Spectrum orientation matrix is incomplete")
    require(len(penetration_entry_ids) >= 17, "Penetration matrix is incomplete")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(taxonomy, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    report = {
        "schemaVersion": 1,
        "drugFamilyCount": len(drug_families),
        "drugSubfamilyCount": sum(len(family["subfamilies"]) for family in drug_families),
        "mappedDrugCount": len(mapped_drugs),
        "organismFamilyCount": len(organism_families),
        "organismCardCount": sum(len(group["organismEntryIds"]) for group in organism_families),
        "anaerobeOrganismCount": len(anaerobes["organismEntryIds"]),
        "anaerobeTherapyCount": len(anaerobes["therapyEntryIds"]),
        "infectionSystemCount": len(infection_groups),
        "infectionSyndromeCount": sum(len(group["entryIds"]) for group in infection_groups),
        "tissueSiteCount": len(tissue_sites),
        "spectrumClassCount": len(spectrum_entry_ids),
        "penetrationClassCount": len(penetration_entry_ids),
        "taxonomyBytes": args.output.stat().st_size,
        "taxonomySha256": sha256(args.output),
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
