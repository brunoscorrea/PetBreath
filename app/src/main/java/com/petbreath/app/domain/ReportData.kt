package com.petbreath.app.domain

import java.time.Instant

/** Framework-free snapshot of everything that goes into a vet report. */
data class ReportData(
    val generatedAt: Instant,
    val pet: ReportPet,
    val range: NormalRange,
    val periodStart: Instant?,
    val periodEnd: Instant,
    val measurements: List<ReportMeasurement>,
    val medications: List<ReportMedication>,
) {
    val stats: MeasurementStats by lazy { MeasurementStats.from(measurements.map { it.bpm }, range) }
}

data class ReportPet(
    val name: String,
    val species: String,
    val breed: String,
    val ageText: String,
    val weightText: String,
    val medicalNotes: String,
)

data class ReportMeasurement(
    val takenAt: Instant,
    val bpm: Int,
    val breathCount: Int?,
    val durationSeconds: Int?,
    val context: String,
    val notes: String,
)

data class ReportMedication(
    val name: String,
    val dosage: String,
    val instructions: String,
    val active: Boolean,
)
