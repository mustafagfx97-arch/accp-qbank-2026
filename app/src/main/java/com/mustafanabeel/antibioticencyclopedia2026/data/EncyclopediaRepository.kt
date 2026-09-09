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
    private var entryIndex: Map<String, ReferenceEntry> = emptyMap()

    suspend fun load(): EncyclopediaDataset = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        val bytes = context.assets.open(ASSET_NAME).use { it.readBytes() }
        val digest = bytes.sha256()
        check(digest == EXPECTED_DATASET_SHA256) {
            "Bundled encyclopedia failed its integrity check."
        }
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(EncyclopediaDataset::class.java)
        val parsed = requireNotNull(adapter.fromJson(bytes.toString(Charsets.UTF_8))) {
            "Unable to parse the bundled encyclopedia."
        }
        validate(parsed)
        entryIndex = parsed.entries.associateBy(ReferenceEntry::id)
        cached = parsed
        parsed
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
        const val EXPECTED_DATASET_SHA256 =
            "4317e8ec7ce842ed2d7d34dc3299ec2f9d019f062e2ae27870b7067194d611d4"

        private val QUERY_EXPANSIONS = mapOf(
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
