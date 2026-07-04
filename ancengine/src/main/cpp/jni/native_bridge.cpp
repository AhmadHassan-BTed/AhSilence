#include <jni.h>

#include "../audio/AncEngine.h"

using anc::audio::AncEngine;

namespace {
inline AncEngine *toEngine(jlong handle) {
    return reinterpret_cast<AncEngine *>(handle);
}
} // namespace

// Function names are mangled from the Kotlin class:
// com.bted.ahsilence.data.engine.NativeAudioDSP
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_bted_ahsilence_data_engine_NativeAudioDSP_nativeCreate(
        JNIEnv * /*env*/, jobject /*thiz*/, jint sampleRate) {
    // One-time heap allocation at engine-creation time -- NOT inside the
    // audio callback, so this does not violate the zero-allocation contract.
    auto *engine = new AncEngine(static_cast<int32_t>(sampleRate));
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT jboolean JNICALL
Java_com_bted_ahsilence_data_engine_NativeAudioDSP_nativeStart(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    AncEngine *engine = toEngine(handle);
    return (engine != nullptr && engine->start() == oboe::Result::OK) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_bted_ahsilence_data_engine_NativeAudioDSP_nativeStop(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    if (AncEngine *engine = toEngine(handle)) {
        engine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_bted_ahsilence_data_engine_NativeAudioDSP_nativeSetFrequency(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle, jfloat frequencyHz) {
    if (AncEngine *engine = toEngine(handle)) {
        engine->setTargetFrequency(static_cast<float>(frequencyHz));
    }
}

JNIEXPORT void JNICALL
Java_com_bted_ahsilence_data_engine_NativeAudioDSP_nativeUpdateParameters(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle, jfloat gainPercent, jfloat phaseDegrees) {
    if (AncEngine *engine = toEngine(handle)) {
        engine->updateParameters(static_cast<float>(gainPercent), static_cast<float>(phaseDegrees));
    }
}

JNIEXPORT void JNICALL
Java_com_bted_ahsilence_data_engine_NativeAudioDSP_nativeDestroy(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    delete toEngine(handle);
}

} // extern "C"
