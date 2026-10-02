// DocuPodcast persistent batch wrapper for llama.cpp's experimental TTS pipeline.
// Build against the exact llama.cpp revision shipped with the Qwen3-TTS runtime.
#include "arg.h"
#include "common.h"
#include "sampling.h"
#include "log.h"
#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#include <cstdio>
#include <algorithm>
#include <filesystem>
#include <iterator>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

struct request_manifest {
    std::string prompt_file;
    std::string output_file;
    std::string speaker_file;
    std::string language;
    int max_frames = 512;
};

static void usage(int, char ** argv) {
    LOG("\nusage: %s -m backbone.gguf -mm codec.gguf [llama options]\n", argv[0]);
    LOG("reads one five-line request-manifest path per stdin line\n");
}

static bool read_manifest(const std::string & path, request_manifest & out) {
    std::ifstream in(std::filesystem::u8path(path), std::ios::binary);
    std::string frames;
    if (!in || !std::getline(in, out.prompt_file) || !std::getline(in, out.output_file)
            || !std::getline(in, out.speaker_file) || !std::getline(in, out.language)
            || !std::getline(in, frames)) return false;
    for (auto * value : {&out.prompt_file, &out.output_file, &out.speaker_file, &out.language, &frames}) {
        if (!value->empty() && value->back() == '\r') value->pop_back();
    }
    try { out.max_frames = std::max(1, std::stoi(frames)); }
    catch (...) { return false; }
    return true;
}

static std::string read_prompt(const std::string & path) {
    std::ifstream in(std::filesystem::u8path(path), std::ios::binary);
    return in ? std::string(std::istreambuf_iterator<char>(in), {}) : std::string();
}

static bool write_output(const std::string & path, const char * data, size_t size) {
    std::ofstream out(std::filesystem::u8path(path), std::ios::binary | std::ios::trunc);
    if (!out) return false;
    out.write(data, static_cast<std::streamsize>(size));
    return out.good();
}

int main(int argc, char ** argv) {
    common_params params;
    common_init();
    if (!common_params_parse(argc, argv, params, LLAMA_EXAMPLE_TTS, usage)) return 1;
    if (params.mmproj.path.empty()) {
        LOG_ERR("no mmproj provided\n");
        return 1;
    }
    params.embedding = true;
    mtmd_helper_log_set(common_log_default_callback, nullptr);
    llama_backend_init();
    llama_numa_init(params.numa);

    auto llama_init = common_init_from_params(params);
    llama_model * model = llama_init->model();
    llama_context * lctx = llama_init->context();
    common_sampler * sampler = llama_init->sampler(0);
    if (!model || !lctx || !sampler) return 1;

    mtmd_context_params mtmd_params = mtmd_context_params_default();
    mtmd_params.use_gpu = params.mmproj_use_gpu;
    mtmd_params.device = params.mmproj_device;
    mtmd::context_ptr mctx(mtmd_init_from_file(params.mmproj.path.c_str(), model, mtmd_params));
    if (!mctx || mtmd_gen_audio_get_info(mctx.get()).type == MTMD_GEN_AUDIO_TYPE_NONE) return 1;

    mtmd_helper::gen_audio generator(lctx, mctx.get());
    std::string cached_speaker_path;
    mtmd::bitmap_ptr cached_speaker;

    std::cout << "READY" << std::endl;
    std::string manifest_path;
    while (std::getline(std::cin, manifest_path)) {
        if (manifest_path == "QUIT") break;
        request_manifest request;
        if (!read_manifest(manifest_path, request)) {
            std::cout << "ERR\tinvalid-manifest" << std::endl;
            continue;
        }
        std::string prompt = read_prompt(request.prompt_file);
        if (prompt.empty()) {
            std::cout << "ERR\tempty-prompt" << std::endl;
            continue;
        }
        if (request.speaker_file != cached_speaker_path) {
            auto wrapper = mtmd_helper_bitmap_init_from_file(mctx.get(), request.speaker_file.c_str(), false);
            if (!wrapper.bitmap) {
                std::cout << "ERR\tinvalid-speaker" << std::endl;
                continue;
            }
            cached_speaker.reset(wrapper.bitmap);
            cached_speaker_path = request.speaker_file;
        }

        generator.reset();
        common_sampler_reset(sampler);
        llama_memory_clear(llama_get_memory(lctx), true);
        mtmd_helper_gen_audio_inp input{};
        input.seq_id = 0;
        input.prompt = prompt.c_str();
        input.prompt_len = prompt.size();
        input.speaker_ref = cached_speaker.get();
        input.lang = request.language.c_str();
        input.top_k = params.sampling.top_k;
        input.top_p = params.sampling.top_p;
        input.seed = params.sampling.seed;
        input.out_type = MTMD_HELPER_GEN_AUDIO_OUTTYPE_WAV;
        if (generator.set_input(&input) != 0) {
            std::cout << "ERR\tset-input" << std::endl;
            continue;
        }
        bool failed = false;
        for (;;) {
            int result = generator.step_prompt(params.n_batch);
            if (result < 0) { failed = true; break; }
            if (result == 0) break;
        }
        if (failed) {
            std::cout << "ERR\tprompt" << std::endl;
            continue;
        }
        auto sample = [&]() {
            llama_token token = common_sampler_sample(sampler, lctx, -1);
            common_sampler_accept(sampler, token, true);
            return token;
        };
        llama_token sampled = sample();
        const float * hidden = llama_get_embeddings_ith(lctx, -1);
        bool stop = false;
        for (int frame = 0; !stop && frame < request.max_frames; frame++) {
            const float * next = nullptr;
            if (generator.step_gen(sampled, hidden, &next, &stop) != 0) { failed = true; break; }
            if (!next) break;
            hidden = next;
            sampled = sample();
        }
        int32_t sample_rate = 0;
        const char * data = nullptr;
        size_t data_len = 0;
        int64_t samples = 0;
        if (failed || generator.get_output(&sample_rate, &data, &data_len, &samples) != 0
                || !write_output(request.output_file, data, data_len)) {
            std::cout << "ERR\tgeneration" << std::endl;
            continue;
        }
        std::cout << "OK\t" << data_len << "\t" << sample_rate << std::endl;
    }
    llama_backend_free();
    return 0;
}
