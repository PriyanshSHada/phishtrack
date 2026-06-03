package com.example.phishtrack.di

import android.content.Context
import androidx.room.Room
import com.example.phishtrack.data.api.ApiService
import com.example.phishtrack.data.local.AppDatabase
import com.example.phishtrack.data.local.CaseDao
import com.example.phishtrack.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://phishtrack.onrender.com/"

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Attaches Bearer token to every authenticated request
        val authInterceptor = Interceptor { chain ->
            val token = tokenManager.getToken()
            val requestBuilder = chain.request().newBuilder()
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        // Auto-logout: clears the stored token whenever the server returns 401 Unauthorized.
        // Since isLoggedIn() checks for a non-null token, the Navigation layer will redirect
        // to Login automatically on the next app resume — no event bus needed.
        val unauthorizedInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                tokenManager.clearToken()
            }
            response
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // Handles Render free-tier cold start (~50s)
            .readTimeout(60, TimeUnit.SECONDS)     // Handles long-running analysis responses
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(okHttpClient: OkHttpClient): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "phishtrack_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideCaseDao(database: AppDatabase): CaseDao {
        return database.caseDao()
    }
}
