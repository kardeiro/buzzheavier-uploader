package com.buzzheavier.uploader.data

import com.google.gson.annotations.SerializedName

data class UploadResult(
    val success: Boolean,
    val url: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val error: String = "",
    val fileId: String = "",
    val parentId: String = ""
)

data class FileInfo(
    val id: String = "",
    val name: String = "",
    val size: Long = 0,
    val type: String = "",
    val createdAt: String = "",
    val url: String = "",
    val parentId: String = "",
    val isDirectory: Boolean = false,
    val note: String = "",
    @SerializedName("childrenCount")
    val childrenCount: Int = 0
)

data class DirectoryInfo(
    val id: String = "",
    val name: String = "",
    val parentId: String = "",
    val children: List<FileInfo> = emptyList()
)

data class AccountInfo(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val storageUsed: Long = 0,
    val storageLimit: Long = 0
)

data class StorageLocation(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val isDefault: Boolean = false
)

data class UploadProgress(
    val bytesUploaded: Long = 0,
    val totalBytes: Long = 0,
    val speed: Long = 0,
    val remainingTime: Long = 0,
    val percentage: Int = 0
) {
    val isComplete: Boolean get() = percentage >= 100
}

enum class UploadStatus {
    IDLE,
    PREPARING,
    UPLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
