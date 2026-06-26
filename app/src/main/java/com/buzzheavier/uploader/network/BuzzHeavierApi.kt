package com.buzzheavier.uploader.network

import com.buzzheavier.uploader.UploadConstants
import com.buzzheavier.uploader.data.AccountInfo
import com.buzzheavier.uploader.data.DirectoryInfo
import com.buzzheavier.uploader.data.FileInfo
import com.buzzheavier.uploader.data.StorageLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class BuzzHeavierApi(private val accountId: String = "") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun buildRequest(url: String, method: String = "GET", body: String? = null): Request {
        val builder = Request.Builder().url(url)
        if (accountId.isNotEmpty()) {
            builder.header("Authorization", "Bearer $accountId")
        }
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(body!!.toRequestBody("application/json".toMediaType()))
            "PATCH" -> builder.patch(body!!.toRequestBody("application/json".toMediaType()))
            "DELETE" -> builder.delete()
        }
        return builder.build()
    }

    suspend fun getAccountInfo(): Result<AccountInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest("${UploadConstants.API_URL}/account")
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            gson.fromJson(response.body?.string(), AccountInfo::class.java)
        }
    }

    suspend fun getStorageLocations(): Result<List<StorageLocation>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest("${UploadConstants.API_URL}/locations")
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val type = object : TypeToken<List<StorageLocation>>() {}.type
            gson.fromJson(response.body?.string(), type)
        }
    }

    suspend fun getRootDirectory(): Result<DirectoryInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest("${UploadConstants.API_URL}/fs")
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            gson.fromJson(response.body?.string(), DirectoryInfo::class.java)
        }
    }

    suspend fun getDirectory(directoryId: String): Result<DirectoryInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest("${UploadConstants.API_URL}/fs/$directoryId")
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            gson.fromJson(response.body?.string(), DirectoryInfo::class.java)
        }
    }

    suspend fun createDirectory(parentId: String, name: String): Result<DirectoryInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val body = gson.toJson(mapOf("name" to name))
            val request = buildRequest("${UploadConstants.API_URL}/fs/$parentId", "POST", body)
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            gson.fromJson(response.body?.string(), DirectoryInfo::class.java)
        }
    }

    suspend fun deleteItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest("${UploadConstants.API_URL}/fs/$itemId", "DELETE")
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        }
    }

    suspend fun renameItem(itemId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = gson.toJson(mapOf("name" to newName))
            val request = buildRequest("${UploadConstants.API_URL}/fs/$itemId", "PATCH", body)
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        }
    }
}
