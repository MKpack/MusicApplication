package com.example.musicapplication.data.local.notification

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppNotificationModule {

    @Binds
    @Singleton
    abstract fun bindAppNotificationStore(
        impl: DataStoreAppNotificationStore
    ): AppNotificationStore
}
