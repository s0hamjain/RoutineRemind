package com.routineremind.app.data

// Plain Kotlin data classes; serialized/deserialized reflectively via
// Moshi's KotlinJsonAdapterFactory (see ApiClient).

data class AppUser(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val linkedUserIds: List<String> = emptyList(),
    val shareCode: String? = null,
    val photoUrl: String? = null,
)

data class LinkedStudent(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val shareCode: String? = null,
    val photoUrl: String? = null,
)

data class ScheduleItem(
    val id: String,
    val time: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val completedAt: String? = null,
)

data class Schedule(
    val id: String,
    val ownerUid: String,
    val date: String,
    val title: String,
    val status: String,
    val items: List<ScheduleItem> = emptyList(),
)

data class Question(
    val id: String,
    val scheduleId: String,
    val ownerUid: String,
    val prompt: String,
    val type: String = "text",
    val order: Int = 0,
)

data class QuestionResponse(
    val id: String,
    val questionId: String,
    val studentUid: String,
    val text: String? = null,
    val mediaUrl: String? = null,
    val transcript: String? = null,
    val createdAt: String? = null,
)

data class MediaUpload(
    val responseId: String,
    val uploadUrl: String,
    val objectName: String,
    val mediaUrl: String,
    val contentType: String,
)

data class RoleRequest(val role: String)

data class LinkRequest(val shareCode: String)

data class TextResponseRequest(val text: String)

data class MediaUploadRequest(val contentType: String)

data class TranscribeRequest(val languageCode: String = "en-US")

data class DeviceRegistration(
    val token: String,
    val platform: String = "android",
    val label: String = "Android",
)
