package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NoRippleTextButton(
    text: String,
    onClick: ()->Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = Color.Black)
        )
    }
}

@Preview
@Composable
fun showNoRippleTextButton() {
    Row(modifier = Modifier.fillMaxSize().background(Color.Green),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NoRippleTextButton("忘记密码", {})
        NoRippleTextButton("没有账号去注册") { }
    }
}