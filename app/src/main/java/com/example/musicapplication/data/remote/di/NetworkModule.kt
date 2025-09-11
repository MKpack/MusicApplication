package com.example.musicapplication.data.remote.di


import com.example.musicapplication.data.remote.api.LoginApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    //创建http客户端
    @Provides
    @Singleton
    fun provideOkhttp(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()

    //创建retrofit实例
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    //生成api实例,通过注解推送
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): LoginApi  {
        return retrofit.create(LoginApi::class.java)
    }

}


