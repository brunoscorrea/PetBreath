package com.petbreath.app.ui.learn

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petbreath.app.R
import com.petbreath.app.ui.components.PetBreathTopBar

private data class LearnSection(@StringRes val title: Int, @StringRes val body: Int, val urgent: Boolean = false)

private val sections = listOf(
    LearnSection(R.string.learn_what_title, R.string.learn_what_body),
    LearnSection(R.string.learn_why_title, R.string.learn_why_body),
    LearnSection(R.string.learn_how_title, R.string.learn_how_body),
    LearnSection(R.string.learn_normal_title, R.string.learn_normal_body),
    LearnSection(R.string.learn_contact_title, R.string.learn_contact_body),
    LearnSection(R.string.learn_emergency_title, R.string.learn_emergency_body, urgent = true),
    LearnSection(R.string.learn_disclaimer_title, R.string.disclaimer_full),
)

@Composable
fun LearnScreen(onBack: () -> Unit) {
    Scaffold(topBar = { PetBreathTopBar(stringResource(R.string.learn_title), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sections.forEach { section ->
                Card(
                    colors = if (section.urgent) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    } else {
                        CardDefaults.cardColors()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(section.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(stringResource(section.body), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
