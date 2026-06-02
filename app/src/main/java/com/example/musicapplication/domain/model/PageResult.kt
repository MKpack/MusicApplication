package com.example.musicapplication.domain.model

data class PageResult<T>(
    val records: List<T>,
    val total: Long,
    val size: Long,
    val current: Long,
    val pages: Long
)


