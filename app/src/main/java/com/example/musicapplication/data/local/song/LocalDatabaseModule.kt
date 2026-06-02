package com.example.musicapplication.data.local.song

import android.content.Context
import androidx.room.Room
import com.example.musicapplication.data.local.song.database.SongDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object LocalDatabaseModule {

    @Provides
    @Singleton
    fun provideSongDatabase(
        @ApplicationContext context: Context
    ): SongDatabase {
        return Room.databaseBuilder(
            context,
            SongDatabase::class.java,
            "song_database"
        ).build()
    }

    @Provides
    fun provideSongDao(db: SongDatabase) = db.songDao()

    @Provides
    fun provideSongListDao(db: SongDatabase) = db.songListDao()

    @Provides
    fun provideSongListMetaDao(db: SongDatabase) = db.songListMetaDao()
}