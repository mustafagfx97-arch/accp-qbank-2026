package com.mustafanabeel.accpqbank2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mustafanabeel.accpqbank2026.ui.theme.CorrectGreen
import com.mustafanabeel.accpqbank2026.ui.theme.DarkTealPrimaryContainer
import com.mustafanabeel.accpqbank2026.ui.theme.TealOnPrimary
import com.mustafanabeel.accpqbank2026.ui.theme.TealPrimary
import com.mustafanabeel.accpqbank2026.ui.theme.WarningAmber
import com.mustafanabeel.accpqbank2026.ui.viewmodel.QBankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: QBankViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToBookmarks: () -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ACCP QBank 2026",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "بنك أسئلة البورد الأمريكي للصيدلة السريرية",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reloadDatabaseFromAssets() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث من الملف",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToBookmarks) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "المفضلة",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Quick Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "الأسئلة المتاحة",
                        value = "${stats.totalUsableQuestions}",
                        subtitle = "من 533 مصدرياً",
                        color = TealPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "تم حلها",
                        value = "${stats.totalAttempted}",
                        subtitle = "${stats.totalCorrect} صحيحة",
                        color = CorrectGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "نسبة الدقة",
                        value = "${stats.accuracyPercentage}%",
                        subtitle = "${stats.bookmarkedCount} في المفضلة",
                        color = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Clinical Notice Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنبيه المحتوى المصدري (Source Notice)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TealPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "تم استبعاد حالتين سريريتين (6 و 7) من فصل Chronic Care Cardiology لغيابهما من المستند المصدري الأصلي للـ ACCP. الأسئلة المتاحة للاختبار: 531 سؤالاً موثقاً بالكامل.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Actions Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = TealPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "جلسة اختبار سريعة",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealOnPrimary
                            )
                        )
                        Text(
                            text = "10 أسئلة عشوائية من جميع الفصول مع تصحيح وتفسير فوري",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TealOnPrimary.copy(alpha = 0.9f)
                            ),
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.startQuickSession(onNavigateToQuiz)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = TealPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "ابدأ الآن", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateToSetup,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    contentColor = TealOnPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "تخصيص جلسة")
                            }
                        }
                    }
                }
            }

            // Chapters Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فصول بنك الأسئلة (${chapters.size} فصلاً)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "اختر فصلاً لبدء دراسته",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chapters List
            itemsIndexed(chapters) { index, chapter ->
                val chStat = stats.chapterStats[chapter.id]
                val attempted = chStat?.attempted ?: 0
                val total = chapter.assessmentCount + chapter.caseCount
                val progress = if (total > 0) attempted.toFloat() / total.toFloat() else 0f
                val accuracy = chStat?.accuracy ?: 0

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.startChapterSession(chapter.id, "instant", onNavigateToQuiz)
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Chapter Number Circle
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // English Chapter Title LTR
                            Column(modifier = Modifier.weight(1f)) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${chapter.assessmentCount} تقييم • ${chapter.caseCount} حالات سريرية",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.startChapterSession(chapter.id, "instant", onNavigateToQuiz)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "بدء الاختبار",
                                    tint = TealPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = TealPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تم حل: $attempted من $total",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (attempted > 0) {
                                Text(
                                    text = "الدقة: $accuracy%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (accuracy >= 70) CorrectGreen else WarningAmber
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
