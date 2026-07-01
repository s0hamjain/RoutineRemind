#ifndef ROUTINEREMIND_AUDIO_PROCESSOR_H
#define ROUTINEREMIND_AUDIO_PROCESSOR_H

#include <cstddef>

namespace routineremind {

/**
 * Computes the RMS (root-mean-square) amplitude of a PCM sample buffer.
 * Placeholder for the real audio pre-processing pipeline (M5): noise
 * reduction, normalization, and encoding before upload / Speech-to-Text.
 */
float computeRms(const float *samples, size_t count);

} // namespace routineremind

#endif // ROUTINEREMIND_AUDIO_PROCESSOR_H
