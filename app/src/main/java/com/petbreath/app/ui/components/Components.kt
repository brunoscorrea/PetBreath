package com.petbreath.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petbreath.app.R
import com.petbreath.app.data.db.Species
import com.petbreath.app.domain.NormalRange
import com.petbreath.app.domain.RangeStatus
import com.petbreath.app.ui.theme.LocalStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetBreathTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = actions,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

data class StatusStyle(
    val container: Color,
    val content: Color,
    val accent: Color,
    val icon: ImageVector,
    @StringRes val label: Int,
)

@Composable
fun statusStyle(status: RangeStatus): StatusStyle {
    val c = LocalStatusColors.current
    return when (status) {
        RangeStatus.NORMAL -> StatusStyle(c.normalContainer, c.onNormalContainer, c.normal, Icons.Filled.CheckCircle, R.string.status_normal)
        RangeStatus.ABOVE -> StatusStyle(c.aboveContainer, c.onAboveContainer, c.above, Icons.Filled.Warning, R.string.status_above)
        RangeStatus.BELOW -> StatusStyle(c.belowContainer, c.onBelowContainer, c.below, Icons.AutoMirrored.Filled.TrendingDown, R.string.status_below)
    }
}

/** A compact pill that shows the range status with an icon and text (never color alone). */
@Composable
fun StatusChip(status: RangeStatus, modifier: Modifier = Modifier) {
    val style = statusStyle(status)
    Surface(color = style.container, contentColor = style.content, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(style.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(style.label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun rangeText(range: NormalRange): String =
    if (range.lowerBpm != null) {
        stringResource(R.string.range_between, range.lowerBpm, range.upperBpm)
    } else {
        stringResource(R.string.range_up_to, range.upperBpm)
    }

@Composable
fun speciesLabel(species: Species): String = stringResource(
    when (species) {
        Species.DOG -> R.string.species_dog
        Species.CAT -> R.string.species_cat
        Species.OTHER -> R.string.species_other
    },
)

@Composable
fun SpeciesIcon(species: Species, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    // A generic paw works for every species and keeps the UI calm and simple.
    Icon(Icons.Filled.Pets, contentDescription = speciesLabel(species), modifier = modifier, tint = tint)
}

/** Persistent, plain-language reminder that the app does not diagnose. */
@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.disclaimer_short), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action()
    }
}
