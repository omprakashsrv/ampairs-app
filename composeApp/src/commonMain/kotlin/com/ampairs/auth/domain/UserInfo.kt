package com.ampairs.auth.domain

data class UserInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val countryCode: Long,
    val phone: String,
    val lastLogin: Long = 0,
    val loginCount: Int = 0,
    val isAuthenticated: Boolean = false,
    val hasSelectedWorkspace: Boolean = false
)