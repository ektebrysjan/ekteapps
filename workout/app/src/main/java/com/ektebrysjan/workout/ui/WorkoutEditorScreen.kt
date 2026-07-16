package com.ektebrysjan.workout.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ektebrysjan.workout.R
import com.ektebrysjan.workout.data.WorkoutMeta
import com.ektebrysjan.workout.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    viewModel: WorkoutViewModel,
    entryId: Long,
    onDone: () -> Unit
) {
    val isNew = entryId == 0L

    var type by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf(WorkoutMeta.MEDIUM) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var loaded by remember { mutableStateOf(isNew) }
    var showPicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        if (!isNew) {
            viewModel.getEntry(entryId)?.let { e ->
                type = e.type
                duration = e.durationMinutes.toString()
                intensity = e.intensity
                note = e.note
                date = e.date
            }
            loaded = true
        }
    }

    fun save() {
        viewModel.saveWorkout(
            id = entryId,
            type = type,
            durationMinutes = duration.toIntOrNull() ?: 0,
            intensity = intensity,
            note = note,
            date = date
        )
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.new_workout else R.string.edit_workout)) },
                navigationIcon = {
                    IconButton(onClick = { save() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text(stringResource(R.string.type_label)) },
                placeholder = { Text(stringResource(R.string.type_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            // Type suggestions.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutMeta.TYPES.forEach { suggestion ->
                    FilterChip(
                        selected = type.equals(suggestion, ignoreCase = true),
                        onClick = { type = suggestion },
                        label = { Text(suggestion) }
                    )
                }
            }

            OutlinedTextField(
                value = duration,
                onValueChange = { new -> duration = new.filter { it.isDigit() }.take(4) },
                label = { Text(stringResource(R.string.duration_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Intensity selector.
            Text(
                text = stringResource(R.string.intensity_label),
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutMeta.INTENSITIES.forEach { level ->
                    FilterChip(
                        selected = intensity == level,
                        onClick = { intensity = level },
                        label = { Text(level) }
                    )
                }
            }

            // Date.
            OutlinedButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Text(
                    text = DateUtils.prettyDate(date),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(stringResource(R.string.note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it }
                    showPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) { DatePicker(state = state) }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_workout_title)) },
            text = { Text(stringResource(R.string.delete_workout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteById(entryId)
                    showDelete = false
                    onDone()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
