package com.mustafanabeel.antibioticencyclopedia2026.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

class EncyclopediaRepository(private val context: Context) {
    private var cached: EncyclopediaDataset? = null
    private var cachedTaxonomy: ClinicalTaxonomy? = null
    private var entryIndex: Map<String, ReferenceEntry> = emptyMap()

    suspend fun load(): EncyclopediaDataset = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        val bytes = context.assets.open(ASSET_NAME).use { it.readBytes() }
        val digest = bytes.sha256()
        check(digest == EXPECTED_DATASET_SHA256) {
            "Bundled encyclopedia failed its integrity check."
        }
        val taxonomyBytes = context.assets.open(TAXONOMY_ASSET_NAME).use { it.readBytes() }
        check(taxonomyBytes.sha256() == EXPECTED_TAXONOMY_SHA256) {
            "Bundled clinical navigation failed its integrity check."
        }
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(EncyclopediaDataset::class.java)
        val parsed = requireNotNull(adapter.fromJson(bytes.toString(Charsets.UTF_8))) {
            "Unable to parse the bundled encyclopedia."
        }
        val taxonomyAdapter = moshi.adapter(ClinicalTaxonomy::class.java)
        val taxonomy = requireNotNull(
            taxonomyAdapter.fromJson(taxonomyBytes.toString(Charsets.UTF_8))
        ) {
            "Unable to parse the clinical navigation."
        }
        validate(parsed)
        validateTaxonomy(parsed, taxonomy)
        entryIndex = parsed.entries.associateBy(ReferenceEntry::id)
        cachedTaxonomy = taxonomy
        cached = parsed
        parsed
    }

    fun taxonomy(): ClinicalTaxonomy = checkNotNull(cachedTaxonomy) {
        "The clinical navigation has not been loaded."
    }

    fun entriesFor(drug: DrugRecord): List<ReferenceEntry> =
        drug.relatedEntryIds.mapNotNull(entryIndex::get)

    fun search(
        dataset: EncyclopediaDataset,
        rawQuery: String,
        filter: ContentFilter,
        limit: Int = 250,
    ): List<SearchItem> {
        val expanded = expandQuery(rawQuery)
        val terms = expanded.map(::normalize).filter(String::isNotBlank).distinct()
        val query = normalize(rawQuery)
        if (terms.isEmpty() && filter == ContentFilter.ALL) return emptyList()
        val results = ArrayList<SearchItem>()

        if (filter == ContentFilter.ALL || filter == ContentFilter.ANTIBIOTIC) {
            dataset.drugs.forEach { drug ->
                val title = normalize(drug.name + " " + drug.aliases.joinToString(" "))
                val body = normalize(
                    listOf(
                        drug.family,
                        drug.spectrumUse,
                        drug.adultDose,
                        drug.renalRrt,
                        drug.hepaticAdjustment,
                        drug.administrationPkPd,
                        drug.toxicityMonitoring,
                        drug.distributionSummary,
                    ).joinToString(" ")
                )
                val score = relevance(title, body, query, terms, structured = true)
                if (terms.isEmpty() || score > 0) results += SearchItem.Drug(drug, score)
            }
        }

        if (filter != ContentFilter.ANTIBIOTIC) {
            dataset.entries.asSequence()
                .filter { entry -> filter.accepts(entry) }
                .filter { entry -> terms.isNotEmpty() || entry.isPrimaryBrowseRecord(filter) }
                .forEach { entry ->
                    val title = normalize(entry.title)
                    val body = normalize(entry.subtitle + " " + entry.text)
                    val structured = entry.fields.isNotEmpty()
                    val score = relevance(title, body, query, terms, structured)
                    if (terms.isEmpty() || score > 0) results += SearchItem.Entry(entry, score)
                }
        }

        return results
            .sortedWith(
                compareByDescending<SearchItem> { it.score }
                    .thenBy { if (it is SearchItem.Drug) 0 else 1 }
                    .thenBy { it.title.lowercase(Locale.US) }
            )
            .take(limit)
    }

    private fun validate(dataset: EncyclopediaDataset) {
        check(dataset.schemaVersion == 1) { "Unsupported encyclopedia schema." }
        check(dataset.sources.size == 8) { "The source manifest is incomplete." }
        check(dataset.sources.sumOf(SourceInfo::pages) == 381) { "Source page count mismatch." }
        check(dataset.drugs.size == 108) { "Structured drug count mismatch." }
        check(dataset.entries.size == 1715) { "Search record count mismatch." }
        check(dataset.drugs.all { it.name.isNotBlank() && it.adultDose.isNotBlank() && it.renalRrt.isNotBlank() }) {
            "One or more drug cards are incomplete."
        }
    }

    private fun validateTaxonomy(dataset: EncyclopediaDataset, taxonomy: ClinicalTaxonomy) {
        check(taxonomy.schemaVersion == 1) { "Unsupported clinical navigation schema." }
        check(taxonomy.drugFamilies.size == 9) { "Drug-family navigation is incomplete." }
        check(taxonomy.organismFamilies.size == 9) { "Organism-family navigation is incomplete." }
        check(taxonomy.infectionGroups.size == 11) { "Infection-system navigation is incomplete." }
        check(taxonomy.tissueSites.size == 10) { "Tissue-site navigation is incomplete." }

        val mappedDrugs = taxonomy.drugFamilies
            .flatMap(DrugFamilyDefinition::subfamilies)
            .flatMap(DrugSubfamilyDefinition::drugNames)
        check(mappedDrugs.size == dataset.drugs.size && mappedDrugs.distinct().size == mappedDrugs.size) {
            "A drug is missing from the family navigation or is listed twice."
        }
        check(mappedDrugs.toSet() == dataset.drugs.map(DrugRecord::name).toSet()) {
            "Drug-family navigation does not match the bundled monographs."
        }

        val entryIds = dataset.entries.map(ReferenceEntry::id).toSet()
        val referencedIds = buildList {
            addAll(taxonomy.spectrumEntryIds)
            addAll(taxonomy.penetrationEntryIds)
            taxonomy.organismFamilies.forEach { group ->
                add(group.overviewEntryId)
                addAll(group.organismEntryIds)
                addAll(group.therapyEntryIds)
                addAll(group.deepDiveEntryIds)
            }
            taxonomy.infectionGroups.forEach { addAll(it.entryIds) }
            taxonomy.tissueSites.forEach { site ->
                addAll(site.summaryEntryIds)
                addAll(site.supportEntryIds)
            }
        }
        check(referencedIds.all(entryIds::contains)) {
            "Clinical navigation points to a missing source record."
        }
        check(taxonomy.infectionGroups.sumOf { it.entryIds.size } == 78) {
            "Infection-system navigation is incomplete."
        }
        val anaerobes = taxonomy.organismFamilies.firstOrNull { it.id == "anaerobes" }
        check(anaerobes != null && anaerobes.organismEntryIds.size >= 8 && anaerobes.therapyEntryIds.size >= 6) {
            "The dedicated anaerobe map is incomplete."
        }
        check(taxonomy.spectrumEntryIds.size >= 23 && taxonomy.penetrationEntryIds.size >= 17) {
            "The clinical spectrum or penetration matrix is incomplete."
        }
    }

    private fun relevance(
        title: String,
        body: String,
        query: String,
        terms: List<String>,
        structured: Boolean,
    ): Int {
        if (terms.isEmpty()) return if (structured) 40 else 5
        if (terms.any { it !in title && it !in body }) return 0
        var score = if (structured) 35 else 0
        if (query.isNotBlank()) {
            when {
                title == query -> score += 1200
                title.startsWith(query) -> score += 800
                query in title -> score += 520
            }
        }
        terms.forEach { term ->
            if (title == term) score += 500
            if (title.startsWith(term)) score += 220
            score += title.windowCount(term) * 90
            score += body.windowCount(term).coerceAtMost(12) * 12
        }
        return score
    }

    private fun expandQuery(raw: String): List<String> {
        var translated = normalize(raw)
        if (translated.isBlank()) return emptyList()
        QUERY_EXPANSIONS.forEach { (key, replacement) ->
            translated = translated.replace(key, replacement)
        }
        return translated.split(Regex("\\s+")).filter(String::isNotBlank)
    }

    private fun ContentFilter.accepts(entry: ReferenceEntry): Boolean = when (this) {
        ContentFilter.ALL -> true
        ContentFilter.ANTIBIOTIC -> false
        ContentFilter.INFECTION -> entry.kind == "infection"
        ContentFilter.BACTERIA -> entry.kind == "bacteria"
        ContentFilter.DISTRIBUTION -> entry.kind == "distribution" ||
            (entry.sourceId == "spectrum_distribution" && entry.kind == "antibiotic")
        ContentFilter.CULTURE -> entry.kind == "culture"
        ContentFilter.QUICK -> entry.quick || entry.kind == "quick"
    }

    private fun ReferenceEntry.isPrimaryBrowseRecord(filter: ContentFilter): Boolean = when (filter) {
        ContentFilter.ALL -> false
        ContentFilter.ANTIBIOTIC -> false
        ContentFilter.INFECTION -> sourceId == "empiric_quick" && fields.isNotEmpty() &&
            subtitle.matches(Regex("^(?:[5-9]|1[0-5])\\. .+"))
        ContentFilter.BACTERIA -> sourceId == "spectrum_distribution" &&
            kind == "bacteria" && fields.isNotEmpty() &&
            title !in setOf("Organism/group", "1. Gram-Positive Cocci") &&
            title.firstOrNull()?.isDigit() != true
        ContentFilter.DISTRIBUTION -> sourceId == "spectrum_distribution" &&
            kind == "distribution" && page in 22..23 && title != "Nitrofurantoin"
        ContentFilter.CULTURE -> sourceId.startsWith("culture_") && fields.isNotEmpty()
        ContentFilter.QUICK -> quick && fields.isNotEmpty()
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.US), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('β', 'b')
        .replace(Regex("[^a-z0-9+./<>=\\-\\u0600-\\u06ff]+"), " ")
        .trim()

    private fun String.windowCount(term: String): Int {
        if (term.isBlank()) return 0
        var count = 0
        var index = indexOf(term)
        while (index >= 0) {
            count++
            index = indexOf(term, index + term.length)
        }
        return count
    }

    private fun ByteArray.sha256(): String = MessageDigest
        .getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }

    companion object {
        const val ASSET_NAME = "encyclopedia.json"
        const val TAXONOMY_ASSET_NAME = "clinical_taxonomy.json"
        const val EXPECTED_DATASET_SHA256 =
            "4317e8ec7ce842ed2d7d34dc3299ec2f9d019f062e2ae27870b7067194d611d4"
        const val EXPECTED_TAXONOMY_SHA256 =
            "a0f0c70491d8c30d2d906ac1ce8b78160c60687ad4806aaaa792ca3e1dc5f401"

        private val QUERY_EXPANSIONS = mapOf(
            "لاهوائيات" to "anaerobes",
            "لاهوائي" to "anaerobe",
            "زائفة" to "pseudomonas",
            "عنقودية" to "staphylococcus",
            "عقدية" to "streptococcus",
            "معويات" to "enterobacterales",
            "سحايا" to "meningitis",
            "بروستات" to "prostate",
            "خراج" to "abscess",
            "إنتان" to "sepsis",
            "رئة" to "pneumonia",
            "تنفسي" to "respiratory",
            "بول" to "urinary",
            "كلية" to "renal",
            "كلى" to "renal",
            "كبد" to "hepatic",
            "دماغ" to "cns",
            "دم" to "blood",
            "عظم" to "bone",
            "بطن" to "intra-abdominal",
            "جلد" to "skin",
            "جرعة" to "dose",
            "زراعة" to "culture",
            "زرع" to "culture",
        )
    }
}
