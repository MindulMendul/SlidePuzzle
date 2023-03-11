# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("slidepuzzle")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)
add_library( # Sets the name of the library.
        JNIDriverGPIO

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        JNIDriverGPIO.c)
add_library( # Sets the name of the library.
        JNIDriver7seg

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        JNIDriver7seg.c)
add_library( # Sets the name of the library.
        JNIDriverLED

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        JNIDriverLED.c)
add_library( # Sets the name of the library.
        OpenCLDriver

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        OpenCLDriver.c)


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
find_library( # Sets the name of the path variable.
        jnigraphics-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        jnigraphics)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
target_link_libraries( # Specifies the target library.
        JNIDriverGPIO

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
target_link_libraries( # Specifies the target library.
        JNIDriver7seg

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
target_link_libraries( # Specifies the target library.
        JNIDriverLED

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
target_link_libraries( # Specifies the target library.
        OpenCLDriver

        # Links the target library to the log library
        # included in the NDK.
        C:/Download/mpclass/libGLES_mali.so ${log-lib} ${jnigraphics-lib})

add_library(GLES_mali STATIC IMPORTED)
find_library( jnigraphics-lib jnigraphics )
target_link_libraries( # Specifies the target library.
        native-lib # OpenCL
        "C:/Download/mpclass/libGLES_mali.so"
        ${log-lib}
        ${jnigraphics-lib}
        )
target_include_directories(native-lib PRIVATE src/main/cpp)