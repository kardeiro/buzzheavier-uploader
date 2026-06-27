package com.buzzheavier.uploader.network

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.buzzheavier.uploader.UploadConstants
import com.buzzheavier.uploader.data.UploadApiResponse
import com.buzzheavier.uploader.data.UploadProgress
import com.buzzheavier.uploader.data.UploadResult
import com.buzzheavier.uploader.data.UploadStatus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.util.Base64
import java.util.concurrent.TimeUnit

class UploadManager(private val context: Context) {

    private val client = HttpClientProvider.baseClient.newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var currentCall: Call? = null

    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState

    data class UploadState(
        val status: UploadStatus = UploadStatus.IDLE,
        val progress: UploadProgress = UploadProgress(),
        val result: UploadResult? = null,
        val currentFile: String = ""
    )

    suspend fun uploadFile(
        uri: Uri,
        accountId: String = "",
        parentId: String = "",
        locationId: String = "",
        note: String = "",
        onProgress: (UploadProgress) -> Unit = {}
    ): UploadResult = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri) ?: "unknown"
        val fileSize = getFileSize(uri)

        _uploadState.value = UploadState(
            status = UploadStatus.PREPARING,
            currentFile = fileName
        )

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open file: $fileName")

            val requestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
                override fun contentLength() = fileSize

                override fun writeTo(sink: BufferedSink) {
                    val source = inputStream.source()
                    val buffer = okio.Buffer()
                    var totalWritten = 0L
                    val bufferSize = 8192L
                    var lastSpeed = 0L
                    var lastTime = System.currentTimeMillis()
                    var lastBytes = 0L

                    while (true) {
                        val read = source.read(buffer, bufferSize)
                        if (read == -1L) break
                        sink.write(buffer, read)
                        totalWritten += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val bytesDelta = totalWritten - lastBytes
                            lastSpeed = (bytesDelta * 1000) / elapsed
                            val remaining = if (lastSpeed > 0) (fileSize - totalWritten) / lastSpeed else 0
                            val percentage = if (fileSize > 0) ((totalWritten * 100) / fileSize).toInt() else 0

                            val progress = UploadProgress(
                                bytesUploaded = totalWritten,
                                totalBytes = fileSize,
                                speed = lastSpeed,
                                remainingTime = remaining,
                                percentage = percentage.coerceAtMost(100)
                            )
                            onProgress(progress)
                            _uploadState.value = UploadState(
                                status = UploadStatus.UPLOADING,
                                progress = progress,
                                currentFile = fileName
                            )
                            lastTime = now
                            lastBytes = totalWritten
                        }
                    }
                    source.close()

                    val finalProgress = UploadProgress(
                        bytesUploaded = fileSize,
                        totalBytes = fileSize,
                        speed = lastSpeed,
                        remainingTime = 0,
                        percentage = 100
                    )
                    onProgress(finalProgress)
                    _uploadState.value = UploadState(
                        status = UploadStatus.COMPLETED,
                        progress = finalProgress,
                        currentFile = fileName
                    )
                }
            }

            val encodedName = fileName.replace(" ", "%20")
            var url = "${UploadConstants.BASE_URL}/$encodedName"
            val params = mutableListOf<String>()
            if (locationId.isNotEmpty()) params.add("locationId=$locationId")
            if (note.isNotEmpty()) {
                val encodedNote = Base64.getEncoder().encodeToString(note.toByteArray())
                params.add("note=$encodedNote")
            }
            if (params.isNotEmpty()) url += "?${params.joinToString("&")}"

            if (parentId.isNotEmpty()) {
                url = "${UploadConstants.BASE_URL}/$parentId/$encodedName"
                if (params.isNotEmpty()) url += "?${params.joinToString("&")}"
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .put(requestBody)

            if (accountId.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $accountId")
            }

            val request = requestBuilder.build()
            val call = client.newCall(request)
            currentCall = call
            val response = call.execute()
            currentCall = null

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val fileId = try {
                    gson.fromJson(responseBody, UploadApiResponse::class.java)?.data?.id ?: ""
                } catch (_: Exception) { "" }
                val fileUrl = if (fileId.isNotEmpty()) "https://buzzheavier.com/$fileId"
                    else "https://buzzheavier.com/$fileName"
                val result = UploadResult(
                    success = true,
                    url = fileUrl,
                    fileName = fileName,
                    fileSize = fileSize,
                    fileId = fileId
                )
                _uploadState.value = _uploadState.value.copy(
                    status = UploadStatus.COMPLETED,
                    result = result
                )
                result
            } else {
                val error = "HTTP ${response.code}: ${response.message}"
                val result = UploadResult(success = false, error = error, fileName = fileName)
                _uploadState.value = _uploadState.value.copy(
                    status = UploadStatus.FAILED,
                    result = result
                )
                result
            }
        } catch (e: Exception) {
            val result = UploadResult(success = false, error = e.message ?: "Unknown error", fileName = fileName)
            _uploadState.value = _uploadState.value.copy(
                status = UploadStatus.FAILED,
                result = result
            )
            result
        }
    }

    fun cancelUpload() {
        currentCall?.cancel()
        currentCall = null
        _uploadState.value = _uploadState.value.copy(status = UploadStatus.CANCELLED)
    }

    fun resetState() {
        currentCall = null
        _uploadState.value = UploadState()
    }

    internal fun getFileName(uri: Uri): String? {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                }
            }
            else -> uri.lastPathSegment
        }
    }

    internal fun getFileSize(uri: Uri): Long {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                } ?: 0L
            }
            else -> 0L
        }
    }
}
