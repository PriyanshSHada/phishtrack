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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://phishtrack-production.up.railway.app/"

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

        // Auto-refresh token interceptor: intercept 401 Unauthorized responses, attempt to refresh
        // the access token using the refresh token, and retry the original request.
        val unauthorizedInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            
            if (response.code == 401 && !request.url.encodedPath.contains("auth/refresh")) {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    response.close() // Avoid resource leak
                    
                    // Make synchronous request to /auth/refresh
                    val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val jsonBody = "{\"refreshToken\":\"$refreshToken\"}"
                    val refreshRequestBody = jsonBody.toRequestBody(jsonMediaType)
                    
                    val refreshRequest = okhttp3.Request.Builder()
                        .url("${BASE_URL}api/auth/refresh")
                        .post(refreshRequestBody)
                        .build()
                        
                    val tempClient = OkHttpClient()
                    val refreshResponse = try {
                        tempClient.newCall(refreshRequest).execute()
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (refreshResponse?.isSuccessful == true) {
                        val bodyString = refreshResponse.body?.string()
                        if (bodyString != null) {
                            try {
                                val jsonObject = org.json.JSONObject(bodyString)
                                val newToken = jsonObject.optString("token")
                                val newRefreshToken = jsonObject.optString("refreshToken", "")
                                
                                if (newToken.isNotEmpty()) {
                                    tokenManager.saveToken(newToken)
                                    if (newRefreshToken.isNotEmpty()) {
                                        tokenManager.saveRefreshToken(newRefreshToken)
                                    }
                                    
                                    val newRequest = request.newBuilder()
                                        .header("Authorization", "Bearer $newToken")
                                        .build()
                                    return@Interceptor chain.proceed(newRequest)
                                }
                            } catch (e: Exception) {
                                // JSON parse failed
                            }
                        }
                    }
                    refreshResponse?.close()
                }
                
                // Refresh failed or no refresh token
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

    @Provides
    @Singleton
    fun provideDashboardDao(database: AppDatabase): com.example.phishtrack.data.local.DashboardDao {
        return database.dashboardDao()
    }
}
