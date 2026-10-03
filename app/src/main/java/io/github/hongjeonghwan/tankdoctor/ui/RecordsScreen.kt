@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hongjeonghwan.tankdoctor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hongjeonghwan.tankdoctor.AppViewModel
import io.github.hongjeonghwan.tankdoctor.UiState
import io.github.hongjeonghwan.tankdoctor.data.LogCategory
import io.github.hongjeonghwan.tankdoctor.data.LogEntry

/** The whole care log, filterable by category; the home screen only shows the latest few. */
@Composable
fun RecordsScreen(state: UiState, vm: AppViewModel) {
    val visible = state.entries.filter { state.filter == null || it.category == state.filter }
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<LogEntry?>(null) }

    LaunchedEffect(state.toast) {
        val message = state.toast ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        vm.consumeToast()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("기록 ${visible.size}개", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = vm::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.startNewEntry(state.filter?.takeIf { it != LogCategory.DIAGNOSIS } ?: LogCategory.WATER) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("기록 추가", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "filter") { RecordFilterRow(state.filter, vm::setFilter) }
            if (visible.isEmpty()) {
                item(key = "empty") { EmptyRecords(filtered = state.filter != null) }
            }
            recordDays(visible, vm.photoDir, onOpen = vm::openEntry, onDelete = { pendingDelete = it })
        }
    }

    pendingDelete?.let { target ->
        DeleteEntryDialog(
            target,
            onConfirm = {
                vm.deleteEntry(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
