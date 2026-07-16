package com.ektebrysjan.todo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ektebrysjan.todo.R
import com.ektebrysjan.todo.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    viewModel: TodoViewModel,
    taskId: Long,
    listId: Long,
    onDone: () -> Unit
) {
    val isNew = taskId == 0L

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    // -1 means "no due date" (mutableLongState can't hold null).
    var dueDate by remember { mutableLongStateOf(-1L) }
    var loaded by remember { mutableStateOf(isNew) }
    var showPicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (!isNew) {
            viewModel.getTask(taskId)?.let { t ->
                title = t.title
                note = t.note
                dueDate = t.dueDate ?: -1L
            }
            loaded = true
        }
    }

    fun save() {
        viewModel.saveTask(
            taskId = taskId,
            listId = listId,
            title = title,
            note = note,
            dueDate = dueDate.takeIf { it > 0 }
        )
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.new_task else R.string.edit_task)) },
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
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.title_hint)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )

            // Due date row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Text(
                        text = if (dueDate > 0) DateUtils.prettyDate(dueDate)
                        else stringResource(R.string.set_due_date),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (dueDate > 0) {
                    TextButton(onClick = { dueDate = -1L }) {
                        Text(stringResource(R.string.clear_due_date))
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(stringResource(R.string.note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.takeIf { it > 0 }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dueDate = it }
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
            title = { Text(stringResource(R.string.delete_task_title)) },
            text = { Text(stringResource(R.string.delete_task_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTaskById(taskId)
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
