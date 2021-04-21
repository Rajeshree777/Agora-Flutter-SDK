/// \file
/// \addtogroup Types
/// @{
///
// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from types.djinni

#pragma once

#include <bnb/utils/defs.hpp>
#include <vector>

namespace bnb { namespace interfaces {

struct camera_position;
struct pixel_rect;

class BNB_EXPORT face_data {
public:
    virtual ~face_data() {}

    virtual std::vector<float> get_landmarks() const = 0;

    virtual std::vector<float> get_latents() const = 0;

    virtual std::vector<float> get_vertices() const = 0;

    virtual camera_position get_camera_position() const = 0;

    virtual bool has_face() = 0;

    virtual pixel_rect get_face_rect() = 0;
};

} }  // namespace bnb::interfaces
/// @}

