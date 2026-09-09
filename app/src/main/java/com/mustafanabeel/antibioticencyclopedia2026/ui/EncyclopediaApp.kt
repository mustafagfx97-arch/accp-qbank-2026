package com.mustafanabeel.antibioticencyclopedia2026.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mustafanabeel.antibioticencyclopedia2026.EncyclopediaViewModel
import com.mustafanabeel.antibioticencyclopedia2026.data.ContentFilter
import com.mustafanabeel.antibioticencyclopedia2026.data.DrugRecord
import com.mustafanabeel.antibioticencyclopedia2026.data.EncyclopediaDataset
import com.mustafanabeel.antibioticencyclopedia2026.data.ReferenceEntry
import com.mustafanabeel.antibioticencyclopedia2026.data.SearchItem
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.ClinicalTeal
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.CultureViolet
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.DoseAmber
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.HepaticRose
import com.mustafanabeel.antibioticencyclopedia2026.ui.theme.RenalBlue

private const val HOME = "home"
private const val SEARCH = "search"
private const val SAVED = "saved"
private const val ABOUT = "about"

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private data class CategorySpec(
    val filter: ContentFilter,
    val title: String,
    val subtitle: String,
    val count: Int,
    val icon: ImageVector,
    val tint: Color,
)

@Composable
fun EncyclopediaApp(viewModel: EncyclopediaViewModel) {
    val state by viewModel.loadState.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when {
            state.loading -> LoadingScreen()
            state.error != null -> ErrorScreen(state.error.orEmpty(), viewModel::reload)
            state.dataset != null -> EncyclopediaNavigation(state.dataset!!, viewModel)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ClinicalTeal)
            Spacer(Modifier.height(18.dp))
            Text("جارٍ تجهيز الموسوعة الطبية…", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            LtrText("Loading the offline clinical database", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text("تعذر فتح الموسوعة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LtrText(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            AssistChip(onClick = onRetry, label = { Text("إعادة المحاولة") }, leadingIcon = {
                Icon(Icons.Default.Refresh, null)
            })
        }
    }
}

@Composable
private fun EncyclopediaNavigation(
    dataset: EncyclopediaDataset,
    viewModel: EncyclopediaViewModel,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val roots = setOf(HOME, SEARCH, SAVED)
    Scaffold(
        bottomBar = {
            if (route in roots) RootNavigationBar(navController, route)
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(HOME) {
                HomeScreen(
                    dataset = dataset,
                    onSearch = { query ->
                        viewModel.beginSearch(query, ContentFilter.ALL)
                        navController.navigate(SEARCH)
                    },
                    onCategory = { filter ->
                        viewModel.beginSearch(filter = filter)
                        navController.navigate(SEARCH)
                    },
                    onAbout = { navController.navigate(ABOUT) },
                )
            }
            composable(SEARCH) {
                SearchScreen(
                    viewModel = viewModel,
                    onOpenDrug = { navController.navigate("drug/$it") },
                    onOpenEntry = { navController.navigate("entry/$it") },
                )
            }
            composable(SAVED) {
                SavedScreen(
                    viewModel = viewModel,
                    onOpenDrug = { navController.navigate("drug/$it") },
                    onOpenEntry = { navController.navigate("entry/$it") },
                )
            }
            composable(
                route = "drug/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val drug = entry.arguments?.getString("id")?.let(viewModel::drug)
                if (drug != null) {
                    DrugDetailScreen(
                        drug = drug,
                        related = viewModel.relatedEntries(drug),
                        bookmarked = drug.id in viewModel.bookmarks.collectAsStateWithLifecycle().value,
                        onBookmark = { viewModel.toggleBookmark(drug.id) },
                        onBack = navController::popBackStack,
                        onOpenEntry = { navController.navigate("entry/$it") },
                    )
                }
            }
            composable(
                route = "entry/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val item = entry.arguments?.getString("id")?.let(viewModel::entry)
                if (item != null) {
                    EntryDetailScreen(
                        entry = item,
                        bookmarked = item.id in viewModel.bookmarks.collectAsStateWithLifecycle().value,
                        onBookmark = { viewModel.toggleBookmark(item.id) },
                        onBack = navController::popBackStack,
                    )
                }
            }
            composable(ABOUT) {
                SourcesScreen(dataset = dataset, onBack = navController::popBackStack)
            }
        }
    }
}

@Composable
private fun RootNavigationBar(navController: NavHostController, currentRoute: String?) {
    val destinations = listOf(
        BottomDestination(HOME, "الرئيسية", Icons.Default.Home),
        BottomDestination(SEARCH, "البحث", Icons.Default.Search),
        BottomDestination(SAVED, "المحفوظات", Icons.Default.Bookmark),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, null) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
}

@Composable
private fun HomeScreen(
    dataset: EncyclopediaDataset,
    onSearch: (String) -> Unit,
    onCategory: (ContentFilter) -> Unit,
    onAbout: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val categories = remember(dataset) {
        listOf(
            CategorySpec(ContentFilter.ANTIBIOTIC, "المضادات الحيوية", "Drug monographs", dataset.drugs.size, Icons.Default.Medication, ClinicalTeal),
            CategorySpec(ContentFilter.INFECTION, "مكان العدوى", "Empiric therapy", dataset.entries.count { it.kind == "infection" }, Icons.Default.LocalHospital, DoseAmber),
            CategorySpec(ContentFilter.BACTERIA, "نوع البكتيريا", "Organism-directed", dataset.entries.count { it.kind == "bacteria" }, Icons.Default.Biotech, CultureViolet),
            CategorySpec(ContentFilter.DISTRIBUTION, "الانتشار داخل الجسم", "Tissue penetration", dataset.entries.count { it.kind == "distribution" }, Icons.Default.Public, RenalBlue),
            CategorySpec(ContentFilter.CULTURE, "الزرع والتشخيص", "Culture · AST · MIC", dataset.entries.count { it.kind == "culture" }, Icons.Default.Science, HepaticRose),
            CategorySpec(ContentFilter.QUICK, "دليل الجناح السريع", "Ward pocket guides", dataset.entries.count { it.quick || it.kind == "quick" }, Icons.Default.Speed, DoseAmber),
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(
                                Icons.Default.Medication,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp).size(28.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            LtrText(
                                "Clinical Antibiotic Encyclopedia",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text("موسوعة المضادات السريرية 2026", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onAbout) { Icon(Icons.Default.Info, "المصادر") }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("ابحث عن دواء، جرثومة، موقع عدوى أو نسيج", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        placeholder = { LtrText("e.g. meropenem, MRSA, CNS, pneumonia") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, "مسح")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(query) }),
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = { onSearch(query) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Default.Search, null)
                        Spacer(Modifier.width(8.dp))
                        Text("بحث شامل")
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill("${dataset.drugs.size}", "دواء", Modifier.weight(1f))
                StatPill("${dataset.entries.size}", "سجل", Modifier.weight(1f))
                StatPill("${dataset.sources.sumOf { it.pages }}", "صفحة مصدر", Modifier.weight(1f))
            }
        }
        item {
            Text(
                "استكشف الموسوعة",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(610.dp).padding(horizontal = 14.dp),
                userScrollEnabled = false,
                contentPadding = PaddingValues(6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(categories) { category ->
                    CategoryCard(category = category, onClick = { onCategory(category.filter) })
                }
            }
        }
        item {
            SafetyBanner(dataset.safetyNotice, Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(vertical = 11.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LtrText(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun CategoryCard(category: CategorySpec, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(188.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = RoundedCornerShape(14.dp), color = category.tint.copy(alpha = 0.15f)) {
                Icon(category.icon, null, tint = category.tint, modifier = Modifier.padding(10.dp).size(26.dp))
            }
            Column {
                Text(category.title, fontWeight = FontWeight.Bold, maxLines = 2)
                LtrText(category.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                LtrText("${category.count} entries", color = category.tint, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    viewModel: EncyclopediaViewModel,
    onOpenDrug: (String) -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("البحث السريري", fontWeight = FontWeight.Bold)
                    LtrText("Search every source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text("دواء، بكتيريا، عدوى، عضو…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                AnimatedVisibility(query.isNotBlank()) {
                    IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Close, "مسح") }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ContentFilter.entries) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { viewModel.setFilter(item) },
                    label = { Text(item.arabicLabel) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${results.size} نتيجة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            LtrText(filter.englishLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        if (results.isEmpty()) {
            EmptyState("لا توجد نتيجة مطابقة", "جرّب الاسم العلمي، الاختصار أو غيّر نوع البحث.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(results, key = SearchItem::stableId) { item ->
                    SearchResultCard(
                        item = item,
                        bookmarked = item.stableId in bookmarks,
                        onBookmark = { viewModel.toggleBookmark(item.stableId) },
                        onClick = {
                            when (item) {
                                is SearchItem.Drug -> onOpenDrug(item.value.id)
                                is SearchItem.Entry -> onOpenEntry(item.value.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchItem,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onClick: () -> Unit,
) {
    val tint = kindColor(item.kind)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    KindBadge(item.kind, tint)
                    Spacer(Modifier.height(8.dp))
                    LtrText(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (item.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        LtrText(
                            item.subtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                        )
                    }
                }
                IconButton(onClick = onBookmark) {
                    Icon(
                        if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        "حفظ",
                        tint = if (bookmarked) DoseAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.preview.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                LtrText(
                    item.preview.replace('\n', ' '),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedScreen(
    viewModel: EncyclopediaViewModel,
    onOpenDrug: (String) -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val items = remember(bookmarks) { viewModel.bookmarkedItems() }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("المحفوظات", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        if (items.isEmpty()) {
            EmptyState("لا توجد عناصر محفوظة", "اضغط علامة الحفظ على أي دواء أو جدول للعودة إليه سريعًا.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(items, key = SearchItem::stableId) { item ->
                    SearchResultCard(
                        item = item,
                        bookmarked = true,
                        onBookmark = { viewModel.toggleBookmark(item.stableId) },
                        onClick = {
                            when (item) {
                                is SearchItem.Drug -> onOpenDrug(item.value.id)
                                is SearchItem.Entry -> onOpenEntry(item.value.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrugDetailScreen(
    drug: DrugRecord,
    related: List<ReferenceEntry>,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بطاقة الدواء", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                actions = {
                    IconButton(onClick = onBookmark) {
                        Icon(
                            if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            "حفظ",
                            tint = if (bookmarked) DoseAmber else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        LtrText(drug.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(6.dp))
                        LtrText(drug.family, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                    }
                }
            }
            item { ClinicalSection("الطيف والاستعمال السريري", "Spectrum & clinical use", drug.spectrumUse, ClinicalTeal, Icons.Default.Biotech) }
            item { ClinicalSection("جرعة البالغين", "Adult dose", drug.adultDose, DoseAmber, Icons.Default.Medication) }
            item { ClinicalSection("تعديل الكلى والغسيل", "Renal adjustment · HD · CRRT · RRT", drug.renalRrt, RenalBlue, Icons.Default.Science) }
            item {
                ClinicalSection(
                    "اعتبارات الكبد",
                    "Hepatic adjustment / liver-related notes",
                    drug.hepaticAdjustment.ifBlank {
                        "No specific hepatic adjustment is stated for this drug in the source tables. Verify the current product label in clinically significant hepatic disease."
                    },
                    HepaticRose,
                    Icons.Default.LocalHospital,
                )
            }
            item { ClinicalSection("الإعطاء والهدف الدوائي", "Administration & PK/PD", drug.administrationPkPd, CultureViolet, Icons.Default.Speed) }
            item {
                ClinicalSection(
                    "الانتشار والاختراق النسيجي",
                    "Distribution · penetration · where it works",
                    drug.distributionSummary.ifBlank {
                        "No separate drug-specific distribution row was identified. Use the spectrum/use and related source entries below for site-specific evidence."
                    },
                    RenalBlue,
                    Icons.Default.Public,
                )
            }
            item { ClinicalSection("السمية والمراقبة", "Toxicity & monitoring", drug.toxicityMonitoring, HepaticRose, Icons.Default.Info) }
            if (related.isNotEmpty()) {
                item {
                    Column(Modifier.padding(top = 8.dp)) {
                        Text("الأدلة المرتبطة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        LtrText("Organisms, infection sites, spectrum and distribution", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(related.take(18), key = ReferenceEntry::id) { entry ->
                    EvidenceRow(entry = entry, onClick = { onOpenEntry(entry.id) })
                }
            }
            item {
                SourceLine(
                    title = "Focused & Concise Antibiotic Reference 2026",
                    page = drug.sourceRefs.firstOrNull()?.page ?: 0,
                )
            }
        }
    }
}

@Composable
private fun ClinicalSection(
    arabicTitle: String,
    englishTitle: String,
    content: String,
    tint: Color,
    icon: ImageVector,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.32f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = tint.copy(alpha = 0.14f)) {
                    Icon(icon, null, tint = tint, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(arabicTitle, fontWeight = FontWeight.Bold)
                    LtrText(englishTitle, color = tint, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            LtrText(content, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun EvidenceRow(entry: ReferenceEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = kindColor(entry.kind).copy(alpha = 0.15f)) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = kindColor(entry.kind), modifier = Modifier.padding(8.dp).size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                LtrText(entry.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                LtrText("${kindEnglish(entry.kind)} · page ${entry.page}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetailScreen(
    entry: ReferenceEntry,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kindArabic(entry.kind), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                actions = {
                    IconButton(onClick = onBookmark) {
                        Icon(
                            if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            "حفظ",
                            tint = if (bookmarked) DoseAmber else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item {
                KindBadge(entry.kind, kindColor(entry.kind))
                Spacer(Modifier.height(10.dp))
                LtrText(entry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                if (entry.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    LtrText(entry.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (entry.fields.isNotEmpty()) {
                items(entry.fields) { field ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            LtrText(field.label, color = kindColor(entry.kind), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(7.dp))
                            LtrText(field.value, lineHeight = 22.sp)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        LtrText(entry.text, modifier = Modifier.padding(17.dp), lineHeight = 23.sp)
                    }
                }
            }
            item { SourceLine(entry.sourceTitle, entry.page) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourcesScreen(dataset: EncyclopediaDataset, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المصادر وسلامة المحتوى", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LtrText(dataset.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                LtrText(dataset.sourceEdition, color = MaterialTheme.colorScheme.primary)
            }
            item { SafetyBanner(dataset.safetyNotice) }
            item {
                Text("الملفات المعتمدة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            items(dataset.sources, key = { it.id }) { source ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        LtrText(source.title, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        LtrText(source.role, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(7.dp))
                        LtrText("${source.pages} pages · SHA-256 ${source.sha256.take(12)}…", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                LtrText(
                    "The 140-page combined master was intentionally excluded from indexing because its clinical volumes are already represented by the standalone editions, preventing duplicate results.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SafetyBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DoseAmber.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, DoseAmber.copy(alpha = 0.4f)),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = DoseAmber)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("تنبيه سريري", fontWeight = FontWeight.Bold, color = DoseAmber)
                Spacer(Modifier.height(4.dp))
                LtrText(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SourceLine(title: String, page: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text("المصدر", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            LtrText(title, style = MaterialTheme.typography.bodySmall)
            if (page > 0) LtrText("PDF page $page", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun KindBadge(kind: String, tint: Color) {
    Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.14f)) {
        LtrText(
            kindEnglish(kind),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )
    }
}

private fun kindEnglish(kind: String): String = when (kind) {
    "antibiotic" -> "ANTIBIOTIC"
    "infection" -> "INFECTION SITE"
    "bacteria" -> "BACTERIA"
    "distribution" -> "DISTRIBUTION"
    "culture" -> "CULTURE / DIAGNOSTICS"
    "quick" -> "WARD QUICK GUIDE"
    else -> "REFERENCE"
}

private fun kindArabic(kind: String): String = when (kind) {
    "antibiotic" -> "المضادات الحيوية"
    "infection" -> "موقع العدوى والعلاج"
    "bacteria" -> "البكتيريا والعلاج الموجّه"
    "distribution" -> "الانتشار والاختراق"
    "culture" -> "الزرع والتشخيص"
    "quick" -> "دليل الجناح"
    else -> "المرجع السريري"
}

@Composable
private fun kindColor(kind: String): Color = when (kind) {
    "antibiotic" -> ClinicalTeal
    "infection" -> DoseAmber
    "bacteria" -> CultureViolet
    "distribution" -> RenalBlue
    "culture" -> HepaticRose
    "quick" -> DoseAmber
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun LtrText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
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
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            maxLines = maxLines,
            style = style,
        )
    }
}
