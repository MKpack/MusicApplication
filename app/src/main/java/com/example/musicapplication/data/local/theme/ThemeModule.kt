package com.example.musicapplication.data.local.theme

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindsThemeStore(
        impl: DataStoreThemeStore
    ) : ThemeStore
}