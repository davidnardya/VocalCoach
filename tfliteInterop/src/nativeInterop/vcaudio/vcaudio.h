#ifndef VC_AUDIO_PLANE_H
#define VC_AUDIO_PLANE_H

#include <stddef.h>

static inline const float *vc_audio_plane(const float *const *planes, int channel) {
    return planes[channel];
}

static inline float vc_audio_sample(const float *const *planes, int channel, int frame) {
    return planes[channel][frame];
}

#endif
