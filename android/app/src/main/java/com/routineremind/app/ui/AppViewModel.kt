package com.routineremind.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routineremind.app.auth.AuthManager
import com.routineremind.app.auth.PushTokenManager
import com.routineremind.app.data.ApiClient
import com.routineremind.app.data.AppUser
import com.routineremind.app.data.ChatRequest
import com.routineremind.app.data.ChatResponse
import com.routineremind.app.data.DeviceRegistration
import com.routineremind.app.data.LinkRequest
import com.routineremind.app.data.LinkedStudent
import com.routineremind.app.data.RoleRequest
import com.routineremind.app.data.Schedule
import com.routineremind.app.nativebridge.NativeAudio
import kotlinx.coroutines.launch

enum class Screen { LOADING, LOGIN, HOME }

class AppViewModel : ViewModel() {

    var screen by mutableStateOf(Screen.LOADING)
        private set
    var profile by mutableStateOf<AppUser?>(null)
        private set
    var schedule by mutableStateOf<Schedule?>(null)
        private set
    var linkedStudents by mutableStateOf<List<LinkedStudent>>(emptyList())
        private set
    var selectedStudentUid by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var completingItemId by mutableStateOf<String?>(null)
        private set
    var chatAnswer by mutableStateOf<ChatResponse?>(null)
        private set
    var askingChat by mutableStateOf(false)
        private set
    var nativeAudioStatus by mutableStateOf("Native media module not checked yet.")
        private set

    init {
        checkNativeAudio()
        if (AuthManager.currentUser != null) {
            bootstrap()
        } else {
            screen = Screen.LOGIN
        }
    }

    fun signIn(email: String, password: String) = runAuth { AuthManager.signIn(email, password) }

    fun signUp(email: String, password: String) = runAuth { AuthManager.signUp(email, password) }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            error = null
            try {
                AuthManager.resetPassword(email)
                error = "Password reset email sent."
            } catch (e: Exception) {
                error = e.message ?: "Could not send reset email."
            }
        }
    }

    private fun runAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
                bootstrap()
            } catch (e: Exception) {
                error = friendly(e.message)
            } finally {
                busy = false
            }
        }
    }

    fun bootstrap() {
        viewModelScope.launch {
            screen = Screen.LOADING
            error = null
            try {
                profile = ApiClient.api.createSession()
                registerPushToken()
                if (profile?.role == "parent") {
                    loadLinkedStudents()
                }
                if (profile?.role != null && (profile?.role != "parent" || selectedStudentUid != null)) {
                    loadToday()
                }
                screen = Screen.HOME
            } catch (e: Exception) {
                error = e.message ?: "Failed to load account."
                screen = Screen.HOME
            }
        }
    }

    fun chooseRole(role: String) {
        viewModelScope.launch {
            busy = true
            try {
                profile = ApiClient.api.setRole(RoleRequest(role))
                loadToday()
            } catch (e: Exception) {
                error = e.message ?: "Failed to set role."
            } finally {
                busy = false
            }
        }
    }

    fun linkStudent(code: String) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                profile = ApiClient.api.linkStudent(LinkRequest(code))
                loadLinkedStudents()
                loadToday()
            } catch (e: Exception) {
                error = e.message ?: "Could not link student."
            } finally {
                busy = false
            }
        }
    }

    fun selectStudent(uid: String) {
        selectedStudentUid = uid
        viewModelScope.launch {
            loadToday()
        }
    }

    fun unlinkSelectedStudent() {
        val uid = selectedStudentUid ?: return
        viewModelScope.launch {
            busy = true
            error = null
            try {
                profile = ApiClient.api.unlinkStudent(uid)
                schedule = null
                loadLinkedStudents()
                if (selectedStudentUid != null) {
                    loadToday()
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not unlink student."
            } finally {
                busy = false
            }
        }
    }

    fun completeItem(itemId: String) {
        val currentSchedule = schedule ?: return
        viewModelScope.launch {
            completingItemId = itemId
            error = null
            try {
                schedule = ApiClient.api.completeItem(currentSchedule.id, itemId)
            } catch (e: Exception) {
                error = e.message ?: "Could not complete item."
            } finally {
                completingItemId = null
            }
        }
    }

    fun askChat(message: String) {
        if (message.isBlank()) {
            error = "Question cannot be empty."
            return
        }
        viewModelScope.launch {
            askingChat = true
            error = null
            try {
                chatAnswer = ApiClient.api.askChat(ChatRequest(message = message.trim()))
                loadToday()
            } catch (e: Exception) {
                error = e.message ?: "Could not answer that question."
            } finally {
                askingChat = false
            }
        }
    }

    fun checkNativeAudio() {
        nativeAudioStatus = try {
            val rms = NativeAudio.nativeComputeRms(floatArrayOf(0.0f, 0.25f, -0.25f, 0.5f))
            "${NativeAudio.nativeVersion()} · sample RMS ${"%.3f".format(rms)}"
        } catch (e: Throwable) {
            "Native media module unavailable: ${e.message}"
        }
    }

    private suspend fun loadToday() {
        schedule = try {
            val loadedSchedule = ApiClient.api.todaySchedule(if (profile?.role == "parent") selectedStudentUid else null)
            loadedSchedule
        } catch (e: Exception) {
            null // 404 → empty state
        }
    }

    private suspend fun loadLinkedStudents() {
        linkedStudents = ApiClient.api.linkedStudents()
        if (linkedStudents.none { it.uid == selectedStudentUid }) {
            selectedStudentUid = linkedStudents.firstOrNull()?.uid
        }
    }

    private suspend fun registerPushToken() {
        try {
            val token = PushTokenManager.currentToken()
            ApiClient.api.registerDevice(DeviceRegistration(token = token))
        } catch (_: Exception) {
            // Push registration should never block the core app experience.
        }
    }

    fun signOut() {
        AuthManager.signOut()
        profile = null
        schedule = null
        screen = Screen.LOGIN
    }

    private fun friendly(message: String?): String {
        val m = message ?: return "Something went wrong."
        return when {
            m.contains("password", true) -> "Incorrect email or password."
            m.contains("no user", true) -> "No account found for that email."
            m.contains("already in use", true) -> "An account already exists for that email."
            else -> "Sign-in failed. Please check your details and try again."
        }
    }
}
