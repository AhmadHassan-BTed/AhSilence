cmake_minimum_required(VERSION 3.22.1)

project("ancengine" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# --- Release-only optimization for the DSP hot path -------------------------
# -O3 + loop unrolling for the tight per-sample oscillator/FxLMS loops.
# -fno-math-errno drops errno bookkeeping from libm calls (safe: nothing here
# ever checks errno).
#
# Deliberately NOT using -ffast-math: it permits reordering and flushing of
# small floating-point values -- including legitimate near-zero adaptive
# weights -- in ways that make convergence behavior harder to reason about
# and reproduce while debugging. Denormals are instead handled explicitly via
# the leaky term + epsilon floor inside FxLmsFilter.
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -funroll-loops -fno-math-errno")

# Oboe is consumed as a prefab package from Google Maven (see the note below
# this file for the two lines your module's build.gradle needs).
find_package(oboe REQUIRED CONFIG)

add_library(ancengine SHARED
        dsp/Oscillator.h
        dsp/FxLmsFilter.h
        dsp/SecondaryPathModel.h
        audio/AncEngine.h
        audio/AncEngine.cpp
        jni/native_bridge.cpp
)

target_include_directories(ancengine PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(ancengine
        PRIVATE
        oboe::oboe
        android
        log
)

# ------------------------------------------------------------------------
# Companion build.gradle(.kts) changes (Oboe is not vendored by this file):
#
#   android {
#       defaultConfig {
#           externalNativeBuild { cmake { arguments "-DANDROID_STL=c++_shared" } }
#       }
#       buildFeatures { prefab = true }
#   }
#   dependencies {
#       implementation("com.google.oboe:oboe:1.10.0") // check github.com/google/oboe/releases for newer
#   }
# ------------------------------------------------------------------------
