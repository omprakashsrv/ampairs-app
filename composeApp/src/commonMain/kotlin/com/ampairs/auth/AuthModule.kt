package com.ampairs.auth

import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.AuthApiImpl
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.AuthRoomDatabase
import com.ampairs.auth.db.TokenRepositoryImpl
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.db.UserWorkspaceRepositoryImpl
import com.ampairs.auth.firebase.FirebaseAuthRepository
import com.ampairs.auth.ui.LoginScope
import com.ampairs.auth.viewmodel.DeviceManagementViewModel
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.auth.viewmodel.UserSelectionViewModel
import com.ampairs.auth.viewmodel.UserUpdateViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val authModule: Module = module {
    single { AuthApiImpl(get(), get()) } bind (AuthApi::class)
    single { TokenRepositoryImpl(get(), get(), get()) } bind (TokenRepository::class)
    single { UserWorkspaceRepositoryImpl(get()) } bind (UserWorkspaceRepository::class)
    // Database is provided by platform-specific modules
    single { get<AuthRoomDatabase>().userDao() }
    single { get<AuthRoomDatabase>().userTokenDao() }
    single { get<AuthRoomDatabase>().userSessionDao() }
    // DeviceService and RecaptchaService are provided by platform-specific modules
    single { UserRepository(get(), get(), get(), get(), get(), get()) }

    // Firebase authentication repository (FirebaseAuthProvider provided by platform modules)
    single { FirebaseAuthRepository(get()) }

    // Direct ViewModel injection
    viewModelOf(::LoginViewModel)
    viewModelOf(::DeviceManagementViewModel)
    viewModelOf(::UserUpdateViewModel)
    viewModelOf(::UserSelectionViewModel)

    scope<LoginScope> {
        scoped { LoginViewModel(get(), get(), get(), get(), get()) }
    }
}

fun authModule() = authModule