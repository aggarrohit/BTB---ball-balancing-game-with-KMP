package com.rohit.balancetheball.di

import com.rohit.balancetheball.domain.usecase.ClaimUsernameUseCase
import com.rohit.balancetheball.domain.usecase.CreateRoomUseCase
import com.rohit.balancetheball.domain.usecase.JoinRoomUseCase
import org.koin.dsl.module

/** Use cases — each depends only on a repository interface from [dataModule]. */
val domainModule = module {
    single { ClaimUsernameUseCase(get()) }
    single { CreateRoomUseCase(get()) }
    single { JoinRoomUseCase(get()) }
}
