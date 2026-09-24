package com.petbreath.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.petbreath.app.R
import com.petbreath.app.data.db.MeasurementContext
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.Species
import com.petbreath.app.data.db.normalRange
import com.petbreath.app.data.repository.MeasurementRepository
import com.petbreath.app.data.repository.MedicationRepository
import com.petbreath.app.data.settings.WeightUnit
import com.petbreath.app.domain.CsvReportBuilder
import com.petbreath.app.domain.PetAge
import com.petbreath.app.domain.ReportData
import com.petbreath.app.domain.ReportMeasurement
import com.petbreath.app.domain.ReportMedication
import com.petbreath.app.domain.ReportPet
import com.petbreath.app.domain.Weight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ReportFormat(val mimeType: String, val extension: String) {
    PDF("application/pdf", "pdf"),
    CSV("text/csv", "csv"),
}

/**
 * Writes reports to the app's private cache and exposes them through a
 * [FileProvider] so the user can share them with their vet. Nothing is
 * uploaded anywhere by the app itself.
 */
class ReportExporter(
    private val context: Context,
    private val measurements: MeasurementRepository,
    private val medications: MedicationRepository,
) {
    suspend fun export(
        pet: PetEntity,
        periodDays: Int?,
        format: ReportFormat,
        weightUnit: WeightUnit,
    ): File = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val start = periodDays?.let { now.minus(Duration.ofDays(it.toLong())) }
        val data = ReportData(
            generatedAt = now,
            pet = reportPet(context, pet, weightUnit, now),
            range = pet.normalRange,
            periodStart = start,
            periodEnd = now,
            measurements = measurements.listForPetSince(pet.id, start?.toEpochMilli() ?: 0L).map {
                ReportMeasurement(
                    takenAt = Instant.ofEpochMilli(it.takenAt),
                    bpm = it.bpm,
                    breathCount = it.breathCount,
                    durationSeconds = it.durationSeconds,
                    context = contextLabel(context, it.context),
                    notes = it.notes,
                )
            },
            medications = medications.listForPet(pet.id).map {
                ReportMedication(it.name, it.dosage, it.instructions, it.active)
            },
        )
        val dir = File(context.cacheDir, REPORT_DIR).apply { mkdirs() }
        // Only keep the latest report around; they can contain health information.
        dir.listFiles()?.forEach { it.delete() }
        val safeName = pet.name.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "pet" }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ROOT).format(now.atZone(zone))
        val file = File(dir, "PetBreath_${safeName}_$stamp.${format.extension}")
        when (format) {
            ReportFormat.CSV -> file.writeText(CsvReportBuilder.build(data, zone))
            ReportFormat.PDF -> PdfReportWriter(context).write(data, zone, file)
        }
        file
    }

    fun shareIntent(file: File, format: ReportFormat, petName: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(format.mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_share_subject, petName))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, context.getString(R.string.export_share_title))
    }

    companion object {
        private const val REPORT_DIR = "reports"

        fun speciesLabel(context: Context, species: Species): String = context.getString(
            when (species) {
                Species.DOG -> R.string.species_dog
                Species.CAT -> R.string.species_cat
                Species.OTHER -> R.string.species_other
            },
        )

        fun contextLabel(context: Context, value: MeasurementContext): String = context.getString(
            when (value) {
                MeasurementContext.SLEEPING -> R.string.context_sleeping
                MeasurementContext.RESTING -> R.string.context_resting
            },
        )

        fun ageText(context: Context, pet: PetEntity, now: Instant): String {
            val years = pet.ageYears ?: return ""
            val current = PetAge.currentAgeYears(years, Instant.ofEpochMilli(pet.ageRecordedAt ?: now.toEpochMilli()), now)
            val (y, m) = PetAge.yearsAndMonths(current)
            return when {
                y == 0 -> context.resources.getQuantityString(R.plurals.age_months, m, m)
                m == 0 -> context.resources.getQuantityString(R.plurals.age_years, y, y)
                else -> context.getString(
                    R.string.age_years_months,
                    context.resources.getQuantityString(R.plurals.age_years, y, y),
                    context.resources.getQuantityString(R.plurals.age_months, m, m),
                )
            }
        }

        fun weightText(context: Context, weightKg: Double?, unit: WeightUnit): String {
            weightKg ?: return ""
            return when (unit) {
                WeightUnit.KG -> context.getString(R.string.weight_kg, weightKg)
                WeightUnit.LB -> context.getString(R.string.weight_lb, Weight.kgToLb(weightKg))
            }
        }

        fun reportPet(context: Context, pet: PetEntity, unit: WeightUnit, now: Instant) = ReportPet(
            name = pet.name,
            species = speciesLabel(context, pet.species),
            breed = pet.breed,
            ageText = ageText(context, pet, now),
            weightText = weightText(context, pet.weightKg, unit),
            medicalNotes = pet.medicalNotes,
        )
    }
}
