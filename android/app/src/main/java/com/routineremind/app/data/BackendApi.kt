package com.routineremind.app.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface BackendApi {

    @POST("auth/session")
    suspend fun createSession(): AppUser

    @GET("me")
    suspend fun me(): AppUser

    @POST("devices")
    suspend fun registerDevice(@Body request: DeviceRegistration): Map<String, String>

    @POST("me/role")
    suspend fun setRole(@Body request: RoleRequest): AppUser

    @POST("me/link")
    suspend fun linkStudent(@Body request: LinkRequest): AppUser

    @GET("me/linked-students")
    suspend fun linkedStudents(): List<LinkedStudent>

    @DELETE("me/linked-students/{studentUid}")
    suspend fun unlinkStudent(@Path("studentUid") studentUid: String): AppUser

    @GET("schedule/today")
    suspend fun todaySchedule(@Query("ownerUid") ownerUid: String? = null): Schedule

    @POST("schedules/{id}/items/{itemId}/complete")
    suspend fun completeItem(
        @Path("id") scheduleId: String,
        @Path("itemId") itemId: String,
    ): Schedule

    @GET("schedules/{scheduleId}/questions")
    suspend fun questions(@Path("scheduleId") scheduleId: String): List<Question>

    @POST("questions/{questionId}/responses")
    suspend fun submitTextResponse(
        @Path("questionId") questionId: String,
        @Body request: TextResponseRequest,
    ): QuestionResponse

    @POST("chat")
    suspend fun askChat(@Body request: ChatRequest): ChatResponse

    @GET("chat/history")
    suspend fun chatHistory(
        @Query("ownerUid") ownerUid: String? = null,
        @Query("limit") limit: Int = 20,
    ): List<ChatMessage>

    @POST("questions/{questionId}/responses/media")
    suspend fun createMediaUpload(
        @Path("questionId") questionId: String,
        @Body request: MediaUploadRequest,
    ): MediaUpload

    @POST("responses/{responseId}/transcribe")
    suspend fun transcribeResponse(
        @Path("responseId") responseId: String,
        @Body request: TranscribeRequest = TranscribeRequest(),
    ): QuestionResponse
}
