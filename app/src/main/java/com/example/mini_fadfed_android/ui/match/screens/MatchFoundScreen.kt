package com.example.mini_fadfed_android.ui.match.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mini_fadfed_android.ui.theme.FadfedBorder
import com.example.mini_fadfed_android.ui.theme.FadfedMutedBlueGrey
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBackground
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBlue
import com.example.mini_fadfed_android.ui.theme.FadfedSecondaryIndigo
import com.example.mini_fadfed_android.ui.theme.FadfedSelectedBlueBg
import com.example.mini_fadfed_android.ui.theme.FadfedSelectedIndigoBg
import com.example.mini_fadfed_android.ui.theme.FadfedTextPrimary

@Composable
fun MatchFoundScreen(
    peerName: String,
    peerGender: String?,
    youAccepted: Boolean,
    peerAccepted: Boolean,
    onAccept: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = FadfedPrimaryBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Match Found!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = FadfedPrimaryBlue
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "You've been paired with a new friend",
                style = MaterialTheme.typography.bodyMedium,
                color = FadfedMutedBlueGrey
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Peer Profile Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FadfedBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(FadfedSecondaryIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Peer Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FadfedTextPrimary
                    )

                    if (peerGender != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(FadfedSelectedBlueBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (peerGender == "f") "Female" else "Male",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FadfedPrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status Display Card
                    val statusBg = when {
                        peerAccepted -> FadfedSelectedIndigoBg
                        youAccepted -> FadfedSelectedBlueBg
                        else -> FadfedPrimaryBackground
                    }
                    val statusTextColor = when {
                        peerAccepted -> FadfedSecondaryIndigo
                        youAccepted -> FadfedPrimaryBlue
                        else -> FadfedMutedBlueGrey
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(statusBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                youAccepted && !peerAccepted -> "⏳ You accepted! Waiting for $peerName..."
                                !youAccepted && peerAccepted -> "✨ $peerName accepted! Tap Accept to chat."
                                youAccepted && peerAccepted -> "Both accepted! Connecting..."
                                else -> "Both users must accept to start the chat"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (peerAccepted || youAccepted) FontWeight.Bold else FontWeight.Normal,
                            color = statusTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Action Buttons: Skip & Accept
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skip Button
                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = FadfedMutedBlueGrey
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, FadfedMutedBlueGrey),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = FadfedMutedBlueGrey
                    )
                }

                // Accept Button
                Button(
                    onClick = onAccept,
                    enabled = !youAccepted,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FadfedSecondaryIndigo,
                        contentColor = Color.White,
                        disabledContainerColor = FadfedSecondaryIndigo.copy(alpha = 0.5f),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    if (youAccepted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accepted",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Accepted",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
