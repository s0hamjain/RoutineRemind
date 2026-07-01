package com.routineremind.app.nativebridge

/**
 * JNI bridge to the C++ `routineremind_media` module. In M5 this will handle
 * audio/video encoding and speech pre-processing; for now it exposes a version
 * string and an RMS calculation to prove the native pipeline is wired up.
 */
object NativeAudio {
    init {
        System.loadLibrary("routineremind_media")
    }

    external fun nativeVersion(): String

    external fun nativeComputeRms(samples: FloatArray): Float
}
