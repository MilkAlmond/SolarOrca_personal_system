package com.yujing.tool.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yujing.tool.data.ApiException
import com.yujing.tool.data.Formats
import com.yujing.tool.data.NoteItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun NotesScreen(viewModel: AppViewModel) {
    var editorNote by remember { mutableStateOf<NoteItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var openId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(openId) {
        val id = openId ?: return@LaunchedEffect
        try {
            editorNote = viewModel.loadNote(id)
        } catch (ex: Exception) {
            // The list keeps showing; the snackbar is triggered by the view model next sync.
        } finally {
            openId = null
        }
    }

    if (editorNote != null || creating) {
        NoteEditor(
            viewModel = viewModel,
            initial = editorNote,
            onClose = {
                editorNote = null
                creating = false
            },
        )
        return
    }

    var keyword by remember { mutableStateOf(viewModel.noteKeyword) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("按标题搜索") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { scope.launch { viewModel.searchNotes(keyword) } }) {
                    Text("搜索")
                }
            }
            HorizontalDivider()
            if (viewModel.notes.isEmpty()) {
                EmptyHint("还没有笔记。\n点击右下角新建，停止输入约 2 秒会自动保存。")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.notes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openId = note.id },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        note.title.ifBlank { "无标题笔记" },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        "更新于 ${Formats.dateTime(note.updatedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteNote(note) },
                                    enabled = !viewModel.readOnly,
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除笔记")
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建笔记")
        }
    }
}

@Composable
private fun NoteEditor(
    viewModel: AppViewModel,
    initial: NoteItem?,
    onClose: () -> Unit,
) {
    var noteId by remember { mutableStateOf(initial?.id) }
    var version by remember { mutableStateOf(initial?.version) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var dirty by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (initial == null) "" else "已保存") }
    var error by remember { mutableStateOf<String?>(null) }
    val mutex = remember { Mutex() }

    BackHandler { onClose() }

    LaunchedEffect(title, body) {
        if (!dirty || (title.isBlank() && body.isBlank())) {
            return@LaunchedEffect
        }
        delay(2000)
        val snapshotTitle = title
        val snapshotBody = body
        status = "保存中…"
        mutex.withLock {
            try {
                val saved = viewModel.saveNote(noteId, snapshotTitle, snapshotBody, version)
                noteId = saved.id
                version = saved.version
                status = "已保存"
                error = null
            } catch (ex: ApiException) {
                if (ex.versionConflict && noteId != null) {
                    try {
                        val latest = viewModel.loadNote(noteId!!)
                        version = latest.version
                        val saved = viewModel.saveNote(noteId, snapshotTitle, snapshotBody, version)
                        noteId = saved.id
                        version = saved.version
                        status = "已保存"
                        error = null
                    } catch (retry: Exception) {
                        status = "未保存"
                        error = retry.message
                    }
                } else {
                    status = "未保存"
                    error = ex.message
                }
            } catch (ex: Exception) {
                status = "未保存"
                error = ex.message
            }
        }
        if (title == snapshotTitle && body == snapshotBody) {
            dirty = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) { Text("返回列表") }
            Text(
                status,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                dirty = true
            },
            label = { Text("标题") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
        OutlinedTextField(
            value = body,
            onValueChange = {
                body = it
                dirty = true
            },
            label = { Text("正文") },
            minLines = 10,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )
        if (error != null) {
            Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Text(
            "停止输入约 2 秒后自动保存；标题为空时也会保存为无标题笔记。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
