package com.ampairs.common.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun createAppDataStore(context: Context): DataStore<Preferences> =
    createAppDataStore(
        producePath = {
            File(context.filesDir, appDataStoreFileName).absolutePath
        }
    )