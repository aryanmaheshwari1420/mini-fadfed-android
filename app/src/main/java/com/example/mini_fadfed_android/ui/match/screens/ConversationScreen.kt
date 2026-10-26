package com.example.mini_fadfed_android.ui.match.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mini_fadfed_android.domain.model.ChatMessage
import com.example.mini_fadfed_android.domain.model.MessageStatus
import com.example.mini_fadfed_android.ui.theme.FadfedBorder
import com.example.mini_fadfed_android.ui.theme.FadfedErrorRed
import com.example.mini_fadfed_android.ui.theme.FadfedMutedBlueGrey
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBackground
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBlue
import com.example.mini_fadfed_android.ui.theme.FadfedSecondaryIndigo
import com.example.mini_fadfed_android.ui.theme.FadfedTextPrimary
import com.example.mini_fadfed_android.ui.theme.FadfedYellowAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    peerName: String,
    chatId: String,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onRetryMessage: (String) -> Unit,
    onMarkMessageVisible: (String) -> Unit,
    onSetVisible: (Boolean) -> Unit,
    onEndChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showEndChatDialog by remember { mutableStateOf(false) }
    var lastSendTimestamp by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onSetVisible(true)
        onDispose {
            onSetVisible(false)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = FadfedPrimaryBackground,
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FadfedSecondaryIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peerName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FadfedTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(FadfedPrimaryBlue)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Connected • Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FadfedMutedBlueGrey
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showEndChatDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Leave Conversation",
                            tint = FadfedMutedBlueGrey
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .border(1.dp, FadfedBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        placeholder = {
                            Text(
                                text = "Type a message...",
                                color = FadfedMutedBlueGrey
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FadfedPrimaryBackground,
                            unfocusedContainerColor = FadfedPrimaryBackground,
                            focusedBorderColor = FadfedPrimaryBlue,
                            unfocusedBorderColor = FadfedBorder,
                            focusedTextColor = FadfedTextPrimary,
                            unfocusedTextColor = FadfedTextPrimary,
                            cursorColor = FadfedSecondaryIndigo
                        )
                    )

                    val canSend = inputText.trim().isNotEmpty()

                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastSendTimestamp > 300) {
                                lastSendTimestamp = now
                                val toSend = inputText.trim()
                                if (toSend.isNotEmpty()) {
                                    onSendMessage(toSend)
                                    inputText = ""
                                }
                            }
                        },
                        enabled = canSend,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FadfedSecondaryIndigo,
                            contentColor = Color.White,
                            disabledContainerColor = FadfedMutedBlueGrey.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                        )
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
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FadfedPrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👋", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "You're connected with $peerName!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FadfedTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Say hello to start the conversation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FadfedMutedBlueGrey
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = messages, key = { it.id }) { msg ->
                        if (!msg.isFromMe) {
                            LaunchedEffect(msg.id) {
                                onMarkMessageVisible(msg.id)
                            }
                        }

                        MessageBubble(
                            message = msg,
                            onRetry = { onRetryMessage(msg.id) }
                        )
                    }
                }
            }
        }

        if (showEndChatDialog) {
            AlertDialog(
                onDismissRequest = { showEndChatDialog = false },
                title = {
                    Text(
                        text = "Leave Conversation",
                        fontWeight = FontWeight.Bold,
                        color = FadfedTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to end this conversation with $peerName?",
                        color = FadfedMutedBlueGrey
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEndChatDialog = false
                            onEndChat()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FadfedErrorRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Leave Chat", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndChatDialog = false }) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            color = FadfedMutedBlueGrey
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMe = message.isFromMe
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val backgroundColor = if (isMe) {
        FadfedSecondaryIndigo
    } else {
        Color.White
    }

    val textColor = if (isMe) {
        Color.White
    } else {
        FadfedTextPrimary
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            shadowElevation = if (isMe) 0.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (!isMe) Modifier.border(1.dp, FadfedBorder, bubbleShape) else Modifier
                )
                .clickable(enabled = message.status == MessageStatus.FAILED, onClick = onRetry)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeStr = remember(message.timestamp) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        sdf.format(Date(message.timestamp))
                    }

                    Text(
                        text = timeStr,
                        fontSize = 11.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.75f) else FadfedMutedBlueGrey
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(status = message.status)
                    }
                }

                if (message.status == MessageStatus.FAILED) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Failed to send. Tap to retry.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FadfedYellowAccent,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

/**
 * Renders the exact message status:
 * - SENDING: Clock icon
 * - SENT: Single tick
 * - READ: Double ticks in yellow accent
 * - FAILED: Warning indicator in error red
 */
@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val neutralColor = Color.White.copy(alpha = 0.8f)
    val readColor = FadfedYellowAccent
    val errorColor = FadfedErrorRed

    when (status) {
        MessageStatus.SENDING -> {
            Canvas(modifier = modifier.size(13.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2) - 1f
                drawCircle(
                    color = neutralColor,
                    radius = radius,
                    style = Stroke(width = 1.5f)
                )
                drawLine(
                    color = neutralColor,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.5f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = neutralColor,
                    start = center,
                    end = Offset(center.x + radius * 0.4f, center.y),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }
        MessageStatus.SENT -> {
            Canvas(modifier = modifier.size(14.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.45f, size.height * 0.75f)
                    lineTo(size.width * 0.85f, size.height * 0.25f)
                }
                drawPath(
                    path = path,
                    color = neutralColor,
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )
            }
        }
        MessageStatus.READ -> {
            Canvas(modifier = modifier.size(17.dp)) {
                val path1 = Path().apply {
                    moveTo(size.width * 0.05f, size.height * 0.5f)
                    lineTo(size.width * 0.35f, size.height * 0.75f)
                    lineTo(size.width * 0.7f, size.height * 0.25f)
                }
                drawPath(
                    path = path1,
                    color = readColor,
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )

                val path2 = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.5f)
                    lineTo(size.width * 0.55f, size.height * 0.75f)
                    lineTo(size.width * 0.95f, size.height * 0.25f)
                }
                drawPath(
                    path = path2,
                    color = readColor,
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )
            }
        }
        MessageStatus.FAILED -> {
            Canvas(modifier = modifier.size(13.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2) - 1f
                drawCircle(
                    color = errorColor,
                    radius = radius,
                    style = Stroke(width = 1.5f)
                )
                drawLine(
                    color = errorColor,
                    start = Offset(center.x, center.y - radius * 0.55f),
                    end = Offset(center.x, center.y + radius * 0.1f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = errorColor,
                    radius = 1f,
                    center = Offset(center.x, center.y + radius * 0.5f)
                )
            }
        }
    }
}
