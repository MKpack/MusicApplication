package com.example.musicapplication.data.remote.dto.response

import com.example.musicapplication.domain.model.PageResult

data class PageResponse<T> (
    val records: List<T> = emptyList(),
    val total: Long = 0,
    val size: Long = 0,
    val current: Long = 0,
    val pages: Long = 0
)

/**
 * 扩展函数map
 * 把 PageResponse<T> 里面的每一条 T 数据，转换成 S，最后返回一个 PageResult<S>。
 */
fun <T,S> PageResponse<T>.map(transform: (T) -> S): PageResult<S> {
    return PageResult(
        records = records.map(transform),
        total = total,
        size = size,
        current = current,
        pages = pages
    )
}