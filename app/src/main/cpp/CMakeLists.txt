cmake_minimum_required(VERSION 3.22.1)

project("vpxplayer")

find_library(log-lib log)
find_library(android-lib android)
find_library(egl-lib EGL)
find_library(gles2-lib GLESv2)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

add_library(vpx STATIC IMPORTED)
set_target_properties(vpx PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libvpx.a)

add_library(webm STATIC IMPORTED)
set_target_properties(webm PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI}/libwebm.a)

add_library(
        vpxplayer
        SHARED
        vpx_player.cpp
)

set_target_properties(vpxplayer PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
)

target_link_libraries(
        vpxplayer
        vpx
        webm
        ${log-lib}
        ${android-lib}
        ${egl-lib}
        ${gles2-lib}
)