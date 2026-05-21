package com.ampairs.update.di

// Replaced Koin updateModule. Injectable classes are annotated with @Inject directly:
// - UpdateApiImpl: @Inject @SingleIn(AppScope) @ContributesBinding
// - UpdateChecker, UpdateDownloader, UpdateInstaller: @Inject

fun updateModule() = Unit
