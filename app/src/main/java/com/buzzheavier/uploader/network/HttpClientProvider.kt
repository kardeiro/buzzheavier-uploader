package com.buzzheavier.uploader.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object HttpClientProvider {

    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .dispatcher(
                Dispatcher(
                    ThreadPoolExecutor(
                        0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
                        SynchronousQueue()
                    )
                )
            )
            .build()
    }
}
