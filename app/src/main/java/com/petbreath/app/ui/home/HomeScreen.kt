package com.petbreath.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petbreath.app.R
import com.petbreath.app.data.db.MeasurementEntity
import com.petbreath.app.data.db.PetEntity
import com.petbreath.app.data.db.normalRange
import com.petbreath.app.data.repository.MeasurementRepository
import com.petbreath.app.data.repository.PetRepository
import com.petbreath.app.ui.appViewModelFactory
import com.petbreath.app.ui.components.DisclaimerBanner
import com.petbreath.app.ui.components.EmptyState
import com.petbreath.app.ui.components.PetBreathTopBar
import com.petbreath.app.ui.components.SpeciesIcon
import com.petbreath.app.ui.components.StatusChip
import com.petbreath.app.ui.components.speciesLabel
import com.petbreath.app.ui.formatRelativeDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PetSummary(val pet: PetEntity, val latest: MeasurementEntity?)

data class HomeUiState(val loading: Boolean = true, val pets: List<PetSummary> = emptyList())

class HomeViewModel(pets: PetRepository, measurements: MeasurementRepository) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        pets.observePets(),
        measurements.observeLatestPerPet(),
    ) { petList, latest ->
        val byPet = latest.associateBy { it.petId }
        HomeUiState(loading = false, pets = petList.map { PetSummary(it, byPet[it.id]) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val Factory = appViewModelFactory { c, _ -> HomeViewModel(c.petRepository, c.measurementRepository) }
    }
}

@Composable
fun HomeScreen(
    onAddPet: () -> Unit,
    onOpenPet: (Long) -> Unit,
    onMeasure: (Long) -> Unit,
    onLearn: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            PetBreathTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onLearn) {
                        Icon(Icons.Filled.School, contentDescription = stringResource(R.string.learn_title))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.pets.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddPet,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_pet)) },
                )
            }
        },
    ) { padding ->
        if (!state.loading && state.pets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.Pets,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                ) {
                    Button(onClick = onAddPet) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_pet))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.pets, key = { it.pet.id }) { summary ->
                    PetCard(summary, onOpen = { onOpenPet(summary.pet.id) }, onMeasure = { onMeasure(summary.pet.id) })
                }
                item { DisclaimerBanner(Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
private fun PetCard(summary: PetSummary, onOpen: () -> Unit, onMeasure: () -> Unit) {
    val pet = summary.pet
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { SpeciesIcon(pet.species, Modifier.size(28.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(pet.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(speciesLabel(pet.species), pet.breed).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            val latest = summary.latest
            if (latest == null) {
                Text(stringResource(R.string.home_no_measurements), style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.bpm_value, latest.bpm),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.home_last_measured, formatRelativeDateTime(latest.takenAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusChip(pet.normalRange.classify(latest.bpm))
                }
            }
            Spacer(Modifier.size(12.dp))
            Button(onClick = onMeasure, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Filled.Air, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_measure_pet, pet.name))
            }
        }
    }
}
