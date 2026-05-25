package com.ampairs.auth.api

interface UserDataService {
    suspend fun getUserDisplayName(): String?
}
