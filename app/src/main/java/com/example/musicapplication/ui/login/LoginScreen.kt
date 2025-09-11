package com.example.musicapplication.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicapplication.R
import com.example.musicapplication.ui.component.ButtonRightTextField
import com.example.musicapplication.ui.component.NewTextField
import com.example.musicapplication.ui.component.NoRippleTextButton
import com.example.musicapplication.ui.component.PasswordTextField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.log

@Composable
fun LoginEntry(loginViewModel: LoginViewModel) {
//    var mode by remember { mutableStateOf(LoginMode.ACCOUNT) }
    val mode by loginViewModel.mode.collectAsState()
    BackHandler(enabled = mode != LoginMode.ACCOUNT) {
        loginViewModel.changeMode(LoginMode.ACCOUNT)
    }
    Column (
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.music_image),
            contentDescription = "icon",
            modifier = Modifier.padding(top = 54.dp, bottom = 16.dp)
                .width(60.dp)
                .height(60.dp)
        )
        Text(
            text = "Music App",
            modifier = Modifier.padding(bottom = 95.dp)
                .fillMaxWidth()
                .height(50.dp),
            textAlign = TextAlign.Center,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
        )

        when (mode) {
            LoginMode.ACCOUNT -> AccountForm(loginViewModel = loginViewModel)
            LoginMode.REGISTER -> RegisterForm(loginViewModel = loginViewModel)
            LoginMode.FORGET -> ForgetForm(loginViewModel = loginViewModel)
        }
        Row(
            modifier = Modifier.fillMaxWidth(0.8f).padding(top = 180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                color = Color.Black,
                thickness = 1.dp,
                modifier = Modifier.width(100.dp)
            )
            Text("其他登陆方式",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
            )
            Divider(
                color = Color.Black,
                thickness = 1.dp,
                modifier = Modifier.width(100.dp)
            )
        }

    }
}

//登陆组件
@Composable
fun AccountForm(loginViewModel: LoginViewModel) {
//    var email by rememberSaveable { mutableStateOf("") }
//    var pwd by rememberSaveable { mutableStateOf("") }
//    var canLogin by rememberSaveable { mutableStateOf(false) }

    val email by loginViewModel.email.collectAsState()
    val pwd by loginViewModel.password.collectAsState()
    Column {
        NewTextField(
            text = email,
            onValueChange = { loginViewModel.emailUpdate(it) },
            hint = "用户id或邮箱"
        )
        Spacer(modifier = Modifier.height(10.dp))
        PasswordTextField(
            pwd = pwd,
            onValueChange = { loginViewModel.passwordUpdate(it) },
            hint = "密码"
        )
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {},
            enabled = loginViewModel.ifCanLogin(),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(45.dp),
            colors = ButtonDefaults.buttonColors(
                Color(0xFF383125)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                "登陆",
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoRippleTextButton("忘记密码", ) {
                loginViewModel.changeMode(LoginMode.FORGET)
            }
            NoRippleTextButton("还没有账号?去注册") {
                loginViewModel.changeMode(LoginMode.REGISTER)
            }
        }

    }

}

//注册组件
@Composable
fun RegisterForm(loginViewModel: LoginViewModel) {

    val email by loginViewModel.email.collectAsState()
    val pwd by loginViewModel.password.collectAsState()
    val pwdAgain by loginViewModel.passwordAgain.collectAsState()
    val authCode by loginViewModel.authCode.collectAsState()
    Column {
        NewTextField(email, { loginViewModel.emailUpdate(it) }, hint = "邮箱")
        Spacer(modifier = Modifier.height(10.dp))
        ButtonRightTextField(authCode, { loginViewModel.authCodeUpdate(it) },  "验证码",
            onClick = {
                //网络请求
        })
        Spacer(modifier = Modifier.height(10.dp))
        PasswordTextField(pwd, { loginViewModel.passwordUpdate(it) }, "设置密码")
        Spacer(modifier = Modifier.height(10.dp))
        PasswordTextField(pwdAgain, { loginViewModel.passwordAgainUpdate(it) }, "重复密码")
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {

            },
            enabled = loginViewModel.ifCanRegister(),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(45.dp),
            colors = ButtonDefaults.buttonColors(
                Color(0xFF383125)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("注册")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoRippleTextButton("已经有账号，去登陆", ) {
                loginViewModel.changeMode(LoginMode.ACCOUNT)
            }
        }
    }
}

//忘记密码组件
@Composable
fun ForgetForm(loginViewModel: LoginViewModel) {
    val email by loginViewModel.email.collectAsState()
    val authCode by loginViewModel.authCode.collectAsState()
    val pwd by loginViewModel.password.collectAsState()
    Column {
        NewTextField(email, { loginViewModel.emailUpdate(it) }, hint = "邮箱")
        Spacer(modifier = Modifier.height(10.dp))
        ButtonRightTextField(authCode, { loginViewModel.authCodeUpdate(it) }, hint = "验证码", onClick = { })
        Spacer(modifier = Modifier.height(10.dp))
        PasswordTextField(pwd, { loginViewModel.passwordUpdate(it) }, "设置新密码")
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {},
            enabled = loginViewModel.ifCanReset(),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(45.dp),
            colors = ButtonDefaults.buttonColors(
                Color(0xFF383125)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("重置密码")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoRippleTextButton("想起密码了，去登陆", ) {
                loginViewModel.changeMode(LoginMode.ACCOUNT)
            }
        }
    }
}