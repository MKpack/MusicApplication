package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PasswordTextField(
    pwd: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    var hidden by rememberSaveable { mutableStateOf(true) }
    BasicTextField(
        value = pwd,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = if (hidden) PasswordVisualTransformation('*') else VisualTransformation.None,
        cursorBrush = SolidColor(Color(0xFF1E88E5)),
        textStyle = TextStyle(
            fontSize = 18.sp,
        ),
        decorationBox = { innerField ->
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
                    .padding(10.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (pwd.isEmpty()) {
                        Text(
                            text = hint,
                            fontSize = 16.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                    innerField()
                }
                IconButton(onClick = {hidden = !hidden}) {
                    Icon(
                        imageVector =
                            if (hidden) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

        },
    )
}

@Preview
@Composable
fun showPassTextField() {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PasswordTextField(
            text,
            onValueChange = { text = it},
            hint = "密码"
        )
    }

}