package com.ampairs.agent.speech.whisper

import co.touchlab.kermit.Logger
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelInstallStatus
import com.ampairs.agent.llm.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                val filtersPath = ensureFiltersDownloaded()
                ensureAssets(filtersPath)
                val interp = ensureInterpreter(modelPath)
                val ext = extractor ?: error("assets not loaded")
                val tok = tokenizer ?: error("assets not loaded")

                val mel = ext.logMel(pcm) // [nMel * N_FRAMES]
                val nFrames = WhisperFeatureExtractor.N_FRAMES
                val input = reshape(mel, mel.size / nFrames, nFrames)
                // The model defines its own output length (whisper-base emits [1, 451] token ids; tiny
                // differs), so size the buffer from the actual output tensor — a fixed guess fails the
                // shape-checked copy ("[1, 451] to [1, 224]").
                val outShape = interp.getOutputTensor(0).shape()
                val batch = outShape.firstOrNull() ?: 1
                val tokens = outShape.lastOrNull() ?: 0
                val output = Array(batch) { IntArray(tokens) }
                interp.run(input, output)
                Logger.i(tag = LOG_TAG) { "Whisper produced $tokens tokens" }
                tok.decode(output[0])
            }
        }

    /**
     * Resolve the mel/vocab companion's local path, downloading it first if absent. The companion is
     * tiny (~0.5 MB) but isn't part of the model-picker download, so the first transcription fetches it
     * and waits (rather than failing) — otherwise voice mode silently never works on first use.
     */
    private suspend fun ensureFiltersDownloaded(): String {
        modelManager.localPathOrNull(filtersModel)?.let { return it }
        Logger.i(tag = LOG_TAG) { "Whisper assets missing — downloading ${filtersModel.fileName}" }
        modelManager.download(filtersModel)
        repeat(FILTERS_WAIT_TICKS) {
            delay(FILTERS_WAIT_STEP_MS)
            modelManager.localPathOrNull(filtersModel)?.let {
                Logger.i(tag = LOG_TAG) { "Whisper assets ready: ${filtersModel.fileName}" }
                return it
            }
            if (modelManager.statusOf(filtersModel.id) is ModelInstallStatus.Failed) {
                error("Couldn't download the Whisper assets. Check your connection and try again.")
            }
        }
        error("Whisper assets are still downloading — please try again in a moment.")
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
        const val LOG_TAG = "AgentWhisper"
        const val FILTERS_WAIT_TICKS = 60 // ~60 s max wait for the ~0.5 MB companion
        const val FILTERS_WAIT_STEP_MS = 1_000L
    }
}
