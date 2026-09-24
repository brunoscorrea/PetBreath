package com.petbreath.app.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.Species
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.settings.SettingsRepository
import com.petbreath.app.data.settings.WeightUnit
import com.petbreath.app.domain.NormalRange
import com.petbreath.app.domain.PetAge
import com.petbreath.app.domain.Weight
import com.petbreath.app.ui.appViewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale

/** Editable form state. Numeric fields are kept as text so partial input is preserved. */
data class PetForm(
    val name: String = "",
    val species: Species = Species.DOG,
    val breed: String = "",
    val age: String = "",
    val weight: String = "",
    val medicalNotes: String = "",
    val upperThreshold: String = NormalRange.DEFAULT_UPPER_BPM.toString(),
    val lowerThreshold: String = "",
)

enum class PetFormError { NAME_REQUIRED, AGE_INVALID, WEIGHT_INVALID, UPPER_INVALID, LOWER_INVALID }

data class PetEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val form: PetForm = PetForm(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val errors: Set<PetFormError> = emptySet(),
    val saving: Boolean = false,
)

object PetFormValidator {
    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun validate(form: PetForm): Set<PetFormError> = buildSet {
        if (form.name.isBlank()) add(PetFormError.NAME_REQUIRED)
        if (form.age.isNotBlank() && parseDecimal(form.age).let { it == null || it < 0 || it > 40 }) {
            add(PetFormError.AGE_INVALID)
        }
        if (form.weight.isNotBlank() && parseDecimal(form.weight).let { it == null || it <= 0 || it > 200 }) {
            add(PetFormError.WEIGHT_INVALID)
        }
        val upper = form.upperThreshold.trim().toIntOrNull()
        if (upper == null || upper !in NormalRange.MIN_CONFIGURABLE_BPM..NormalRange.MAX_CONFIGURABLE_BPM) {
            add(PetFormError.UPPER_INVALID)
        }
        if (form.lowerThreshold.isNotBlank()) {
            val lower = form.lowerThreshold.trim().toIntOrNull()
            if (lower == null || lower < 1 || (upper != null && lower >= upper)) add(PetFormError.LOWER_INVALID)
        }
    }
}

class PetEditViewModel(
    private val petId: Long?,
    private val pets: PetRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PetEditUiState(isNew = petId == null))
    val state: StateFlow<PetEditUiState> = _state.asStateFlow()

    private val _saved = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val saved: SharedFlow<Long> = _saved.asSharedFlow()

    private var original: PetEntity? = null

    init {
        viewModelScope.launch {
            val unit = settings.settings.first().weightUnit
            val pet = petId?.let { pets.getPet(it) }
            original = pet
            val form = pet?.let { p ->
                PetForm(
                    name = p.name,
                    species = p.species,
                    breed = p.breed,
                    age = p.ageYears?.let { years ->
                        val current = PetAge.currentAgeYears(years, Instant.ofEpochMilli(p.ageRecordedAt ?: 0), Instant.now())
                        format(current)
                    }.orEmpty(),
                    weight = p.weightKg?.let { format(if (unit == WeightUnit.KG) it else Weight.kgToLb(it)) }.orEmpty(),
                    medicalNotes = p.medicalNotes,
                    upperThreshold = p.upperThresholdBpm.toString(),
                    lowerThreshold = p.lowerThresholdBpm?.toString().orEmpty(),
                )
            } ?: PetForm()
            _state.update { it.copy(loading = false, form = form, weightUnit = unit) }
        }
    }

    fun update(transform: (PetForm) -> PetForm) {
        _state.update { s ->
            val form = transform(s.form)
            // Only re-validate fields that already had errors, so users aren't nagged while typing.
            s.copy(form = form, errors = if (s.errors.isEmpty()) s.errors else PetFormValidator.validate(form))
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.loading) return
        val errors = PetFormValidator.validate(s.form)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(saving = true) }
        val f = s.form
        val now = System.currentTimeMillis()
        val age = PetFormValidator.parseDecimal(f.age)
        val weightInput = PetFormValidator.parseDecimal(f.weight)
        val weightKg = weightInput?.let { if (s.weightUnit == WeightUnit.KG) it else Weight.lbToKg(it) }
        val base = original ?: PetEntity(name = "", species = f.species, createdAt = now)
        val pet = base.copy(
            name = f.name.trim(),
            species = f.species,
            breed = f.breed.trim(),
            ageYears = age,
            ageRecordedAt = age?.let { now },
            weightKg = weightKg,
            medicalNotes = f.medicalNotes.trim(),
            upperThresholdBpm = f.upperThreshold.trim().toInt(),
            lowerThresholdBpm = f.lowerThreshold.trim().toIntOrNull(),
        )
        viewModelScope.launch {
            val id = pets.save(pet)
            _saved.emit(id)
        }
    }

    private fun format(value: Double): String {
        val rounded = Math.round(value * 10) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else String.format(Locale.getDefault(), "%.1f", rounded)
    }

    companion object {
        const val ARG_PET_ID = "petId"

        val Factory = appViewModelFactory { c, handle ->
            PetEditViewModel(
                petId = handle.get<Long>(ARG_PET_ID)?.takeIf { it > 0 },
                pets = c.petRepository,
                settings = c.settingsRepository,
            )
        }
    }
}
