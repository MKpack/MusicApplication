package com.example.musicapplication.data.local.userstat

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserStatModule {

    @Binds
    @Singleton
    abstract fun bindUserStatStore(
        impl: DataStoreUserStatStore
    ): UserStatStore
}
