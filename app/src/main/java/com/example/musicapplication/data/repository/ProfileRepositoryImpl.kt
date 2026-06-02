package com.example.musicapplication.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.local.profile.ProfileStore
import com.example.musicapplication.data.remote.api.ProfileApi
import com.example.musicapplication.data.remote.dto.request.ProfileRequest
import com.example.musicapplication.data.remote.dto.response.ProfileResponse
import com.example.musicapplication.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    @ApplicationContext val context: Context,
    private val profileStore: ProfileStore
) : ProfileRepository {


//    Eagerly 的意思是：
//    立刻开始收集上游 Flow，不管有没有人 collect 这个 profileFlow。
    override val profileFlow: StateFlow<UserProfile?> =
        profileStore.profileFlow.stateIn(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val TAG = "ProfileRepository"

    override suspend fun getUserProfile(): RepositoryWorkResult<UserProfile> {
        try {
            val response = profileApi.getUserProfile()
            return if (response.code == 200 && response.data != null) {
                val profile = response.data.toUserProfile()
                profileStore.saveProfile(profile)
                RepositoryWorkResult.Success(profile)
            } else {
                RepositoryWorkResult.Failure(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            return RepositoryWorkResult.Failure("获取网络请求失败", throwable = e)
        }
    }

    override suspend fun updateUserProfile(userProfile: UserProfile): RepositoryWorkResult<UserProfile> {
        try {
            val response = profileApi.updateUserProfile(ProfileRequest(
                userProfile.userId,
                userProfile.email,
                userProfile.nickName,
                userProfile.avatarUrl
                )
            )
            return if (response.code == 200 && response.data != null) {
                val profile = response.data.toUserProfile()
                profileStore.saveProfile(profile)
                RepositoryWorkResult.Success(profile)            } else {
                RepositoryWorkResult.Failure(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            return RepositoryWorkResult.Failure("更新网络请求失败", throwable = e)
        }
    }

    override suspend fun updateUserAvatar(uri: Uri): RepositoryWorkResult<UserProfile> {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use{
                it.readBytes()
            } ?: return RepositoryWorkResult.Failure("读取本地图片失败")
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

            val requestBody = bytes.toRequestBody(
                mimeType.toMediaTypeOrNull()
            )

            val avatarPart = MultipartBody.Part.createFormData(
                name = "file",
                filename = "avatar.jpg",
                body = requestBody
            )

            val response = profileApi.updateUserAvatar(avatarPart)

            return if (response.code == 200 && response.data != null) {
                val profile = response.data.toUserProfile()
                profileStore.saveProfile(profile)
                RepositoryWorkResult.Success(profile)            } else {
                RepositoryWorkResult.Failure(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user avatar", e)
            return RepositoryWorkResult.Failure("更新网络请求失败", throwable = e)
        }
    }

    private fun ProfileResponse.toUserProfile(): UserProfile {
        return UserProfile(
            userId = userId,
            email = email,
            nickName = nickName,
            avatarUrl = avatarUrl
        )
    }
}