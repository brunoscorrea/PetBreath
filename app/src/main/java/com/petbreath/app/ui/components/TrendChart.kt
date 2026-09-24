package com.petbreath.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.petbreath.app.R
import com.petbreath.app.domain.MeasurementStats
import com.petbreath.app.domain.NormalRange
import com.petbreath.app.domain.RangeStatus
import com.petbreath.app.ui.theme.LocalStatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class ChartPoint(val time: Instant, val bpm: Int)

/**
 * Simple line chart of breaths/minute over time with the normal-range limits
 * drawn as dashed lines. Screen readers get a text summary instead of the plot.
 */
@Composable
fun TrendChart(
    points: List<ChartPoint>,
    range: NormalRange,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(points) { points.sortedBy { it.time } }
    val stats = remember(sorted, range) { MeasurementStats.from(sorted.map { it.bpm }, range) }
    val summary = if (stats.count == 0) {
        stringResource(R.string.chart_empty)
    } else {
        stringResource(
            R.string.chart_summary,
            stats.count, stats.averageBpm ?: 0, stats.minBpm ?: 0, stats.maxBpm ?: 0, stats.aboveRangeCount,
        )
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val status = LocalStatusColors.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.merge(TextStyle(color = labelColor))
    val limitStyle = MaterialTheme.typography.labelSmall.merge(TextStyle(color = status.above))
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault()) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = summary },
    ) {
        if (sorted.isEmpty()) return@Canvas
        val leftPad = 32.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val topPad = 8.dp.toPx()
        val chartLeft = leftPad
        val chartRight = size.width - 8.dp.toPx()
        val chartTop = topPad
        val chartBottom = size.height - bottomPad

        val maxBpm = maxOf(sorted.maxOf { it.bpm }, range.upperBpm) + 5
        val minBpm = (minOf(sorted.minOf { it.bpm }, range.lowerBpm ?: Int.MAX_VALUE) - 5).coerceAtLeast(0)
        val t0 = sorted.first().time.toEpochMilli()
        val t1 = sorted.last().time.toEpochMilli()
        val span = (t1 - t0).coerceAtLeast(1)

        fun x(t: Instant): Float =
            if (sorted.size == 1) (chartLeft + chartRight) / 2
            else chartLeft + (t.toEpochMilli() - t0).toFloat() / span * (chartRight - chartLeft)
        fun y(bpm: Int): Float = chartBottom - (bpm - minBpm).toFloat() / (maxBpm - minBpm) * (chartBottom - chartTop)

        drawLine(axisColor, Offset(chartLeft, chartTop), Offset(chartLeft, chartBottom), strokeWidth = 1.dp.toPx())
        drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), strokeWidth = 1.dp.toPx())
        drawText(textMeasurer, maxBpm.toString(), Offset(0f, chartTop - 6.dp.toPx()), labelStyle)
        drawText(textMeasurer, minBpm.toString(), Offset(0f, chartBottom - 12.dp.toPx()), labelStyle)

        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        listOfNotNull(range.upperBpm, range.lowerBpm).forEach { limit ->
            val ly = y(limit)
            drawLine(status.above, Offset(chartLeft, ly), Offset(chartRight, ly), strokeWidth = 1.5.dp.toPx(), pathEffect = dash)
            drawText(textMeasurer, limit.toString(), Offset(0f, ly - 8.dp.toPx()), limitStyle)
        }

        if (sorted.size > 1) {
            val path = Path()
            sorted.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(p.time), y(p.bpm)) else path.lineTo(x(p.time), y(p.bpm)) }
            drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx()))
        }
        sorted.forEach { p ->
            val color = if (range.classify(p.bpm) == RangeStatus.NORMAL) lineColor else status.above
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(x(p.time), y(p.bpm)))
        }

        val start = dateFormat.format(sorted.first().time)
        val end = dateFormat.format(sorted.last().time)
        drawText(textMeasurer, start, Offset(chartLeft, chartBottom + 4.dp.toPx()), labelStyle)
        if (sorted.size > 1) {
            val endWidth = textMeasurer.measure(end, labelStyle).size.width
            drawText(textMeasurer, end, Offset(chartRight - endWidth, chartBottom + 4.dp.toPx()), labelStyle)
        }
    }
}
