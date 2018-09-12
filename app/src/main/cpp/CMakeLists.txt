cmake_minimum_required(VERSION 3.4.1)

add_library(
        decryptmetroid SHARED
        decryptmetroid.c
)

target_link_libraries(
        decryptmetroid
)
