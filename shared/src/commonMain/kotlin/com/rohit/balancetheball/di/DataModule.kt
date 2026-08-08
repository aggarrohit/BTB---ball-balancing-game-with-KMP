package com.rohit.balancetheball.di

import com.rohit.balancetheball.data.auth.FirebaseAuthRepository
import com.rohit.balancetheball.data.remote.FirebaseHistoryDataSource
import com.rohit.balancetheball.data.remote.FirebaseInviteDataSource
import com.rohit.balancetheball.data.remote.FirebaseRoomDataSource
import com.rohit.balancetheball.data.remote.FirebaseUserDataSource
import com.rohit.balancetheball.data.repository.HistoryRepositoryImpl
import com.rohit.balancetheball.data.repository.InviteRepositoryImpl
import com.rohit.balancetheball.data.repository.RoomRepositoryImpl
import com.rohit.balancetheball.data.repository.UserRepositoryImpl
import com.rohit.balancetheball.domain.repository.AuthRepository
import com.rohit.balancetheball.domain.repository.HistoryRepository
import com.rohit.balancetheball.domain.repository.InviteRepository
import com.rohit.balancetheball.domain.repository.RoomRepository
import com.rohit.balancetheball.domain.repository.UserRepository
import org.koin.dsl.module

/** Firebase data sources and the repository implementations that wrap them. */
val dataModule = module {
    single { FirebaseUserDataSource() }
    single { FirebaseRoomDataSource() }
    single { FirebaseHistoryDataSource() }
    single { FirebaseInviteDataSource() }

    single<AuthRepository> { FirebaseAuthRepository() }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<RoomRepository> { RoomRepositoryImpl(get()) }
    single<HistoryRepository> { HistoryRepositoryImpl(get()) }
    single<InviteRepository> { InviteRepositoryImpl(get()) }
}
