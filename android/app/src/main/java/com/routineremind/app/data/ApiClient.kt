package com.routineremind.app.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Builds the Retrofit client for the Spring Boot backend. Every request is
 * decorated with a fresh Identity Platform ID token via an OkHttp interceptor.
 *
 * 10.0.2.2 is the host machine's localhost as seen from the Android emulator.
 */
object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8080/api/v1/"

    val api: BackendApi by lazy { build() }

    private fun build(): BackendApi {
        val authInterceptor = okhttp3.Interceptor { chain ->
            val builder = chain.request().newBuilder()
            val token = currentIdTokenBlocking()
            if (token != null) {
                builder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BackendApi::class.java)
    }

    private fun currentIdTokenBlocking(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            Tasks.await(user.getIdToken(false)).token
        } catch (e: Exception) {
            null
        }
    }
}
