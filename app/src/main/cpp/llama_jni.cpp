#include <jni.h>
#include <algorithm>
#include <atomic>
#include <climits>
#include <limits>
#include <cstdint>
#include <mutex>
#include <string>
#include <vector>
#include "llama.h"

namespace {
std::mutex engine_mutex;
std::atomic<uint64_t> cancel_generation{0};
llama_model *model = nullptr;
llama_context *ctx = nullptr;
int context_capacity = 2048;
int worker_threads = 4;

std::string as_string(JNIEnv *env, jstring value) {
    if (!value) return {};
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

void unload() {
    cancel_generation.fetch_add(1);
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
}

// Convert raw UTF-8 bytes through Java UTF-8 decoder; NewStringUTF is not safe here.
jstring to_java_string(JNIEnv *env, const std::string &value) {
    jclass string_class = env->FindClass("java/lang/String");
    if (!string_class) return nullptr;
    jclass charset_class = env->FindClass("java/nio/charset/StandardCharsets");
    if (!charset_class) { env->DeleteLocalRef(string_class); return nullptr; }
    jfieldID utf8_field = env->GetStaticFieldID(charset_class, "UTF_8", "Ljava/nio/charset/Charset;");
    if (!utf8_field) { env->DeleteLocalRef(charset_class); env->DeleteLocalRef(string_class); return nullptr; }
    jobject charset = env->GetStaticObjectField(charset_class, utf8_field);
    jmethodID ctor = env->GetMethodID(string_class, "<init>", "([BLjava/nio/charset/Charset;)V");
    if (!ctor || !charset || value.size() > static_cast<size_t>(std::numeric_limits<jsize>::max())) {
        if (charset) env->DeleteLocalRef(charset);
        env->DeleteLocalRef(charset_class); env->DeleteLocalRef(string_class); return nullptr;
    }
    jbyteArray bytes = env->NewByteArray(static_cast<jsize>(value.size()));
    if (!bytes) { env->DeleteLocalRef(charset); env->DeleteLocalRef(charset_class); env->DeleteLocalRef(string_class); return nullptr; }
    if (!value.empty()) env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(value.size()), reinterpret_cast<const jbyte *>(value.data()));
    jobject result = env->NewObject(string_class, ctor, bytes, charset);
    env->DeleteLocalRef(bytes); env->DeleteLocalRef(charset); env->DeleteLocalRef(charset_class); env->DeleteLocalRef(string_class);
    return static_cast<jstring>(result);
}

bool reset_context() {
    if (!model) return false;
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    auto cp = llama_context_default_params();
    cp.n_ctx = static_cast<uint32_t>(context_capacity);
    cp.n_batch = 256;
    cp.n_threads = worker_threads;
    ctx = llama_init_from_model(model, cp);
    return ctx != nullptr;
}

std::string token_piece(const llama_vocab *vocab, llama_token token) {
    std::vector<char> buffer(256);
    int32_t count = llama_token_to_piece(vocab, token, buffer.data(),
                                          static_cast<int32_t>(buffer.size()), 0, false);
    if (count < 0) {
        buffer.resize(static_cast<size_t>(-count));
        count = llama_token_to_piece(vocab, token, buffer.data(),
                                     static_cast<int32_t>(buffer.size()), 0, false);
    }
    return count > 0 ? std::string(buffer.data(), static_cast<size_t>(count)) : std::string();
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_automatesmartbot_data_ai_LocalLlamaBridge_loadModel(
    JNIEnv *env, jobject, jstring path, jint contextSize, jint threads) {
    std::lock_guard<std::mutex> lock(engine_mutex);
    unload();
    const auto filename = as_string(env, path);
    if (filename.empty()) return JNI_FALSE;
    llama_backend_init();
    auto mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    model = llama_model_load_from_file(filename.c_str(), mp);
    if (!model) return JNI_FALSE;
    context_capacity = std::clamp(static_cast<int>(contextSize), 512, 8192);
    worker_threads = std::clamp(static_cast<int>(threads), 1, 8);
    if (!reset_context()) { unload(); return JNI_FALSE; }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_automatesmartbot_data_ai_LocalLlamaBridge_generate(
    JNIEnv *env, jobject, jstring prompt, jint maxNewTokens) {
    std::lock_guard<std::mutex> lock(engine_mutex);
    const uint64_t generation = cancel_generation.load();
    if (!model || !ctx) return env->NewStringUTF("");
    const auto input = as_string(env, prompt);
    if (input.empty() || input.size() > static_cast<size_t>(INT32_MAX) || !reset_context()) return env->NewStringUTF("");
    if (cancel_generation.load() != generation) return env->NewStringUTF("");

    const llama_vocab *vocab = llama_model_get_vocab(model);
    int32_t count = llama_tokenize(vocab, input.c_str(),
                                   static_cast<int32_t>(input.size()), nullptr, 0, true, true);
    if (count == INT32_MIN) return env->NewStringUTF("");
    count = count < 0 ? -count : count;
    if (count <= 0 || count >= context_capacity - 1) return env->NewStringUTF("");
    std::vector<llama_token> tokens(static_cast<size_t>(count));
    count = llama_tokenize(vocab, input.c_str(),
                           static_cast<int32_t>(input.size()), tokens.data(), count, true, true);
    if (count <= 0) return env->NewStringUTF("");
    tokens.resize(static_cast<size_t>(count));

    const int output_limit = std::min({std::max(1, static_cast<int>(maxNewTokens)),
                                       512, context_capacity - count - 1});
    if (output_limit <= 0) return env->NewStringUTF("");
    // Prompt prefill in chunks: avoid exceeding n_batch=256.
    for (int offset = 0; offset < count; offset += 256) {
        const int size = std::min(256, count - offset);
        auto batch = llama_batch_get_one(tokens.data() + offset, size);
        if (cancel_generation.load() != generation || llama_decode(ctx, batch) != 0) return env->NewStringUTF("");
    }

    llama_sampler *sampler = llama_sampler_init_greedy();
    if (!sampler) return env->NewStringUTF("");
    std::string output;
    for (int i = 0; i < output_limit && cancel_generation.load() == generation; ++i) {
        const llama_token token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        output += token_piece(vocab, token);
        if (i + 1 >= output_limit) break;
        llama_token next = token;
        auto batch = llama_batch_get_one(&next, 1);
        if (llama_decode(ctx, batch) != 0) break;
    }
    llama_sampler_free(sampler);
    if (cancel_generation.load() != generation) {
        return env->NewStringUTF("");
    }
    return to_java_string(env, output);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_automatesmartbot_data_ai_LocalLlamaBridge_unloadModel(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(engine_mutex);
    unload();
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_automatesmartbot_data_ai_LocalLlamaBridge_isLoaded(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(engine_mutex);
    return model && ctx ? JNI_TRUE : JNI_FALSE;
}

// No engine_mutex: cancellation must be callable while generate() holds that mutex.
extern "C" JNIEXPORT void JNICALL
Java_com_example_automatesmartbot_data_ai_LocalLlamaBridge_cancelGeneration(JNIEnv *, jobject) {
    cancel_generation.fetch_add(1);
}
