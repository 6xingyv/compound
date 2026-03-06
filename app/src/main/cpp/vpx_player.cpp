#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <android/log.h>
#include <thread>
#include <atomic>
#include <mutex>
#include <condition_variable>
#include <string>
#include <vector>

#include "webm/webm_parser.h"
#include "webm/callback.h"
#include "webm/status.h"
#include "vpx/vpx_decoder.h"
#include "vpx/vp8dx.h"
#include "webm/file_reader.h"

#define LOG_TAG "VpxPlayerNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


static const char* VERTEX_SHADER = R"(
    attribute vec4 aPosition;
    attribute vec2 aTexCoord;
    varying vec2 vTexCoord;
    void main() {
        gl_Position = aPosition;
        vTexCoord = aTexCoord;
    }
)";

static const char* FRAGMENT_SHADER = R"(
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D texY, texU, texV, texA;

void main() {
    float y = texture2D(texY, vTexCoord).r;
    float u = texture2D(texU, vTexCoord).r - 0.5;
    float v = texture2D(texV, vTexCoord).r - 0.5;
    float a = texture2D(texA, vTexCoord).r;

    vec3 rgb;
    rgb.r = y + 1.402 * v;
    rgb.g = y - 0.34414 * u - 0.71414 * v;
    rgb.b = y + 1.772 * u;

    gl_FragColor = vec4(rgb * a, a);
}
)";


class VpxDecoderContext {
public:
    std::string filePath;
    ANativeWindow* window = nullptr;
    std::thread decodeThread;
    std::atomic<bool> playing{false}, quit{false};
    std::atomic<int> surfaceWidth{0}, surfaceHeight{0};

    // EGL 资源
    EGLDisplay display = EGL_NO_DISPLAY;
    EGLSurface surface = EGL_NO_SURFACE;
    EGLContext context = EGL_NO_CONTEXT;
    GLuint program = 0;
    GLuint textures[4] = {0}; // Y, U, V, A

    std::mutex mtx;
    std::condition_variable cv;

    VpxDecoderContext(const char* path) : filePath(path) {}
    ~VpxDecoderContext() { stop(); if (window) ANativeWindow_release(window); }

    void setSurface(ANativeWindow* currWindow, int w, int h) {
        std::lock_guard<std::mutex> lock(mtx);
        if (window) ANativeWindow_release(window);
        window = currWindow;
        surfaceWidth = w; surfaceHeight = h;
        if (window) ANativeWindow_acquire(window);
        cv.notify_all();
    }

    void play(bool loop) {
        if (playing.exchange(true)) return;
        quit = false;
        decodeThread = std::thread(&VpxDecoderContext::decodeLoop, this, loop);
    }

    void stop() {
        quit = true; playing = false;
        cv.notify_all();
        if (decodeThread.joinable()) decodeThread.join();
    }

    void render(vpx_image_t* img, vpx_image_t* alphaImg);

private:
    bool initEGL();
    void destroyEGL();
    void decodeLoop(bool loop);

    GLuint loadShader(GLenum type, const char* src) {
        GLuint s = glCreateShader(type);
        glShaderSource(s, 1, &src, nullptr);
        glCompileShader(s);
        GLint ok; glGetShaderiv(s, GL_COMPILE_STATUS, &ok);
        if (!ok) { char buf[512]; glGetShaderInfoLog(s, 512, nullptr, buf); LOGE("Shader: %s", buf); glDeleteShader(s); return 0; }
        return s;
    }

    void uploadPlane(GLuint tex, const uint8_t* data, int w, int h, int stride) {
        glBindTexture(GL_TEXTURE_2D, tex);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        if (stride == w) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, w, h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data);
        } else {
            std::vector<uint8_t> packed(w * h);
            for (int y = 0; y < h; y++) memcpy(packed.data() + y * w, data + y * stride, w);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, w, h, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, packed.data());
        }
    }
};

// ==================== EGL ====================

bool VpxDecoderContext::initEGL() {
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, nullptr, nullptr);

    const EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8,
        EGL_NONE
    };
    EGLConfig config; EGLint nc;
    eglChooseConfig(display, attribs, &config, 1, &nc);

    const EGLint ctxAttr[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
    context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctxAttr);

    { std::unique_lock<std::mutex> lk(mtx);
      cv.wait(lk, [this]{ return window != nullptr || quit; }); }
    if (quit) return false;

    surface = eglCreateWindowSurface(display, config, window, nullptr);
    eglMakeCurrent(display, surface, surface, context);

    GLuint vs = loadShader(GL_VERTEX_SHADER, VERTEX_SHADER);
    GLuint fs = loadShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
    program = glCreateProgram();
    glAttachShader(program, vs); glAttachShader(program, fs);
    glLinkProgram(program); glUseProgram(program);

    glGenTextures(4, textures);
    for (int i = 0; i < 4; i++) {
        glBindTexture(GL_TEXTURE_2D, textures[i]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
    glUniform1i(glGetUniformLocation(program, "texY"), 0);
    glUniform1i(glGetUniformLocation(program, "texU"), 1);
    glUniform1i(glGetUniformLocation(program, "texV"), 2);
    glUniform1i(glGetUniformLocation(program, "texA"), 3);

    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    return true;
}

void VpxDecoderContext::destroyEGL() {
    if (display != EGL_NO_DISPLAY) {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (context != EGL_NO_CONTEXT) eglDestroyContext(display, context);
        if (surface != EGL_NO_SURFACE) eglDestroySurface(display, surface);
        eglTerminate(display);
    }
    display = EGL_NO_DISPLAY; context = EGL_NO_CONTEXT; surface = EGL_NO_SURFACE;
}

// ==================== Render ====================

void VpxDecoderContext::render(vpx_image_t* img, vpx_image_t* alphaImg) {
    int sw = surfaceWidth.load(), sh = surfaceHeight.load();
    if (sw <= 0 || sh <= 0) {
        eglQuerySurface(display, surface, EGL_WIDTH, &sw);
        eglQuerySurface(display, surface, EGL_HEIGHT, &sh);
    }

    glViewport(0, 0, sw, sh);
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    // Upload YUV
    glActiveTexture(GL_TEXTURE0);
    uploadPlane(textures[0], img->planes[VPX_PLANE_Y], img->d_w, img->d_h, img->stride[VPX_PLANE_Y]);
    glActiveTexture(GL_TEXTURE1);
    uploadPlane(textures[1], img->planes[VPX_PLANE_U], (img->d_w+1)/2, (img->d_h+1)/2, img->stride[VPX_PLANE_U]);
    glActiveTexture(GL_TEXTURE2);
    uploadPlane(textures[2], img->planes[VPX_PLANE_V], (img->d_w+1)/2, (img->d_h+1)/2, img->stride[VPX_PLANE_V]);

    // Upload Alpha
    glActiveTexture(GL_TEXTURE3);
    if (alphaImg) {
        uploadPlane(textures[3], alphaImg->planes[VPX_PLANE_Y], alphaImg->d_w, alphaImg->d_h, alphaImg->stride[VPX_PLANE_Y]);
    } else {
        std::vector<uint8_t> opaque(img->d_w * img->d_h, 255);
        uploadPlane(textures[3], opaque.data(), img->d_w, img->d_h, img->d_w);
    }

    glUseProgram(program);

    // Aspect-fit scaling
    float va = (float)img->d_w / (float)img->d_h;
    float sa = (float)sw / (float)sh;
    float sx = 1.0f, sy = 1.0f;
    if (va > sa) sy = sa / va; else sx = va / sa;

    GLint posLoc = glGetAttribLocation(program, "aPosition");
    GLint texLoc = glGetAttribLocation(program, "aTexCoord");

    float verts[] = {
        -sx, -sy, 0.f, 1.f,
         sx, -sy, 1.f, 1.f,
        -sx,  sy, 0.f, 0.f,
         sx,  sy, 1.f, 0.f,
    };
    glVertexAttribPointer(posLoc, 2, GL_FLOAT, GL_FALSE, 4*sizeof(float), verts);
    glEnableVertexAttribArray(posLoc);
    glVertexAttribPointer(texLoc, 2, GL_FLOAT, GL_FALSE, 4*sizeof(float), verts+2);
    glEnableVertexAttribArray(texLoc);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    eglSwapBuffers(display, surface);
}

// ==================== WebM Callback ====================

class WebmHandler : public webm::Callback {
    VpxDecoderContext* ctx;
    vpx_codec_ctx_t* video_codec;
    vpx_codec_ctx_t* alpha_codec;

    uint64_t video_track_num = 0;
    bool is_video_block = false;
    vpx_image_t* last_video_img = nullptr;

    // Cluster-level absolute timecode (nanoseconds -> we store the raw cluster timecode)
    uint64_t cluster_timecode = 0;
    uint64_t timecode_scale = 1000000; // default 1ms

    // Frame pacing
    long long prev_abs_ns = -1;

public:
    WebmHandler(VpxDecoderContext* c, vpx_codec_ctx_t* vc, vpx_codec_ctx_t* ac)
        : ctx(c), video_codec(vc), alpha_codec(ac) {}

    // Capture timecode scale from Info
    webm::Status OnInfo(const webm::ElementMetadata& metadata,
                        const webm::Info& info) override {
        if (info.timecode_scale.is_present())
            timecode_scale = info.timecode_scale.value();
        return webm::Status(webm::Status::kOkCompleted);
    }

    // Capture video track number
    webm::Status OnTrackEntry(const webm::ElementMetadata& metadata,
                              const webm::TrackEntry& track_entry) override {
        if (track_entry.track_type.value() == webm::TrackType::kVideo) {
            video_track_num = track_entry.track_number.value();
        }
        return webm::Status(webm::Status::kOkCompleted);
    }

    // Capture cluster timecode
    webm::Status OnClusterBegin(const webm::ElementMetadata& metadata,
                                const webm::Cluster& cluster,
                                webm::Action* action) override {
        if (cluster.timecode.is_present())
            cluster_timecode = cluster.timecode.value();
        *action = webm::Action::kRead;
        return webm::Status(webm::Status::kOkCompleted);
    }

    // ---- SimpleBlock (no alpha) ----

    webm::Status OnSimpleBlockBegin(const webm::ElementMetadata& metadata,
                                    const webm::SimpleBlock& sb,
                                    webm::Action* action) override {
        is_video_block = (sb.track_number == video_track_num);
        *action = is_video_block ? webm::Action::kRead : webm::Action::kSkip;
        return webm::Status(webm::Status::kOkCompleted);
    }

    webm::Status OnSimpleBlockEnd(const webm::ElementMetadata& metadata,
                                  const webm::SimpleBlock& sb) override {
        if (is_video_block && last_video_img) {
            ctx->render(last_video_img, nullptr);
            doPacing(sb.timecode);
            last_video_img = nullptr;
        }
        return webm::Status(webm::Status::kOkCompleted);
    }

    // ---- BlockGroup (may have alpha in BlockAdditions) ----

    webm::Status OnBlockBegin(const webm::ElementMetadata& metadata,
                              const webm::Block& block,
                              webm::Action* action) override {
        is_video_block = (block.track_number == video_track_num);
        *action = is_video_block ? webm::Action::kRead : webm::Action::kSkip;
        return webm::Status(webm::Status::kOkCompleted);
    }

    webm::Status OnBlockEnd(const webm::ElementMetadata& metadata,
                            const webm::Block& block) override {
        // Don't render here — wait for OnBlockGroupEnd which has the full BlockAdditions
        return webm::Status(webm::Status::kOkCompleted);
    }

    webm::Status OnBlockGroupEnd(const webm::ElementMetadata& metadata,
                                 const webm::BlockGroup& bg) override {
        if (!is_video_block || !last_video_img)
            return webm::Status(webm::Status::kOkCompleted);

        vpx_image_t* alpha_img = nullptr;

        // Extract alpha from BlockAdditions if present
        if (bg.additions.is_present()) {
            const auto& additions = bg.additions.value();
            for (const auto& more_elem : additions.block_mores) {
                const auto& bm = more_elem.value();
                // BlockAddID == 1 is the alpha channel by convention
                if (bm.id.value() == 1 && bm.data.is_present()) {
                    const auto& alpha_data = bm.data.value();
                    if (!alpha_data.empty()) {
                        vpx_codec_err_t err = vpx_codec_decode(
                            alpha_codec, alpha_data.data(), alpha_data.size(), nullptr, 0);
                        if (err == VPX_CODEC_OK) {
                            vpx_codec_iter_t it = nullptr;
                            alpha_img = vpx_codec_get_frame(alpha_codec, &it);
                        }
                    }
                    break;
                }
            }
        }

        ctx->render(last_video_img, alpha_img);
        doPacing(bg.block.is_present() ? bg.block.value().timecode : 0);
        last_video_img = nullptr;
        return webm::Status(webm::Status::kOkCompleted);
    }

    // ---- Frame data (shared for SimpleBlock and Block) ----

    webm::Status OnFrame(const webm::FrameMetadata& metadata,
                         webm::Reader* reader,
                         std::uint64_t* bytes_remaining) override {
        if (!is_video_block || !bytes_remaining || *bytes_remaining == 0) {
            return webm::Status(webm::Status::kOkCompleted);
        }

        std::vector<uint8_t> buf(*bytes_remaining);
        std::uint64_t read = 0;
        auto st = reader->Read(*bytes_remaining, buf.data(), &read);
        if (!st.ok()) return st;
        *bytes_remaining -= read;

        vpx_codec_err_t err = vpx_codec_decode(video_codec, buf.data(), buf.size(), nullptr, 0);
        if (err == VPX_CODEC_OK) {
            vpx_codec_iter_t it = nullptr;
            vpx_image_t* img;
            while ((img = vpx_codec_get_frame(video_codec, &it)) != nullptr) {
                last_video_img = img;
            }
        }
        return webm::Status(webm::Status::kOkCompleted);
    }

private:
    void doPacing(int16_t block_timecode) {
        // Absolute time in nanoseconds
        long long abs_ns = (long long)(cluster_timecode + block_timecode) * (long long)timecode_scale;
        if (prev_abs_ns >= 0 && abs_ns > prev_abs_ns) {
            long long delta_ms = (abs_ns - prev_abs_ns) / 1000000LL;
            if (delta_ms > 0 && delta_ms < 200) {
                std::this_thread::sleep_for(std::chrono::milliseconds(delta_ms));
            }
        }
        prev_abs_ns = abs_ns;
    }
};

// ==================== Decode Loop ====================

void VpxDecoderContext::decodeLoop(bool loop) {
    if (!initEGL()) return;

    do {
        FILE* f = fopen(filePath.c_str(), "rb");
        if (!f) { LOGE("Failed to open %s", filePath.c_str()); break; }

        webm::FileReader reader(f);
        webm::WebmParser parser;
        vpx_codec_ctx_t v_codec = {}, a_codec = {};
        vpx_codec_dec_cfg_t cfg = {};
        cfg.threads = 2;

        vpx_codec_dec_init(&v_codec, vpx_codec_vp9_dx(), &cfg, 0);
        vpx_codec_dec_init(&a_codec, vpx_codec_vp9_dx(), &cfg, 0);

        WebmHandler handler(this, &v_codec, &a_codec);

        while (!quit) {
            auto status = parser.Feed(&handler, &reader);
            if (status.completed_ok() || !status.ok()) break;
        }

        vpx_codec_destroy(&v_codec);
        vpx_codec_destroy(&a_codec);
        fclose(f);
    } while (loop && !quit);

    destroyEGL();
}

// ==================== JNI ====================

extern "C" JNIEXPORT jlong JNICALL
Java_com_mocharealm_compound_ui_composable_base_VpxDecoder_nativeInit(
        JNIEnv *env, jobject thiz, jstring jFilePath) {
    const char *filePath = env->GetStringUTFChars(jFilePath, nullptr);
    auto* ctx = new VpxDecoderContext(filePath);
    env->ReleaseStringUTFChars(jFilePath, filePath);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mocharealm_compound_ui_composable_base_VpxDecoder_nativeSetSurface(
        JNIEnv *env, jobject thiz, jlong ptr, jobject surf, jint width, jint height) {
    auto* ctx = reinterpret_cast<VpxDecoderContext*>(ptr);
    if (!ctx) return;
    ANativeWindow* window = surf ? ANativeWindow_fromSurface(env, surf) : nullptr;
    ctx->setSurface(window, width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mocharealm_compound_ui_composable_base_VpxDecoder_nativePlay(
        JNIEnv *env, jobject thiz, jlong ptr, jboolean loop) {
    auto* ctx = reinterpret_cast<VpxDecoderContext*>(ptr);
    if (ctx) ctx->play(loop);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mocharealm_compound_ui_composable_base_VpxDecoder_nativeStop(
        JNIEnv *env, jobject thiz, jlong ptr) {
    auto* ctx = reinterpret_cast<VpxDecoderContext*>(ptr);
    if (ctx) ctx->stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_mocharealm_compound_ui_composable_base_VpxDecoder_nativeRelease(
        JNIEnv *env, jobject thiz, jlong ptr) {
    auto* ctx = reinterpret_cast<VpxDecoderContext*>(ptr);
    if (ctx) delete ctx;
}
