package com.kevage.todowidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import java.io.IOException

private val colors = lightColorScheme(
    primary = Color(0xFF34456B),
    background = Color(0xFFF4F5FA),
    surface = Color.White,
    onSurface = Color(0xFF242A3B),
    onSurfaceVariant = Color(0xFF555F74),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TaskStore.get(this)
        setContent {
            MaterialTheme(colorScheme = colors) {
                TasksScreen(store) { TaskWidget().updateAll(this) }
            }
        }
    }
}

class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = TaskStore.get(this)
        setContent {
            MaterialTheme(colorScheme = colors) {
                TaskEditor(originalTitle = "", onCancel = ::finish) { title ->
                    store.add(title)
                    TaskWidget().updateAll(this@QuickAddActivity)
                    finish()
                }
            }
        }
    }
}

@Composable
private fun TasksScreen(store: TaskStore, refreshWidget: suspend () -> Unit) {
    val tasks by store.tasks.collectAsState()
    val scope = rememberCoroutineScope()
    val messages = remember { SnackbarHostState() }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val open = tasks.filterNot(Task::completed)
    val completed = tasks.filter(Task::completed)

    fun save(change: suspend () -> Unit, after: () -> Unit = {}) {
        scope.launch {
            try {
                change()
                refreshWidget()
                after()
            } catch (_: IOException) {
                messages.showSnackbar("Speichern fehlgeschlagen. Bitte erneut versuchen.")
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(messages) }) { insets ->
        LazyColumn(
            modifier = Modifier.padding(insets),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF242A3B),
                    contentColor = Color(0xFFF3F5FF),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Meine Aufgaben", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            when (open.size) {
                                0 -> "Keine offenen Aufgaben"
                                1 -> "1 Aufgabe offen"
                                else -> "${open.size} Aufgaben offen"
                            },
                            color = Color(0xFFCED6E8),
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { adding = true }) { Text("+  Aufgabe") }
                    }
                }
            }
            if (open.isNotEmpty()) {
                item { SectionTitle("Offen") }
                items(open, key = Task::id) { task ->
                    TaskCard(task, { save({ store.setCompleted(task.id, it) }) },
                        { editingId = task.id }, { deletingId = task.id })
                }
            }
            if (completed.isNotEmpty()) {
                item { SectionTitle("Erledigt") }
                items(completed, key = Task::id) { task ->
                    TaskCard(task, { save({ store.setCompleted(task.id, it) }) },
                        { editingId = task.id }, { deletingId = task.id })
                }
            }
        }
    }

    val editing = tasks.firstOrNull { it.id == editingId }
    if (adding || editing != null) {
        val close = {
            adding = false
            editingId = null
        }
        Dialog(onDismissRequest = close) {
            TaskEditor(editing?.title.orEmpty(), close) { title ->
                if (editing == null) store.add(title) else store.rename(editing.id, title)
                refreshWidget()
                close()
            }
        }
    }
    val deleting = tasks.firstOrNull { it.id == deletingId }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("Aufgabe löschen?") },
            text = { Text(deleting.title) },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Abbrechen") } },
            confirmButton = {
                TextButton(onClick = { save({ store.delete(deleting.id) }) { deletingId = null } }) {
                    Text("Löschen")
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun TaskCard(task: Task, onCheck: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val checkLabel = if (task.completed) "${task.title} wieder öffnen" else "${task.title} abhaken"
            Checkbox(
                checked = task.completed,
                onCheckedChange = onCheck,
                modifier = Modifier.semantics { contentDescription = checkLabel },
            )
            Text(
                task.title,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
            )
            Box {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.semantics { contentDescription = "Menü für ${task.title}" },
                ) { Text("⋯", style = MaterialTheme.typography.titleLarge) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Bearbeiten") }, onClick = {
                        menu = false
                        onEdit()
                    })
                    DropdownMenuItem(text = { Text("Löschen") }, onClick = {
                        menu = false
                        onDelete()
                    })
                }
            }
        }
    }
}

@Composable
private fun TaskEditor(originalTitle: String, onCancel: () -> Unit, onSave: suspend (String) -> Unit) {
    var title by rememberSaveable { mutableStateOf(originalTitle) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val trimmed = title.trim()

    fun submit() {
        if (saving || trimmed.isEmpty()) return
        saving = true
        saveFailed = false
        scope.launch {
            try {
                onSave(trimmed)
            } catch (_: IOException) {
                saveFailed = true
            } finally {
                saving = false
            }
        }
    }

    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (originalTitle.isEmpty()) "Neue Aufgabe" else "Aufgabe bearbeiten",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                label = { Text("Aufgabe") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                isError = saveFailed,
                supportingText = if (saveFailed) {
                    { Text("Speichern fehlgeschlagen. Bitte erneut versuchen.") }
                } else null,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel, enabled = !saving) { Text("Abbrechen") }
                Button(onClick = ::submit, enabled = trimmed.isNotEmpty() && !saving) { Text("Speichern") }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
