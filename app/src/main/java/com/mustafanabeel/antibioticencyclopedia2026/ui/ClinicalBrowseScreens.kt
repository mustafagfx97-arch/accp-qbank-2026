package com.mustafanabeel.antibioticencyclopedia2026.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mustafanabeel.antibioticencyclopedia2026.UiLanguage
import com.mustafanabeel.antibioticencyclopedia2026.data.ClinicalAxis
import com.mustafanabeel.antibioticencyclopedia2026.data.ClinicalTaxonomy
import com.mustafanabeel.antibioticencyclopedia2026.data.DrugFamilyDefinition
import com.mustafanabeel.antibioticencyclopedia2026.data.DrugRecord
import com.mustafanabeel.antibioticencyclopedia2026.data.EncyclopediaDataset
import com.mustafanabeel.antibioticencyclopedia2026.data.InfectionGroupDefinition
import com.mustafanabeel.antibioticencyclopedia2026.data.OrganismFamilyDefinition
import com.mustafanabeel.antibioticencyclopedia2026.data.ReferenceEntry
import com.mustafanabeel.antibioticencyclopedia2026.data.TissueSiteDefinition
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.ClinicalTeal
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.CultureViolet
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.DoseAmber
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.HepaticRose
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.RenalBlue

private data class TopicCardModel(
    val id: String,
    val title: String,
    val description: String,
    val count: Int,
    val countLabel: String,
)

private fun UiLanguage.pick(arabic: String, english: String): String =
    if (this == UiLanguage.ARABIC) arabic else english

private fun localized(
    language: UiLanguage,
    arabic: String,
    english: String,
): String = language.pick(arabic, english)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClinicalTopBar(
    title: String,
    language: UiLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    localized(language, "رجوع", "Back"),
                )
            }
        },
        actions = {
            FilledTonalIconButton(onClick = onToggleLanguage) {
                Text(
                    if (language == UiLanguage.ARABIC) "EN" else "ع",
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
fun ClinicalCatalogScreen(
    axis: ClinicalAxis,
    taxonomy: ClinicalTaxonomy,
    dataset: EncyclopediaDataset,
    language: UiLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenTopic: (String) -> Unit,
) {
    val axisTitle = when (axis) {
        ClinicalAxis.DRUG_FAMILIES -> localized(language, "عوائل المضادات", "Antibiotic families")
        ClinicalAxis.ORGANISM_FAMILIES -> localized(language, "عوائل الجراثيم", "Organism families")
        ClinicalAxis.INFECTION_SYSTEMS -> localized(language, "العدوى حسب الجهاز", "Infections by system")
        ClinicalAxis.TISSUE_DISTRIBUTION -> localized(language, "التوزيع داخل الجسم", "Tissue distribution")
    }
    val axisDescription = when (axis) {
        ClinicalAxis.DRUG_FAMILIES -> localized(
            language,
            "ابدأ بالعائلة ثم المجموعة الفرعية ثم بطاقة الدواء والجرعة وتعديلات الكلية والكبد.",
            "Start with the family, then subfamily, then the full dose, renal and hepatic monograph.",
        )
        ClinicalAxis.ORGANISM_FAMILIES -> localized(
            language,
            "ابدأ ببنية الجرثومة ثم النوع ونمط المقاومة وخيارات الطيف المرتبطة بالمصدر.",
            "Start with organism structure, then species, resistance phenotype and source-backed spectrum options.",
        )
        ClinicalAxis.INFECTION_SYSTEMS -> localized(
            language,
            "كل متلازمة مرتبة إلى ممرضات متوقعة، علاج تجريبي، ثم العامل الذي يغيّر القرار.",
            "Each syndrome is organized into likely pathogens, empiric therapy and the modifier that changes management.",
        )
        ClinicalAxis.TISSUE_DISTRIBUTION -> localized(
            language,
            "اختر العضو لرؤية العوامل المفيدة، الممنوعات العملية، ومصفوفة الاختراق مرتبة.",
            "Choose an organ to see useful agents, practical avoidances and a ranked penetration matrix.",
        )
    }
    val cards = remember(axis, taxonomy, dataset, language) {
        when (axis) {
            ClinicalAxis.DRUG_FAMILIES -> taxonomy.drugFamilies.map { family ->
                TopicCardModel(
                    family.id,
                    localized(language, family.titleAr, family.titleEn),
                    localized(language, family.descriptionAr, family.descriptionEn),
                    family.subfamilies.sumOf { it.drugNames.size },
                    localized(language, "دواء", "drugs"),
                )
            }
            ClinicalAxis.ORGANISM_FAMILIES -> taxonomy.organismFamilies.map { family ->
                TopicCardModel(
                    family.id,
                    localized(language, family.titleAr, family.titleEn),
                    localized(language, family.descriptionAr, family.descriptionEn),
                    family.organismEntryIds.size,
                    localized(language, "جرثومة/مجموعة", "organisms/groups"),
                )
            }
            ClinicalAxis.INFECTION_SYSTEMS -> taxonomy.infectionGroups.map { group ->
                TopicCardModel(
                    group.id,
                    localized(language, group.titleAr, group.titleEn),
                    localized(language, group.descriptionAr, group.descriptionEn),
                    group.entryIds.size,
                    localized(language, "متلازمة", "syndromes"),
                )
            }
            ClinicalAxis.TISSUE_DISTRIBUTION -> taxonomy.tissueSites.map { site ->
                val rows = if (site.matrixField.isBlank()) {
                    site.summaryEntryIds.size + site.supportEntryIds.size
                } else {
                    taxonomy.penetrationEntryIds.mapNotNull { id ->
                        dataset.entries.firstOrNull { it.id == id }
                    }.count { entry -> entry.field(site.matrixField).isNotBlank() }
                }
                TopicCardModel(
                    site.id,
                    localized(language, site.titleAr, site.titleEn),
                    localized(language, site.descriptionAr, site.descriptionEn),
                    rows,
                    localized(language, "دليل اختراق", "penetration rows"),
                )
            }
        }
    }
    val tint = axis.tint()
    Scaffold(
        topBar = {
            ClinicalTopBar(axisTitle, language, onBack, onToggleLanguage)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item {
                ClinicalIntroCard(
                    title = localized(language, "مسار سريري منظّم", "Structured clinical path"),
                    description = axisDescription,
                    tint = tint,
                    icon = axis.icon(),
                )
            }
            items(cards, key = { it.id }) { card ->
                ClinicalTopicCard(card, tint, onClick = { onOpenTopic(card.id) })
            }
        }
    }
}

@Composable
private fun ClinicalIntroCard(
    title: String,
    description: String,
    tint: Color,
    icon: ImageVector,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.36f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.18f)) {
                Icon(icon, null, tint = tint, modifier = Modifier.padding(9.dp).size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = tint)
                Spacer(Modifier.height(5.dp))
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ClinicalTopicCard(
    topic: TopicCardModel,
    tint: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    topic.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(8.dp))
                ClinicalLtrText(
                    "${topic.count} ${topic.countLabel}",
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.KeyboardArrowRight, null, tint = tint)
        }
    }
}

@Composable
fun DrugFamilyScreen(
    family: DrugFamilyDefinition,
    dataset: EncyclopediaDataset,
    language: UiLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenDrug: (String) -> Unit,
) {
    val drugsByName = remember(dataset) { dataset.drugs.associateBy(DrugRecord::name) }
    Scaffold(
        topBar = {
            ClinicalTopBar(
                localized(language, family.titleAr, family.titleEn),
                language,
                onBack,
                onToggleLanguage,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ClinicalIntroCard(
                    localized(language, "خريطة العائلة", "Family map"),
                    localized(language, family.descriptionAr, family.descriptionEn),
                    ClinicalTeal,
                    Icons.Default.Medication,
                )
            }
            family.subfamilies.forEach { subfamily ->
                item(key = "header-${subfamily.id}") {
                    SectionHeading(
                        localized(language, subfamily.titleAr, subfamily.titleEn),
                        localized(
                            language,
                            "${subfamily.drugNames.size} دواء",
                            "${subfamily.drugNames.size} drugs",
                        ),
                    )
                }
                items(subfamily.drugNames, key = { "${subfamily.id}-$it" }) { name ->
                    drugsByName[name]?.let { drug ->
                        DrugNavigationCard(drug, language, onClick = { onOpenDrug(drug.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DrugNavigationCard(
    drug: DrugRecord,
    language: UiLanguage,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClinicalLtrText(
                    drug.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Icon(Icons.Default.KeyboardArrowRight, null, tint = ClinicalTeal)
            }
            Spacer(Modifier.height(6.dp))
            ClinicalLtrText(
                drug.spectrumUse,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MiniLabel(localized(language, "الجرعة", "Dose"), DoseAmber)
                MiniLabel(localized(language, "كلية/RRT", "Renal/RRT"), RenalBlue)
                if (drug.hepaticAdjustment.isNotBlank()) {
                    MiniLabel(localized(language, "ملاحظة كبد", "Liver note"), HepaticRose)
                }
            }
        }
    }
}

@Composable
fun OrganismFamilyScreen(
    family: OrganismFamilyDefinition,
    taxonomy: ClinicalTaxonomy,
    dataset: EncyclopediaDataset,
    language: UiLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val entryMap = remember(dataset) { dataset.entries.associateBy(ReferenceEntry::id) }
    val overview = entryMap[family.overviewEntryId]
    val organisms = family.organismEntryIds.mapNotNull(entryMap::get)
    val therapy = family.therapyEntryIds.mapNotNull(entryMap::get)
    val coverage = taxonomy.spectrumEntryIds.mapNotNull(entryMap::get)
        .mapNotNull { entry ->
            val value = entry.field(family.coverageField)
            if (family.coverageField.isBlank() || value.isBlank() || value.isNegativeCoverage()) null
            else entry to value
        }
        .sortedWith(compareByDescending<Pair<ReferenceEntry, String>> { it.second.coverageRank() }.thenBy { it.first.title })

    Scaffold(
        topBar = {
            ClinicalTopBar(
                localized(language, family.titleAr, family.titleEn),
                language,
                onBack,
                onToggleLanguage,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ClinicalIntroCard(
                    if (family.id == "anaerobes") {
                        localized(language, "قسم اللاهوائيات", "Dedicated anaerobe section")
                    } else {
                        localized(language, "هوية المجموعة", "Group identity")
                    },
                    localized(language, family.descriptionAr, family.descriptionEn),
                    CultureViolet,
                    Icons.Default.Biotech,
                )
            }
            if (overview != null) {
                item {
                    SectionHeading(localized(language, "الخلاصة السريرية", "Clinical orientation"))
                }
                item {
                    SourceBackedCard(overview, CultureViolet, onClick = { onOpenEntry(overview.id) })
                }
            }
            if (therapy.isNotEmpty()) {
                item {
                    SectionHeading(
                        localized(language, "ماذا نستخدم؟", "What do we use?"),
                        localized(language, "اختيارات وفجوات مرتبطة بالمصدر", "Source-backed choices and gaps"),
                    )
                }
                items(therapy, key = { "therapy-${it.id}" }) { entry ->
                    SourceBackedCard(entry, ClinicalTeal, onClick = { onOpenEntry(entry.id) })
                }
            }
            if (coverage.isNotEmpty()) {
                item {
                    SectionHeading(
                        localized(language, "خريطة تغطية العائلات", "Antibiotic-family coverage map"),
                        localized(
                            language,
                            "طيف عائلي أولي؛ القرار النهائي حسب النوع والمقاومة والموقع وAST/MIC",
                            "Family-level orientation; finalize by species, phenotype, site and AST/MIC",
                        ),
                    )
                }
                items(coverage, key = { "coverage-${it.first.id}" }) { (entry, value) ->
                    CoverageCard(entry, value, onClick = { onOpenEntry(entry.id) })
                }
            }
            item {
                SectionHeading(
                    localized(language, "الجراثيم داخل العائلة", "Organisms in this family"),
                    localized(language, "${organisms.size} بطاقة مرتبة", "${organisms.size} organized cards"),
                )
            }
            items(organisms, key = { "organism-${it.id}" }) { entry ->
                OrganismCard(entry, onClick = { onOpenEntry(entry.id) })
            }
            item {
                ClinicalCaution(
                    localized(
                        language,
                        "خريطة الطيف لا تعني أن كل دواء مناسب لكل عدوى. طابق الجرثومة ونمط المقاومة مع موقع العدوى والتعرّض والسيطرة على المصدر.",
                        "Spectrum does not make every active drug appropriate for every infection. Match organism and phenotype to site exposure and source control.",
                    )
                )
            }
        }
    }
}

@Composable
private fun CoverageCard(
    entry: ReferenceEntry,
    coverage: String,
    onClick: () -> Unit,
) {
    val gaps = entry.field("Major gaps")
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ClinicalTeal.copy(alpha = 0.075f)),
        border = BorderStroke(1.dp, ClinicalTeal.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = ClinicalTeal, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                ClinicalLtrText(entry.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(7.dp))
            ClinicalLtrText(coverage, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            if (gaps.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                ClinicalLtrText("Gaps: $gaps", color = HepaticRose, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun OrganismCard(entry: ReferenceEntry, onClick: () -> Unit) {
    val syndrome = entry.firstField("Typical syndromes", "Common settings")
    val interpretation = entry.firstField("High-yield interpretation", "Resistance/interpretation pearl", "Clinical interpretation")
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CultureViolet.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            ClinicalLtrText(entry.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (syndrome.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                ClinicalLtrText(syndrome, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
            }
            if (interpretation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(color = CultureViolet.copy(alpha = 0.1f), shape = RoundedCornerShape(11.dp)) {
                    ClinicalLtrText(
                        interpretation,
                        modifier = Modifier.padding(10.dp),
                        color = CultureViolet,
                        lineHeight = 19.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun InfectionGroupScreen(
    group: InfectionGroupDefinition,
    dataset: EncyclopediaDataset,
    language: UiLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val entryMap = remember(dataset) { dataset.entries.associateBy(ReferenceEntry::id) }
    val syndromes = group.entryIds.mapNotNull(entryMap::get)
    Scaffold(
        topBar = {
            ClinicalTopBar(
                localized(language, group.titleAr, group.titleEn),
                language,
                onBack,
                onToggleLanguage,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ClinicalIntroCard(
                    localized(language, "خريطة العلاج التجريبي", "Empiric therapy map"),
                    localized(language, group.descriptionAr, group.descriptionEn),
                    DoseAmber,
                    Icons.Default.LocalHospital,
                )
            }
            item {
                SectionHeading(
                    localized(language, "المتلازمات", "Syndromes"),
                    localized(language, "${syndromes.size} سيناريو سريري", "${syndromes.size} clinical scenarios"),
                )
            }
            items(syndromes, key = ReferenceEntry::id) { entry ->
                SyndromeCard(entry, language, onClick = { onOpenEntry(entry.id) })
            }
            item {
                ClinicalCaution(
                    localized(
                        language,
                        "النظم المعروضة تبدأ من وظيفة كلوية طبيعية كما ورد في المصدر؛ افتح بطاقة الدواء واضبطها حسب الكلية والكبد وRRT والحساسية ونتائج الزرع.",
                        "Displayed regimens assume normal renal function as stated in the source. Open each drug monograph and adjust for renal/hepatic function, RRT, allergy and cultures.",
                    )
                )
            }
        }
    }
}

@Composable
private fun SyndromeCard(
    entry: ReferenceEntry,
    language: UiLanguage,
    onClick: () -> Unit,
) {
    val pathogens = entry.field("Likely pathogens")
    val regimen = entry.field("Empiric regimen")
    val action = entry.field("Key modifier / action")
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DoseAmber.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            ClinicalLtrText(entry.title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            if (pathogens.isNotBlank()) {
                KeyValueLine(localized(language, "الممرضات", "Pathogens"), pathogens, CultureViolet)
            }
            if (regimen.isNotBlank()) {
                KeyValueLine(localized(language, "العلاج التجريبي", "Empiric"), regimen, ClinicalTeal)
            }
            if (action.isNotBlank()) {
                KeyValueLine(localized(language, "ما يغيّر القرار", "Modifier"), action, DoseAmber)
            }
        }
    }
}

@Composable
fun TissueSiteScreen(
    site: TissueSiteDefinition,
    taxonomy: ClinicalTaxonomy,
    dataset: EncyclopediaDataset,
    language: UiLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val entryMap = remember(dataset) { dataset.entries.associateBy(ReferenceEntry::id) }
    val guidance = (site.summaryEntryIds + site.supportEntryIds).mapNotNull(entryMap::get)
    val penetration = if (site.matrixField.isBlank()) {
        emptyList()
    } else {
        taxonomy.penetrationEntryIds.mapNotNull(entryMap::get)
            .mapNotNull { entry ->
                entry.field(site.matrixField).takeIf(String::isNotBlank)?.let { entry to it }
            }
            .sortedWith(compareByDescending<Pair<ReferenceEntry, String>> { it.second.penetrationRank() }.thenBy { it.first.title })
    }
    Scaffold(
        topBar = {
            ClinicalTopBar(
                localized(language, site.titleAr, site.titleEn),
                language,
                onBack,
                onToggleLanguage,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ClinicalIntroCard(
                    localized(language, "قرار حسب مكان العدوى", "Site-specific decision"),
                    localized(language, site.descriptionAr, site.descriptionEn),
                    RenalBlue,
                    Icons.Default.Public,
                )
            }
            if (guidance.isNotEmpty()) {
                item {
                    SectionHeading(localized(language, "اختيارات وحدود عملية", "Useful agents & practical limits"))
                }
                items(guidance, key = { "guidance-${it.id}" }) { entry ->
                    SourceBackedCard(entry, RenalBlue, onClick = { onOpenEntry(entry.id) })
                }
            }
            if (penetration.isNotEmpty()) {
                item {
                    SectionHeading(
                        localized(language, "مصفوفة الاختراق المرتبة", "Ranked penetration matrix"),
                        localized(language, "${penetration.size} عائلة/عامل", "${penetration.size} classes/agents"),
                    )
                }
                item { PenetrationLegend(language) }
                items(penetration, key = { "penetration-${it.first.id}" }) { (entry, value) ->
                    PenetrationCard(entry, value, onClick = { onOpenEntry(entry.id) })
                }
            }
            item {
                ClinicalCaution(
                    localized(
                        language,
                        "الاختراق وحده لا يكفي: يجب أن يكون الطيف فعّالاً وأن تتحقق جرعة PK/PD المناسبة مع السيطرة على المصدر.",
                        "Penetration alone is insufficient: spectrum must be active, the PK/PD exposure adequate and source control achieved.",
                    )
                )
            }
        }
    }
}

@Composable
private fun PenetrationLegend(language: UiLanguage) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(localized(language, "مفتاح المصفوفة", "Matrix legend"), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            ClinicalLtrText(
                "+++ excellent/preferred · ++ generally useful · + limited/variable · 0 poor · X unsuitable/inactivated",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun PenetrationCard(
    entry: ReferenceEntry,
    value: String,
    onClick: () -> Unit,
) {
    val rank = value.penetrationRank()
    val tint = when {
        rank >= 4 -> ClinicalTeal
        rank >= 3 -> RenalBlue
        rank >= 2 -> DoseAmber
        else -> HepaticRose
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(15.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ClinicalLtrText(entry.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Surface(color = tint.copy(alpha = 0.13f), shape = RoundedCornerShape(10.dp)) {
                ClinicalLtrText(
                    value,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SourceBackedCard(
    entry: ReferenceEntry,
    tint: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.26f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = tint, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                ClinicalLtrText(entry.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                ClinicalLtrText("p.${entry.page}", color = tint, fontSize = 11.sp)
            }
            entry.fields.drop(1).take(4).forEach { field ->
                Spacer(Modifier.height(7.dp))
                ClinicalLtrText(field.label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                ClinicalLtrText(field.value, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun KeyValueLine(label: String, value: String, tint: Color) {
    Spacer(Modifier.height(9.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    Spacer(Modifier.height(8.dp))
    Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    Spacer(Modifier.height(3.dp))
    ClinicalLtrText(value, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
}

@Composable
private fun MiniLabel(text: String, tint: Color) {
    Surface(color = tint.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun ClinicalCaution(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        color = DoseAmber.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, DoseAmber.copy(alpha = 0.34f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, null, tint = DoseAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
        }
    }
}

private fun ReferenceEntry.field(label: String): String =
    fields.firstOrNull { it.label.equals(label, ignoreCase = true) }?.value.orEmpty()

private fun ReferenceEntry.firstField(vararg labels: String): String =
    labels.firstNotNullOfOrNull { label -> field(label).takeIf(String::isNotBlank) }.orEmpty()

private fun String.isNegativeCoverage(): Boolean {
    val normalized = trim().lowercase()
    return normalized == "0" || normalized == "no" || normalized == "none" ||
        normalized.startsWith("no ") || normalized.startsWith("none ") ||
        normalized.startsWith("not reliable") || normalized.startsWith("not a primary")
}

private fun String.coverageRank(): Int {
    val normalized = lowercase()
    return when {
        "excellent" in normalized -> 6
        "strong" in normalized || "good" in normalized || "broad" in normalized -> 5
        "mrsa" in normalized || "bacteroides" in normalized || "streptococci" in normalized -> 4
        "selected" in normalized || "partial" in normalized || "variable" in normalized -> 2
        else -> 3
    }
}

private fun String.penetrationRank(): Int = when {
    "+++" in this -> 4
    "++" in this -> 3
    trim().startsWith("+") -> 2
    trim().startsWith("0") || trim().startsWith("X", ignoreCase = true) -> 0
    else -> 1
}

private fun ClinicalAxis.tint(): Color = when (this) {
    ClinicalAxis.DRUG_FAMILIES -> ClinicalTeal
    ClinicalAxis.ORGANISM_FAMILIES -> CultureViolet
    ClinicalAxis.INFECTION_SYSTEMS -> DoseAmber
    ClinicalAxis.TISSUE_DISTRIBUTION -> RenalBlue
}

private fun ClinicalAxis.icon(): ImageVector = when (this) {
    ClinicalAxis.DRUG_FAMILIES -> Icons.Default.Medication
    ClinicalAxis.ORGANISM_FAMILIES -> Icons.Default.Biotech
    ClinicalAxis.INFECTION_SYSTEMS -> Icons.Default.LocalHospital
    ClinicalAxis.TISSUE_DISTRIBUTION -> Icons.Default.Public
}

@Composable
private fun ClinicalLtrText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            overflow = overflow,
            maxLines = maxLines,
            style = style,
        )
    }
}
