package com.ampairs

import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Entry point called from AppDelegate.swift after Firebase is configured.
 * Lives in the shared module so it is included in the exported ObjC header.
 */
@OptIn(ExperimentalForeignApi::class)
fun onDidFinishLaunchingWithOptions() {
    println("KMP Initializer: Starting KMP-specific setup...")
    println("KMP Initializer: ✅ Setup complete")
}
