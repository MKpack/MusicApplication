package com.example.musicapplication.data.common

sealed interface RepositoryWorkResult<out T> {
    data class Success<T> (
        val data: T
    ): RepositoryWorkResult<T>

    data class Failure(
        val message: String,
        val code: Int? = null,
        val throwable: Throwable? = null
    ): RepositoryWorkResult<Nothing>
}