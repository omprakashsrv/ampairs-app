package com.ampairs.tally

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val tallyModule: Module = module {
    single { TallyApiImpl(get()) } bind (TallyApi::class)
    single { TallyRepository(get()) }
}

fun tallyModule() = tallyModule