package com.ektebrysjan.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ektebrysjan.workout.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(viewModel: WorkoutViewModel) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val empty = summary.weekSessions == 0 && summary.monthSessions == 0 &&
        summary.monthByType.isEmpty()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_summary)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (empty) {
                Text(
                    text = stringResource(R.string.summary_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
                return@Column
            }

            PeriodCard(
                title = stringResource(R.string.this_week),
                sessions = summary.weekSessions,
                minutes = summary.weekMinutes
            )
            PeriodCard(
                title = stringResource(R.string.this_month),
                sessions = summary.monthSessions,
                minutes = summary.monthMinutes
            )

            if (summary.monthByType.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.by_type_month),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            summary.monthByType.forEach { stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stat.type,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${stat.sessions} · ${formatDuration(stat.minutes)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(title: String, sessions: Int, minutes: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(
                label = stringResource(R.string.total_sessions),
                value = sessions.toString(),
                modifier = Modifier.weight(1f)
            )
            Stat(
                label = stringResource(R.string.total_time),
                value = formatDuration(minutes),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatDuration(minutes: Int): String =
    if (minutes >= 60) stringResource(R.string.hours_minutes, minutes / 60, minutes % 60)
    else stringResource(R.string.minutes_only, minutes)
