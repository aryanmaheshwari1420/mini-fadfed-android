package com.example.mini_fadfed_android.ui.match.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mini_fadfed_android.domain.model.MatchFilter
import com.example.mini_fadfed_android.domain.model.UserWallet
import com.example.mini_fadfed_android.ui.theme.FadfedBorder
import com.example.mini_fadfed_android.ui.theme.FadfedErrorRed
import com.example.mini_fadfed_android.ui.theme.FadfedMutedBlueGrey
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBackground
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBlue
import com.example.mini_fadfed_android.ui.theme.FadfedSecondaryIndigo
import com.example.mini_fadfed_android.ui.theme.FadfedSelectedBlueBg
import com.example.mini_fadfed_android.ui.theme.FadfedTextPrimary
import com.example.mini_fadfed_android.ui.theme.FadfedYellowAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    userGender: String,
    wallet: UserWallet,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onStartSearch: (MatchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = {
                Text(
                    text = "Notice",
                    fontWeight = FontWeight.Bold,
                    color = FadfedErrorRed
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    color = FadfedTextPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.Bold,
                        color = FadfedSecondaryIndigo
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = FadfedPrimaryBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mini FadFed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = FadfedPrimaryBlue
                        )
                        Text(
                            text = "Welcome, $userName",
                            style = MaterialTheme.typography.bodySmall,
                            color = FadfedMutedBlueGrey
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, FadfedBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(FadfedYellowAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🪙",
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${wallet.coins} coins",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = FadfedTextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FadfedPrimaryBackground
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card Summary
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FadfedBorder, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(FadfedSelectedBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = FadfedSecondaryIndigo,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FadfedTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(FadfedSelectedBlueBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (userGender == "f") "Female" else "Male",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FadfedPrimaryBlue
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Matchmaking Section Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Start Matching",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FadfedTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a preference to find a conversation partner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FadfedMutedBlueGrey
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Both (Free entry point)
            FilterOptionCard(
                title = "Everyone (Both)",
                subtitle = "Free search • No coins required",
                badge = "FREE",
                badgeBg = FadfedPrimaryBlue,
                badgeTextColor = Color.White,
                accentColor = FadfedPrimaryBlue,
                onClick = { onStartSearch(MatchFilter.BOTH) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Male (Paid filter)
            val maleCost = if (wallet.genderCost > 0) wallet.genderCost else 9
            FilterOptionCard(
                title = "Male Only",
                subtitle = "Filtered search • Costs $maleCost coins",
                badge = "$maleCost COINS",
                badgeBg = FadfedYellowAccent,
                badgeTextColor = FadfedTextPrimary,
                accentColor = FadfedSecondaryIndigo,
                onClick = { onStartSearch(MatchFilter.MALE) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Female (Paid filter)
            val femaleCost = if (wallet.genderCost > 0) wallet.genderCost else 9
            FilterOptionCard(
                title = "Female Only",
                subtitle = "Filtered search • Costs $femaleCost coins",
                badge = "$femaleCost COINS",
                badgeBg = FadfedYellowAccent,
                badgeTextColor = FadfedTextPrimary,
                accentColor = FadfedSecondaryIndigo,
                onClick = { onStartSearch(MatchFilter.FEMALE) }
            )
        }
    }
}

@Composable
private fun FilterOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeBg: Color,
    badgeTextColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FadfedBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FadfedTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = FadfedMutedBlueGrey
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badge,
                    color = badgeTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
