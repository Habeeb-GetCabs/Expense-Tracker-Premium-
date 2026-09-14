package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "₹0.00"
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.format(amount).replace("INR", "₹")
    } catch (e: Exception) {
        String.format(Locale.US, "₹%,.2f", amount)
    }
}

fun formatDate(timestampMs: Long): String {
    if (timestampMs <= 0) return ""
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        sdf.format(Date(timestampMs))
    } catch (e: Exception) {
        ""
    }
}

fun getCategoryColor(categoryName: String): Color {
    return when (categoryName.lowercase(Locale.ROOT)) {
        "grocery" -> Color(0xFF10B981)
        "medical" -> Color(0xFFEF4444)
        "hospital" -> Color(0xFFDC2626)
        "dining" -> Color(0xFFF59E0B)
        "shopping" -> Color(0xFF8B5CF6)
        "transport" -> Color(0xFF3B82F6)
        "utilities" -> Color(0xFF6366F1)
        "salary" -> Color(0xFF059669)
        "rent" -> Color(0xFFD97706)
        "entertainment" -> Color(0xFFEC4899)
        else -> Color(0xFF6B7280)
    }
}

fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase(Locale.ROOT)) {
        "grocery" -> Icons.Default.ShoppingCart
        "medical" -> Icons.Default.MedicalServices
        "hospital" -> Icons.Default.LocalHospital
        "dining" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingBag
        "transport" -> Icons.Default.DirectionsCar
        "utilities" -> Icons.Default.ElectricalServices
        "salary" -> Icons.Default.Payments
        "rent" -> Icons.Default.HomeWork
        "entertainment" -> Icons.Default.Movie
        else -> Icons.Default.Category
    }
}

@Composable
fun CategoryIconBadge(
    categoryName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val color = getCategoryColor(categoryName)
    val icon = getCategoryIcon(categoryName)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryName,
            tint = color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun TransactionTypeTag(
    type: String,
    modifier: Modifier = Modifier
) {
    val isIncome = type.uppercase(Locale.ROOT) == "INCOME"
    val containerColor = if (isIncome) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val contentColor = if (isIncome) Color(0xFF065F46) else Color(0xFF991B1B)

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = if (isIncome) "+ INCOME" else "- EXPENSE",
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
