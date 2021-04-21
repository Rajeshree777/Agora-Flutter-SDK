/// \file
/// \addtogroup EffectPlayer
/// @{
///
// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from effect_player.djinni

#pragma once

#include <bnb/utils/defs.hpp>
#include <string>

namespace bnb { namespace interfaces {

class BNB_EXPORT debug_interface {
public:
    virtual ~debug_interface() {}

    virtual void set_autotest_config(const std::string & config) = 0;
};

} }  // namespace bnb::interfaces
/// @}

