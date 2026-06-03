package com.example.musicapplication.ui.mainPage.profile.account

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.musicapplication.R
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import com.example.musicapplication.utils.LocalAudioMetaDataReader

@Composable
fun ProfileAccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    profileAccountViewModel: ProfileAccountViewModel = hiltViewModel()
) {

    val uiState by profileAccountViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        profileAccountViewModel.consumeErrorMessage()
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            profileAccountViewModel.onAvatarPicked(result.uriContent)
        } else {
            profileAccountViewModel.onCropAvatarFailed(result.error?.message)
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cropLauncher.launch(
                CropImageContractOptions(
                    uri,
                    cropImageOptions = CropImageOptions(
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        fixAspectRatio = true,
                        outputCompressQuality = 90
                    )
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LocalMusicThemeColors.current.bgTop,
                        LocalMusicThemeColors.current.bgBottom
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 10.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AccountTopBar(
                    onBack = onBack,
                    isSaving = uiState.isSaving,
                    onSaveClick = { profileAccountViewModel.saveProfile() }
                )
            }

            item {
                AccountEditCard(
                    avatarModel = uiState.pendingAvatarUri ?: LocalAudioMetaDataReader.buildMediaUrl(uiState.avatarUrl),
                    nickName = uiState.nickName,
                    onAvatarClick = {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onNickNameChange = {
                        profileAccountViewModel.onNickNameChange(it)
                    }
                )
            }

            item {
                ReadOnlyProfileCard(
                    userId = uiState.userId,
                    email = uiState.email
                )
            }
        }
    }
}

@Composable
private fun AccountTopBar(
    onBack: () -> Unit,
    isSaving: Boolean,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.textPrimary
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "账号资料",
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
//            Text(
//                text = "头像和昵称可以随时更新",
//                color = MusicTextSecondary,
//                fontSize = 13.sp
//            )
        }

        Button(
            onClick = {
                onSaveClick()
                onBack()
            },
            enabled = !isSaving,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalMusicThemeColors.current.primary,
                contentColor = Color.White,
                disabledContainerColor = LocalMusicThemeColors.current.disabledContainer,
                disabledContentColor = LocalMusicThemeColors.current.disabledContent
            ),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Text(
                text = if (isSaving) "保存中" else "保存",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AccountEditCard(
    avatarModel: Any?,
    nickName: String,
    onAvatarClick: () -> Unit,
    onNickNameChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(22.dp))
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(104.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = avatarModel,
                    placeholder = painterResource(R.drawable.default_cover),
                    error = painterResource(R.drawable.default_cover)
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(4.dp, LocalMusicThemeColors.current.cardSoft, CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LocalMusicThemeColors.current.primary)
                    .border(3.dp, LocalMusicThemeColors.current.surface, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAvatarClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = LocalMusicThemeColors.current.surface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "更换头像",
            color = LocalMusicThemeColors.current.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(22.dp))

        NickNameField(
            nickName = nickName,
            onNickNameChange = onNickNameChange
        )
    }
}

@Composable
private fun NickNameField(
    nickName: String,
    onNickNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "昵称",
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = nickName,
            onValueChange = onNickNameChange,
            singleLine = true,
            cursorBrush = SolidColor(LocalMusicThemeColors.current.primary),
            textStyle = TextStyle(
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            ),
            decorationBox = { innerField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalMusicThemeColors.current.field)
                        .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = LocalMusicThemeColors.current.iconMuted,
                        modifier = Modifier.size(19.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (nickName.isBlank()) {
                            Text(
                                text = "输入昵称",
                                color = LocalMusicThemeColors.current.textHint,
                                fontSize = 15.sp
                            )
                        }
                        innerField()
                    }
                }
            }
        )
    }
}

@Composable
private fun ReadOnlyProfileCard(
    userId: Int?,
    email: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(22.dp))
            .padding(vertical = 6.dp)
    ) {
        ReadOnlyInfoRow(
            label = "用户 ID",
            value = userId.toString(),
            icon = Icons.Default.Badge
        )

        ReadOnlyInfoRow(
            label = "邮箱",
            value = email,
            icon = Icons.Default.Email
        )

        Text(
            text = "用户 ID 和邮箱不可修改",
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun ReadOnlyInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LocalMusicThemeColors.current.primarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = label,
                color = LocalMusicThemeColors.current.textSecondary,
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = LocalMusicThemeColors.current.iconMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

