package com.mustafanabeel.antibioticencyclopedia2026.data

data class EncyclopediaDataset(
    val schemaVersion: Int = 0,
    val title: String = "",
    val contentLanguage: String = "en",
    val sourceEdition: String = "",
    val safetyNotice: String = "",
    val sources: List<SourceInfo> = emptyList(),
    val excludedDuplicates: List<ExcludedSource> = emptyList(),
    val drugs: List<DrugRecord> = emptyList(),
    val entries: List<ReferenceEntry> = emptyList(),
)

data class ClinicalTaxonomy(
    val schemaVersion: Int = 0,
    val title: String = "",
    val drugFamilies: List<DrugFamilyDefinition> = emptyList(),
    val organismFamilies: List<OrganismFamilyDefinition> = emptyList(),
    val infectionGroups: List<InfectionGroupDefinition> = emptyList(),
    val tissueSites: List<TissueSiteDefinition> = emptyList(),
    val spectrumEntryIds: List<String> = emptyList(),
    val penetrationEntryIds: List<String> = emptyList(),
)

data class DrugFamilyDefinition(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val subfamilies: List<DrugSubfamilyDefinition> = emptyList(),
)

data class DrugSubfamilyDefinition(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val drugNames: List<String> = emptyList(),
)

data class OrganismFamilyDefinition(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val overviewEntryId: String = "",
    val organismEntryIds: List<String> = emptyList(),
    val coverageField: String = "",
    val therapyEntryIds: List<String> = emptyList(),
    val deepDiveEntryIds: List<String> = emptyList(),
)

data class InfectionGroupDefinition(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val entryIds: List<String> = emptyList(),
)

data class TissueSiteDefinition(
    val id: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val matrixField: String = "",
    val summaryEntryIds: List<String> = emptyList(),
    val supportEntryIds: List<String> = emptyList(),
)

enum class ClinicalAxis(val key: String) {
    DRUG_FAMILIES("drug-families"),
    ORGANISM_FAMILIES("organism-families"),
    INFECTION_SYSTEMS("infection-systems"),
    TISSUE_DISTRIBUTION("tissue-distribution");

    companion object {
        fun fromKey(key: String?): ClinicalAxis? = entries.firstOrNull { it.key == key }
    }
}

data class SourceInfo(
    val id: String = "",
    val filename: String = "",
    val title: String = "",
    val role: String = "",
    val quick: Boolean = false,
    val pages: Int = 0,
    val sha256: String = "",
    val bytes: Long = 0,
)

data class ExcludedSource(
    val filename: String = "",
    val reason: String = "",
    val pages: Int = 0,
    val sha256: String = "",
    val bytes: Long = 0,
)

data class SourceRef(
    val sourceId: String = "",
    val page: Int = 0,
)

data class DrugRecord(
    val id: String = "",
    val name: String = "",
    val aliases: List<String> = emptyList(),
    val family: String = "",
    val spectrumUse: String = "",
    val adultDose: String = "",
    val renalRrt: String = "",
    val hepaticAdjustment: String = "",
    val administrationPkPd: String = "",
    val toxicityMonitoring: String = "",
    val distributionSummary: String = "",
    val relatedEntryIds: List<String> = emptyList(),
    val sourceRefs: List<SourceRef> = emptyList(),
)

data class ReferenceField(
    val label: String = "",
    val value: String = "",
)

data class ReferenceEntry(
    val id: String = "",
    val kind: String = "reference",
    val title: String = "",
    val subtitle: String = "",
    val fields: List<ReferenceField> = emptyList(),
    val text: String = "",
    val sourceId: String = "",
    val sourceTitle: String = "",
    val page: Int = 0,
    val quick: Boolean = false,
    val additionalSourcePages: List<Int> = emptyList(),
)

enum class ContentFilter(
    val key: String,
    val arabicLabel: String,
    val englishLabel: String,
) {
    ALL("all", "الكل", "All"),
    ANTIBIOTIC("antibiotic", "المضادات", "Antibiotics"),
    INFECTION("infection", "موقع العدوى", "Infection site"),
    BACTERIA("bacteria", "البكتيريا", "Bacteria"),
    DISTRIBUTION("distribution", "الانتشار", "Distribution"),
    CULTURE("culture", "الزرع والتشخيص", "Culture & diagnostics"),
    QUICK("quick", "دليل الجناح", "Ward quick guide"),
}

sealed interface SearchItem {
    val stableId: String
    val title: String
    val subtitle: String
    val preview: String
    val kind: String
    val score: Int

    data class Drug(
        val value: DrugRecord,
        override val score: Int,
    ) : SearchItem {
        override val stableId: String = value.id
        override val title: String = value.name
        override val subtitle: String = value.family
        override val preview: String = value.spectrumUse
        override val kind: String = "antibiotic"
    }

    data class Entry(
        val value: ReferenceEntry,
        override val score: Int,
    ) : SearchItem {
        override val stableId: String = value.id
        override val title: String = value.title
        override val subtitle: String = value.subtitle
        override val preview: String = value.text
        override val kind: String = value.kind
    }
}
