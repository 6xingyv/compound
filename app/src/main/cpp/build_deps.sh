#!/bin/bash
set -e

# Base directories
BASE_DIR=$(pwd)
SRC_DIR="$BASE_DIR/deps_src"
OUT_DIR="$BASE_DIR/libs"
INC_DIR="$BASE_DIR/include"
NDK_VERSION="android-ndk-r29"
NDK_DIR="$BASE_DIR/$NDK_VERSION"

mkdir -p "$SRC_DIR"
mkdir -p "$OUT_DIR"
mkdir -p "$INC_DIR"

# Download NDK
if [ ! -d "$NDK_DIR" ]; then
    echo "Downloading Linux NDK locally for WSL compatibility..."
    wget -c "https://dl.google.com/android/repository/${NDK_VERSION}-linux.zip" -O ndk.zip
    unzip -q ndk.zip
    rm ndk.zip
fi

export ANDROID_NDK_HOME="$NDK_DIR"
TOOLCHAIN="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64"

ABIS=("arm64-v8a" "armeabi-v7a" "x86_64" "x86")
declare -A VPX_TARGETS
VPX_TARGETS=(
    ["arm64-v8a"]="arm64-android-gcc"
    ["armeabi-v7a"]="armv7-android-gcc"
    ["x86_64"]="x86_64-android-gcc"
    ["x86"]="x86-android-gcc"
)

MIN_SDK=24

# Pull source code
cd "$SRC_DIR"
if [ ! -d "libvpx" ]; then
    echo "Cloning libvpx..."
    git clone https://chromium.googlesource.com/webm/libvpx.git
fi
if [ ! -d "libwebm" ]; then
    echo "Cloning libwebm..."
    git clone https://chromium.googlesource.com/webm/libwebm.git
fi

# Compile libvpx
echo "Building libvpx..."
cd "$SRC_DIR/libvpx"

for ABI in "${ABIS[@]}"; do
    echo "========== Building libvpx for $ABI =========="
    make clean > /dev/null 2>&1 || true
    
    VPX_TARGET=${VPX_TARGETS[$ABI]}
    
    if [ "$ABI" = "arm64-v8a" ]; then
        CROSS="aarch64-linux-android"
    elif [ "$ABI" = "armeabi-v7a" ]; then
        CROSS="armv7a-linux-androideabi"
    elif [ "$ABI" = "x86_64" ]; then
        CROSS="x86_64-linux-android"
    elif [ "$ABI" = "x86" ]; then
        CROSS="i686-linux-android"
    fi
    
    export CC="$TOOLCHAIN/bin/${CROSS}${MIN_SDK}-clang"
    export CXX="$TOOLCHAIN/bin/${CROSS}${MIN_SDK}-clang++"
    export AR="$TOOLCHAIN/bin/llvm-ar"
    export NM="$TOOLCHAIN/bin/llvm-nm"
    export STRIP="$TOOLCHAIN/bin/llvm-strip"
    export LD="$TOOLCHAIN/bin/ld"

    if [[ "$ABI" == "x86" || "$ABI" == "x86_64" ]]; then
        export AS="yasm"
    else
        export AS="$CC"
    fi

    ./configure \
        --target=$VPX_TARGET \
        --disable-examples \
        --disable-tools \
        --disable-docs \
        --enable-multithread \
        --enable-pic \
        --enable-vp8 \
        --enable-vp9 \
        --disable-vp9-encoder \
        --disable-vp8-encoder
        
    make -j$(nproc)
    
    ABI_OUT="$OUT_DIR/$ABI"
    mkdir -p "$ABI_OUT"
    cp libvpx.a "$ABI_OUT/"
done

# Copy libvpx header files
mkdir -p "$INC_DIR/vpx"
cp -r vpx/*.h "$INC_DIR/vpx/"

# Compile libwebm
echo "Building libwebm..."
cd "$SRC_DIR/libwebm"

for ABI in "${ABIS[@]}"; do
    echo "========== Building libwebm for $ABI =========="
    BUILD_DIR="build_$ABI"
    rm -rf "$BUILD_DIR" && mkdir -p "$BUILD_DIR"
    cd "$BUILD_DIR"
    
    cmake .. \
        -DCMAKE_TOOLCHAIN_FILE="$NDK_DIR/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI=$ABI \
        -DANDROID_NATIVE_API_LEVEL=$MIN_SDK \
        -DANDROID_STL=c++_shared \
        -DBUILD_SHARED_LIBS=OFF \
        -DENABLE_WEBM_PARSER=ON \
        -DENABLE_WEBM_MUXER=ON \
        -DCMAKE_CXX_STANDARD=17 \
        -DCMAKE_BUILD_TYPE=Release
        
    make -j$(nproc)
    
    ABI_OUT="$OUT_DIR/$ABI"
    cp libwebm.a "$ABI_OUT/"
    cd ..
done

# Copy libwebm header files
mkdir -p "$INC_DIR/libwebm"
cp -r "$SRC_DIR/libwebm/webm_parser/include/webm" "$INC_DIR/libwebm/"
cp "$SRC_DIR/libwebm"/*.h "$INC_DIR/libwebm/"

echo "-------------------------------------------"
echo "Build complete!"
echo "Libs: $OUT_DIR"
echo "Headers: $INC_DIR"