package com.example.musicapplication.ui.component

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.Text

@Composable
fun ButtonRightTextField(
    text: String,
    onValueChange: (String) -> Unit,
    hint: String,
    onClick: () -> Unit
) {
    var btnText by rememberSaveable { mutableStateOf("获取验证码") }
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        decorationBox = { innerField->
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFDFE5E5),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .background(
                        color = Color(0xFFDCDCDC),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = hint,
                            fontSize = 16.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                    innerField()
                }
                val interactionSource = remember { MutableInteractionSource() }
                val scope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                scope.launch {
                                    onClick
                                    for (i in 60 downTo 1) {
                                        btnText = "${i}s"
                                        delay(1000)
                                    }
                                    btnText = "获取验证码"
                                }
                            },
                            enabled = btnText == "获取验证码"
                        )
                ) {
                    BasicText(
                        text = btnText,
                        style = TextStyle(color = Color.Black)
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun showButtonRightTextField() {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ButtonRightTextField(
            text,
            { text = it },
            "获取验证码",
            {},
        )
    }
}