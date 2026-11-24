package com.shelfie.app.presentation.scanner.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.presentation.common.FreshnessRing
import com.shelfie.app.presentation.common.displayEmoji
import com.shelfie.app.presentation.common.formatQuantity
import com.shelfie.app.presentation.common.labelRes
import com.shelfie.app.presentation.theme.extendedColors
import com.shelfie.app.presentation.theme.freshnessPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DetectedItemCard(
    item: PantryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Set for an item that looks spoiled or expired; the card then asks what to do with it. */
    warning: String? = null,
    onKeep: () -> Unit = {},
    onDiscard: () -> Unit = {},
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val today = remember { LocalDate.now() }
    val status = item.freshnessStatus(today)

    OutlinedCard(
        onClick = onEdit,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(
            if (warning != null) 2.dp else 1.dp,
            if (warning != null) MaterialTheme.colorScheme.error else MaterialTheme.extendedColors.glassBorder,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The same ring as in the pantry, from the estimated date: what will need eating first.
                FreshnessRing(
                    emoji = item.displayEmoji,
                    shelfLifeLeft = item.shelfLifeLeft(today),
                    status = status,
                    size = 54.dp,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.scanner_item_summary,
                            formatQuantity(item.quantity),
                            stringResource(item.unit.labelRes),
                            stringResource(item.category.labelRes),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            if (item.isExpiryEstimated) R.string.scanner_item_expiry_estimated else R.string.scanner_item_expiry,
                            item.expiryDate.format(dateFormatter),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.freshnessPalette.forStatus(status).onContainer,
                    )
                }

                IconButton(onClick = onEdit, enabled = enabled) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.scanner_action_edit_item, item.name),
                    )
                }
                IconButton(onClick = onDelete, enabled = enabled) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.scanner_action_delete_item, item.name),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            if (warning != null) {
                SpoiledWarning(warning = warning, enabled = enabled, onKeep = onKeep, onDiscard = onDiscard)
            }
        }
    }
}

/** "⚠ Looks spoiled: mould on top", with the two ways out: bin it or keep it after all. */
@Composable
private fun SpoiledWarning(warning: String, enabled: Boolean, onKeep: () -> Unit, onDiscard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
    ) {
        Text(
            text = "⚠ $warning",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = stringResource(R.string.scanner_spoiled_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onKeep, enabled = enabled) { Text(stringResource(R.string.scanner_spoiled_keep)) }
            TextButton(onClick = onDiscard, enabled = enabled) {
                Text(stringResource(R.string.scanner_spoiled_discard), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
