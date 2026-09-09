package com.mustafanabeel.accpqbank2026.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mustafanabeel.accpqbank2026.data.local.entity.QuestionEntity
import com.mustafanabeel.accpqbank2026.ui.theme.CorrectGreen
import com.mustafanabeel.accpqbank2026.ui.theme.CorrectGreenContainer
import com.mustafanabeel.accpqbank2026.ui.theme.IncorrectRed
import com.mustafanabeel.accpqbank2026.ui.theme.IncorrectRedContainer
import com.mustafanabeel.accpqbank2026.ui.theme.TealOnPrimary
import com.mustafanabeel.accpqbank2026.ui.theme.TealPrimary
import com.mustafanabeel.accpqbank2026.ui.theme.WarningAmber
import com.mustafanabeel.accpqbank2026.ui.viewmodel.QBankViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: QBankViewModel,
    onNavigateHome: () -> Unit,
    onRetryIncorrect: () -> Unit,
    onNewQuiz: () -> Unit
) {
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val total = quizState.questions.size
    val correct = quizState.questions.count { q ->
        val selected = quizState.selectedAnswers[q.id]
        selected != null && selected.equals(q.answer, ignoreCase = true)
    }
    val percentage = if (total > 0) (correct * 100) / total else 0

    val minutes = quizState.elapsedSeconds / 60
    val seconds = quizState.elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    var reviewFilter by remember { mutableStateOf("all") } // "all", "incorrect", "correct"

    val displayedQuestions = remember(quizState.questions, quizState.selectedAnswers, reviewFilter) {
        when (reviewFilter) {
            "correct" -> quizState.questions.filter { q ->
                val sel = quizState.selectedAnswers[q.id]
                sel != null && sel.equals(q.answer, ignoreCase = true)
            }
            "incorrect" -> quizState.questions.filter { q ->
                val sel = quizState.selectedAnswers[q.id]
                sel == null || !sel.equals(q.answer, ignoreCase = true)
            }
            else -> quizState.questions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تقرير نتيجة الاختبار",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرئيسية",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary)
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
                // Performance Hero Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular Gauge
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    if (percentage >= 70) CorrectGreen.copy(alpha = 0.12f)
                                    else WarningAmber.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ),
                                    color = if (percentage >= 70) CorrectGreen else WarningAmber
                                )
                                Text(
                                    text = "الدرجة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$correct",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CorrectGreen
                                    )
                                )
                                Text("صحيحة", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${total - correct}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = IncorrectRed
                                    )
                                )
                                Text("خاطئة", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$total",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                )
                                Text("المجموع", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text("الوقت", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (total - correct > 0) {
                                Button(
                                    onClick = onRetryIncorrect,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TealPrimary,
                                        contentColor = TealOnPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Replay, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة الخاطئة")
                                }
                            }
                            OutlinedButton(
                                onClick = onNewQuiz,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("جلسة جديدة")
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مراجعة الأسئلة:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    FilterChip(
                        selected = reviewFilter == "all",
                        onClick = { reviewFilter = "all" },
                        label = { Text("الكل ($total)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = TealOnPrimary
                        )
                    )
                    FilterChip(
                        selected = reviewFilter == "incorrect",
                        onClick = { reviewFilter = "incorrect" },
                        label = { Text("الخاطئة (${total - correct})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncorrectRed,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = reviewFilter == "correct",
                        onClick = { reviewFilter = "correct" },
                        label = { Text("الصحيحة ($correct)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CorrectGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Questions list
            items(displayedQuestions) { question ->
                val selected = quizState.selectedAnswers[question.id]
                val isCorrect = selected != null && selected.equals(question.answer, ignoreCase = true)
                QuestionReviewCard(
                    question = question,
                    selectedOption = selected,
                    isCorrect = isCorrect
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun QuestionReviewCard(
    question: QuestionEntity,
    selectedOption: String?,
    isCorrect: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCorrect) CorrectGreen.copy(alpha = 0.5f) else IncorrectRed.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isCorrect) CorrectGreen else IncorrectRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCorrect) "إجابة صحيحة" else "إجابة خاطئة",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) CorrectGreen else IncorrectRed
                        )
                    )
                }

                StatusBadge(
                    text = "Your: ${selectedOption ?: "None"} • Answer: ${question.answer}",
                    containerColor = if (isCorrect) CorrectGreenContainer else IncorrectRedContainer,
                    contentColor = if (isCorrect) CorrectGreen else IncorrectRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stem LTR
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = question.stem,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggle explanation button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "إخفاء التفسير الطبي" else "عرض التفسير الطبي الموثق",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TealPrimary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "Source Citation: Question p. ${question.questionPage ?: "N/A"}, Answer p. ${question.answerPage ?: "N/A"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TealPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
