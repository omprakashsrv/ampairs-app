package com.ampairs.agent.speech.whisper

import co.touchlab.kermit.Logger
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import kotlin.concurrent.Volatile

/**
 * Android Whisper inference via **LiteRT** (`com.google.ai.edge.litert`, classic `org.tensorflow.lite`
 * Interpreter API). Runs the all-in-one `whisper.tflite` graph (encoder + decoder + greedy decoding):
 * log-mel in → token ids out. The mel front-end ([WhisperFeatureExtractor]) and detokenizer
 * ([WhisperTokenizer]) are shared commonMain code; their mel/vocab data comes from the downloadable
 * [WhisperModelCatalog.tfliteFilters] companion (resolved here via [ModelManager]).
 *
 * Tensor shapes / token handling are runtime-tuned on device against the exact model.
 */
class AndroidWhisperTranscriber(
    private val modelManager: ModelManager,
    private val filtersModel: ModelDescriptor,
) : WhisperTranscriber {

    private val mutex = Mutex()

    @Volatile private var interpreter: Interpreter? = null
    @Volatile private var modelPathLoaded: String? = null
    @Volatile private var extractor: WhisperFeatureExtractor? = null
    @Volatile private var tokenizer: WhisperTokenizer? = null
    @Volatile private var assetsPathLoaded: String? = null

    override val isAvailable: Boolean = true

    override suspend fun transcribe(pcm: FloatArray, modelPath: String, languageTag: String?): String =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val filtersPath = modelManager.localPathOrNull(filtersModel)
                    ?: run {
                        modelManager.download(filtersModel) // fetch the mel/vocab companion, then retry
                        throw IllegalStateException("Preparing Whisper assets — please try again shortly.")
                    }
                ensureAssets(filtersPath)
                val interp = ensureInterpreter(modelPath)
                val ext = extractor ?: error("assets not loaded")
                val tok = tokenizer ?: error("assets not loaded")

                val mel = ext.logMel(pcm) // [nMel * N_FRAMES]
                val nFrames = WhisperFeatureExtractor.N_FRAMES
                val input = reshape(mel, mel.size / nFrames, nFrames)
                val output = Array(1) { IntArray(MAX_TOKENS) }
                interp.run(input, output)
                tok.decode(output[0])
            }
        }

    private fun ensureAssets(filtersPath: String) {
        if (assetsPathLoaded == filtersPath && extractor != null) return
        val assets = WhisperAssets.load(filtersPath)
        extractor = WhisperFeatureExtractor(assets)
        tokenizer = WhisperTokenizer(assets)
        assetsPathLoaded = filtersPath
    }

    private fun ensureInterpreter(modelPath: String): Interpreter {
        interpreter?.let { if (modelPathLoaded == modelPath) return it }
        runCatching { interpreter?.close() }
        Logger.i(tag = LOG_TAG) { "Loading Whisper tflite: $modelPath" }
        val interp = Interpreter(File(modelPath), Interpreter.Options().apply { setNumThreads(4) })
        interpreter = interp
        modelPathLoaded = modelPath
        return interp
    }

    /** Flatten [nMel * nFrames] → model input shape [1][nMel][nFrames]. */
    private fun reshape(mel: FloatArray, nMel: Int, nFrames: Int): Array<Array<FloatArray>> =
        Array(1) { Array(nMel) { m -> FloatArray(nFrames) { f -> mel[m * nFrames + f] } } }

    override fun release() {
        runCatching { interpreter?.close() }
        interpreter = null
        modelPathLoaded = null
    }

    private companion object {
        const val MAX_TOKENS = 224
        const val LOG_TAG = "AgentWhisper"
    }
}
