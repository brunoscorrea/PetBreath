package com.petbreath.app.ui.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petbreath.app.data.db.MedicationDoseEntity
import com.petbreath.app.data.db.MedicationEntity
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.ReminderType
import com.petbreath.app.data.repository.MedicationRepository
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.data.repository.ReminderRepository
import com.petbreath.app.ui.appViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration

data class MedicationItem(
    val medication: MedicationEntity,
    val reminderTimes: List<Pair<Int, Int>>,
    val recentDoses: List<MedicationDoseEntity>,
)

data class MedicationsUiState(
    val loading: Boolean = true,
    val pet: PetEntity? = null,
    val items: List<MedicationItem> = emptyList(),
)

class MedicationsViewModel(
    private val petId: Long,
    pets: PetRepository,
    private val medications: MedicationRepository,
    reminders: ReminderRepository,
    now: Long = System.currentTimeMillis(),
) : ViewModel() {

    val state: StateFlow<MedicationsUiState> = combine(
        pets.observePet(petId),
        medications.observeForPet(petId),
        reminders.observeForPet(petId),
        medications.observeDosesSince(petId, now - Duration.ofDays(DOSE_HISTORY_DAYS).toMillis()),
    ) { pet, meds, reminderList, doses ->
        val timesByMed = reminderList
            .filter { it.type == ReminderType.MEDICATION && it.medicationId != null }
            .groupBy({ it.medicationId!! }, { it.hour to it.minute })
        val dosesByMed = doses.groupBy { it.medicationId }
        MedicationsUiState(
            loading = false,
            pet = pet,
            items = meds.map { med ->
                MedicationItem(
                    medication = med,
                    reminderTimes = timesByMed[med.id].orEmpty().sortedWith(compareBy({ it.first }, { it.second })),
                    recentDoses = dosesByMed[med.id].orEmpty(),
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedicationsUiState())

    fun save(medication: MedicationEntity, times: List<Pair<Int, Int>>) = viewModelScope.launch {
        medications.save(
            if (medication.id == 0L) medication.copy(petId = petId, createdAt = System.currentTimeMillis()) else medication,
            times,
        )
    }

    fun delete(medicationId: Long) = viewModelScope.launch { medications.delete(medicationId) }

    fun markGiven(medicationId: Long) = viewModelScope.launch {
        medications.logDose(medicationId, System.currentTimeMillis())
    }

    fun undoDose(doseId: Long) = viewModelScope.launch { medications.deleteDose(doseId) }

    companion object {
        const val ARG_PET_ID = "petId"
        const val DOSE_HISTORY_DAYS = 7L

        val Factory = appViewModelFactory { c, handle ->
            MedicationsViewModel(
                petId = checkNotNull(handle.get<Long>(ARG_PET_ID)),
                pets = c.petRepository,
                medications = c.medicationRepository,
                reminders = c.reminderRepository,
            )
        }
    }
}
