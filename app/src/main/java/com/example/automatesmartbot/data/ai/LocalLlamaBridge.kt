package com.example.automatesmartbot.data.ai

/** Native llama.cpp bridge. Call model loading and inference off the main UI thread. */
class LocalLlamaBridge {
    companion object {
        init { System.loadLibrary("karirmate_llama") }
    }

    external fun loadModel(absolutePath: String, contextSize: Int = 2048, threads: Int = 4): Boolean
    external fun generate(prompt: String, maxNewTokens: Int = 160): String
    external fun isLoaded(): Boolean
    external fun unloadModel()
    external fun cancelGeneration()
}
