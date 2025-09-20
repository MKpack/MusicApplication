package com.example.musicapplication.ui.login

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.R
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.ui.component.ButtonRightTextField
import com.example.musicapplication.ui.component.NewTextField
import com.example.musicapplication.ui.component.NoRippleTextButton
import com.example.musicapplication.ui.component.PasswordTextField
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher

@Composable
fun LoginEntry(
    loginViewModel: LoginViewModel,
    context: Context,
    navController: NavController
) {
    //这是新创建了个navController导致后面跳转的是空的
//    val navController = rememberNavController()
//    var mode by remember { mutableStateOf(LoginMode.ACCOUNT) }
    val mode by loginViewModel.mode.collectAsState()
    val loginStatus by loginViewModel.loginStatus.collectAsState()
    BackHandler(enabled = mode != LoginMode.ACCOUNT) {
        loginViewModel.changeMode(LoginMode.ACCOUNT)
    }

    //副作用
    LaunchedEffect(loginStatus) {
        if (loginStatus == "success") {
            if (!RouterConfig.MAINPAGE.isEmpty()) {
                navController.navigate(RouterConfig.MAINPAGE)
            }
            Toast.makeText(context, "登陆成功", Toast.LENGTH_SHORT).show()
            loginViewModel.changeLoginStatus()
        }
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
            LoginMode.REGISTER -> RegisterForm(loginViewModel = loginViewModel, context)
            LoginMode.FORGET -> ForgetForm(loginViewModel = loginViewModel, context)
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
    val loginStatus by loginViewModel.loginStatus.collectAsState()
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
        Spacer(modifier = Modifier.height(3.dp))
        if (loginStatus == "fail") {
            Text(
                "  用户名或密码错误",
                fontSize = 10.sp,
                color = Color.Red
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { loginViewModel.login() },
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
fun RegisterForm(loginViewModel: LoginViewModel, context: Context) {

    val email by loginViewModel.email.collectAsState()
    val pwd by loginViewModel.password.collectAsState()
    val pwdAgain by loginViewModel.passwordAgain.collectAsState()
    val authCode by loginViewModel.authCode.collectAsState()
    val registerStatus by loginViewModel.registerStatus.collectAsState()
    Column {
        NewTextField(email, { loginViewModel.emailUpdate(it) }, hint = "邮箱")
        Spacer(modifier = Modifier.height(10.dp))
        ButtonRightTextField(authCode, { loginViewModel.authCodeUpdate(it) },  "验证码",
            onClick = {
                loginViewModel.sendCode()
                Toast.makeText(context, "发送验证码成功", Toast.LENGTH_SHORT).show()
            },
            isOk = loginViewModel.isEmailValid(email) && email != ""
        )
        if (!loginViewModel.isEmailValid(email) && email != "") {
            Text(
                "邮箱格式不正确",
                fontSize = 10.sp,
                color = Color.Red
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        PasswordTextField(pwd, { loginViewModel.passwordUpdate(it) }, "设置密码")
        Spacer(modifier = Modifier.height(10.dp))
        PasswordTextField(pwdAgain, { loginViewModel.passwordAgainUpdate(it) }, "重复密码")
        Spacer(modifier = Modifier.height(3.dp))
        if (pwd != "" && pwdAgain != "" && pwd != pwdAgain) {
            Text(
                "两次密码必须相同",
                fontSize = 10.sp,
                color = Color.Red
            )
        }
        if (registerStatus == "fail") {
            Text(
                "验证码过期或错误",
                fontSize = 10.sp,
                color = Color.Red
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                val msg = loginViewModel.register()
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
fun ForgetForm(loginViewModel: LoginViewModel, context: Context) {
    val email by loginViewModel.email.collectAsState()
    val authCode by loginViewModel.authCode.collectAsState()
    val pwd by loginViewModel.password.collectAsState()
    Column {
        NewTextField(email, { loginViewModel.emailUpdate(it) }, hint = "邮箱")
        Spacer(modifier = Modifier.height(10.dp))
        ButtonRightTextField(authCode, { loginViewModel.authCodeUpdate(it) }, hint = "验证码",
            onClick = { },
            isOk = loginViewModel.isEmailValid(email) && email != ""
        )
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