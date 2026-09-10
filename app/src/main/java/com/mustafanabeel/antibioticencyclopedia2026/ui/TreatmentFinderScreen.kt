package com.mustafanabeel.antibioticencyclopedia2026.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mustafanabeel.antibioticencyclopedia2026.UiLanguage
import com.mustafanabeel.antibioticencyclopedia2026.data.BetaLactamAllergy
import com.mustafanabeel.antibioticencyclopedia2026.data.ClinicalTaxonomy
import com.mustafanabeel.antibioticencyclopedia2026.data.DecisionNote
import com.mustafanabeel.antibioticencyclopedia2026.data.DecisionNoteLevel
import com.mustafanabeel.antibioticencyclopedia2026.data.DecisionSeverity
import com.mustafanabeel.antibioticencyclopedia2026.data.DrugRecord
import com.mustafanabeel.antibioticencyclopedia2026.data.EncyclopediaDataset
import com.mustafanabeel.antibioticencyclopedia2026.data.KidneyStatus
import com.mustafanabeel.antibioticencyclopedia2026.data.ReferenceEntry
import com.mustafanabeel.antibioticencyclopedia2026.data.TherapyClassFit
import com.mustafanabeel.antibioticencyclopedia2026.data.TreatmentDecision
import com.mustafanabeel.antibioticencyclopedia2026.data.TreatmentDecisionEngine
import com.mustafanabeel.antibioticencyclopedia2026.data.TreatmentSelection
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.ClinicalTeal
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.CultureViolet
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.DoseAmber
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.HepaticRose
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.RenalBlue
import java.text.Normalizer
import java.util.Locale

private data class FinderOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val searchText: String = "$title $subtitle",
)

private enum class OpenPicker {
    SYNDROME,
    SITE,
    ORGANISM,
}

private fun UiLanguage.finderText(arabic: String, english: String): String =
    if (this == UiLanguage.ARABIC) arabic else english

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentFinderScreen(
    dataset: EncyclopediaDataset,
    taxonomy: ClinicalTaxonomy,
    language: UiLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenDrug: (String) -> Unit,
) {
    val engine = remember(dataset, taxonomy) { TreatmentDecisionEngine(dataset, taxonomy) }
    val entryMap = remember(dataset) { dataset.entries.associateBy(ReferenceEntry::id) }

    val syndromeOptions = remember(dataset, taxonomy, language) {
        taxonomy.infectionGroups.flatMap { group ->
            group.entryIds.mapNotNull(entryMap::get).map { entry ->
                val groupName = language.finderText(group.titleAr, group.titleEn)
                FinderOption(
                    id = entry.id,
                    title = entry.title,
                    subtitle = groupName,
                    searchText = "${entry.title} $groupName ${entry.fieldValue("Likely pathogens")}",
                )
            }
        }
    }
    val siteOptions = remember(taxonomy, language) {
        taxonomy.tissueSites.map { site ->
            FinderOption(
                site.id,
                language.finderText(site.titleAr, site.titleEn),
                site.titleEn,
                "${site.titleAr} ${site.titleEn} ${site.descriptionAr} ${site.descriptionEn}",
            )
        }
    }
    val organismOptions = remember(dataset, taxonomy, language) {
        taxonomy.organismFamilies.flatMap { family ->
            family.organismEntryIds.mapNotNull(entryMap::get).map { entry ->
                val familyName = language.finderText(family.titleAr, family.titleEn)
                FinderOption(
                    id = entry.id,
                    title = entry.title,
                    subtitle = familyName,
                    searchText = "${entry.title} $familyName ${entry.fieldValue("Typical syndromes")}",
                )
            }
        }.distinctBy(FinderOption::id)
    }

    var syndromeId by rememberSaveable { mutableStateOf<String?>(null) }
    var siteId by rememberSaveable { mutableStateOf<String?>(null) }
    var organismId by rememberSaveable { mutableStateOf<String?>(null) }
    var severityName by rememberSaveable { mutableStateOf(DecisionSeverity.STABLE.name) }
    var allergyName by rememberSaveable { mutableStateOf(BetaLactamAllergy.NONE.name) }
    var kidneyName by rememberSaveable { mutableStateOf(KidneyStatus.NORMAL_OR_UNKNOWN.name) }
    var hepaticImpairment by rememberSaveable { mutableStateOf(false) }
    var openPicker by remember { mutableStateOf<OpenPicker?>(null) }
    var submittedDecision by remember { mutableStateOf<TreatmentDecision?>(null) }
    var showSelectionError by remember { mutableStateOf(false) }

    fun invalidateDecision() {
        submittedDecision = null
        showSelectionError = false
    }

    val selectedSyndrome = syndromeOptions.firstOrNull { it.id == syndromeId }
    val selectedSite = siteOptions.firstOrNull { it.id == siteId }
    val selectedOrganism = organismOptions.firstOrNull { it.id == organismId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            language.finderText("مساعد اختيار العلاج", "Therapy decision aid"),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            language.finderText("مطابقة محلية مع المصادر", "Offline source matching"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            language.finderText("رجوع", "Back"),
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
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FinderIntro(language)
            }
            item {
                Text(
                    language.finderText("1. ماذا تعرف عن الحالة؟", "1. What do you know?"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    language.finderText(
                        "اختر معلومة واحدة على الأقل؛ الجمع بين الثلاثة يعطي مطابقة أدق.",
                        "Choose at least one item; combining all three produces a more specific match.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                FinderSelectionCard(
                    title = language.finderText("نوع الالتهاب أو المتلازمة", "Infection or syndrome"),
                    placeholder = language.finderText("مثال: التهاب رئة مكتسب من المجتمع", "Example: community-acquired pneumonia"),
                    value = selectedSyndrome?.title,
                    subtitle = selectedSyndrome?.subtitle,
                    icon = Icons.Default.LocalHospital,
                    tint = DoseAmber,
                    onClick = { openPicker = OpenPicker.SYNDROME },
                    onClear = {
                        syndromeId = null
                        invalidateDecision()
                    },
                )
            }
            item {
                FinderSelectionCard(
                    title = language.finderText("مكان العدوى أو النسيج", "Infection site or tissue"),
                    placeholder = language.finderText("مثال: الرئة، الدم، البروستات", "Example: lung, blood, prostate"),
                    value = selectedSite?.title,
                    subtitle = selectedSite?.subtitle,
                    icon = Icons.Default.Public,
                    tint = RenalBlue,
                    onClick = { openPicker = OpenPicker.SITE },
                    onClear = {
                        siteId = null
                        invalidateDecision()
                    },
                )
            }
            item {
                FinderSelectionCard(
                    title = language.finderText("البكتيريا أو نمط المقاومة", "Organism or resistance phenotype"),
                    placeholder = language.finderText("مثال: MRSA أو Pseudomonas", "Example: MRSA or Pseudomonas"),
                    value = selectedOrganism?.title,
                    subtitle = selectedOrganism?.subtitle,
                    icon = Icons.Default.Biotech,
                    tint = CultureViolet,
                    onClick = { openPicker = OpenPicker.ORGANISM },
                    onClear = {
                        organismId = null
                        invalidateDecision()
                    },
                )
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 3.dp))
                Text(
                    language.finderText("2. عوامل تغيّر القرار", "2. Decision modifiers"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            item {
                ModifierSection(
                    title = language.finderText("شدة الحالة", "Severity"),
                    choices = listOf(
                        DecisionSeverity.STABLE.name to language.finderText("مستقرة", "Stable"),
                        DecisionSeverity.SEVERE_OR_SHOCK.name to language.finderText("شديدة / صدمة", "Severe / shock"),
                    ),
                    selected = severityName,
                    tint = HepaticRose,
                    onSelected = {
                        severityName = it
                        invalidateDecision()
                    },
                )
            }
            item {
                ModifierSection(
                    title = language.finderText("حساسية البيتا لاكتام", "Beta-lactam allergy"),
                    choices = listOf(
                        BetaLactamAllergy.NONE.name to language.finderText("لا توجد", "None"),
                        BetaLactamAllergy.NON_SEVERE_OR_UNCERTAIN.name to language.finderText("خفيفة / غير واضحة", "Mild / uncertain"),
                        BetaLactamAllergy.IMMEDIATE_SEVERE.name to language.finderText("فورية شديدة", "Immediate severe"),
                    ),
                    selected = allergyName,
                    tint = DoseAmber,
                    onSelected = {
                        allergyName = it
                        invalidateDecision()
                    },
                )
            }
            item {
                ModifierSection(
                    title = language.finderText("وظيفة الكلية", "Kidney function"),
                    choices = listOf(
                        KidneyStatus.NORMAL_OR_UNKNOWN.name to language.finderText("طبيعية / غير معروفة", "Normal / unknown"),
                        KidneyStatus.IMPAIRED.name to language.finderText("قصور كلوي", "Impaired"),
                        KidneyStatus.DIALYSIS_OR_RRT.name to language.finderText("غسيل / RRT", "Dialysis / RRT"),
                    ),
                    selected = kidneyName,
                    tint = RenalBlue,
                    onSelected = {
                        kidneyName = it
                        invalidateDecision()
                    },
                )
            }
            item {
                ModifierSection(
                    title = language.finderText("وظيفة الكبد", "Liver function"),
                    choices = listOf(
                        "normal" to language.finderText("لا يوجد قصور مهم", "No major impairment"),
                        "impaired" to language.finderText("قصور كبدي مهم", "Significant impairment"),
                    ),
                    selected = if (hepaticImpairment) "impaired" else "normal",
                    tint = HepaticRose,
                    onSelected = {
                        hepaticImpairment = it == "impaired"
                        invalidateDecision()
                    },
                )
            }
            item {
                if (showSelectionError) {
                    Text(
                        language.finderText(
                            "اختر نوع التهاب أو مكانًا أو بكتيريا أولًا.",
                            "Choose an infection, site, or organism first.",
                        ),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = {
                        if (syndromeId == null && siteId == null && organismId == null) {
                            showSelectionError = true
                        } else {
                            submittedDecision = engine.evaluate(
                                TreatmentSelection(
                                    syndromeEntryId = syndromeId,
                                    organismEntryId = organismId,
                                    tissueSiteId = siteId,
                                    severity = DecisionSeverity.valueOf(severityName),
                                    betaLactamAllergy = BetaLactamAllergy.valueOf(allergyName),
                                    kidneyStatus = KidneyStatus.valueOf(kidneyName),
                                    hepaticImpairment = hepaticImpairment,
                                )
                            )
                            showSelectionError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Icon(Icons.Default.HealthAndSafety, null)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        language.finderText("اعرض المطابقة العلاجية", "Show therapy match"),
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                TextButton(
                    onClick = {
                        syndromeId = null
                        siteId = null
                        organismId = null
                        severityName = DecisionSeverity.STABLE.name
                        allergyName = BetaLactamAllergy.NONE.name
                        kidneyName = KidneyStatus.NORMAL_OR_UNKNOWN.name
                        hepaticImpairment = false
                        invalidateDecision()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(language.finderText("مسح الاختيارات", "Clear selections"))
                }
            }

            submittedDecision?.let { decision ->
                item {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        language.finderText("النتيجة", "Result"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        language.finderText(
                            "مصدر مباشر أولًا، ثم تحقق الجرثومة والموقع وعوامل المريض.",
                            "Direct source regimen first, followed by organism, site, and patient checks.",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                treatmentDecisionItems(
                    decision = decision,
                    language = language,
                    entryMap = entryMap,
                    onOpenEntry = onOpenEntry,
                    onOpenDrug = onOpenDrug,
                    onUseSyndrome = { id ->
                        syndromeId = id
                        submittedDecision = engine.evaluate(
                            decision.selection.copy(syndromeEntryId = id)
                        )
                    },
                )
            }
        }
    }

    when (openPicker) {
        OpenPicker.SYNDROME -> FinderPickerDialog(
            title = language.finderText("اختر نوع الالتهاب", "Choose infection or syndrome"),
            searchHint = language.finderText("اكتب pneumonia أو cystitis…", "Type pneumonia, cystitis…"),
            options = syndromeOptions,
            selectedId = syndromeId,
            language = language,
            onDismiss = { openPicker = null },
            onSelect = {
                syndromeId = it
                openPicker = null
                invalidateDecision()
            },
        )
        OpenPicker.SITE -> FinderPickerDialog(
            title = language.finderText("اختر مكان العدوى", "Choose infection site"),
            searchHint = language.finderText("الرئة، الدم، البول…", "Lung, blood, urine…"),
            options = siteOptions,
            selectedId = siteId,
            language = language,
            onDismiss = { openPicker = null },
            onSelect = {
                siteId = it
                openPicker = null
                invalidateDecision()
            },
        )
        OpenPicker.ORGANISM -> FinderPickerDialog(
            title = language.finderText("اختر البكتيريا", "Choose organism"),
            searchHint = language.finderText("اكتب MRSA أو E. coli…", "Type MRSA, E. coli…"),
            options = organismOptions,
            selectedId = organismId,
            language = language,
            onDismiss = { openPicker = null },
            onSelect = {
                organismId = it
                openPicker = null
                invalidateDecision()
            },
        )
        null -> Unit
    }
}

@Composable
private fun FinderIntro(language: UiLanguage) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ClinicalTeal.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ClinicalTeal.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = ClinicalTeal.copy(alpha = 0.18f)) {
                Icon(
                    Icons.Default.HealthAndSafety,
                    null,
                    tint = ClinicalTeal,
                    modifier = Modifier.padding(10.dp).size(25.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    language.finderText("ليس تخمينًا بالذكاء الاصطناعي", "Not an AI guess"),
                    fontWeight = FontWeight.ExtraBold,
                    color = ClinicalTeal,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    language.finderText(
                        "يعرض النظام التجريبي المكتوب في الموسوعة حرفيًا، ثم يربطه بالطيف والاختراق وتعديلات الكلية والكبد. لا يحتاج إنترنت أو اشتراك.",
                        "The app displays the encyclopedia's exact empiric regimen, then cross-links spectrum, penetration, renal, and hepatic notes. No internet or subscription is required.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun FinderSelectionCard(
    title: String,
    placeholder: String,
    value: String?,
    subtitle: String?,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.34f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.14f)) {
                Icon(icon, null, tint = tint, modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(3.dp))
                FinderLtrText(
                    value ?: placeholder,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (value == null) null else FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            if (value != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = tint)
            }
        }
    }
}

@Composable
private fun ModifierSection(
    title: String,
    choices: List<Pair<String, String>>,
    selected: String,
    tint: Color,
    onSelected: (String) -> Unit,
) {
    Column {
        Text(title, fontWeight = FontWeight.Bold)
        LazyRow(
            contentPadding = PaddingValues(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(choices, key = Pair<String, String>::first) { (key, label) ->
                FilterChip(
                    selected = selected == key,
                    onClick = { onSelected(key) },
                    label = { Text(label) },
                    leadingIcon = if (selected == key) {
                        { Icon(Icons.Default.CheckCircle, null, tint = tint, modifier = Modifier.size(17.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun FinderPickerDialog(
    title: String,
    searchHint: String,
    options: List<FinderOption>,
    selectedId: String?,
    language: UiLanguage,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, options) {
        val normalized = query.finderNormalize()
        if (normalized.isBlank()) options
        else options.filter { normalized in it.searchText.finderNormalize() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(searchHint) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    language.finderText("${filtered.size} خيار", "${filtered.size} options"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(5.dp))
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            language.finderText("لا توجد مطابقة؛ جرّب الاسم الإنكليزي أو الاختصار.", "No match; try the English name or abbreviation."),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(filtered, key = FinderOption::id) { option ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(option.id) },
                                color = if (option.id == selectedId) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        FinderLtrText(option.title, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(2.dp))
                                        Text(option.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                    if (option.id == selectedId) {
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.treatmentDecisionItems(
    decision: TreatmentDecision,
    language: UiLanguage,
    entryMap: Map<String, ReferenceEntry>,
    onOpenEntry: (String) -> Unit,
    onOpenDrug: (String) -> Unit,
    onUseSyndrome: (String) -> Unit,
) {
    if (decision.notes.isNotEmpty()) {
        item {
            ResultSectionTitle(language.finderText("تنبيهات المطابقة", "Match checks"))
        }
        items(decision.notes, key = { "note-${it.titleEn}" }) { note ->
            DecisionNoteCard(
                note = note,
                language = language,
                onOpenEvidence = note.evidenceEntryId
                    ?.takeIf(entryMap::containsKey)
                    ?.let { id -> ({ onOpenEntry(id) }) },
            )
        }
    }

    decision.syndrome?.let { syndrome ->
        item {
            ResultSectionTitle(
                language.finderText("الإجابة المباشرة من المرجع", "Direct answer from the source"),
                language.finderText("نظام تجريبي للمتلازمة المختارة", "Empiric regimen for the selected syndrome"),
            )
        }
        item {
            DirectRegimenCard(decision, language, onOpenEntry = { onOpenEntry(syndrome.id) })
        }
    }

    if (decision.escalationOptions.isNotEmpty()) {
        item {
            ResultSectionTitle(
                language.finderText("قارن مسار الحالة الشديدة", "Compare the severe-care pathway"),
                language.finderText("لم أستبدل النظام تلقائيًا", "The regimen was not substituted automatically"),
            )
        }
        items(decision.escalationOptions, key = { "escalation-${it.id}" }) { entry ->
            SuggestionCard(entry, language, onUse = { onUseSyndrome(entry.id) }, onOpen = { onOpenEntry(entry.id) })
        }
    }

    decision.organism?.let { organism ->
        item {
            ResultSectionTitle(
                language.finderText("الجرثومة المختارة", "Selected organism"),
                decision.organismFamily?.let { language.finderText(it.titleAr, it.titleEn) },
            )
        }
        item {
            SourceEvidenceCard(organism, CultureViolet, onClick = { onOpenEntry(organism.id) })
        }
    }

    if (decision.organismDeepDives.isNotEmpty()) {
        item {
            ResultSectionTitle(
                language.finderText("العلاج الموجّه والمقاومة", "Organism-directed therapy & resistance"),
                language.finderText("أقرب صفحات تفصيلية مطابقة", "Closest matching detailed source pages"),
            )
        }
        items(decision.organismDeepDives, key = { "deep-${it.id}" }) { entry ->
            SourceEvidenceCard(entry, CultureViolet, onClick = { onOpenEntry(entry.id) })
        }
    }

    if (decision.relatedSyndromes.isNotEmpty()) {
        item {
            ResultSectionTitle(
                language.finderText("حدد المتلازمة للحصول على نظام مباشر", "Choose a syndrome for a direct regimen"),
                language.finderText("اقتراحات مبنية على الموقع والجرثومة", "Suggestions based on the site and organism"),
            )
        }
        items(decision.relatedSyndromes, key = { "related-${it.id}" }) { entry ->
            SuggestionCard(entry, language, onUse = { onUseSyndrome(entry.id) }, onOpen = { onOpenEntry(entry.id) })
        }
    }

    if (decision.classFits.isNotEmpty()) {
        item {
            ResultSectionTitle(
                language.finderText("تقاطع الطيف مع الاختراق", "Spectrum × penetration cross-check"),
                language.finderText(
                    "ترتيب عائلي للمراجعة وليس وصفة أو بديلًا عن AST/MIC",
                    "A class-level review list, not a prescription or substitute for AST/MIC",
                ),
            )
        }
        items(decision.classFits, key = { "fit-${it.spectrumEntry.id}" }) { fit ->
            TherapyFitCard(fit, language, onClick = { onOpenEntry(fit.spectrumEntry.id) })
        }
    }

    if (decision.tissueEvidence.isNotEmpty()) {
        item {
            ResultSectionTitle(
                language.finderText("دليل الوصول إلى الموقع", "Site penetration evidence"),
                decision.tissueSite?.let { language.finderText(it.titleAr, it.titleEn) },
            )
        }
        items(decision.tissueEvidence, key = { "tissue-${it.id}" }) { entry ->
            SourceEvidenceCard(entry, RenalBlue, onClick = { onOpenEntry(entry.id) })
        }
    }

    if (decision.linkedDrugs.isNotEmpty()) {
        item {
            ResultSectionTitle(
                language.finderText("بطاقات الأدوية المذكورة", "Mentioned drug monographs"),
                language.finderText("الجرعة، الكلية/RRT، الكبد والسمية", "Dose, renal/RRT, hepatic and toxicity details"),
            )
        }
        items(decision.linkedDrugs, key = { "drug-${it.id}" }) { drug ->
            LinkedDrugCard(drug, language, onClick = { onOpenDrug(drug.id) })
        }
    }

    item {
        FinderFinalSafety(language)
    }
}

@Composable
private fun ResultSectionTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 5.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun DecisionNoteCard(
    note: DecisionNote,
    language: UiLanguage,
    onOpenEvidence: (() -> Unit)?,
) {
    val tint = when (note.level) {
        DecisionNoteLevel.CRITICAL -> MaterialTheme.colorScheme.error
        DecisionNoteLevel.WARNING -> DoseAmber
        DecisionNoteLevel.INFO -> RenalBlue
    }
    val icon = if (note.level == DecisionNoteLevel.INFO) Icons.Default.Info else Icons.Default.WarningAmber
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpenEvidence != null) Modifier.clickable(onClick = onOpenEvidence) else Modifier),
        color = tint.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.38f)),
        shape = RoundedCornerShape(17.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    language.finderText(note.titleAr, note.titleEn),
                    fontWeight = FontWeight.ExtraBold,
                    color = tint,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    language.finderText(note.bodyAr, note.bodyEn),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
                if (onOpenEvidence != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        language.finderText("افتح الدليل المصدر", "Open source evidence"),
                        color = tint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectRegimenCard(
    decision: TreatmentDecision,
    language: UiLanguage,
    onOpenEntry: () -> Unit,
) {
    val syndrome = decision.syndrome ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ClinicalTeal.copy(alpha = 0.09f)),
        border = BorderStroke(1.dp, ClinicalTeal.copy(alpha = 0.42f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            FinderLtrText(syndrome.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            if (decision.likelyPathogens.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                ResultField(
                    language.finderText("الممرضات المتوقعة", "Likely pathogens"),
                    decision.likelyPathogens,
                    CultureViolet,
                )
            }
            if (decision.empiricRegimen.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                ResultField(
                    language.finderText("النظام التجريبي في المرجع", "Source empiric regimen"),
                    decision.empiricRegimen,
                    ClinicalTeal,
                    emphasized = true,
                )
            }
            if (decision.keyModifier.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                ResultField(
                    language.finderText("العامل الذي يغيّر القرار", "Key modifier / action"),
                    decision.keyModifier,
                    DoseAmber,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenEntry, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null)
                Spacer(Modifier.width(7.dp))
                Text(language.finderText("افتح السطر والمصدر", "Open row and source"))
            }
            FinderSourceLine(syndrome, language)
        }
    }
}

@Composable
private fun ResultField(
    title: String,
    value: String,
    tint: Color,
    emphasized: Boolean = false,
) {
    Column {
        Text(title, color = tint, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        FinderLtrText(
            value,
            fontWeight = if (emphasized) FontWeight.Bold else null,
            fontSize = if (emphasized) 17.sp else 14.sp,
            lineHeight = if (emphasized) 24.sp else 21.sp,
        )
    }
}

@Composable
private fun SourceEvidenceCard(entry: ReferenceEntry, tint: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            FinderLtrText(entry.title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            FinderLtrText(
                entry.text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(8.dp))
            FinderLtrText(
                "${entry.sourceTitle} · p. ${entry.page}",
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    entry: ReferenceEntry,
    language: UiLanguage,
    onUse: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DoseAmber.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            FinderLtrText(entry.title, fontWeight = FontWeight.ExtraBold)
            val pathogens = entry.fieldValue("Likely pathogens")
            if (pathogens.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                FinderLtrText(pathogens, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onUse, modifier = Modifier.weight(1f)) {
                    Text(language.finderText("استخدم هذا النوع", "Use this syndrome"), maxLines = 1)
                }
                TextButton(onClick = onOpen) {
                    Text(language.finderText("التفاصيل", "Details"))
                }
            }
        }
    }
}

@Composable
private fun TherapyFitCard(fit: TherapyClassFit, language: UiLanguage, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ClinicalTeal.copy(alpha = 0.27f)),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = ClinicalTeal.copy(alpha = 0.14f)) {
                    Icon(Icons.Default.Medication, null, tint = ClinicalTeal, modifier = Modifier.padding(7.dp).size(18.dp))
                }
                Spacer(Modifier.width(9.dp))
                FinderLtrText(fit.spectrumEntry.title, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            ResultField(language.finderText("الطيف", "Spectrum"), fit.coverage, ClinicalTeal)
            if (fit.penetration.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                ResultField(language.finderText("الاختراق في الموقع", "Site penetration"), fit.penetration, RenalBlue)
            }
            if (fit.majorGaps.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                ResultField(language.finderText("فجوات مهمة", "Major gaps"), fit.majorGaps, DoseAmber)
            }
        }
    }
}

@Composable
private fun LinkedDrugCard(drug: DrugRecord, language: UiLanguage, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, RenalBlue.copy(alpha = 0.27f)),
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            FinderLtrText(drug.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(language.finderText("الكلية / RRT", "Renal / RRT"), color = RenalBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(3.dp))
            FinderLtrText(drug.renalRrt, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (drug.hepaticAdjustment.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(language.finderText("الكبد", "Hepatic"), color = HepaticRose, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(3.dp))
                FinderLtrText(drug.hepaticAdjustment, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FinderFinalSafety(language: UiLanguage) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
        color = DoseAmber.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, DoseAmber.copy(alpha = 0.38f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.WarningAmber, null, tint = DoseAmber)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    language.finderText("حدود آمنة", "Safety boundary"),
                    fontWeight = FontWeight.ExtraBold,
                    color = DoseAmber,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    language.finderText(
                        "هذا مرجع تعليمي للصيادلة وليس أمرًا علاجيًا. ثبّت التشخيص، خذ المزروعات، راجع AST/MIC والحساسية والتداخلات والحمل والعمر والوزن ووظائف الأعضاء والبروتوكول المحلي، ثم أعد التقييم خلال 48–72 ساعة.",
                        "This is an educational pharmacist reference, not a treatment order. Confirm the diagnosis, obtain cultures, review AST/MIC, allergy, interactions, pregnancy, age, weight, organ function, and local protocol, then reassess at 48–72 hours.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun FinderSourceLine(entry: ReferenceEntry, language: UiLanguage) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        FinderLtrText(entry.sourceTitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        FinderLtrText(
            language.finderText("PDF page ${entry.page}", "PDF page ${entry.page}"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

private fun ReferenceEntry.fieldValue(label: String): String =
    fields.firstOrNull { it.label.equals(label, ignoreCase = true) }?.value.orEmpty()

private fun String.finderNormalize(): String = Normalizer
    .normalize(lowercase(Locale.US), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .replace(Regex("[^a-z0-9\\u0600-\\u06ff]+"), " ")
    .trim()

@Composable
private fun FinderLtrText(
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
