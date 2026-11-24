package com.shelfie.app.presentation.dashboard.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.shelfie.app.core.ui.R
import com.shelfie.app.presentation.common.item.EditItemSheet
import com.shelfie.app.presentation.dashboard.AddItemState
import com.shelfie.app.presentation.dashboard.DashboardIntent
import com.shelfie.app.presentation.theme.extendedColors

/** "Add product": the regular edit form plus a barcode button that pre-fills it. */
@Composable
fun AddItemSheet(state: AddItemState, onIntent: (DashboardIntent) -> Unit) {
    val scanBarcode = rememberBarcodeScanner(
        onScanned = { onIntent(DashboardIntent.BarcodeScanned(it)) },
        onUnavailable = { onIntent(DashboardIntent.BarcodeScannerUnavailable) },
    )

    EditItemSheet(
        item = state.draft,
        title = stringResource(R.string.add_item_title),
        onDismiss = { onIntent(DashboardIntent.DismissAddItem) },
        onSave = { name, quantity, unit, category, expiryDate, location ->
            onIntent(DashboardIntent.SaveNewItem(name, quantity, unit, category, expiryDate, location))
        },
        headerContent = {
            // A packaged product fills the whole form from its barcode; no AI quota used.
            Surface(
                onClick = scanBarcode,
                enabled = !state.isLookingUp,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.extendedColors.glassBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_barcode),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.add_item_scan_barcode),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.isLookingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            state.messageRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (messageRes == R.string.add_item_found) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
    )
}

/**
 * Google's code scanner: Play services shows its own camera UI, so the app needs no camera
 * permission. Returns a function that starts a scan; a cancelled scan does nothing.
 */
@Composable
private fun rememberBarcodeScanner(onScanned: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val scanner = remember(context) { createScanner(context) }
    return remember(scanner) {
        {
            scanner.startScan()
                .addOnSuccessListener { barcode -> barcode.rawValue?.let(currentOnScanned) }
                .addOnFailureListener { error ->
                    // CANCELLED is the user backing out, not a failure worth reporting.
                    if ((error as? MlKitException)?.errorCode != MlKitException.CANCELLED) currentOnUnavailable()
                }
        }
    }
}

private fun createScanner(context: Context): GmsBarcodeScanner =
    GmsBarcodeScanning.getClient(
        context,
        GmsBarcodeScannerOptions.Builder()
            // Product barcodes only; QR codes on packaging are usually marketing links.
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .enableAutoZoom()
            .build(),
    )
