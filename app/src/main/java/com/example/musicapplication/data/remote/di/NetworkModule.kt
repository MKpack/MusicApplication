package com.example.musicapplication.data.remote.di


import android.content.Context
import com.example.musicapplication.data.remote.api.LoginApi
import com.example.musicapplication.data.remote.api.RegisterApi
import com.example.musicapplication.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    @Provides
    @Singleton
    fun provideAuthInterceptor(@ApplicationContext context: Context): AuthInterceptor {
        return AuthInterceptor(context)
    }
    //创建http客户端
    @Provides
    @Singleton
    fun provideOkhttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

    //创建retrofit实例
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://192.168.255.75:8080")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())   //字符串解析
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

}


