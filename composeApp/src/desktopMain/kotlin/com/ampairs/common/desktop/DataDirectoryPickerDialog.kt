package com.ampairs.common.desktop

import OperatingSystem
import currentOperatingSystem
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

/**
 * Dialog that prompts the user to select a data directory on first launch.
 *
 * This prevents the app from using default system directories (AppData, user home)
 * which can be restored to old versions during system recovery, causing database
 * version conflicts.
 */
@Composable
fun DataDirectoryPickerDialog(
    onDirectorySelected: (File) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var isSelecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dialogState = rememberDialogState(width = 600.dp, height = 400.dp)

    DialogWindow(
        onCloseRequest = { onDismiss?.invoke() },
        title = "Select Data Directory - Ampairs",
        state = dialogState,
        resizable = false
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Welcome to Ampairs",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Warning icon - using a simple text icon instead
                Text(
                    text = "📁",
                    style = MaterialTheme.typography.displayMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Explanation
                Text(
                    text = "Please select a directory where Ampairs will store its data (databases, preferences, and cache).",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "⚠️ Important: Do not use system folders or network drives. Choose a local directory that won't be affected by system restore operations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Recommended locations
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recommended locations:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getRecommendedLocations(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Selected path display
                if (selectedPath != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Selected directory:",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = selectedPath!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    Button(
                        onClick = {
                            isSelecting = true
                            errorMessage = null
                        },
                        enabled = !isSelecting
                    ) {
                        Text(if (selectedPath == null) "Choose Directory" else "Change Directory")
                    }

                    Button(
                        onClick = {
                            selectedPath?.let { path ->
                                val dir = File(path)
                                if (DataDirectoryManager.setDataDirectory(dir)) {
                                    onDirectorySelected(dir)
                                } else {
                                    errorMessage = "Failed to set data directory. Please choose another location."
                                }
                            } ?: run {
                                errorMessage = "Please select a directory first"
                            }
                        },
                        enabled = selectedPath != null && !isSelecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }

    // File chooser launcher
    LaunchedEffect(isSelecting) {
        if (isSelecting) {
            try {
                // Call directly - LaunchedEffect already runs on Main (Swing EDT)
                val result = showDirectoryChooser()
                if (result != null) {
                    selectedPath = result.absolutePath
                    println("DataDirectoryPicker: Selected path: ${result.absolutePath}")
                } else {
                    println("DataDirectoryPicker: No directory selected")
                }
            } catch (e: Exception) {
                errorMessage = "Error selecting directory: ${e.message}"
                e.printStackTrace()
            } finally {
                isSelecting = false
            }
        }
    }
}

private fun showDirectoryChooser(): File? {
    println("DataDirectoryPicker: showDirectoryChooser() called")

    // Set system look and feel for native appearance
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        println("DataDirectoryPicker: LAF set to ${UIManager.getLookAndFeel().name}")
    } catch (e: Exception) {
        println("DataDirectoryPicker: Failed to set LAF: ${e.message}")
    }

    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select Data Directory for Ampairs"

        // Set initial directory to user's Documents folder
        val documentsDir = when (currentOperatingSystem) {
            OperatingSystem.Windows -> File(System.getProperty("user.home"), "Documents")
            OperatingSystem.MacOS -> File(System.getProperty("user.home"), "Documents")
            OperatingSystem.Linux -> File(System.getProperty("user.home"), "Documents")
            else -> File(System.getProperty("user.home"))
        }

        if (documentsDir.exists()) {
            currentDirectory = documentsDir
            println("DataDirectoryPicker: Initial directory set to: ${documentsDir.absolutePath}")
        }
    }

    println("DataDirectoryPicker: About to show dialog...")
    val result = chooser.showDialog(null, "Select")
    println("DataDirectoryPicker: Dialog result: $result")

    return when (result) {
        JFileChooser.APPROVE_OPTION -> {
            println("DataDirectoryPicker: File selected: ${chooser.selectedFile?.absolutePath}")
            chooser.selectedFile
        }
        else -> {
            println("DataDirectoryPicker: Dialog cancelled or closed")
            null
        }
    }
}

private fun getRecommendedLocations(): String {
    return when (currentOperatingSystem) {
        OperatingSystem.Windows -> """
            • Documents\Ampairs
            • D:\Ampairs (if you have a separate data drive)

            ❌ Avoid: C:\Users\...\AppData, C:\ProgramData
        """.trimIndent()

        OperatingSystem.MacOS -> """
            • ~/Documents/Ampairs
            • ~/Ampairs

            ❌ Avoid: ~/Library
        """.trimIndent()

        OperatingSystem.Linux -> """
            • ~/Documents/Ampairs
            • ~/ampairs-data

            ❌ Avoid: ~/.local, ~/.config
        """.trimIndent()

        else -> "Choose a local directory on your system"
    }
}
