package com.ampairs.event.di

// Replaced Koin eventModule.
//
// The event module provides EventManager instances via EventManagerFactory (a static object).
// EventManager is not injected via DI — it is created/retrieved by callers using:
//   EventManagerFactory.getOrCreate(workspaceId, userId, deviceId, httpClient, tokenProvider, tokenRefresher, baseUrl)
//
// This factory pattern does not map directly to Metro @Inject because it requires
// runtime parameters (workspaceId, userId, deviceId) that are not available at graph creation time.
// Callers should access EventManagerFactory directly rather than via DI injection.

fun eventModule() = Unit
