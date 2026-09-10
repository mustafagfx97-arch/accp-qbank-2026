package com.mustafanabeel.accpqbank2026.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mustafanabeel.accpqbank2026.data.repository.SessionConfig
import com.mustafanabeel.accpqbank2026.data.repository.SessionQuestionOrder
import com.mustafanabeel.accpqbank2026.ui.theme.TealOnPrimary
import com.mustafanabeel.accpqbank2026.ui.theme.TealPrimary
import com.mustafanabeel.accpqbank2026.ui.viewmodel.QBankViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionSetupScreen(
    viewModel: QBankViewModel,
    onBack: () -> Unit,
    onStartQuiz: () -> Unit
) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableStateOf("instant") }
    var selectedType by remember { mutableStateOf("all") }
    var selectedStatus by remember { mutableStateOf("all") }
    var selectedOrder by remember { mutableStateOf(SessionQuestionOrder.BOOK) }
    var selectedChapterIds by remember { mutableStateOf(setOf<String>()) }
    var questionCount by remember { mutableIntStateOf(20) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إعداد جلسة مخصصة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (questionCount == 0) "كل الأسئلة المطابقة" else "$questionCount سؤالاً",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TealPrimary
                        )
                        Text(
                            text = if (selectedOrder == SessionQuestionOrder.BOOK) {
                                "بالترتيب الأصلي للكتاب"
                            } else {
                                "عشوائي مع تجميع الحالة"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val config = SessionConfig(
                                mode = selectedMode,
                                questionType = selectedType,
                                selectedChapterIds = selectedChapterIds,
                                statusFilter = selectedStatus,
                                questionOrder = selectedOrder,
                                questionCount = questionCount
                            )
                            viewModel.startSession(config, onStartQuiz)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            contentColor = TealOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "ابدأ الجلسة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. نمط الاختبار (Study Mode)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Instant Mode
                    ModeCard(
                        title = "مراجعة فورية",
                        subtitle = "عرض الإجابة والتفسير الطبي مباشرة بعد كل اختيار",
                        icon = Icons.Default.FactCheck,
                        isSelected = selectedMode == "instant",
                        onClick = { selectedMode = "instant" },
                        modifier = Modifier.weight(1f)
                    )
                    // Exam Mode
                    ModeCard(
                        title = "نمط الامتحان",
                        subtitle = "إجراء اختبار كامل بدون تلميحات وتقييم نهائي",
                        icon = Icons.Default.HourglassBottom,
                        isSelected = selectedMode == "exam",
                        onClick = { selectedMode = "exam" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "2. نوع الأسئلة (Question Type)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == "all",
                        onClick = { selectedType = "all" },
                        label = { Text("جميع الأنواع") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedType == "assessment",
                        onClick = { selectedType = "assessment" },
                        label = { Text("أسئلة تقييم (Assessment)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedType == "case",
                        onClick = { selectedType = "case" },
                        label = { Text("حالات سريرية (Cases)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                }
            }

            item {
                Text(
                    text = "3. تصفية الأسئلة السابقة (Status Filter)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == "all",
                        onClick = { selectedStatus = "all" },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedStatus == "unanswered",
                        onClick = { selectedStatus = "unanswered" },
                        label = { Text("غير المجابة فقط") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedStatus == "incorrect",
                        onClick = { selectedStatus = "incorrect" },
                        label = { Text("الخاطئة سابقاً") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedStatus == "bookmarked",
                        onClick = { selectedStatus = "bookmarked" },
                        label = { Text("المفضلة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                }
            }

            item {
                Text(
                    text = "4. ترتيب الأسئلة (Question Order)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedOrder == SessionQuestionOrder.BOOK,
                        onClick = { selectedOrder = SessionQuestionOrder.BOOK },
                        label = { Text("ترتيب الكتاب") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                    FilterChip(
                        selected = selectedOrder == SessionQuestionOrder.RANDOM,
                        onClick = { selectedOrder = SessionQuestionOrder.RANDOM },
                        label = { Text("عشوائي — الحالة تبقى معاً") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أسئلة Patient Case الواحدة ستظهر متجاورة ومرتبة. قد يزيد العدد المحدد قليلاً لإكمال الحالة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text(
                    text = "5. عدد الأسئلة (Question Count)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 50, 0).forEach { count ->
                        val label = if (count == 0) "الكل" else "$count"
                        FilterChip(
                            selected = questionCount == count,
                            onClick = { questionCount = count },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = TealOnPrimary
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "6. الفصول المستهدفة (${selectedChapterIds.size}/${chapters.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { selectedChapterIds = chapters.map { it.id }.toSet() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تحديد الكل", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { selectedChapterIds = emptySet() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("مسح", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chapters.forEach { chapter ->
                        val isSelected = selectedChapterIds.contains(chapter.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = selectedChapterIds.toMutableSet()
                                if (isSelected) next.remove(chapter.id) else next.add(chapter.id)
                                selectedChapterIds = next
                            },
                            label = {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(chapter.title, fontSize = 12.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = TealOnPrimary
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TealPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = TealPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
