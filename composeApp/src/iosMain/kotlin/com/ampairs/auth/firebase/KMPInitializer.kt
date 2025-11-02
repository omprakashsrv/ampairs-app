package com.ampairs.auth.firebase

import kotlinx.cinterop.ExperimentalForeignApi

/**
 * KMP Initializer called from AppDelegate
 *
 * NOTE: Firebase is now initialized in AppDelegate.swift BEFORE this function is called.
 * Do NOT call FIRApp.configure() here as it will cause a crash due to double initialization.
 */
@OptIn(ExperimentalForeignApi::class)
fun onDidFinishLaunchingWithOptions() {
    println("KMP Initializer: Starting KMP-specific setup...")

    // Firebase is already configured in AppDelegate.swift
    // Add any other KMP initialization here if needed

    println("KMP Initializer: ✅ Setup complete")
}