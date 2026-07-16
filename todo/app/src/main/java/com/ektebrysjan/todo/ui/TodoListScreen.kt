package com.ektebrysjan.todo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ektebrysjan.todo.R
import com.ektebrysjan.todo.data.TaskItem
import com.ektebrysjan.todo.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    onOpenTask: (taskId: Long, listId: Long) -> Unit
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val selectedList = lists.firstOrNull { it.id == selectedId }

    var showAddList by remember { mutableStateOf(false) }
    var showRenameList by remember { mutableStateOf(false) }
    var showDeleteList by remember { mutableStateOf(false) }
    var listMenuOpen by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }

    val active = tasks.filter { !it.isDone && !it.isArchived }
        .sortedWith(compareBy({ it.dueDate == null }, { it.dueDate }, { -it.createdAt }))
    val completed = tasks.filter { it.isDone && !it.isArchived }
    val archived = tasks.filter { it.isArchived }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.display_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.author_credit),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (selectedList != null) {
                        IconButton(onClick = { listMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = listMenuOpen,
                            onDismissRequest = { listMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename_list)) },
                                onClick = { listMenuOpen = false; showRenameList = true }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_list)) },
                                onClick = { listMenuOpen = false; showDeleteList = true }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedId != null) {
                FloatingActionButton(onClick = { onOpenTask(0L, selectedId!!) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // List selector chips + add-list chip.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                lists.forEach { list ->
                    FilterChip(
                        selected = list.id == selectedId,
                        onClick = { viewModel.selectList(list.id) },
                        label = { Text(list.name) }
                    )
                }
                AssistChip(
                    onClick = { showAddList = true },
                    label = { Text(stringResource(R.string.add_list)) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
            }

            if (selectedList == null) {
                EmptyMessage(stringResource(R.string.no_lists))
            } else if (tasks.isEmpty()) {
                EmptyMessage(stringResource(R.string.no_tasks))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(active, key = { it.id }) { task ->
                        TaskRow(task, viewModel, onOpenTask)
                    }

                    if (completed.isNotEmpty()) {
                        item {
                            SectionHeader(
                                text = "${stringResource(R.string.completed_header)} (${completed.size})",
                                expanded = showCompleted,
                                onToggle = { showCompleted = !showCompleted }
                            )
                        }
                        if (showCompleted) {
                            items(completed, key = { it.id }) { task ->
                                TaskRow(task, viewModel, onOpenTask)
                            }
                        }
                    }

                    if (archived.isNotEmpty()) {
                        item {
                            SectionHeader(
                                text = "${stringResource(R.string.archived_header)} (${archived.size})",
                                expanded = showArchived,
                                onToggle = { showArchived = !showArchived }
                            )
                        }
                        if (showArchived) {
                            items(archived, key = { it.id }) { task ->
                                TaskRow(task, viewModel, onOpenTask)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddList) {
        ListNameDialog(
            title = stringResource(R.string.add_list),
            onConfirm = { viewModel.addList(it); showAddList = false },
            onDismiss = { showAddList = false }
        )
    }
    if (showRenameList && selectedList != null) {
        ListNameDialog(
            title = stringResource(R.string.rename_list),
            initialName = selectedList.name,
            onConfirm = { viewModel.renameList(selectedList, it); showRenameList = false },
            onDismiss = { showRenameList = false }
        )
    }
    if (showDeleteList && selectedList != null) {
        DeleteListDialog(
            onConfirm = { viewModel.deleteList(selectedList.id); showDeleteList = false },
            onDismiss = { showDeleteList = false }
        )
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp)
    )
}

@Composable
private fun SectionHeader(text: String, expanded: Boolean, onToggle: () -> Unit) {
    Text(
        text = if (expanded) "▾ $text" else "▸ $text",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskItem,
    viewModel: TodoViewModel,
    onOpenTask: (Long, Long) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val overdue = !task.isDone && task.dueDate != null && task.dueDate < System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenTask(task.id, task.listId) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { viewModel.toggleDone(task) })
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (task.dueDate != null) {
                    Text(
                        text = DateUtils.prettyDate(task.dueDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            if (task.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(stringResource(if (task.isArchived) R.string.unarchive else R.string.archive))
                    },
                    onClick = { viewModel.setArchived(task, !task.isArchived); menuOpen = false }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = { viewModel.deleteTask(task); menuOpen = false }
                )
            }
        }
    }
}
