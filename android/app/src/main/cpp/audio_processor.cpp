#include "audio_processor.h"
#include <cmath>

namespace routineremind {

float computeRms(const float *samples, size_t count) {
    if (samples == nullptr || count == 0) {
        return 0.0f;
    }
    double sumSquares = 0.0;
    for (size_t i = 0; i < count; ++i) {
        sumSquares += static_cast<double>(samples[i]) * samples[i];
    }
    return static_cast<float>(std::sqrt(sumSquares / static_cast<double>(count)));
}

} // namespace routineremind
