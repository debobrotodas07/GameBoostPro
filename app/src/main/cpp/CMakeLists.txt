cmake_minimum_required(VERSION 3.22)
project(advantage)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(advantage SHARED advantage.cpp)

target_compile_options(advantage PRIVATE
    -O2
    -fvisibility=hidden
    -Wall
    -Wextra
    -Wno-unused-parameter
)

target_link_libraries(advantage
    android
    log
    dl
)

set_target_properties(advantage PROPERTIES
    ANDROID_STL c++_shared
)