package com.learning.movies.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class TmdbAuthInterceptor(
    private val readAccessToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $readAccessToken")
            .build()
        return chain.proceed(request)
    }
}
