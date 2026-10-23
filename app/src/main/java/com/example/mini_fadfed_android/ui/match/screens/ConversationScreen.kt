package com.example.mini_fadfed_android.ui.match.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peerName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

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
                        enabled = inputText.trim().isNotEmpty(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Send", fontWeight = FontWeight.Bold)
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
                    Text(
                        text = "You are now connected with $peerName!\nSay hello to start chatting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
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
                title = { Text("Leave Conversation") },
                text = { Text("Are you sure you want to end this conversation with $peerName?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showEndChatDialog = false
                            onEndChat()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Leave Chat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndChatDialog = false }) {
                        Text("Cancel")
                    }
                }
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
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val backgroundColor = if (isMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clickable(enabled = message.status == MessageStatus.FAILED, onClick = onRetry)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                        color = textColor.copy(alpha = 0.7f)
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
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

/**
 * Renders the exact message status:
 * - SENDING: Clock icon (not yet written to socket)
 * - SENT: Single tick (written to open socket)
 * - READ: Double ticks (peer sent ack with status: "read")
 * - FAILED: Warning indicator (unacknowledged after 5s timeout and 3 retries)
 */
@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val errorColor = MaterialTheme.colorScheme.error

    when (status) {
        MessageStatus.SENDING -> {
            // Clock icon
            Canvas(modifier = modifier.size(13.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2) - 1f
                drawCircle(
                    color = neutralColor,
                    radius = radius,
                    style = Stroke(width = 1.5f)
                )
                // Hour hand
                drawLine(
                    color = neutralColor,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.5f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
                // Minute hand
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
            // Single tick
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
            // Double ticks in active accent color
            Canvas(modifier = modifier.size(17.dp)) {
                // First tick (left)
                val path1 = Path().apply {
                    moveTo(size.width * 0.05f, size.height * 0.5f)
                    lineTo(size.width * 0.35f, size.height * 0.75f)
                    lineTo(size.width * 0.7f, size.height * 0.25f)
                }
                drawPath(
                    path = path1,
                    color = primaryColor,
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )

                // Second tick (right, offset)
                val path2 = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.5f)
                    lineTo(size.width * 0.55f, size.height * 0.75f)
                    lineTo(size.width * 0.95f, size.height * 0.25f)
                }
                drawPath(
                    path = path2,
                    color = primaryColor,
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )
            }
        }
        MessageStatus.FAILED -> {
            // Exclamation error circle
            Canvas(modifier = modifier.size(13.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2) - 1f
                drawCircle(
                    color = errorColor,
                    radius = radius,
                    style = Stroke(width = 1.5f)
                )
                // Exclamation bar
                drawLine(
                    color = errorColor,
                    start = Offset(center.x, center.y - radius * 0.55f),
                    end = Offset(center.x, center.y + radius * 0.1f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
                // Exclamation dot
                drawCircle(
                    color = errorColor,
                    radius = 1f,
                    center = Offset(center.x, center.y + radius * 0.5f)
                )
            }
        }
    }
}
