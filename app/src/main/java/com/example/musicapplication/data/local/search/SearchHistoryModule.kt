package com.example.musicapplication.data.local.search

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchHistoryModule {

    @Binds
    @Singleton
    abstract fun bindSearchHistoryStore(
        impl: DataStoreSearchHistoryStore
    ): SearchHistoryStore
}
