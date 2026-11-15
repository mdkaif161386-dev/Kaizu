package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AiQuizRepository
import com.example.data.FirestoreRepository
import com.example.focus.FocusPreferences
import com.example.focus.StudyFocusService
import com.example.model.ExamCatalog
import com.example.model.UserProfile
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChaptersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.FocusGoalScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MegaQuizSetupScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuizPlayScreen
import com.example.ui.screens.QuizResultScreen
import com.example.ui.screens.QuizSetupScreen
import com.example.ui.screens.SavedQuestionsScreen
import com.example.ui.screens.StudyChatScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ForgeGold
import com.example.ui.theme.RankForgeTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.ChatPeerCandidate
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.FlashcardViewModel
import com.example.ui.viewmodel.FocusViewModel
import com.example.ui.viewmodel.LeaderboardUiState
import com.example.ui.viewmodel.LeaderboardViewModel
import com.example.ui.viewmodel.ProfileUiState
import com.example.ui.viewmodel.QuizSessionState
import com.example.ui.viewmodel.QuizViewModel
import com.example.util.FocusCycleCalculator
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_SUBJECTS = "subjects"
const val ROUTE_CHAPTERS = "chapters"
const val ROUTE_QUIZ_SETUP = "quiz_setup"
const val ROUTE_FLASHCARDS = "flashcards"
const val ROUTE_MEGA_QUIZ = "mega_quiz"
const val ROUTE_FOCUS_GOAL = "focus_goal"
const val ROUTE_STUDY_CHAT = "study_chat"
const val ROUTE_SAVED_QUESTIONS = "saved_questions"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_LEADERBOARD = "leaderboard"
const val ROUTE_PROFILE = "profile"
const val ROUTE_EDIT_PROFILE = "edit_profile"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val mainTabs = listOf(
    NavTab(ROUTE_SUBJECTS, "Arena", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_subjects"),
    NavTab(ROUTE_FOCUS_GOAL, "Focus", Icons.Filled.Shield, Icons.Outlined.Shield, "nav_focus"),
    NavTab(ROUTE_STUDY_CHAT, "Chat", Icons.Filled.Forum, Icons.Outlined.Forum, "nav_chat"),
    NavTab(ROUTE_SAVED_QUESTIONS, "Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_saved_questions"),
    NavTab(ROUTE_LEADERBOARD, "Ranks", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_leaderboard")
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {

    private val blockedAppLabelState = MutableStateFlow<String?>(null)
    private val openFocusTabState = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)
        handleFocusIntent(intent)

        setContent {
            val blockedAppLabel by blockedAppLabelState.collectAsStateWithLifecycle()
            val openFocusRequested by openFocusTabState.collectAsStateWithLifecycle()
            RankForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        blockedAppLabel = blockedAppLabel,
                        onDismissBlockedDialog = { blockedAppLabelState.value = null },
                        openFocusRequested = openFocusRequested,
                        onHandledOpenFocus = { openFocusTabState.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFocusIntent(intent)
    }

    private fun handleFocusIntent(intent: Intent?) {
        val blocked = intent?.getStringExtra(StudyFocusService.EXTRA_BLOCKED_APP_LABEL)
        if (!blocked.isNullOrBlank()) {
            blockedAppLabelState.value = blocked
            openFocusTabState.value = true
        } else if (intent?.getBooleanExtra(StudyFocusService.EXTRA_OPEN_FOCUS_TAB, false) == true) {
            openFocusTabState.value = true
        }
    }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    blockedAppLabel: String? = null,
    onDismissBlockedDialog: () -> Unit = {},
    openFocusRequested: Boolean = false,
    onHandledOpenFocus: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives transition */ })
    } else {
        key(user.uid) {
            AuthenticatedRoot(
                currentUserId = user.uid,
                blockedAppLabel = blockedAppLabel,
                onDismissBlockedDialog = onDismissBlockedDialog,
                openFocusRequested = openFocusRequested,
                onHandledOpenFocus = onHandledOpenFocus
            )
        }
    }
}

@Composable
private fun AuthenticatedRoot(
    currentUserId: String,
    blockedAppLabel: String?,
    onDismissBlockedDialog: () -> Unit,
    openFocusRequested: Boolean,
    onHandledOpenFocus: () -> Unit
) {
    val appViewModel: AppViewModel = viewModel(
        key = "app_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AppViewModel(
                    firestoreRepo = FirestoreRepository(db),
                    aiRepo = AiQuizRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val quizViewModel: QuizViewModel = viewModel(
        key = "quiz_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                QuizViewModel(
                    aiRepo = AiQuizRepository(),
                    firestoreRepo = FirestoreRepository(db)
                )
            }
        }
    )

    val profileState by appViewModel.profileState.collectAsStateWithLifecycle()
    val onboardingSaving by appViewModel.onboardingSaving.collectAsStateWithLifecycle()
    val onboardingError by appViewModel.onboardingError.collectAsStateWithLifecycle()

    when (val state = profileState) {
        is ProfileUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ForgeGold)
            }
        }

        is ProfileUiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        is ProfileUiState.NeedsOnboarding -> {
            OnboardingScreen(
                isEditing = false,
                isSaving = onboardingSaving,
                errorMessage = onboardingError,
                onSubmit = { name, dob, phone, examId ->
                    appViewModel.saveProfile(
                        fullName = name,
                        dob = dob,
                        phoneNumber = phone,
                        selectedExamId = examId,
                        isNewProfile = true
                    )
                }
            )
        }

        is ProfileUiState.Ready -> {
            MainAppScaffold(
                currentUserId = currentUserId,
                profile = state.profile,
                appViewModel = appViewModel,
                quizViewModel = quizViewModel,
                blockedAppLabel = blockedAppLabel,
                onDismissBlockedDialog = onDismissBlockedDialog,
                openFocusRequested = openFocusRequested,
                onHandledOpenFocus = onHandledOpenFocus
            )
        }
    }
}

@Composable
private fun MainAppScaffold(
    currentUserId: String,
    profile: UserProfile,
    appViewModel: AppViewModel,
    quizViewModel: QuizViewModel,
    blockedAppLabel: String?,
    onDismissBlockedDialog: () -> Unit,
    openFocusRequested: Boolean,
    onHandledOpenFocus: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val quizSessionState by quizViewModel.sessionState.collectAsStateWithLifecycle()

    val leaderboardViewModel: LeaderboardViewModel = viewModel(
        key = "lb_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LeaderboardViewModel(
                    firestoreRepo = FirestoreRepository(db),
                    initialExamId = profile.selectedExamId
                )
            }
        }
    )

    val focusViewModel: FocusViewModel = viewModel(
        key = "focus_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FocusViewModel(
                    appContext = app.applicationContext,
                    firestoreRepo = FirestoreRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val chatViewModel: ChatViewModel = viewModel(
        key = "chat_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ChatViewModel(
                    firestoreRepo = FirestoreRepository(db),
                    aiRepo = AiQuizRepository(),
                    currentUserId = currentUserId,
                    initialExamId = profile.selectedExamId
                )
            }
        }
    )

    val flashcardViewModel: FlashcardViewModel = viewModel(
        key = "flashcards_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FlashcardViewModel(
                    aiRepo = AiQuizRepository(),
                    firestoreRepo = FirestoreRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val activeExam = remember(profile.selectedExamId) {
        ExamCatalog.getExamById(profile.selectedExamId)
    }

    LaunchedEffect(openFocusRequested) {
        if (openFocusRequested) {
            navController.navigate(ROUTE_FOCUS_GOAL) {
                popUpTo(ROUTE_SUBJECTS) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onHandledOpenFocus()
        }
    }

    if (blockedAppLabel != null) {
        val focusStatus by focusViewModel.runtimeStatus.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = onDismissBlockedDialog,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = CrimsonError
                )
            },
            title = {
                Text("$blockedAppLabel Disabled During Focus")
            },
            text = {
                Text(
                    "Your daily study goal still has ${FocusCycleCalculator.formatHoursMinutes(focusStatus.remainingGoalSec)} remaining.\n\n" +
                        "Social media apps automatically unlock for 5 minutes every hour (next break in ${FocusCycleCalculator.formatDurationHms(focusStatus.phaseRemainingSec)}) and stay unlocked once your daily goal is completed."
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissBlockedDialog) {
                    Text("Back to Study")
                }
            }
        )
    }

    // Active full-screen quiz session overlay (Generating / Playing / Submitting / Completed / Error)
    when (val qState = quizSessionState) {
        is QuizSessionState.Generating,
        is QuizSessionState.Playing,
        is QuizSessionState.Submitting,
        is QuizSessionState.Error -> {
            val savedQuestions by appViewModel.savedQuestions.collectAsStateWithLifecycle()
            val bookmarkedQuestionIds = remember(savedQuestions) {
                savedQuestions.map { it.id }.toSet()
            }
            val activeConfig = (qState as? QuizSessionState.Playing)?.config
            QuizPlayScreen(
                sessionState = qState,
                bookmarkedQuestionIds = bookmarkedQuestionIds,
                onToggleBookmark = { question ->
                    appViewModel.toggleQuestionBookmark(
                        examId = activeConfig?.exam?.id ?: activeExam.id,
                        examName = activeConfig?.exam?.name ?: activeExam.name,
                        subject = question.subject,
                        chapter = question.chapter,
                        difficulty = activeConfig?.difficulty?.id ?: "MODERATE",
                        question = question.question,
                        options = question.options,
                        correctIndex = question.correctIndex,
                        explanation = question.explanation
                    )
                },
                onSelectOption = { quizViewModel.selectOption(it) },
                onAdvanceNext = { quizViewModel.advanceToNextQuestion() },
                onFinishEarly = { quizViewModel.finishQuizEarly() },
                onRetryGeneration = {
                    val cfg = (qState as? QuizSessionState.Error)?.config
                    if (cfg != null) quizViewModel.startQuizGeneration(cfg, profile)
                },
                onCancelAndExit = { quizViewModel.resetSession() }
            )
            return
        }

        is QuizSessionState.Completed -> {
            val savedQuestions by appViewModel.savedQuestions.collectAsStateWithLifecycle()
            val bookmarkedQuestionIds = remember(savedQuestions) {
                savedQuestions.map { it.id }.toSet()
            }
            QuizResultScreen(
                attempt = qState.attempt,
                saveError = qState.saveError,
                bookmarkedQuestionIds = bookmarkedQuestionIds,
                onToggleBookmark = { review ->
                    appViewModel.toggleQuestionBookmark(
                        examId = qState.attempt.examId,
                        examName = qState.attempt.examName,
                        subject = review.subject,
                        chapter = review.chapter,
                        difficulty = qState.attempt.difficulty,
                        question = review.question,
                        options = review.options,
                        correctIndex = review.correctIndex,
                        explanation = review.explanation
                    )
                },
                onViewLeaderboard = {
                    leaderboardViewModel.selectExam(qState.attempt.examId)
                    quizViewModel.resetSession()
                    navController.navigate(ROUTE_LEADERBOARD) {
                        popUpTo(ROUTE_SUBJECTS) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onDone = {
                    quizViewModel.resetSession()
                    navController.navigate(ROUTE_SUBJECTS) {
                        popUpTo(ROUTE_SUBJECTS) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
            return
        }

        QuizSessionState.Idle -> Unit
    }

    val isTopLevelRoute = mainTabs.any { it.route == currentRoute }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && isTopLevelRoute) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(ROUTE_SUBJECTS) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && isTopLevelRoute) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(ROUTE_SUBJECTS) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_SUBJECTS
                    ) {
                        composable(ROUTE_SUBJECTS) {
                            val attempts by appViewModel.quizAttempts.collectAsStateWithLifecycle()
                            SubjectsScreen(
                                profile = profile,
                                attempts = attempts,
                                onSubjectClick = { subject ->
                                    navController.navigate("$ROUTE_CHAPTERS/${Uri.encode(subject.id)}")
                                },
                                onMegaQuizClick = {
                                    navController.navigate(ROUTE_MEGA_QUIZ) {
                                        popUpTo(ROUTE_SUBJECTS) { saveState = true }
                                        launchSingleTop = true
                                    }
                                },
                                onFlashcardsClick = {
                                    flashcardViewModel.backToBuilder()
                                    navController.navigate(ROUTE_FLASHCARDS)
                                },
                                onSwitchExam = { newExam ->
                                    appViewModel.switchExam(newExam)
                                    leaderboardViewModel.selectExam(newExam.id)
                                },
                                onStreakClick = {
                                    navController.navigate(ROUTE_DASHBOARD) {
                                        popUpTo(ROUTE_SUBJECTS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(ROUTE_PROFILE) {
                                        popUpTo(ROUTE_SUBJECTS) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_FOCUS_GOAL) {
                            val focusSnapshot by focusViewModel.snapshot.collectAsStateWithLifecycle()
                            val focusStatus by focusViewModel.runtimeStatus.collectAsStateWithLifecycle()
                            val socialApps by focusViewModel.socialApps.collectAsStateWithLifecycle()
                            val hasUsageAccess by focusViewModel.hasUsageAccess.collectAsStateWithLifecycle()
                            val focusStatusMessage by focusViewModel.statusMessage.collectAsStateWithLifecycle()
                            val studyTimeLeaderboardState by leaderboardViewModel.studyTimeLeaderboard.collectAsStateWithLifecycle()
                            val studyTimeEntries = (studyTimeLeaderboardState as? LeaderboardUiState.Success)?.entries.orEmpty()
                            val myTimeEntry = remember(studyTimeEntries, currentUserId) {
                                studyTimeEntries.firstOrNull { it.userId == currentUserId }
                            }
                            val (personalDaySec, personalWeekSec, personalMonthSec) = FocusViewModel.personalTimeBreakdown(
                                entry = myTimeEntry,
                                liveExtraSec = focusStatus.liveSessionElapsedSec
                            )
                            FocusGoalScreen(
                                profile = profile,
                                snapshot = focusSnapshot,
                                status = focusStatus,
                                socialApps = socialApps,
                                hasUsageAccess = hasUsageAccess,
                                personalDaySec = personalDaySec,
                                personalWeekSec = personalWeekSec,
                                personalMonthSec = personalMonthSec,
                                statusMessage = focusStatusMessage,
                                onRefreshPermissions = { focusViewModel.refreshPermissionsAndApps() },
                                onSetDailyGoal = { minutes -> focusViewModel.setDailyGoal(minutes) },
                                onClearDailyGoal = { focusViewModel.clearDailyGoal() },
                                onStartPhysicalStudy = { subject -> focusViewModel.startPhysicalStudy(subject) },
                                onStopPhysicalStudy = { focusViewModel.stopAndLogPhysicalStudy(profile) },
                                onToggleBlockedApp = { pkg -> focusViewModel.toggleBlockedPackage(pkg) },
                                onDismissStatusMessage = { focusViewModel.clearStatusMessage() }
                            )
                        }

                        composable(ROUTE_STUDY_CHAT) {
                            val chatExamId by chatViewModel.selectedExamId.collectAsStateWithLifecycle()
                            val chatActivePeer by chatViewModel.activePeer.collectAsStateWithLifecycle()
                            val chatExamMessages by chatViewModel.examMessages.collectAsStateWithLifecycle()
                            val chatDirectThreads by chatViewModel.directThreads.collectAsStateWithLifecycle()
                            val chatDirectMessages by chatViewModel.directMessages.collectAsStateWithLifecycle()
                            val chatModeration by chatViewModel.moderationStatus.collectAsStateWithLifecycle()
                            val chatSendOutcome by chatViewModel.sendOutcome.collectAsStateWithLifecycle()
                            val studyTimeLeaderboardState by leaderboardViewModel.studyTimeLeaderboard.collectAsStateWithLifecycle()
                            val examLeaderboardState by leaderboardViewModel.examLeaderboard.collectAsStateWithLifecycle()
                            val studyTimeEntries = (studyTimeLeaderboardState as? LeaderboardUiState.Success)?.entries.orEmpty()
                            val peerCandidates = remember(studyTimeEntries, examLeaderboardState, currentUserId) {
                                val fromTime = studyTimeEntries
                                    .filter { it.userId != currentUserId }
                                    .map { ChatPeerCandidate(it.userId, it.displayName, it.examName) }
                                val fromExam = (examLeaderboardState as? LeaderboardUiState.Success)?.entries.orEmpty()
                                    .filter { it.userId != currentUserId }
                                    .map { ChatPeerCandidate(it.userId, it.displayName, it.examName) }
                                (fromTime + fromExam).distinctBy { it.userId }
                            }
                            StudyChatScreen(
                                currentUserId = currentUserId,
                                profile = profile,
                                selectedExamId = chatExamId,
                                activePeer = chatActivePeer,
                                examMessages = chatExamMessages,
                                directThreads = chatDirectThreads,
                                directMessages = chatDirectMessages,
                                peerCandidates = peerCandidates,
                                moderationStatus = chatModeration,
                                sendOutcome = chatSendOutcome,
                                onSelectExamRoom = { chatViewModel.selectExamRoom(it) },
                                onOpenPeerChat = { chatViewModel.openPeerChat(it) },
                                onClosePeerChat = { chatViewModel.closePeerChat() },
                                onSendMessage = { text, attachLabel, attachType, bmp, onSent ->
                                    chatViewModel.sendMessage(
                                        senderName = profile.fullName,
                                        examId = chatExamId,
                                        examName = ExamCatalog.getExamById(chatExamId).name,
                                        text = text,
                                        attachmentLabel = attachLabel,
                                        attachmentType = attachType,
                                        imageBitmap = bmp,
                                        onSentSuccess = onSent
                                    )
                                },
                                onDismissOutcome = { chatViewModel.clearSendOutcome() }
                            )
                        }

                        composable(
                            route = "$ROUTE_CHAPTERS/{subjectId}",
                            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
                            val subject = activeExam.subjects.firstOrNull { it.id == subjectId }
                                ?: activeExam.subjects.first()
                            ChaptersScreen(
                                exam = activeExam,
                                subject = subject,
                                onBack = { navController.popBackStack() },
                                onChapterSelected = { chapter ->
                                    navController.navigate(
                                        "$ROUTE_QUIZ_SETUP/${Uri.encode(subject.id)}/${Uri.encode(chapter)}"
                                    )
                                },
                                onChapterFlashcards = { chapter ->
                                    flashcardViewModel.backToBuilder()
                                    navController.navigate(
                                        "$ROUTE_FLASHCARDS/${Uri.encode(subject.id)}/${Uri.encode(chapter)}"
                                    )
                                }
                            )
                        }

                        composable(ROUTE_FLASHCARDS) {
                            val flashcardUiState by flashcardViewModel.uiState.collectAsStateWithLifecycle()
                            val savedFlashcardDecks by flashcardViewModel.savedDecks.collectAsStateWithLifecycle()
                            FlashcardsScreen(
                                exam = activeExam,
                                uiState = flashcardUiState,
                                savedDecks = savedFlashcardDecks,
                                onGenerateDeck = { flashcardViewModel.generateDeck(it) },
                                onOpenDeck = { flashcardViewModel.openSavedDeck(it) },
                                onDeleteDeck = { flashcardViewModel.deleteSavedDeck(it) },
                                onFlipCard = { flashcardViewModel.flipCard() },
                                onNextCard = { flashcardViewModel.nextCard() },
                                onPrevCard = { flashcardViewModel.previousCard() },
                                onMarkMastered = { id, mastered ->
                                    flashcardViewModel.markCardMastered(id, mastered)
                                },
                                onShuffleDeck = { flashcardViewModel.shuffleDeck() },
                                onToggleFilter = { flashcardViewModel.toggleUnmasteredFilter() },
                                onResetMastery = { flashcardViewModel.resetMasteryInDeck() },
                                onBackToBuilder = { flashcardViewModel.backToBuilder() },
                                onExitScreen = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "$ROUTE_FLASHCARDS/{subjectId}/{chapter}",
                            arguments = listOf(
                                navArgument("subjectId") { type = NavType.StringType },
                                navArgument("chapter") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
                            val chapter = backStackEntry.arguments?.getString("chapter").orEmpty()
                            val flashcardUiState by flashcardViewModel.uiState.collectAsStateWithLifecycle()
                            val savedFlashcardDecks by flashcardViewModel.savedDecks.collectAsStateWithLifecycle()
                            FlashcardsScreen(
                                exam = activeExam,
                                initialSubjectId = subjectId,
                                initialChapter = chapter,
                                uiState = flashcardUiState,
                                savedDecks = savedFlashcardDecks,
                                onGenerateDeck = { flashcardViewModel.generateDeck(it) },
                                onOpenDeck = { flashcardViewModel.openSavedDeck(it) },
                                onDeleteDeck = { flashcardViewModel.deleteSavedDeck(it) },
                                onFlipCard = { flashcardViewModel.flipCard() },
                                onNextCard = { flashcardViewModel.nextCard() },
                                onPrevCard = { flashcardViewModel.previousCard() },
                                onMarkMastered = { id, mastered ->
                                    flashcardViewModel.markCardMastered(id, mastered)
                                },
                                onShuffleDeck = { flashcardViewModel.shuffleDeck() },
                                onToggleFilter = { flashcardViewModel.toggleUnmasteredFilter() },
                                onResetMastery = { flashcardViewModel.resetMasteryInDeck() },
                                onBackToBuilder = { flashcardViewModel.backToBuilder() },
                                onExitScreen = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "$ROUTE_QUIZ_SETUP/{subjectId}/{chapter}",
                            arguments = listOf(
                                navArgument("subjectId") { type = NavType.StringType },
                                navArgument("chapter") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val subjectId = backStackEntry.arguments?.getString("subjectId").orEmpty()
                            val chapter = backStackEntry.arguments?.getString("chapter").orEmpty()
                            val subject = activeExam.subjects.firstOrNull { it.id == subjectId }
                                ?: activeExam.subjects.first()
                            QuizSetupScreen(
                                exam = activeExam,
                                subject = subject,
                                chapter = chapter,
                                onBack = { navController.popBackStack() },
                                onStartQuiz = { config ->
                                    quizViewModel.startQuizGeneration(config, profile)
                                }
                            )
                        }

                        composable(ROUTE_MEGA_QUIZ) {
                            MegaQuizSetupScreen(
                                exam = activeExam,
                                onStartMegaQuiz = { config ->
                                    quizViewModel.startQuizGeneration(config, profile)
                                }
                            )
                        }

                        composable(ROUTE_SAVED_QUESTIONS) {
                            val savedQuestions by appViewModel.savedQuestions.collectAsStateWithLifecycle()
                            SavedQuestionsScreen(
                                savedQuestions = savedQuestions,
                                onRemoveBookmark = { docId ->
                                    appViewModel.removeSavedQuestion(docId)
                                },
                                onGoToArena = {
                                    navController.navigate(ROUTE_SUBJECTS) {
                                        popUpTo(ROUTE_SUBJECTS) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_DASHBOARD) {
                            val attempts by appViewModel.quizAttempts.collectAsStateWithLifecycle()
                            val studyInsight by appViewModel.studyInsight.collectAsStateWithLifecycle()
                            val insightGenState by appViewModel.insightGenState.collectAsStateWithLifecycle()
                            DashboardScreen(
                                profile = profile,
                                attempts = attempts,
                                studyInsight = studyInsight,
                                insightGenState = insightGenState,
                                onGenerateStudyPlan = {
                                    appViewModel.generatePersonalizedStudyPlan(profile, attempts)
                                },
                                onOpenAttemptReview = { attempt ->
                                    quizViewModel.viewSavedAttempt(attempt)
                                },
                                onGoToSubjects = {
                                    navController.navigate(ROUTE_SUBJECTS) {
                                        popUpTo(ROUTE_SUBJECTS) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_LEADERBOARD) {
                            val lbSelectedExamId by leaderboardViewModel.selectedExamId.collectAsStateWithLifecycle()
                            val examLeaderboardState by leaderboardViewModel.examLeaderboard.collectAsStateWithLifecycle()
                            val megaLeaderboardState by leaderboardViewModel.megaLeaderboard.collectAsStateWithLifecycle()
                            val studyTimeLeaderboardState by leaderboardViewModel.studyTimeLeaderboard.collectAsStateWithLifecycle()
                            LeaderboardScreen(
                                currentUserId = currentUserId,
                                selectedExamId = lbSelectedExamId,
                                onSelectExam = { leaderboardViewModel.selectExam(it) },
                                examLeaderboardState = examLeaderboardState,
                                megaLeaderboardState = megaLeaderboardState,
                                studyTimeLeaderboardState = studyTimeLeaderboardState,
                                focusRuntimeStatusFlow = focusViewModel.runtimeStatus
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                profile = profile,
                                onEditProfile = {
                                    appViewModel.clearOnboardingError()
                                    navController.navigate(ROUTE_EDIT_PROFILE)
                                },
                                onSignedOut = {
                                    FocusPreferences(context).clearForSignOut()
                                    StudyFocusService.sync(context, false)
                                }
                            )
                        }

                        composable(ROUTE_EDIT_PROFILE) {
                            val onboardingSaving by appViewModel.onboardingSaving.collectAsStateWithLifecycle()
                            val onboardingError by appViewModel.onboardingError.collectAsStateWithLifecycle()
                            OnboardingScreen(
                                initialName = profile.fullName,
                                initialDob = profile.dob,
                                initialPhone = profile.phoneNumber,
                                initialExamId = profile.selectedExamId,
                                isEditing = true,
                                isSaving = onboardingSaving,
                                errorMessage = onboardingError,
                                onSubmit = { name, dob, phone, examId ->
                                    appViewModel.saveProfile(
                                        fullName = name,
                                        dob = dob,
                                        phoneNumber = phone,
                                        selectedExamId = examId,
                                        isNewProfile = false,
                                        onComplete = {
                                            leaderboardViewModel.selectExam(examId)
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
