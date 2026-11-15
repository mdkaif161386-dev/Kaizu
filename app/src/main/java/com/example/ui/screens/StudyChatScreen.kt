package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ChatAttachmentType
import com.example.model.ChatModerationStatus
import com.example.model.DirectChatThread
import com.example.model.ExamCatalog
import com.example.model.StudyChatMessage
import com.example.model.UserProfile
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForgeGold
import com.example.ui.viewmodel.ChatPeerCandidate
import com.example.ui.viewmodel.ChatSendOutcome
import com.example.util.FocusCycleCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private val chatTimeFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("hh:mm a", Locale.US)
}

@Composable
fun StudyChatScreen(
    currentUserId: String,
    profile: UserProfile,
    selectedExamId: String,
    activePeer: ChatPeerCandidate?,
    examMessages: List<StudyChatMessage>,
    directThreads: List<DirectChatThread>,
    directMessages: List<StudyChatMessage>,
    peerCandidates: List<ChatPeerCandidate>,
    moderationStatus: ChatModerationStatus?,
    sendOutcome: ChatSendOutcome,
    onSelectExamRoom: (String) -> Unit,
    onOpenPeerChat: (ChatPeerCandidate) -> Unit,
    onClosePeerChat: () -> Unit,
    onSendMessage: (
        text: String,
        attachmentLabel: String,
        attachmentType: ChatAttachmentType,
        imageBitmap: Bitmap?,
        onSent: () -> Unit
    ) -> Unit,
    onDismissOutcome: () -> Unit
) {
    val context = LocalContext.current
    // 0 = Exam Study Room, 1 = Direct Peer Chats
    var chatSectionTab by remember { mutableIntStateOf(0) }
    var showPolicyDialog by remember { mutableStateOf(false) }

    var draftText by remember { mutableStateOf("") }
    var attachmentLabel by remember { mutableStateOf("") }
    var attachmentType by remember { mutableStateOf(ChatAttachmentType.NONE) }
    var attachmentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPreparingMedia by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (activePeer != null) {
        BackHandler { onClosePeerChat() }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isPreparingMedia = true
            coroutineScope.launch {
                val (type, label, bitmap) = withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri).orEmpty()
                    val name = resolveDisplayName(context, uri)
                    if (mime.startsWith("video")) {
                        Triple(ChatAttachmentType.VIDEO, name.ifBlank { "Shared study video" }, null)
                    } else {
                        Triple(
                            ChatAttachmentType.IMAGE,
                            name.ifBlank { "Study notes image" },
                            decodeScaledBitmap(context, uri)
                        )
                    }
                }
                attachmentType = type
                attachmentLabel = label
                attachmentBitmap = bitmap
                isPreparingMedia = false
            }
        }
    }

    val nowMs = System.currentTimeMillis()
    val isBanned = moderationStatus?.isCurrentlyBanned(nowMs) == true
    val banRemainingMs = if (isBanned) {
        (moderationStatus!!.effectiveBanEndMs() - nowMs).coerceAtLeast(0L)
    } else 0L
    val activeExam = remember(selectedExamId) { ExamCatalog.getExamById(selectedExamId) }

    val messagesToShow = if (activePeer != null) directMessages else examMessages
    val listState = rememberLazyListState()

    LaunchedEffect(messagesToShow.size, activePeer?.userId, chatSectionTab) {
        if (messagesToShow.isNotEmpty() && (activePeer != null || chatSectionTab == 0)) {
            listState.animateScrollToItem(messagesToShow.lastIndex)
        }
    }

    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Policy,
                    contentDescription = null,
                    tint = ForgeGold
                )
            },
            title = { Text("Kaizu Study Chat Policy") },
            text = {
                Text(
                    "Kaizu Study Chat is strictly for exam doubts, notes, formulas, and motivation.\n\n" +
                        "• Zero-Tolerance Rule: Sending sexual content, nudity, or adult videos/links is strictly prohibited.\n" +
                        "• 3-Strike Enforcement: Violators receive up to 3 warnings.\n" +
                        "• 1-Week Suspension: Any sexual content offense after 3 warnings disables all chat access for 7 full days (1 week)."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPolicyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    when (sendOutcome) {
        is ChatSendOutcome.WarningIssued -> {
            AlertDialog(
                onDismissRequest = onDismissOutcome,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = ForgeGold
                    )
                },
                title = {
                    Text(
                        text = "Policy Warning ${sendOutcome.warningNumber} of ${sendOutcome.maxWarnings}",
                        color = ForgeGold
                    )
                },
                text = {
                    Text(
                        "${sendOutcome.reason}\n\n" +
                            "Your message was blocked and NOT delivered. " +
                            if (sendOutcome.warningNumber >= 3) {
                                "FINAL WARNING: One more violation will disable your chats for 1 full week (7 days)."
                            } else {
                                "After 3 warnings, sending sexual content or adult videos will disable your chats for 1 week."
                            }
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismissOutcome) {
                        Text("I Understand")
                    }
                }
            )
        }

        is ChatSendOutcome.BannedForWeek -> {
            val remaining = (sendOutcome.bannedUntilEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
            AlertDialog(
                onDismissRequest = onDismissOutcome,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = null,
                        tint = CrimsonError
                    )
                },
                title = {
                    Text("Chats Disabled for 1 Week", color = CrimsonError)
                },
                text = {
                    Text(
                        "You continued sending prohibited sexual content after receiving 3 warnings.\n\n" +
                            "Reason: ${sendOutcome.reason}\n" +
                            "Chat access re-opens in: ${FocusCycleCalculator.formatRemainingBan(remaining)}."
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismissOutcome) {
                        Text("Close")
                    }
                }
            )
        }

        is ChatSendOutcome.Error -> {
            AlertDialog(
                onDismissRequest = onDismissOutcome,
                title = { Text("Could Not Send Message") },
                text = { Text(sendOutcome.message) },
                confirmButton = {
                    TextButton(onClick = onDismissOutcome) { Text("OK") }
                }
            )
        }

        else -> Unit
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (activePeer != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClosePeerChat,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_back_peer_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Peer List"
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(
                                text = activePeer.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activePeer.examName} Study Peer • Moderated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Student Study Chat",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Discuss doubts, notes & exam strategy • Safe moderated space",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showPolicyDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_policy_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "View Study Chat Policy",
                        tint = if (isBanned) CrimsonError else ForgeGold
                    )
                }
            }

            // Policy & Warning/Ban Status Banner
            Surface(
                color = when {
                    isBanned -> CrimsonError.copy(alpha = 0.14f)
                    (moderationStatus?.warningCount ?: 0L) > 0L -> ForgeGold.copy(alpha = 0.14f)
                    else -> ElectricCyan.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(
                        1.dp,
                        when {
                            isBanned -> CrimsonError.copy(alpha = 0.55f)
                            (moderationStatus?.warningCount ?: 0L) > 0L -> ForgeGold.copy(alpha = 0.55f)
                            else -> ElectricCyan.copy(alpha = 0.35f)
                        },
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { showPolicyDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isBanned) Icons.Filled.Block else Icons.Filled.Policy,
                        contentDescription = "Chat safety status",
                        tint = when {
                            isBanned -> CrimsonError
                            (moderationStatus?.warningCount ?: 0L) > 0L -> ForgeGold
                            else -> ElectricCyan
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            isBanned ->
                                "Chats disabled for 1 week (${FocusCycleCalculator.formatRemainingBan(banRemainingMs)} left) after >3 sexual content violations."
                            (moderationStatus?.warningCount ?: 0L) > 0L ->
                                "Sexual Content Policy Warning ${moderationStatus!!.warningCount}/3. Next violation after 3 warnings bans chat for 7 days."
                            else ->
                                "Zero-tolerance policy: Sexual content / adult videos strictly banned (3 warnings -> 1-week chat disable)."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Mode Tabs when not inside a specific DM thread
            if (activePeer == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = chatSectionTab == 0,
                        onClick = { chatSectionTab = 0 },
                        leadingIcon = {
                            Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("${activeExam.name} Room") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("chat_tab_room")
                    )
                    FilterChip(
                        selected = chatSectionTab == 1,
                        onClick = { chatSectionTab = 1 },
                        leadingIcon = {
                            Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Direct Peer Chat") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForgeGold.copy(alpha = 0.2f),
                            selectedLabelColor = ForgeGold
                        ),
                        modifier = Modifier.testTag("chat_tab_peers")
                    )
                }

                if (chatSectionTab == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExamCatalog.exams.forEach { exam ->
                            FilterChip(
                                selected = exam.id == selectedExamId,
                                onClick = { onSelectExamRoom(exam.id) },
                                label = { Text(exam.name) }
                            )
                        }
                    }
                }
            }

            // Content Area
            if (activePeer == null && chatSectionTab == 1) {
                // Direct Peer Directory & Threads
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (directThreads.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Study Conversations",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(directThreads, key = { it.id }) { thread ->
                            val peerUid = thread.participantIds.firstOrNull { it != currentUserId }.orEmpty()
                            val peerName = thread.participantNames[peerUid] ?: "Study Peer"
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (peerUid.isNotBlank()) {
                                            onOpenPeerChat(
                                                ChatPeerCandidate(
                                                    userId = peerUid,
                                                    displayName = peerName,
                                                    examName = activeExam.name
                                                )
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(ForgeGold.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = peerName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ForgeGold
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peerName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = thread.lastMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Active Students on Kaizu",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (peerCandidates.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No peers discovered yet",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Ask a question in the ${activeExam.name} Room or tap any student's message there to start a 1-on-1 study chat.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(peerCandidates, key = { it.userId }) { peer ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable { onOpenPeerChat(peer) }
                                    .testTag("chat_peer_${peer.userId}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(ElectricCyan.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = peer.displayName.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ElectricCyan
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = peer.displayName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${peer.examName} Aspirant",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    OutlinedButton(onClick = { onOpenPeerChat(peer) }) {
                                        Text("Message")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Live Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messagesToShow.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Forum,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (activePeer != null) {
                                            "Start studying with ${activePeer.displayName}"
                                        } else {
                                            "Welcome to ${activeExam.name} Study Room"
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Share tricky MCQs, NCERT doubts, formulas, or daily motivation.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(messagesToShow, key = { it.id }) { msg ->
                            val isMe = msg.senderId == currentUserId
                            StudyChatBubble(
                                message = msg,
                                isMe = isMe,
                                onTapSender = {
                                    if (!isMe && activePeer == null) {
                                        onOpenPeerChat(
                                            ChatPeerCandidate(
                                                userId = msg.senderId,
                                                displayName = msg.senderName,
                                                examName = activeExam.name
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Attachment preview chip
                if (attachmentLabel.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${attachmentType.id}: $attachmentLabel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove attachment",
                                    tint = ElectricCyan,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            attachmentLabel = ""
                                            attachmentType = ChatAttachmentType.NONE
                                            attachmentBitmap = null
                                        }
                                )
                            }
                        }
                    }
                }

                // Message Composer (locked if 7-day chat ban active)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            enabled = !isBanned && !isPreparingMedia && sendOutcome !is ChatSendOutcome.Checking,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_attach_button")
                        ) {
                            if (isPreparingMedia) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = "Attach study image or video"
                                )
                            }
                        }

                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            enabled = !isBanned && sendOutcome !is ChatSendOutcome.Checking,
                            placeholder = {
                                Text(
                                    if (isBanned) "Chat disabled for 1 week due to policy violation"
                                    else "Ask a study doubt or share a tip..."
                                )
                            },
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )

                        Spacer(Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                onSendMessage(
                                    draftText,
                                    attachmentLabel,
                                    attachmentType,
                                    attachmentBitmap
                                ) {
                                    draftText = ""
                                    attachmentLabel = ""
                                    attachmentType = ChatAttachmentType.NONE
                                    attachmentBitmap = null
                                }
                            },
                            enabled = !isBanned &&
                                !isPreparingMedia &&
                                sendOutcome !is ChatSendOutcome.Checking &&
                                (draftText.isNotBlank() || attachmentLabel.isNotBlank()),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_send_button")
                        ) {
                            if (sendOutcome is ChatSendOutcome.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = ForgeGold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send study message",
                                    tint = if (isBanned) MaterialTheme.colorScheme.onSurfaceVariant else ForgeGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyChatBubble(
    message: StudyChatMessage,
    isMe: Boolean,
    onTapSender: () -> Unit
) {
    val timeText = remember(message.createdAt) {
        val date = message.createdAt?.toDate()
        if (date != null) {
            chatTimeFormatter.get()?.format(date) ?: "Sent"
        } else {
            "Sending..."
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) ForgeGold.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            modifier = Modifier
                .widthIn(max = 310.dp)
                .border(
                    1.dp,
                    if (isMe) ForgeGold.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isMe) {
                    Text(
                        text = "${message.senderName} • Tap to DM",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricCyan,
                        modifier = Modifier.clickable(onClick = onTapSender)
                    )
                    Spacer(Modifier.height(3.dp))
                }
                if (message.attachmentLabel.isNotBlank()) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "[${message.attachmentType}] ${message.attachmentLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (message.text.isNotBlank()) Spacer(Modifier.height(4.dp))
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull().orEmpty()
}

private fun decodeScaledBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        val maxDim = 512
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        var sampleSize = 1
        while (maxOf(rawW / sampleSize, rawH / sampleSize) > maxDim * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val sampled = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val maxSide = maxOf(sampled.width, sampled.height)
        if (maxSide > maxDim) {
            val scale = maxDim.toFloat() / maxSide.toFloat()
            val scaled = Bitmap.createScaledBitmap(
                sampled,
                (sampled.width * scale).toInt().coerceAtLeast(1),
                (sampled.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != sampled) sampled.recycle()
            scaled
        } else {
            sampled
        }
    }.getOrNull()
}
