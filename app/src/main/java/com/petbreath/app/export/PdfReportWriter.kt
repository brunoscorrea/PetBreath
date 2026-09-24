package com.petbreath.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.petbreath.app.R
import com.petbreath.app.domain.RangeStatus
import com.petbreath.app.domain.ReportData
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Renders a printable A4 report with [PdfDocument]; no third-party libraries. */
class PdfReportWriter(private val context: Context) {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f
    private val contentWidth = pageWidth - 2 * margin
    private val footerTop = pageHeight - 56f

    private val titlePaint = textPaint(20f, bold = true, color = PRIMARY)
    private val headingPaint = textPaint(13f, bold = true, color = PRIMARY)
    private val bodyPaint = textPaint(10.5f)
    private val boldPaint = textPaint(10.5f, bold = true)
    private val smallPaint = textPaint(8.5f, color = Color.DKGRAY)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 0.8f }

    private val dateTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val shortDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun write(data: ReportData, zone: ZoneId, target: File) {
        val document = PdfDocument()
        val writer = PageWriter(document, data, zone)
        try {
            writer.drawHeader()
            writer.drawSummary()
            writer.drawChart()
            writer.drawMedications()
            writer.drawNotes()
            writer.drawTable()
            writer.finish()
            FileOutputStream(target).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private inner class PageWriter(
        private val document: PdfDocument,
        private val data: ReportData,
        private val zone: ZoneId,
    ) {
        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page
        private val canvas: Canvas get() = page.canvas
        private var y = 0f

        init {
            newPage()
        }

        private fun newPage() {
            if (pageNumber > 0) finishPage()
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = margin
        }

        private fun finishPage() {
            val disclaimer = context.getString(R.string.report_disclaimer)
            canvas.drawLine(margin, footerTop, pageWidth - margin, footerTop, linePaint)
            drawWrapped(disclaimer, smallPaint, margin, footerTop + 6f, contentWidth - 50f)
            val pageLabel = context.getString(R.string.report_page, pageNumber)
            canvas.drawText(pageLabel, pageWidth - margin - smallPaint.measureText(pageLabel), footerTop + 16f, smallPaint)
            document.finishPage(page)
        }

        fun finish() = finishPage()

        private fun ensureSpace(height: Float) {
            if (y + height > footerTop - 8f) newPage()
        }

        private fun text(value: String, paint: TextPaint, spacingAfter: Float = 4f) {
            val height = measureWrapped(value, paint, contentWidth)
            ensureSpace(height)
            drawWrapped(value, paint, margin, y, contentWidth)
            y += height + spacingAfter
        }

        private fun heading(value: String) {
            ensureSpace(40f)
            y += 10f
            text(value, headingPaint, spacingAfter = 6f)
        }

        fun drawHeader() {
            text(context.getString(R.string.report_title), titlePaint, spacingAfter = 2f)
            text(
                context.getString(
                    R.string.report_generated,
                    dateTimeFormat.format(data.generatedAt.atZone(zone)),
                ),
                smallPaint,
                spacingAfter = 10f,
            )
            val pet = data.pet
            text(pet.name, textPaint(15f, bold = true), spacingAfter = 2f)
            listOf(
                context.getString(R.string.report_species) to listOf(pet.species, pet.breed).filter { it.isNotBlank() }.joinToString(" · "),
                context.getString(R.string.report_age) to pet.ageText,
                context.getString(R.string.report_weight) to pet.weightText,
            ).filter { it.second.isNotBlank() }.forEach { (label, value) -> labelValue(label, value) }

            val period = if (data.periodStart == null) {
                context.getString(R.string.report_period_all)
            } else {
                "${dateFormat.format(data.periodStart.atZone(zone))} – ${dateFormat.format(data.periodEnd.atZone(zone))}"
            }
            labelValue(context.getString(R.string.report_period), period)
            val range = data.range
            val rangeText = if (range.lowerBpm != null) {
                context.getString(R.string.range_between, range.lowerBpm, range.upperBpm)
            } else {
                context.getString(R.string.range_up_to, range.upperBpm)
            }
            labelValue(context.getString(R.string.report_normal_range), rangeText)
        }

        private fun labelValue(label: String, value: String) {
            ensureSpace(16f)
            canvas.drawText("$label:", margin, y + 10.5f, boldPaint)
            val offset = 110f
            val height = measureWrapped(value, bodyPaint, contentWidth - offset)
            drawWrapped(value, bodyPaint, margin + offset, y, contentWidth - offset)
            y += maxOf(height, 14f) + 2f
        }

        fun drawSummary() {
            heading(context.getString(R.string.report_summary))
            val s = data.stats
            if (s.count == 0) {
                text(context.getString(R.string.report_no_measurements), bodyPaint)
                return
            }
            labelValue(context.getString(R.string.report_readings), s.count.toString())
            labelValue(context.getString(R.string.report_average), context.getString(R.string.bpm_value, s.averageBpm))
            labelValue(
                context.getString(R.string.report_min_max),
                "${context.getString(R.string.bpm_value, s.minBpm)} / ${context.getString(R.string.bpm_value, s.maxBpm)}",
            )
            labelValue(context.getString(R.string.report_above_range), s.aboveRangeCount.toString())
            if (data.range.lowerBpm != null) {
                labelValue(context.getString(R.string.report_below_range), s.belowRangeCount.toString())
            }
        }

        fun drawChart() {
            val points = data.measurements.sortedBy { it.takenAt }
            if (points.size < 2) return
            heading(context.getString(R.string.report_trend))
            val chartHeight = 170f
            ensureSpace(chartHeight + 20f)
            val left = margin + 28f
            val right = pageWidth - margin
            val top = y
            val bottom = y + chartHeight

            val maxBpm = maxOf(points.maxOf { it.bpm }, data.range.upperBpm) + 5
            val minBpm = (minOf(points.minOf { it.bpm }, data.range.lowerBpm ?: Int.MAX_VALUE) - 5).coerceAtLeast(0)
            val t0 = points.first().takenAt.toEpochMilli()
            val t1 = points.last().takenAt.toEpochMilli().coerceAtLeast(t0 + 1)
            fun xOf(t: Long) = left + (t - t0).toFloat() / (t1 - t0) * (right - left)
            fun yOf(bpm: Int) = bottom - (bpm - minBpm).toFloat() / (maxBpm - minBpm) * chartHeight

            val axis = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; strokeWidth = 1f }
            canvas.drawLine(left, top, left, bottom, axis)
            canvas.drawLine(left, bottom, right, bottom, axis)
            canvas.drawText(maxBpm.toString(), margin, top + 8f, smallPaint)
            canvas.drawText(minBpm.toString(), margin, bottom, smallPaint)

            val threshold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ALERT; strokeWidth = 1.2f; style = Paint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            listOfNotNull(data.range.upperBpm, data.range.lowerBpm).forEach { limit ->
                val ly = yOf(limit)
                canvas.drawLine(left, ly, right, ly, threshold)
                canvas.drawText(limit.toString(), margin, ly + 3f, textPaint(8.5f, color = ALERT))
            }

            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PRIMARY; strokeWidth = 1.6f; style = Paint.Style.STROKE }
            val path = Path()
            points.forEachIndexed { i, m ->
                val px = xOf(m.takenAt.toEpochMilli()); val py = yOf(m.bpm)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            canvas.drawPath(path, line)
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            points.forEach { m ->
                dot.color = if (data.range.classify(m.bpm) == RangeStatus.NORMAL) PRIMARY else ALERT
                canvas.drawCircle(xOf(m.takenAt.toEpochMilli()), yOf(m.bpm), 2.6f, dot)
            }
            val startLabel = shortDate.format(points.first().takenAt.atZone(zone))
            val endLabel = shortDate.format(points.last().takenAt.atZone(zone))
            canvas.drawText(startLabel, left, bottom + 11f, smallPaint)
            canvas.drawText(endLabel, right - smallPaint.measureText(endLabel), bottom + 11f, smallPaint)
            y = bottom + 20f
        }

        fun drawMedications() {
            if (data.medications.isEmpty()) return
            heading(context.getString(R.string.report_medications))
            data.medications.forEach { med ->
                val status = if (med.active) "" else " (${context.getString(R.string.medication_inactive)})"
                val details = listOf(med.dosage, med.instructions).filter { it.isNotBlank() }.joinToString(" — ")
                text("• ${med.name}$status" + if (details.isNotEmpty()) ": $details" else "", bodyPaint, spacingAfter = 2f)
            }
        }

        fun drawNotes() {
            if (data.pet.medicalNotes.isBlank()) return
            heading(context.getString(R.string.report_medical_notes))
            text(data.pet.medicalNotes, bodyPaint)
        }

        fun drawTable() {
            if (data.measurements.isEmpty()) return
            heading(context.getString(R.string.report_measurements))
            val columns = listOf(
                context.getString(R.string.report_col_date) to 70f,
                context.getString(R.string.report_col_time) to 50f,
                context.getString(R.string.report_col_bpm) to 40f,
                context.getString(R.string.report_col_status) to 75f,
                context.getString(R.string.report_col_context) to 60f,
                context.getString(R.string.report_col_notes) to contentWidth - 295f,
            )
            fun header() {
                var x = margin
                columns.forEach { (title, width) ->
                    canvas.drawText(title, x, y + 10f, boldPaint)
                    x += width
                }
                y += 14f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 3f
            }
            header()
            data.measurements.sortedByDescending { it.takenAt }.forEach { m ->
                if (y + 15f > footerTop - 8f) {
                    newPage()
                    header()
                }
                val local = m.takenAt.atZone(zone)
                val status = data.range.classify(m.bpm)
                val values = listOf(
                    shortDate.format(local),
                    timeFormat.format(local),
                    m.bpm.toString(),
                    statusText(status),
                    m.context,
                    m.notes.replace('\n', ' '),
                )
                var x = margin
                values.forEachIndexed { i, value ->
                    val width = columns[i].second
                    val paint = if (i == 3 && status != RangeStatus.NORMAL) textPaint(10.5f, bold = true, color = ALERT) else bodyPaint
                    val clipped = TextUtils.ellipsize(value, paint, width - 6f, TextUtils.TruncateAt.END).toString()
                    canvas.drawText(clipped, x, y + 10f, paint)
                    x += width
                }
                y += 15f
            }
        }

        private fun statusText(status: RangeStatus) = context.getString(
            when (status) {
                RangeStatus.NORMAL -> R.string.status_normal
                RangeStatus.ABOVE -> R.string.status_above
                RangeStatus.BELOW -> R.string.status_below
            },
        )

        private fun drawWrapped(value: String, paint: TextPaint, x: Float, top: Float, width: Float) {
            val layout = layout(value, paint, width)
            canvas.save()
            canvas.translate(x, top)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private fun measureWrapped(value: String, paint: TextPaint, width: Float): Float =
        layout(value, paint, width).height.toFloat()

    private fun layout(value: String, paint: TextPaint, width: Float): StaticLayout =
        StaticLayout.Builder.obtain(value, 0, value.length, paint, width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private companion object {
        val PRIMARY = Color.rgb(0x1E, 0x5F, 0x74)
        val ALERT = Color.rgb(0xB3, 0x26, 0x1E)
    }
}
