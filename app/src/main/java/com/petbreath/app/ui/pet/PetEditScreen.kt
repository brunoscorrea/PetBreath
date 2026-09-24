package com.petbreath.app.ui.pet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petbreath.app.R
import com.petbreath.app.data.db.Species
import com.petbreath.app.data.settings.WeightUnit
import com.petbreath.app.domain.NormalRange
import com.petbreath.app.ui.components.PetBreathTopBar
import com.petbreath.app.ui.components.SectionHeader
import com.petbreath.app.ui.components.speciesLabel

@Composable
fun PetEditScreen(
    onBack: () -> Unit,
    onSaved: (petId: Long, isNew: Boolean) -> Unit,
    viewModel: PetEditViewModel = viewModel(factory = PetEditViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { id -> currentOnSaved(id, viewModel.state.value.isNew) }
    }
    val form = state.form
    val errors = state.errors

    Scaffold(
        topBar = {
            PetBreathTopBar(
                title = stringResource(if (state.isNew) R.string.pet_add_title else R.string.pet_edit_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v.take(60)) } },
                label = { Text(stringResource(R.string.field_name)) },
                isError = PetFormError.NAME_REQUIRED in errors,
                supportingText = errorText(PetFormError.NAME_REQUIRED in errors, R.string.error_name_required),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.field_species), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Species.entries.forEachIndexed { i, species ->
                    SegmentedButton(
                        selected = form.species == species,
                        onClick = { viewModel.update { it.copy(species = species) } },
                        shape = SegmentedButtonDefaults.itemShape(i, Species.entries.size),
                    ) { Text(speciesLabel(species)) }
                }
            }
            OutlinedTextField(
                value = form.breed,
                onValueChange = { v -> viewModel.update { it.copy(breed = v.take(60)) } },
                label = { Text(stringResource(R.string.field_breed)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.age,
                    onValueChange = { v -> viewModel.update { it.copy(age = v.take(5)) } },
                    label = { Text(stringResource(R.string.field_age_years)) },
                    isError = PetFormError.AGE_INVALID in errors,
                    supportingText = errorText(PetFormError.AGE_INVALID in errors, R.string.error_age),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.weight,
                    onValueChange = { v -> viewModel.update { it.copy(weight = v.take(6)) } },
                    label = {
                        Text(
                            stringResource(
                                if (state.weightUnit == WeightUnit.KG) R.string.field_weight_kg else R.string.field_weight_lb,
                            ),
                        )
                    },
                    isError = PetFormError.WEIGHT_INVALID in errors,
                    supportingText = errorText(PetFormError.WEIGHT_INVALID in errors, R.string.error_weight),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = form.medicalNotes,
                onValueChange = { v -> viewModel.update { it.copy(medicalNotes = v.take(2000)) } },
                label = { Text(stringResource(R.string.field_medical_notes)) },
                placeholder = { Text(stringResource(R.string.field_medical_notes_hint)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(stringResource(R.string.pet_range_section))
            Text(
                stringResource(R.string.pet_range_help, NormalRange.DEFAULT_UPPER_BPM),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.upperThreshold,
                    onValueChange = { v -> viewModel.update { it.copy(upperThreshold = v.filter(Char::isDigit).take(3)) } },
                    label = { Text(stringResource(R.string.field_upper_threshold)) },
                    isError = PetFormError.UPPER_INVALID in errors,
                    supportingText = errorText(
                        PetFormError.UPPER_INVALID in errors,
                        R.string.error_upper,
                        NormalRange.MIN_CONFIGURABLE_BPM,
                        NormalRange.MAX_CONFIGURABLE_BPM,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.lowerThreshold,
                    onValueChange = { v -> viewModel.update { it.copy(lowerThreshold = v.filter(Char::isDigit).take(3)) } },
                    label = { Text(stringResource(R.string.field_lower_threshold)) },
                    isError = PetFormError.LOWER_INVALID in errors,
                    supportingText = errorText(PetFormError.LOWER_INVALID in errors, R.string.error_lower),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}

private fun errorText(show: Boolean, res: Int, vararg args: Any): (@Composable () -> Unit)? =
    if (show) {
        { Text(stringResource(res, *args)) }
    } else {
        null
    }
