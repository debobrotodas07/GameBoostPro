#include <jni.h>
#include <dlfcn.h>
#include <sys/mman.h>
#include <android/log.h>
#include <cstring>
#include <vector>
#include <string>
#include <unistd.h>

#define LOG_TAG "AdvantageEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static float g_speedMultiplier = 1.0f;
static float g_aimSmoothFactor = 0.0f;

typedef int (*clock_gettime_t)(clockid_t, struct timespec*);
static clock_gettime_t orig_clock_gettime = nullptr;

extern "C" int hooked_clock_gettime(clockid_t clk_id, struct timespec* tp) {
    int ret = orig_clock_gettime(clk_id, tp);
    if (ret == 0 && clk_id == CLOCK_MONOTONIC && g_speedMultiplier > 1.0f) {
        tp->tv_sec = static_cast<time_t>(tp->tv_sec / g_speedMultiplier);
        tp->tv_nsec = static_cast<long>(tp->tv_nsec / g_speedMultiplier);
        if (tp->tv_nsec >= 1000000000L) {
            tp->tv_nsec -= 1000000000L;
            tp->tv_sec += 1;
        }
    }
    return ret;
}

static void install_clock_hook() {
    void* handle = dlopen("libc.so", RTLD_NOW);
    if (handle) {
        orig_clock_gettime = reinterpret_cast<clock_gettime_t>(dlsym(handle, "clock_gettime"));
        if (orig_clock_gettime) {
            void* plt = dlsym(RTLD_DEFAULT, "clock_gettime");
            if (plt && plt != orig_clock_gettime) {
                size_t page_size = getpagesize();
                uintptr_t addr = reinterpret_cast<uintptr_t>(plt);
                uintptr_t page_start = addr & ~(page_size - 1);
                if (mprotect(reinterpret_cast<void*>(page_start), page_size,
                             PROT_READ | PROT_WRITE | PROT_EXEC) == 0) {
                    uint8_t* ptr = reinterpret_cast<uint8_t*>(plt);
                    uintptr_t target = reinterpret_cast<uintptr_t>(hooked_clock_gettime);
                    ptr[0] = 0x58; // ldr x8, #8
                    ptr[1] = 0x00;
                    ptr[2] = 0x00;
                    ptr[3] = 0x14; // b #8
                    *reinterpret_cast<uintptr_t*>(ptr + 4) = target;
                    __builtin___clear_cache(reinterpret_cast<char*>(ptr), reinterpret_cast<char*>(ptr + 12));
                    mprotect(reinterpret_cast<void*>(page_start), page_size,
                             PROT_READ | PROT_EXEC);
                    LOGD("clock_gettime hooked at %p", plt);
                }
            }
        }
        dlclose(handle);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_gameboost_pro_advantage_AdvantageEngine_initSpeedHack(JNIEnv* env, jclass, jfloat multiplier) {
    g_speedMultiplier = multiplier;
    if (multiplier > 1.0f && !orig_clock_gettime) {
        install_clock_hook();
    }
    LOGD("Speed hack init: multiplier=%.2f", multiplier);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gameboost_pro_advantage_AdvantageEngine_setSpeedMultiplier(JNIEnv* env, jclass, jfloat multiplier) {
    g_speedMultiplier = multiplier;
    LOGD("Speed multiplier set: %.2f", multiplier);
}

extern "C" JNIEXPORT void JNICALL
Java_com_gameboost_pro_advantage_AdvantageEngine_setAimSmooth(JNIEnv* env, jclass, jfloat factor) {
    g_aimSmoothFactor = factor;
    LOGD("Aim smooth factor set: %.2f", factor);
}

static std::vector<uint8_t> find_pattern(const uint8_t* data, size_t size, const uint8_t* pattern, size_t pattern_size) {
    std::vector<uint8_t> matches;
    for (size_t i = 0; i + pattern_size <= size; ++i) {
        bool match = true;
        for (size_t j = 0; j < pattern_size; ++j) {
            if (data[i + j] != pattern[j]) {
                match = false;
                break;
            }
        }
        if (match) {
            matches.insert(matches.end(), data + i, data + i + pattern_size);
            return matches;
        }
    }
    return {};
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_gameboost_pro_advantage_AdvantageEngine_patchMemoryValue(
        JNIEnv* env, jclass, jstring lib_name, jbyteArray pattern_arr, jbyteArray replacement_arr) {

    const char* lib = env->GetStringUTFChars(lib_name, nullptr);
    jbyte* pattern = env->GetByteArrayElements(pattern_arr, nullptr);
    jbyte* replacement = env->GetByteArrayElements(replacement_arr, nullptr);
    jsize pattern_len = env->GetArrayLength(pattern_arr);
    jsize repl_len = env->GetArrayLength(replacement_arr);

    if (pattern_len != repl_len) {
        env->ReleaseStringUTFChars(lib_name, lib);
        env->ReleaseByteArrayElements(pattern_arr, pattern, JNI_ABORT);
        env->ReleaseByteArrayElements(replacement_arr, replacement, JNI_ABORT);
        return JNI_FALSE;
    }

    FILE* maps = fopen("/proc/self/maps", "r");
    if (!maps) {
        LOGE("Failed to open /proc/self/maps");
        env->ReleaseStringUTFChars(lib_name, lib);
        env->ReleaseByteArrayElements(pattern_arr, pattern, JNI_ABORT);
        env->ReleaseByteArrayElements(replacement_arr, replacement, JNI_ABORT);
        return JNI_FALSE;
    }

    char line[1024];
    uintptr_t lib_base = 0;
    size_t lib_size = 0;
    while (fgets(line, sizeof(line), maps)) {
        if (strstr(line, lib) && strstr(line, "r-xp")) {
            uintptr_t start, end;
            if (sscanf(line, "%lx-%lx", &start, &end) == 2) {
                lib_base = start;
                lib_size = end - start;
                break;
            }
        }
    }
    fclose(maps);

    if (!lib_base) {
        LOGE("Library %s not found in maps", lib);
        env->ReleaseStringUTFChars(lib_name, lib);
        env->ReleaseByteArrayElements(pattern_arr, pattern, JNI_ABORT);
        env->ReleaseByteArrayElements(replacement_arr, replacement, JNI_ABORT);
        return JNI_FALSE;
    }

    size_t page_size = getpagesize();
    uint8_t* scan_ptr = reinterpret_cast<uint8_t*>(lib_base);
    bool patched = false;

    for (size_t offset = 0; offset + pattern_len <= lib_size; offset += page_size) {
        uintptr_t page_start = lib_base + offset;
        uintptr_t page_end = page_start + page_size;
        if (page_end > lib_base + lib_size) page_end = lib_base + lib_size;

        if (mprotect(reinterpret_cast<void*>(page_start & ~(page_size - 1)), page_size,
                     PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
            continue;
        }

        std::vector<uint8_t> found = find_pattern(scan_ptr + offset, page_end - page_start,
                                                   reinterpret_cast<const uint8_t*>(pattern), pattern_len);
        if (!found.empty()) {
            uint8_t* target = scan_ptr + offset + (found.data() - (scan_ptr + offset));
            memcpy(target, replacement, repl_len);
            __builtin___clear_cache(reinterpret_cast<char*>(target), reinterpret_cast<char*>(target + repl_len));
            patched = true;
            LOGD("Patched %s at offset 0x%lx", lib, offset + (target - scan_ptr));
            break;
        }

        mprotect(reinterpret_cast<void*>(page_start & ~(page_size - 1)), page_size,
                 PROT_READ | PROT_EXEC);
    }

    env->ReleaseStringUTFChars(lib_name, lib);
    env->ReleaseByteArrayElements(pattern_arr, pattern, JNI_ABORT);
    env->ReleaseByteArrayElements(replacement_arr, replacement, JNI_ABORT);

    return patched ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_gameboost_pro_advantage_AdvantageEngine_getSpeedMultiplier(JNIEnv* env, jclass) {
    return g_speedMultiplier;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_gameboost_pro_advantage_AdvantageEngine_getAimSmoothFactor(JNIEnv* env, jclass) {
    return g_aimSmoothFactor;
}