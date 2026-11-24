package com.shelfie.app.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.FreshnessStatus
import com.shelfie.app.presentation.theme.freshnessPalette
import kotlin.math.roundToInt

/** Below this the arc would be a dot; the last day still shows a sliver, so the ring never looks broken. */
private const val MIN_SWEEP = 0.04f

private val RingEmojiShadow = Shadow(color = Color.Black.copy(alpha = 0.3f), offset = Offset(0f, 4f), blurRadius = 8f)

/**
 * Shelfie's signature freshness mark: the product's emoji on a tinted disc, circled by an arc of
 * the shelf life still ahead ([shelfLifeLeft], 0–1) in its freshness colour. The arc empties
 * day by day; an expired item keeps only a faint red track.
 */
@Composable
fun FreshnessRing(
    emoji: String,
    shelfLifeLeft: Float,
    status: FreshnessStatus,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 4.dp,
) {
    val colors = MaterialTheme.freshnessPalette.forStatus(status)
    val expired = shelfLifeLeft <= 0f && status == FreshnessStatus.CRITICAL
    val track = if (expired) colors.accent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
    val target = if (shelfLifeLeft <= 0f) 0f else shelfLifeLeft.coerceIn(MIN_SWEEP, 1f)
    val sweep by animateFloatAsState(target, animationSpec = tween(durationMillis = 600), label = "freshnessRing")
    val description = stringResource(R.string.freshness_ring_description, (shelfLifeLeft * 100).roundToInt())

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (sweep > 0f) {
                drawArc(
                    color = colors.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(strokeWidth + 3.dp)
                .background(colors.container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = (size.value * 0.36f).sp,
                style = TextStyle(shadow = RingEmojiShadow),
            )
        }
    }
}
