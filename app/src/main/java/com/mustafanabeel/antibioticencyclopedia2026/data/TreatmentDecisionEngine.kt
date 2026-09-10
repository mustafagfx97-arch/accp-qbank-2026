package com.mustafanabeel.antibioticencyclopedia2026.data

import java.text.Normalizer
import java.util.Locale

enum class DecisionSeverity {
    STABLE,
    SEVERE_OR_SHOCK,
}

enum class BetaLactamAllergy {
    NONE,
    NON_SEVERE_OR_UNCERTAIN,
    IMMEDIATE_SEVERE,
}

enum class KidneyStatus {
    NORMAL_OR_UNKNOWN,
    IMPAIRED,
    DIALYSIS_OR_RRT,
}

enum class DecisionNoteLevel {
    CRITICAL,
    WARNING,
    INFO,
}

data class TreatmentSelection(
    val syndromeEntryId: String? = null,
    val organismEntryId: String? = null,
    val tissueSiteId: String? = null,
    val severity: DecisionSeverity = DecisionSeverity.STABLE,
    val betaLactamAllergy: BetaLactamAllergy = BetaLactamAllergy.NONE,
    val kidneyStatus: KidneyStatus = KidneyStatus.NORMAL_OR_UNKNOWN,
    val hepaticImpairment: Boolean = false,
)

data class DecisionNote(
    val level: DecisionNoteLevel,
    val titleAr: String,
    val titleEn: String,
    val bodyAr: String,
    val bodyEn: String,
    val evidenceEntryId: String? = null,
)

data class TherapyClassFit(
    val spectrumEntry: ReferenceEntry,
    val penetrationEntry: ReferenceEntry? = null,
    val coverage: String,
    val penetration: String = "",
    val majorGaps: String = "",
    val score: Int,
)

data class TreatmentDecision(
    val selection: TreatmentSelection,
    val syndrome: ReferenceEntry? = null,
    val syndromeGroup: InfectionGroupDefinition? = null,
    val organism: ReferenceEntry? = null,
    val organismFamily: OrganismFamilyDefinition? = null,
    val tissueSite: TissueSiteDefinition? = null,
    val likelyPathogens: String = "",
    val empiricRegimen: String = "",
    val keyModifier: String = "",
    val organismDeepDives: List<ReferenceEntry> = emptyList(),
    val tissueEvidence: List<ReferenceEntry> = emptyList(),
    val classFits: List<TherapyClassFit> = emptyList(),
    val linkedDrugs: List<DrugRecord> = emptyList(),
    val relatedSyndromes: List<ReferenceEntry> = emptyList(),
    val escalationOptions: List<ReferenceEntry> = emptyList(),
    val notes: List<DecisionNote> = emptyList(),
)

/**
 * Deterministic, offline clinical crosswalk over the already verified encyclopedia.
 *
 * The engine never invents a regimen. Exact regimens are displayed only from a selected
 * syndrome's source row. Organism and tissue selections add source-backed context and a
 * conservative class-level spectrum/penetration cross-check.
 */
class TreatmentDecisionEngine(
    private val dataset: EncyclopediaDataset,
    private val taxonomy: ClinicalTaxonomy,
) {
    private val entriesById = dataset.entries.associateBy(ReferenceEntry::id)
    private val infectionMembership = taxonomy.infectionGroups
        .flatMap { group -> group.entryIds.map { it to group } }
        .toMap()
    private val organismMembership = taxonomy.organismFamilies
        .flatMap { family -> family.organismEntryIds.map { it to family } }
        .toMap()

    fun evaluate(selection: TreatmentSelection): TreatmentDecision {
        val syndrome = selection.syndromeEntryId?.let(entriesById::get)
        val syndromeGroup = syndrome?.id?.let(infectionMembership::get)
        val organism = selection.organismEntryId?.let(entriesById::get)
        val organismFamily = organism?.id?.let(organismMembership::get)
        val tissueSite = taxonomy.tissueSites.firstOrNull { it.id == selection.tissueSiteId }

        val likelyPathogens = syndrome?.field("Likely pathogens").orEmpty()
        val empiricRegimen = syndrome?.field("Empiric regimen")
            .orEmpty()
            .ifBlank { syndrome?.field("Core empiric regimen (normal renal function)").orEmpty() }
        val keyModifier = syndrome?.field("Key modifier / action")
            .orEmpty()
            .ifBlank { syndrome?.field("Add only when risk is present").orEmpty() }

        val deepDives = organismDeepDives(organism, organismFamily)
        val tissueEvidence = tissueSite
            ?.let { it.summaryEntryIds + it.supportEntryIds }
            .orEmpty()
            .mapNotNull(entriesById::get)

        val classFits = classFits(organism, organismFamily, tissueSite)
        val linkedDrugs = linkedDrugs(
            listOfNotNull(
                empiricRegimen.takeIf(String::isNotBlank),
                deepDives.firstOrNull()?.text?.takeIf(String::isNotBlank),
            ).joinToString("\n")
        )
        val relatedSyndromes = if (syndrome == null) {
            relatedSyndromes(organism, organismFamily, tissueSite)
        } else {
            emptyList()
        }
        val escalationOptions = if (
            selection.severity == DecisionSeverity.SEVERE_OR_SHOCK &&
            syndromeGroup != null &&
            syndrome?.title?.containsAny(SEVERE_MARKERS) != true
        ) {
            syndromeGroup.entryIds
                .mapNotNull(entriesById::get)
                .filter { it.title.containsAny(SEVERE_MARKERS) }
                .take(4)
        } else {
            emptyList()
        }

        return TreatmentDecision(
            selection = selection,
            syndrome = syndrome,
            syndromeGroup = syndromeGroup,
            organism = organism,
            organismFamily = organismFamily,
            tissueSite = tissueSite,
            likelyPathogens = likelyPathogens,
            empiricRegimen = empiricRegimen,
            keyModifier = keyModifier,
            organismDeepDives = deepDives,
            tissueEvidence = tissueEvidence,
            classFits = classFits,
            linkedDrugs = linkedDrugs,
            relatedSyndromes = relatedSyndromes,
            escalationOptions = escalationOptions,
            notes = decisionNotes(
                selection = selection,
                syndrome = syndrome,
                syndromeGroup = syndromeGroup,
                organism = organism,
                tissueSite = tissueSite,
                likelyPathogens = likelyPathogens,
                empiricRegimen = empiricRegimen,
            ),
        )
    }

    private fun organismDeepDives(
        organism: ReferenceEntry?,
        family: OrganismFamilyDefinition?,
    ): List<ReferenceEntry> {
        if (organism == null || family == null) return emptyList()
        val targetTerms = significantTerms(organism.title)
        if (targetTerms.isEmpty()) return emptyList()
        return family.deepDiveEntryIds
            .mapNotNull(entriesById::get)
            .map { entry -> entry to matchScore(entry.title + " " + entry.text.take(1200), targetTerms) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .take(3)
            .map(Pair<ReferenceEntry, Int>::first)
    }

    private fun relatedSyndromes(
        organism: ReferenceEntry?,
        family: OrganismFamilyDefinition?,
        tissueSite: TissueSiteDefinition?,
    ): List<ReferenceEntry> {
        val siteGroups = tissueSite?.let { SITE_TO_INFECTION_GROUPS[it.id] }.orEmpty()
        val organismTerms = organism?.let { significantTerms(it.title) }.orEmpty()
        val organismSyndromes = organism?.field("Typical syndromes")
            ?.let(::significantTerms)
            .orEmpty()
        val familyTerms = family?.let { significantTerms(it.titleEn) }.orEmpty()

        return taxonomy.infectionGroups
            .flatMap { group ->
                group.entryIds.mapNotNull(entriesById::get).map { entry ->
                    var score = 0
                    if (group.id in siteGroups) score += 120
                    score += matchScore(entry.title, organismSyndromes) * 5
                    score += matchScore(entry.field("Likely pathogens"), organismTerms) * 7
                    score += matchScore(entry.field("Likely pathogens"), familyTerms)
                    entry to score
                }
            }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<ReferenceEntry, Int>> { it.second }.thenBy { it.first.title })
            .take(8)
            .map(Pair<ReferenceEntry, Int>::first)
    }

    private fun classFits(
        organism: ReferenceEntry?,
        family: OrganismFamilyDefinition?,
        tissueSite: TissueSiteDefinition?,
    ): List<TherapyClassFit> {
        val coverageField = family?.coverageField.orEmpty()
        if (coverageField.isBlank()) return emptyList()

        val penetrationEntries = taxonomy.penetrationEntryIds.mapNotNull(entriesById::get)
        val targetMarkers = organism?.title?.targetMarkers().orEmpty()

        return taxonomy.spectrumEntryIds
            .mapNotNull(entriesById::get)
            .mapNotNull { spectrum ->
                val coverage = spectrum.field(coverageField)
                val gaps = spectrum.field("Major gaps")
                val coverageScore = coverage.coverageRank(targetMarkers, gaps)
                if (coverageScore <= 0) return@mapNotNull null

                val penetration = penetrationEntries.bestPenetrationMatch(spectrum.title)
                val siteValue = tissueSite?.matrixField
                    ?.takeIf(String::isNotBlank)
                    ?.let { penetration?.field(it).orEmpty() }
                    .orEmpty()
                val siteScore = when {
                    tissueSite == null || tissueSite.matrixField.isBlank() -> 2
                    siteValue.isBlank() -> 0
                    else -> siteValue.penetrationRank()
                }
                if (tissueSite?.matrixField?.isNotBlank() == true && siteScore <= 0) {
                    return@mapNotNull null
                }

                TherapyClassFit(
                    spectrumEntry = spectrum,
                    penetrationEntry = penetration,
                    coverage = coverage,
                    penetration = siteValue,
                    majorGaps = gaps,
                    score = coverageScore * 10 + siteScore,
                )
            }
            .sortedWith(
                compareByDescending<TherapyClassFit>(TherapyClassFit::score)
                    .thenBy { it.spectrumEntry.title }
            )
            .take(8)
    }

    private fun linkedDrugs(text: String): List<DrugRecord> {
        if (text.isBlank()) return emptyList()
        val normalizedText = text.normalized()
        return dataset.drugs
            .mapNotNull { drug ->
                val candidates = buildList {
                    addAll(drug.aliases)
                    add(drug.name)
                    addAll(drug.name.split(" / "))
                    MANUAL_DRUG_ALIASES[drug.name]?.let { addAll(it) }
                }
                    .map { it.normalized() }
                    .filter { it.length >= 4 }
                    .distinct()
                val longestMatch = candidates.filter(normalizedText::contains).maxOfOrNull(String::length) ?: 0
                drug to longestMatch
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .distinctBy { (drug, _) -> drug.name.removeSuffix(" for TB") }
            .take(12)
            .map { it.first }
    }

    private fun decisionNotes(
        selection: TreatmentSelection,
        syndrome: ReferenceEntry?,
        syndromeGroup: InfectionGroupDefinition?,
        organism: ReferenceEntry?,
        tissueSite: TissueSiteDefinition?,
        likelyPathogens: String,
        empiricRegimen: String,
    ): List<DecisionNote> = buildList {
        if (syndrome == null) {
            add(
                DecisionNote(
                    DecisionNoteLevel.WARNING,
                    "المكان أو الجرثومة وحدهما لا يحددان وصفة واحدة",
                    "A site or organism alone does not define one regimen",
                    "اختر المتلازمة السريرية من الاقتراحات أدناه للحصول على نظام تجريبي مباشر من المرجع. النتائج الحالية هي خريطة طيف واختراق فقط.",
                    "Choose the clinical syndrome from the suggestions below to obtain a direct source regimen. Current results are only a spectrum and penetration map.",
                )
            )
        } else if (empiricRegimen.isBlank()) {
            add(
                DecisionNote(
                    DecisionNoteLevel.WARNING,
                    "لا يوجد نظام علاجي منظم في هذا السطر",
                    "No structured regimen is available in this row",
                    "افتح الصفحة المصدرية ولا تحوّل النص العام إلى وصفة تلقائيًا.",
                    "Open the source page and do not convert general text into an automatic prescription.",
                    syndrome.id,
                )
            )
        }

        if (selection.severity == DecisionSeverity.SEVERE_OR_SHOCK) {
            add(
                DecisionNote(
                    DecisionNoteLevel.CRITICAL,
                    "حالة شديدة أو صدمة",
                    "Severe infection or shock",
                    "هذه الحالة تحتاج تقييمًا وعلاجًا عاجلًا وفق بروتوكول المستشفى، مع مزروعات مبكرة وضبط مصدر العدوى. لا تعتمد على نتيجة التطبيق وحدها.",
                    "This requires urgent assessment and treatment under the hospital protocol, early cultures, and source control. Do not rely on the app result alone.",
                    "empiric_therapy-p6-t1-r1",
                )
            )
        }

        if (organism != null) {
            add(
                DecisionNote(
                    DecisionNoteLevel.INFO,
                    "العلاج الموجّه يحتاج AST/MIC",
                    "Definitive therapy requires AST/MIC",
                    "تأكد أن العزلة تمثل عدوى لا استعمارًا أو تلوثًا، ثم طابق الحساسية والتركيز في موقع العدوى قبل التضييق.",
                    "Confirm that the isolate represents infection rather than colonization or contamination, then match susceptibility and site exposure before narrowing.",
                    "spectrum_distribution-p32-t1-r3",
                )
            )
            if (syndrome != null && likelyPathogens.isNotBlank()) {
                val overlap = significantTerms(organism.title).any { it in likelyPathogens.normalized() }
                if (!overlap) {
                    add(
                        DecisionNote(
                            DecisionNoteLevel.WARNING,
                            "الجرثومة ليست ضمن الممرضات المعتادة في السطر المختار",
                            "The organism is not listed among the usual pathogens",
                            "قد تكون عزلة حقيقية، استعمارًا، أو أن المتلازمة المختارة غير دقيقة. راجع جودة العينة والسياق السريري.",
                            "It may be a true isolate, colonization, or an imprecise syndrome selection. Reassess specimen quality and clinical context.",
                            "spectrum_distribution-p32-t1-r2",
                        )
                    )
                }
            }
        }

        if (tissueSite != null && syndromeGroup != null) {
            val compatibleGroups = SITE_TO_INFECTION_GROUPS[tissueSite.id].orEmpty()
            if (compatibleGroups.isNotEmpty() && syndromeGroup.id !in compatibleGroups) {
                add(
                    DecisionNote(
                        DecisionNoteLevel.WARNING,
                        "عدم تطابق بين موقع العدوى والمتلازمة",
                        "Site and syndrome do not match",
                        "راجع الاختيارين؛ أُبقيت النتيجة ظاهرة كي تستطيع مقارنة المصدرين دون دمجهما كخطة واحدة.",
                        "Review both selections; the result remains visible for comparison but they were not merged into one plan.",
                    )
                )
            }
        }

        when (selection.betaLactamAllergy) {
            BetaLactamAllergy.NONE -> Unit
            BetaLactamAllergy.NON_SEVERE_OR_UNCERTAIN -> add(
                DecisionNote(
                    DecisionNoteLevel.INFO,
                    "تحقق من قصة حساسية البيتا لاكتام",
                    "Clarify the beta-lactam allergy history",
                    "الطفح البعيد أو التفاعل غير المحدد لا يساوي التأق؛ استخدم مسار الحساسية المعتمد محليًا قبل استبعاد أفضل بيتا لاكتام.",
                    "A remote rash or vague reaction is not equivalent to anaphylaxis; use the local allergy pathway before excluding a preferred beta-lactam.",
                    "empiric_therapy-p56-t1-r8",
                )
            )
            BetaLactamAllergy.IMMEDIATE_SEVERE -> add(
                DecisionNote(
                    DecisionNoteLevel.WARNING,
                    "حساسية فورية شديدة للبيتا لاكتام",
                    "Immediate severe beta-lactam allergy",
                    "لا يستبدل التطبيق النظام تلقائيًا. راجع اختلاف السلاسل الجانبية وخيار aztreonam عند ملاءمته واستعن بالحساسية/الأمراض المعدية إذا كان البيتا لاكتام متفوقًا.",
                    "The app does not automatically substitute the regimen. Review side-chain dissimilarity and aztreonam when appropriate; involve allergy/ID when a beta-lactam is clinically superior.",
                    "empiric_therapy-p56-t1-r9",
                )
            )
        }

        if (selection.kidneyStatus != KidneyStatus.NORMAL_OR_UNKNOWN) {
            add(
                DecisionNote(
                    DecisionNoteLevel.WARNING,
                    if (selection.kidneyStatus == KidneyStatus.DIALYSIS_OR_RRT) {
                        "غسيل كلوي أو RRT"
                    } else {
                        "قصور كلوي"
                    },
                    if (selection.kidneyStatus == KidneyStatus.DIALYSIS_OR_RRT) {
                        "Dialysis or RRT"
                    } else {
                        "Renal impairment"
                    },
                    "استخدم بطاقة كل دواء أدناه لضبط الجرعة والفاصل حسب CrCl وطريقة RRT؛ لا تُطبّق الجرعة القياسية آليًا.",
                    "Use each linked drug card below to adjust dose and interval for CrCl and RRT modality; do not apply the standard dose automatically.",
                    "empiric_therapy-p56",
                )
            )
        }

        if (selection.hepaticImpairment) {
            add(
                DecisionNote(
                    DecisionNoteLevel.INFO,
                    "مرض كبدي مهم سريريًا",
                    "Clinically significant hepatic disease",
                    "راجع ملاحظة الكبد والسمية في بطاقة كل دواء؛ بعض الأدوية لا تحتاج تعديل جرعة محددًا لكنها تحتاج مراقبة أو تجنبًا حسب شدة المرض.",
                    "Review the hepatic and toxicity sections of every linked drug card; some agents need monitoring or avoidance even when no numeric dose adjustment is stated.",
                )
            )
        }
    }

    private fun ReferenceEntry.field(label: String): String =
        fields.firstOrNull { it.label.equals(label, ignoreCase = true) }?.value.orEmpty()

    private fun List<ReferenceEntry>.bestPenetrationMatch(spectrumTitle: String): ReferenceEntry? {
        val spectrum = spectrumTitle.normalized()
        val direct = firstOrNull { it.title.normalized() == spectrum }
        if (direct != null) return direct

        val mapped = PENETRATION_CLASS_MAP.entries.firstOrNull { (key, _) -> key in spectrum }?.value
        if (mapped != null) return firstOrNull { mapped in it.title.normalized() }

        val terms = significantTerms(spectrumTitle)
        return map { it to matchScore(it.title, terms) }
            .filter { (_, score) -> score > 0 }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    private fun String.coverageRank(targetMarkers: Set<String>, gaps: String): Int {
        val value = normalized()
        val normalizedGaps = gaps.normalized()
        if (value.isBlank() || value in setOf("no", "none", "n a")) return 0
        if (value.startsWith("no ") || value.startsWith("none ") || "very limited" in value) return 0
        if (targetMarkers.any { marker -> marker in normalizedGaps && marker !in value }) return 0

        val markerHits = targetMarkers.count(value::contains)
        val base = when {
            listOf("excellent", "very broad", "strong", "broad incl", "good").any(value::contains) -> 3
            listOf("variable", "selected", "partial", "limited", "some", "susceptible").any(value::contains) -> 1
            else -> 2
        }
        return (base + markerHits.coerceAtMost(2)).coerceAtMost(5)
    }

    private fun String.penetrationRank(): Int {
        val value = normalized()
        if (value.isBlank() || value.startsWith("x ") || value == "x" || value.startsWith("0 ") || value == "0") {
            return 0
        }
        return when {
            "+++" in this -> 3
            "++" in this -> 2
            "+" in this -> 1
            else -> 0
        }
    }

    private fun String.targetMarkers(): Set<String> {
        val value = normalized()
        return TARGET_MARKERS.filterTo(linkedSetOf()) { it in value }
    }

    private fun String.containsAny(values: Set<String>): Boolean {
        val normalized = normalized()
        return values.any(normalized::contains)
    }

    private fun significantTerms(value: String): Set<String> = value.normalized()
        .split(' ')
        .filter { it.length >= 3 && it !in STOP_WORDS }
        .toSet()

    private fun matchScore(value: String, terms: Set<String>): Int {
        if (terms.isEmpty()) return 0
        val normalized = value.normalized()
        return terms.sumOf { term ->
            when {
                normalized.startsWith(term) -> 5
                term in normalized -> 2
                else -> 0
            }
        }
    }

    private fun String.normalized(): String = Normalizer
        .normalize(lowercase(Locale.US), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('β', 'b')
        .replace(Regex("[^a-z0-9+./<>=\\-]+"), " ")
        .trim()

    companion object {
        private val SEVERE_MARKERS = setOf(
            "severe", "shock", "icu", "hospitalized", "necrotizing", "unstable", "neutropenia",
        )

        private val STOP_WORDS = setOf(
            "and", "the", "group", "other", "species", "spp", "complex", "risk", "same",
            "organisms", "positive", "negative", "gram", "infection", "infections",
        )

        private val TARGET_MARKERS = setOf(
            "mrsa", "mssa", "vre", "pseudomonas", "acinetobacter", "stenotrophomonas",
            "bacteroides", "clostridia", "clostridium", "clostridioides", "enterococcus",
            "listeria", "nocardia", "atypical", "esbl", "ampc", "cre",
        )

        private val SITE_TO_INFECTION_GROUPS = mapOf(
            "blood" to setOf("blood_endovascular"),
            "cns" to setOf("cns"),
            "lung" to setOf("respiratory"),
            "bone" to setOf("bone_joint"),
            "urinary" to setOf("urinary_genital"),
            "prostate" to setOf("urinary_genital"),
            "bile" to setOf("abdominal_hepatobiliary"),
            "abscess" to setOf("abdominal_hepatobiliary", "skin_wound", "cns"),
            "skin" to setOf("skin_wound"),
        )

        private val PENETRATION_CLASS_MAP = mapOf(
            "natural penicillins" to "penicillins",
            "anti staphylococcal penicillins" to "penicillins",
            "aminopenicillins" to "penicillins",
            "amox clav" to "penicillins",
            "amp sulb" to "penicillins",
            "ceftazidime" to "cefepime ceftazidime",
            "cefepime" to "cefepime ceftazidime",
            "linezolid" to "linezolid",
            "tedizolid" to "linezolid",
            "tetracyclines" to "doxy minocycline",
        )

        private val MANUAL_DRUG_ALIASES = mapOf(
            "Piperacillin-tazobactam" to listOf("pip-tazo", "piptazo"),
            "Ampicillin-sulbactam" to listOf("amp-sulb", "amp sulb"),
            "Amoxicillin-clavulanate" to listOf("amox-clav", "co-amoxiclav"),
            "Vancomycin IV" to listOf("vancomycin", "vanco"),
            "Vancomycin PO/rectal" to listOf("oral vancomycin", "vancomycin po"),
            "Nitrofurantoin monohydrate/macrocrystals" to listOf("nitrofurantoin"),
            "Fosfomycin tromethamine oral" to listOf("oral fosfomycin", "fosfomycin"),
            "Colistimethate sodium (colistin)" to listOf("colistin", "colistimethate"),
        )
    }
}
