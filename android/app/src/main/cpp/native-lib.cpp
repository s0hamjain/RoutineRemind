#include <jni.h>
#include <string>
#include <vector>
#include "audio_processor.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_routineremind_app_nativebridge_NativeAudio_nativeVersion(
        JNIEnv *env, jobject /* this */) {
    std::string version = "routineremind-media 0.1 (C++17)";
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_routineremind_app_nativebridge_NativeAudio_nativeComputeRms(
        JNIEnv *env, jobject /* this */, jfloatArray samples) {
    jsize length = env->GetArrayLength(samples);
    if (length == 0) {
        return 0.0f;
    }
    std::vector<float> buffer(static_cast<size_t>(length));
    env->GetFloatArrayRegion(samples, 0, length, buffer.data());
    return routineremind::computeRms(buffer.data(), buffer.size());
}
