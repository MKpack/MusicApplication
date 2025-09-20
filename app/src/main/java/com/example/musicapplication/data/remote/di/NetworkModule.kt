package com.example.musicapplication.data.remote.di


import android.content.Context
import com.example.musicapplication.data.remote.api.LoginApi
import com.example.musicapplication.data.remote.api.MainPageApi
import com.example.musicapplication.data.remote.api.RefreshTokenApi
import com.example.musicapplication.data.remote.api.RegisterApi
import com.example.musicapplication.data.remote.interceptor.AuthInterceptor
import com.example.musicapplication.data.remote.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import javax.inject.Named

/**
 * @Singleton注解全局唯一实例，只在程序初始化时初始一次
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    //为了refresh其中的token而创建的客户端，不然拦截器依赖循环
    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()


    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshRetrofit(@Named("refresh") okHttpClient: OkHttpClient) : Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://192.168.220.75:8080")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideRefreshTokenApiService(@Named("refresh") retrofit: Retrofit): RefreshTokenApi {
        return retrofit.create(RefreshTokenApi::class.java)
    }
    @Provides
    @Singleton
    fun provideAuthInterceptor(@ApplicationContext context: Context): AuthInterceptor {
        return AuthInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(@ApplicationContext context: Context, refreshTokenApi: RefreshTokenApi): TokenAuthenticator {
        return TokenAuthenticator(context, refreshTokenApi)
    }

    //创建http客户端(主要业务)
    @Provides
    @Singleton
    fun provideOkhttp(authInterceptor: AuthInterceptor, tokenAuthenticator: TokenAuthenticator): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

    //创建retrofit实例
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://192.168.220.75:8080")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())   //字符串解析,后期我会全部改成json形式返回给客户端
            .addConverterFactory(GsonConverterFactory.create())     //json字符串解析
            .build()
    }

    //生成api实例,通过注解推送
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): LoginApi  {
        return retrofit.create(LoginApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRegisterApiService(retrofit: Retrofit): RegisterApi {
        return retrofit.create(RegisterApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMainPageApi(retrofit: Retrofit): MainPageApi {
        return retrofit.create(MainPageApi::class.java)
    }
}


