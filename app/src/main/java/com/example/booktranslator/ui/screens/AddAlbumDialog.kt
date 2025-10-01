package com.example.booktranslator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddAlbumDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy album") },
        text = {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tytuł albumu") }
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(title) }) {
                Text("Utwórz")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun confirmDialog(title: String, message: String, onConfirm: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    showDialog = false
                }) { Text("Tak") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Nie") }
            }
        )
    }
}