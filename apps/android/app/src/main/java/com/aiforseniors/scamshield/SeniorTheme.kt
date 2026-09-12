package com.aiforseniors.scamshield

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrandNavy = Color(0xFF0F2C4C)
val BrandTeal = Color(0xFF0E7490)
val BrandBg = Color(0xFFF4F7FB)
val BrandCard = Color(0xFFFFFFFF)
val BrandInk = Color(0xFF0B1220)
val BrandMuted = Color(0xFF475569)
val BrandWarn = Color(0xFF9F1239)
val BrandOk = Color(0xFF166534)

private val SeniorTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 50.sp,
        color = BrandNavy,
    ),
    headlineLarge = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 42.sp,
        color = BrandNavy,
    ),
    headlineMedium = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 36.sp,
        color = BrandInk,
    ),
    titleLarge = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
        color = BrandInk,
    ),
    bodyLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 32.sp,
        color = BrandInk,
    ),
    bodyMedium = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        color = BrandMuted,
    ),
    labelLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp,
    ),
)

val SeniorButtonHeight = 76.dp

@Composable
fun SeniorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandTeal,
            onPrimary = Color.White,
            secondary = BrandNavy,
            onSecondary = Color.White,
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
    containerColor = BrandTeal,
    contentColor = Color.White,
)

@Composable
fun seniorSecondaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = BrandNavy,
    contentColor = Color.White,
)

@Composable
fun seniorWarnButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color.White,
    contentColor = BrandWarn,
)
