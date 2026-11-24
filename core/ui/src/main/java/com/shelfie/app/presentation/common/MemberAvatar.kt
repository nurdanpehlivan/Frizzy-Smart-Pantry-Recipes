package com.shelfie.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Saturated colours with white initials: they stand out on the pale leaf hero and the cream
 * cards in light mode as well as on the dark background.
 */
private val AvatarColors = listOf(
    Color(0xFF2E7D57), // emerald
    Color(0xFFD9822B), // amber
    Color(0xFFD9544A), // coral
    Color(0xFF4F63C4), // indigo
    Color(0xFF8E4FB5), // plum
    Color(0xFF1C8A8A), // teal
    Color(0xFFB5487A), // berry
)

/** The colour a household member gets everywhere in the app: the family tab, the list, the feed. */
fun memberColor(email: String): Color = AvatarColors[Math.floorMod(email.lowercase().hashCode(), AvatarColors.size)]

/**
 * A household member as a round initial in a colour of their own, stable per person, so the
 * same person looks the same on the family tab, the shopping list and the activity feed.
 * Decorative: callers describe the person in their own words where it matters.
 */
@Composable
fun MemberAvatar(email: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(memberColor(email), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName(email).take(1).uppercase(),
            color = Color.White,
            style = when {
                size >= 40.dp -> MaterialTheme.typography.titleMedium
                size >= 28.dp -> MaterialTheme.typography.labelLarge
                else -> MaterialTheme.typography.labelSmall
            },
            fontWeight = FontWeight.SemiBold,
        )
    }
}
