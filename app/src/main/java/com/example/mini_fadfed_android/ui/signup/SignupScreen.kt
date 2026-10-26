package com.example.mini_fadfed_android.ui.signup

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mini_fadfed_android.ui.theme.FadfedBorder
import com.example.mini_fadfed_android.ui.theme.FadfedErrorRed
import com.example.mini_fadfed_android.ui.theme.FadfedMutedBlueGrey
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBackground
import com.example.mini_fadfed_android.ui.theme.FadfedPrimaryBlue
import com.example.mini_fadfed_android.ui.theme.FadfedSecondaryIndigo
import com.example.mini_fadfed_android.ui.theme.FadfedSelectedBlueBg
import com.example.mini_fadfed_android.ui.theme.FadfedTextPrimary
import com.example.mini_fadfed_android.ui.theme.FadfedYellowAccent

@Composable
fun SignupScreen(
    viewModel: SignupViewModel,
    onSignupSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onSignupSuccess()
        }
    }

    Scaffold(
        containerColor = FadfedPrimaryBackground,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: App Title & Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mini FadFed",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = FadfedPrimaryBlue
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FadfedSelectedBlueBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "English",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FadfedSecondaryIndigo
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose your display name and gender to begin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FadfedMutedBlueGrey,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Name input section
                Text(
                    text = "Your name",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FadfedTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChanged,
                    placeholder = {
                        Text(
                            text = "Enter your name",
                            color = FadfedMutedBlueGrey
                        )
                    },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    isError = uiState.errorMessage != null && uiState.name.isBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color(0xFFF0F3F7),
                        focusedBorderColor = FadfedPrimaryBlue,
                        unfocusedBorderColor = FadfedBorder,
                        errorBorderColor = FadfedErrorRed,
                        focusedTextColor = FadfedTextPrimary,
                        unfocusedTextColor = FadfedTextPrimary,
                        cursorColor = FadfedSecondaryIndigo
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Gender Selection Section
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FadfedTextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GenderCard(
                        label = "Male",
                        value = "m",
                        isSelected = uiState.gender == "m",
                        enabled = !uiState.isLoading,
                        onSelect = { viewModel.onGenderChanged("m") },
                        isMale = true,
                        modifier = Modifier.weight(1f)
                    )

                    GenderCard(
                        label = "Female",
                        value = "f",
                        isSelected = uiState.gender == "f",
                        enabled = !uiState.isLoading,
                        onSelect = { viewModel.onGenderChanged("f") },
                        isMale = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error Banner Display
                if (uiState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFECEC)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, FadfedErrorRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = FadfedErrorRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Actions: Consent & Continue Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = FadfedMutedBlueGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = viewModel::onContinueClicked,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FadfedSecondaryIndigo,
                        contentColor = Color.White,
                        disabledContainerColor = FadfedMutedBlueGrey,
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenderCard(
    label: String,
    value: String,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    isMale: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) FadfedPrimaryBlue else FadfedBorder
    val backgroundColor = if (isSelected) FadfedSelectedBlueBg else Color.White
    val textColor = if (isSelected) FadfedSecondaryIndigo else FadfedTextPrimary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled, onClick = onSelect)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Character Illustration
            CircularCharacterAvatar(
                isMale = isMale,
                isSelected = isSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = FadfedPrimaryBlue,
                    unselectedColor = FadfedMutedBlueGrey
                )
            )
        }
    }
}

@Composable
private fun CircularCharacterAvatar(
    isMale: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val ringColor = if (isSelected) FadfedPrimaryBlue else Color.Transparent

    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(if (isMale) Color(0xFFEBF5FF) else Color(0xFFFFF0F5))
            .border(2.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Body / Shirt (Secondary Indigo #616EDA)
            drawArc(
                color = FadfedSecondaryIndigo,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(canvasWidth * 0.1f, canvasHeight * 0.55f),
                size = Size(canvasWidth * 0.8f, canvasHeight * 0.8f)
            )

            // Neck
            drawRect(
                color = Color(0xFFFFDAB9), // Warm peach skin tone
                topLeft = Offset(canvasWidth * 0.42f, canvasHeight * 0.48f),
                size = Size(canvasWidth * 0.16f, canvasHeight * 0.15f)
            )

            // Head / Face
            drawCircle(
                color = Color(0xFFFFE0BD), // Peach skin
                radius = canvasWidth * 0.25f,
                center = Offset(canvasWidth * 0.5f, canvasHeight * 0.36f)
            )

            // Hair / Details based on gender
            if (isMale) {
                // Male Hair: short indigo crop
                val hairPath = Path().apply {
                    moveTo(canvasWidth * 0.25f, canvasHeight * 0.36f)
                    cubicTo(
                        canvasWidth * 0.25f, canvasHeight * 0.14f,
                        canvasWidth * 0.75f, canvasHeight * 0.14f,
                        canvasWidth * 0.75f, canvasHeight * 0.36f
                    )
                    close()
                }
                drawPath(hairPath, color = Color(0xFF2C3246))

                // Yellow shirt accent collar
                drawCircle(
                    color = FadfedYellowAccent,
                    radius = canvasWidth * 0.06f,
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.65f)
                )
            } else {
                // Female Hair: longer curved indigo hair
                val hairPath = Path().apply {
                    moveTo(canvasWidth * 0.22f, canvasHeight * 0.48f)
                    cubicTo(
                        canvasWidth * 0.22f, canvasHeight * 0.12f,
                        canvasWidth * 0.78f, canvasHeight * 0.12f,
                        canvasWidth * 0.78f, canvasHeight * 0.48f
                    )
                    cubicTo(
                        canvasWidth * 0.74f, canvasHeight * 0.32f,
                        canvasWidth * 0.26f, canvasHeight * 0.32f,
                        canvasWidth * 0.22f, canvasHeight * 0.48f
                    )
                    close()
                }
                drawPath(hairPath, color = Color(0xFF2C3246))

                // Yellow hair pin / accessory accent
                drawCircle(
                    color = FadfedYellowAccent,
                    radius = canvasWidth * 0.07f,
                    center = Offset(canvasWidth * 0.32f, canvasHeight * 0.24f)
                )
            }

            // Eyes
            drawCircle(
                color = Color(0xFF1E2230),
                radius = canvasWidth * 0.035f,
                center = Offset(canvasWidth * 0.42f, canvasHeight * 0.36f)
            )
            drawCircle(
                color = Color(0xFF1E2230),
                radius = canvasWidth * 0.035f,
                center = Offset(canvasWidth * 0.58f, canvasHeight * 0.36f)
            )
        }
    }
}
