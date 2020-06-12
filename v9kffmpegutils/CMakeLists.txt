# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(ffmpeg_DIR ${CMAKE_SOURCE_DIR}/src/main/cpp/ffmpeg)
set(x264_DIR ${CMAKE_SOURCE_DIR}/src/main/cpp/x264)

include_directories(${ffmpeg_DIR}/lib/${ANDROID_ABI}/include)
include_directories(${x264_DIR}/lib/${ANDROID_ABI}/include)

add_library(avutil-56 SHARED IMPORTED)
set_target_properties(avutil-56 PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavutil-56.so)

add_library(avformat-58 SHARED IMPORTED)
set_target_properties(avformat-58 PROPERTIES IMPORTED_LOCATION
         ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavformat-58.so)

add_library(avcodec-58 SHARED IMPORTED)
set_target_properties(avcodec-58 PROPERTIES IMPORTED_LOCATION
         ${ffmpeg_DIR}/lib/${ANDROID_ABI}/libavcodec-58.so)

add_library(x264 STATIC IMPORTED)
set_target_properties(x264 PROPERTIES IMPORTED_LOCATION
        ${x264_DIR}/lib/${ANDROID_ABI}/libx264.a)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             v9kffmpegutils

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/v9kffmpegutils.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       v9kffmpegutils

                       avutil-56
                       avformat-58
                       avcodec-58
                       x264
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib})
