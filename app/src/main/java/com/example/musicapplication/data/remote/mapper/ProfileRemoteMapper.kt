package com.example.musicapplication.data.remote.mapper

import com.example.musicapplication.data.remote.dto.response.ProfileResponse
import com.example.musicapplication.data.remote.dto.response.UserStatResponse
import com.example.musicapplication.domain.model.UserStat


// TODO
fun ProfileResponse.toUserProfile() {

}


fun UserStatResponse.toUserStat(): UserStat {
    return UserStat(
        userId = userId,
        playCount = playCount,
        favoriteCount = favoriteCount
    )
}