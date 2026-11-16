package com.example.tvsubtitletranslator

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.max

class MicCapture(private val activity: MainActivity, private val listener: Listener) {
    interface Listener {
        fun onTranscript(translated: String, startSec: Double, endSec: Double)
        fun onStatus(message: String)
    }
    @Volatile private var running = false
    private var recorder: AudioRecord? = null
    private val apiClient = OkHttpClient.Builder()
        .callTimeout(100, TimeUnit.SECONDS).build()
    private val rate = 16000
    private val chunkDurationSec = 10

    fun start() {
        if (running) return
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) { listener.onStatus("AudioRecord indisponibil pe acest dispozitiv."); return }
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, max(min * 4, rate * 2))
        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            listener.onStatus("Microfonul nu poate fi inițializat. Încearcă un microfon USB.")
            return
        }
        running = true
        recorder?.startRecording()
        thread(name = "MicAudioCapture") { loop() }
    }

    private fun loop() {
        val chunk = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        val target = rate * 2 * chunkDurationSec
        var chunkStart = 0.0
        val began = System.nanoTime()
        while (running) {
            val n = recorder?.read(buffer, 0, buffer.size) ?: -1
            if (n > 0) {
                if (chunk.size() == 0) chunkStart = (System.nanoTime() - began) / 1e9
                chunk.write(buffer, 0, n)
                if (chunk.size() >= target) {
                    val bytes = chunk.toByteArray()
                    chunk.reset()
                    val offset = chunkStart
                    thread(name = "TranslateAudio") { process(bytes, offset) }
                }
            } else if (n < 0) {
                listener.onStatus("Eroare la capturarea microfonului: $n")
                break
            }
        }
        release()
    }

    private fun process(pcm: ByteArray, offset: Double) {
        try {
            val key = activity.getSharedPreferences("translator_v2", 0).getString("api_key", "") ?: return
            val wav = wav(pcm)
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("timestamp_granularities[]", "segment")
                .addFormDataPart("file", "capture.wav", wav.toRequestBody("audio/wav".toMediaType()))
                .build()
            val req = Request.Builder().url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer $key").post(requestBody).build()
            val json = apiClient.newCall(req).execute().use { r ->
                val body = r.body?.string() ?: ""
                if (!r.isSuccessful) throw IOException("Whisper ${r.code}: $body")
                JSONObject(body)
            }
            val segs = json.optJSONArray("segments") ?: return
            for (i in 0 until segs.length()) {
                val seg = segs.getJSONObject(i)
                val original = seg.optString("text").trim()
                if (original.isBlank()) continue
                val translated = translate(key, original)
                listener.onTranscript(translated, offset + seg.optDouble("start", 0.0),
                    offset + seg.optDouble("end", 0.0))
            }
            listener.onStatus("Transcriere și traducere în curs. Audio capturat prin microfon.")
        } catch (e: Exception) {
            listener.onStatus("Eroare API/rețea: ${e.message}")
        }
    }

    private fun translate(key: String, source: String): String {
        val payload = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", 0.2)
            put("messages", org.json.JSONArray()
                .put(JSONObject().put("role", "system").put("content",
                    "Tradu în română, natural și concis, replica de film următoare. " +
                    "Răspunde numai cu traducerea, fără ghilimele sau explicații."))
                .put(JSONObject().put("role", "user").put("content", source)))
        }
        val req = Request.Builder().url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $key")
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        apiClient.newCall(req).execute().use { r ->
            val body = r.body?.string() ?: ""
            if (!r.isSuccessful) throw IOException("Traducere ${r.code}: $body")
            return JSONObject(body).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
        }
    }

    private fun wav(pcm: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        fun le(v: Int) { out.write(v and 255); out.write((v shr 8) and 255) }
        fun le32(v: Int) { le(v); le(v shr 16) }
        out.write("RIFF".toByteArray()); le32(36 + pcm.size)
        out.write("WAVEfmt ".toByteArray()); le32(16); le(1); le(1)
        le32(rate); le32(rate * 2); le(2); le(16)
        out.write("data".toByteArray()); le32(pcm.size); out.write(pcm)
        return out.toByteArray()
    }

    fun stop() { running = false; try { recorder?.stop() } catch (_: Exception) {}; release() }
    private fun release() { try { recorder?.release() } catch (_: Exception) {}; recorder = null }
}
