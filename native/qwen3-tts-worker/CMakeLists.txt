cmake_minimum_required(VERSION 3.20)
project(docupodcast_qwen3_tts_worker LANGUAGES C CXX)

if(NOT DEFINED LLAMA_CPP_ROOT)
    message(FATAL_ERROR "Set LLAMA_CPP_ROOT to the matching llama.cpp source tree")
endif()

set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_MTMD ON CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
add_subdirectory("${LLAMA_CPP_ROOT}" llama.cpp)

add_executable(llama-tts-worker tts-worker.cpp)
target_link_libraries(llama-tts-worker PRIVATE llama llama-common mtmd ${CMAKE_THREAD_LIBS_INIT})
target_compile_features(llama-tts-worker PRIVATE cxx_std_17)
