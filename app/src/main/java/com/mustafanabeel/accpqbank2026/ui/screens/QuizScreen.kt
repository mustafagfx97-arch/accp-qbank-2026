package com.mustafanabeel.accpqbank2026.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.mustafanabeel.accpqbank2026.data.repository.SessionQuestionPlanner
import com.mustafanabeel.accpqbank2026.ui.theme.CorrectGreen
import com.mustafanabeel.accpqbank2026.ui.theme.CorrectGreenContainer
import com.mustafanabeel.accpqbank2026.ui.theme.DarkTealPrimaryContainer
import com.mustafanabeel.accpqbank2026.ui.theme.IncorrectRed
import com.mustafanabeel.accpqbank2026.ui.theme.IncorrectRedContainer
import com.mustafanabeel.accpqbank2026.ui.theme.TealOnPrimary
import com.mustafanabeel.accpqbank2026.ui.theme.TealPrimary
import com.mustafanabeel.accpqbank2026.ui.theme.WarningAmber
import com.mustafanabeel.accpqbank2026.ui.viewmodel.QBankViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizScreen(
    viewModel: QBankViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResults: () -> Unit
) {
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    var showExitDialog by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showQuestionPalette by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (quizState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealPrimary)
        }
        return
    }

    if (quizState.questions.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("جلسة الاختبار") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "لا توجد أسئلة مطابقة للخيارات المحددة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يرجى تغيير عوامل التصفية أو اختيار فصول أخرى للبدء.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) {
                        Text("العودة للإعداد")
                    }
                }
            }
        }
        return
    }

    val currentQuestion = quizState.questions[quizState.currentIndex]
    val isBookmarked = bookmarks.any { it.questionId == currentQuestion.id }
    val isExamMode = quizState.config.mode == "exam"
    val isInstantSubmitted = quizState.submittedInstant.contains(currentQuestion.id)
    val selectedOption = quizState.selectedAnswers[currentQuestion.id]
    val isFlagged = quizState.flaggedQuestionIds.contains(currentQuestion.id)
    val sharedCaseQuestions = remember(quizState.questions, currentQuestion.id) {
        SessionQuestionPlanner.sharedCaseQuestions(quizState.questions, currentQuestion)
    }
    val sharedCasePosition = sharedCaseQuestions
        .indexOfFirst { it.id == currentQuestion.id }
        .coerceAtLeast(0) + 1

    // Formatted time mm:ss
    val minutes = quizState.elapsedSeconds / 60
    val seconds = quizState.elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "سؤال ${quizState.currentIndex + 1} من ${quizState.questions.size}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق الاختبار",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (isExamMode) {
                        IconButton(onClick = { viewModel.toggleFlag(currentQuestion.id) }) {
                            Icon(
                                imageVector = if (isFlagged) Icons.Filled.Flag else Icons.Outlined.Flag,
                                contentDescription = "تحديد للمراجعة",
                                tint = if (isFlagged) WarningAmber else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { showQuestionPalette = true }) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "شبكة الأسئلة",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleBookmark(currentQuestion.id) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "إضافة للمفضلة",
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    OutlinedButton(
                        onClick = { viewModel.goToPrevious() },
                        enabled = quizState.currentIndex > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("السابق")
                    }

                    // Finish or Next
                    if (quizState.currentIndex == quizState.questions.size - 1) {
                        Button(
                            onClick = {
                                if (isExamMode) {
                                    showSubmitDialog = true
                                } else {
                                    viewModel.finishSession(onNavigateToResults)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = TealOnPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إنهاء الجلسة", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.goToNext() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealPrimary,
                                contentColor = TealOnPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("التالي")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Linear Progress Bar
            val progress = (quizState.currentIndex + 1).toFloat() / quizState.questions.size.toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = TealPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Scrollable question body
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Header Badges LTR
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatusBadge(
                                text = currentQuestion.chapterId.replace("-", " ").uppercase(),
                                containerColor = TealPrimary.copy(alpha = 0.12f),
                                contentColor = TealPrimary
                            )
                            StatusBadge(
                                text = if (currentQuestion.type == "case") "PATIENT CASE" else "ASSESSMENT",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (sharedCaseQuestions.size > 1) {
                                StatusBadge(
                                    text = "CASE $sharedCasePosition/${sharedCaseQuestions.size}",
                                    containerColor = TealPrimary.copy(alpha = 0.12f),
                                    contentColor = TealPrimary
                                )
                            }
                            if (currentQuestion.questionPage != null) {
                                StatusBadge(
                                    text = "P. ${currentQuestion.questionPage}",
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Case Vignette Card (if present)
                if (currentQuestion.caseContext.isNotBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (sharedCaseQuestions.size > 1) {
                                            "CASE VIGNETTE • QUESTION $sharedCasePosition OF ${sharedCaseQuestions.size}"
                                        } else {
                                            "CASE VIGNETTE"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = TealPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = currentQuestion.caseContext,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 22.sp,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Question Stem
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = currentQuestion.stem,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 24.sp,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Options A, B, C, D
                val options = listOf(
                    "A" to currentQuestion.optionA,
                    "B" to currentQuestion.optionB,
                    "C" to currentQuestion.optionC,
                    "D" to currentQuestion.optionD
                ).filter { it.second.isNotBlank() }

                items(options.size) { idx ->
                    val (key, text) = options[idx]
                    OptionCard(
                        key = key,
                        text = text,
                        isSelected = selectedOption == key,
                        isInstantMode = !isExamMode,
                        isInstantSubmitted = isInstantSubmitted,
                        isCorrectChoice = key.equals(currentQuestion.answer, ignoreCase = true),
                        onClick = {
                            viewModel.selectOption(currentQuestion, key)
                        }
                    )
                }

                // Authoritative Explanation Card (Instant Feedback Mode)
                if (!isExamMode && isInstantSubmitted) {
                    item {
                        val isUserCorrect = selectedOption.equals(currentQuestion.answer, ignoreCase = true)
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUserCorrect) CorrectGreenContainer.copy(alpha = 0.35f)
                                    else IncorrectRedContainer.copy(alpha = 0.35f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isUserCorrect) CorrectGreen else IncorrectRed
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isUserCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (isUserCorrect) CorrectGreen else IncorrectRed,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isUserCorrect) "إجابة صحيحة (Correct)" else "إجابة خاطئة (Incorrect)",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (isUserCorrect) CorrectGreen else IncorrectRed
                                            )
                                        }

                                        StatusBadge(
                                            text = "Answer: ${currentQuestion.answer}",
                                            containerColor = if (isUserCorrect) CorrectGreen else IncorrectRed,
                                            contentColor = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Citations LTR
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Source Citation: Question p. ${currentQuestion.questionPage ?: "N/A"}, Answer p. ${currentQuestion.answerPage ?: "N/A"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Full Rationale
                                        Text(
                                            text = currentQuestion.explanation,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                lineHeight = 22.sp,
                                                fontSize = 14.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
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

    // Question Palette Bottom Sheet (Exam Mode)
    if (showQuestionPalette) {
        ModalBottomSheet(
            onDismissRequest = { showQuestionPalette = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "جدول الأسئلة السريع",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quizState.questions.forEachIndexed { index, q ->
                        val isAnswered = quizState.selectedAnswers.containsKey(q.id)
                        val isCurr = index == quizState.currentIndex
                        val isFlag = quizState.flaggedQuestionIds.contains(q.id)

                        val bgColor = when {
                            isCurr -> TealPrimary
                            isFlag -> WarningAmber.copy(alpha = 0.3f)
                            isAnswered -> TealPrimary.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val textColor = when {
                            isCurr -> Color.White
                            isFlag -> WarningAmber
                            isAnswered -> TealPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable {
                                    viewModel.jumpToQuestion(index)
                                    showQuestionPalette = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Exit Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("إنهاء الجلسة؟") },
            text = { Text("هل أنت متأكد من مغادرة هذه الجلسة؟ سيتم حفظ تقدمك الحالي.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.finishSession(onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("خروج وحفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Submit Exam Dialog
    if (showSubmitDialog) {
        val answeredCount = quizState.selectedAnswers.size
        val totalCount = quizState.questions.size
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("تسليم الامتحان؟") },
            text = {
                Text("لقد قمت بالإجابة على $answeredCount من أصل $totalCount سؤالاً. هل تريد تأكيد التسليم وعرض تقرير النتيجة الكامل؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        viewModel.finishSession(onNavigateToResults)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("تسليم الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("متابعة الحل")
                }
            }
        )
    }
}

@Composable
fun OptionCard(
    key: String,
    text: String,
    isSelected: Boolean,
    isInstantMode: Boolean,
    isInstantSubmitted: Boolean,
    isCorrectChoice: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor) = when {
        isInstantMode && isInstantSubmitted -> {
            if (isCorrectChoice) {
                Triple(CorrectGreenContainer.copy(alpha = 0.45f), CorrectGreen, MaterialTheme.colorScheme.onSurface)
            } else if (isSelected) {
                Triple(IncorrectRedContainer.copy(alpha = 0.45f), IncorrectRed, MaterialTheme.colorScheme.onSurface)
            } else {
                Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        isSelected -> {
            Triple(TealPrimary.copy(alpha = 0.1f), TealPrimary, MaterialTheme.colorScheme.onSurface)
        }
        else -> {
            Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), MaterialTheme.colorScheme.onSurface)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(width = if (isSelected || (isInstantMode && isInstantSubmitted && isCorrectChoice)) 2.dp else 1.dp, color = borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !(isInstantMode && isInstantSubmitted), onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Key circle (A, B, C, D)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isInstantMode && isInstantSubmitted && isCorrectChoice) CorrectGreen
                        else if (isInstantMode && isInstantSubmitted && isSelected) IncorrectRed
                        else if (isSelected) TealPrimary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected || (isInstantMode && isInstantSubmitted && isCorrectChoice)) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Option English LTR Text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
