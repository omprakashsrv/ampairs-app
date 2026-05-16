package com.ampairs.network.security

import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.Desktop
import java.net.URI
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JFrame
import javax.swing.SwingConstants
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Dimension
import kotlin.coroutines.resume
import kotlin.system.exitProcess

class DesktopAppUpdateEnforcer : AppUpdateEnforcer {
    
    private var allowNetworkRequests = true
    
    override suspend fun showUpdateDialog(status: AppUpdateStatus) {
        when (status) {
            is AppUpdateStatus.Required -> showRequiredUpdateDialog()
            is AppUpdateStatus.Recommended -> showRecommendedUpdateDialog(status.reason)
            AppUpdateStatus.NotRequired -> { /* No action needed */ }
        }
    }
    
    override suspend fun enforceUpdate() {
        allowNetworkRequests = false
        showRequiredUpdateDialog()
    }
    
    override suspend fun shouldAllowNetworkRequests(): Boolean {
        return allowNetworkRequests
    }
    
    private suspend fun showRequiredUpdateDialog() = suspendCancellableCoroutine<Unit> { continuation ->
        val dialog = JDialog(null as JFrame?, "App Update Required", true)
        dialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
        dialog.isResizable = false
        
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(400, 150)
        
        val messageLabel = JLabel(
            "<html><center>" +
            "This version of the app is no longer supported due to security updates.<br>" +
            "Please update to the latest version to continue using the app." +
            "</center></html>",
            SwingConstants.CENTER
        )
        mainPanel.add(messageLabel, BorderLayout.CENTER)
        
        val buttonPanel = JPanel(FlowLayout())
        
        val updateButton = JButton("Update Now")
        updateButton.addActionListener {
            openUpdateWebsite()
            dialog.dispose()
            continuation.resume(Unit)
        }
        
        val exitButton = JButton("Exit")
        exitButton.addActionListener {
            dialog.dispose()
            exitProcess(0)
        }
        
        buttonPanel.add(updateButton)
        buttonPanel.add(exitButton)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        dialog.contentPane.add(mainPanel)
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
        
        continuation.invokeOnCancellation {
            dialog.dispose()
        }
    }
    
    private suspend fun showRecommendedUpdateDialog(reason: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val dialog = JDialog(null as JFrame?, "App Update Recommended", true)
        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        dialog.isResizable = false
        
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(450, 180)
        
        val messageLabel = JLabel(
            "<html><center>" +
            "A new version of the app is available with important security updates.<br>" +
            "$reason<br><br>" +
            "Would you like to update now?" +
            "</center></html>",
            SwingConstants.CENTER
        )
        mainPanel.add(messageLabel, BorderLayout.CENTER)
        
        val buttonPanel = JPanel(FlowLayout())
        
        val updateButton = JButton("Update Now")
        updateButton.addActionListener {
            openUpdateWebsite()
            dialog.dispose()
            continuation.resume(Unit)
        }
        
        val laterButton = JButton("Later")
        laterButton.addActionListener {
            dialog.dispose()
            continuation.resume(Unit)
        }
        
        buttonPanel.add(updateButton)
        buttonPanel.add(laterButton)
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        dialog.contentPane.add(mainPanel)
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
        
        continuation.invokeOnCancellation {
            dialog.dispose()
        }
    }
    
    private fun openUpdateWebsite() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                // Replace with your actual update/download URL
                Desktop.getDesktop().browse(URI("https://github.com/your-organization/ampairs/releases/latest"))
            }
        } catch (e: Exception) {
            // Could not open browser
            println("Could not open browser for update: ${e.message}")
        }
    }
}