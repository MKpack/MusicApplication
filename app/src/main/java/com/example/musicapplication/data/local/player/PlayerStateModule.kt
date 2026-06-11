package com.example.musicapplication.data.local.player

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerStateModule {

    @Binds
    @Singleton
    abstract fun bindPlayerStateStore(
        impl: DataStorePlayerStateStore
    ): PlayerStateStore
}
