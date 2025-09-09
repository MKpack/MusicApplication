package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NewTextField(
    text: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(Color(0xFF1E88E5)),
        textStyle = TextStyle(
            fontSize = 18.sp,
        ),
        decorationBox = { innerField ->
            Box(
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
        }
    )
}


@Preview
@Composable
fun showTextField() {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NewTextField(
            text,
            onValueChange = { text = it},
            hint = "邮箱或用户名"
        )
    }

}