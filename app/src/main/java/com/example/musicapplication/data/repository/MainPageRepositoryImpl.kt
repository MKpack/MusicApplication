package com.example.musicapplication.data.repository

import com.example.musicapplication.data.remote.api.MainPageApi
import jakarta.inject.Inject

class MainPageRepositoryImpl @Inject constructor(
    private val mainPageApi: MainPageApi
): MainPageRepository {
    override suspend fun test(): String? {
        val response = mainPageApi.test()
        if (response.isSuccessful) {
            return response.body()?.data
        }
        else {
            return response.body()?.message
        }
    }
}