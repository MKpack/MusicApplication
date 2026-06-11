package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.example.musicapplication.data.remote.dto.response.PageResponse
import com.example.musicapplication.data.remote.dto.response.SongResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface SongApi {

    @GET("/songs/getHotSongs")
    suspend fun getHotSongs(
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long
    ) : ApiResponse<PageResponse<SongResponse>>

    @GET("/songs/favorites")
    suspend fun getLovedSongs(
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long
    ) : ApiResponse<PageResponse<SongResponse>>

    @GET("/songs/search")
    suspend fun searchSongs(
        @Query("keyword") keyword: String,
        @Query("pageNum") pageNum: Long,
        @Query("pageSize") pageSize: Long
    ) : ApiResponse<PageResponse<SongResponse>>

    @POST("/songs/{songId}/favorite")
    suspend fun favoriteSong(
        @Path("songId") songId: Long
    ) : ApiResponse<SongResponse>

    @DELETE("/songs/{songId}/favorite")
    suspend fun unFavoriteSong(
        @Path("songId") songId: Long
    ) : ApiResponse<SongResponse>


    @POST("/songs/{songId}/play")
    suspend fun increaseSongPlayCount(
        @Path("songId") songId: Long
    ) : ApiResponse<Any>
}
