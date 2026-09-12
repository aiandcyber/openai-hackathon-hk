package com.aiforseniors.scamshield

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Mock-aligned calm green palette for senior home */
val BrandInk = Color(0xFF132033)
val BrandMuted = Color(0xFF6B7280)
val BrandBg = Color(0xFFEEF1F4)
val BrandCard = Color(0xFFFFFFFF)
val BrandGreen = Color(0xFF1F6B4A)
val BrandGreenSoft = Color(0xFFE7F3EC)
val BrandMint = Color(0xFFD8EEE3)
val BrandWarn = Color(0xFFDC2626)
val BrandNavy = BrandInk
val BrandTeal = BrandGreen
val BrandOk = BrandGreen

private val SeniorTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        color = BrandInk,
    ),
    titleLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
        color = BrandInk,
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = BrandMuted,
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = BrandMuted,
    ),
    labelLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

@Composable
fun SeniorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandGreen,
            onPrimary = Color.White,
            secondary = BrandInk,
            background = BrandBg,
            onBackground = BrandInk,
            surface = BrandCard,
            onSurface = BrandInk,
            error = BrandWarn,
        ),
        typography = SeniorTypography,
        content = content,
    )
}

@Composable
fun seniorPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = BrandGreen,
    contentColor = Color.White,
)

@Composable
fun seniorSecondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = BrandInk,
    contentColor = Color.White,
)

@Composable
fun seniorWarnButtonColors() = ButtonDefaults.buttonColors(
    containerColor = BrandWarn,
    contentColor = Color.White,
)
