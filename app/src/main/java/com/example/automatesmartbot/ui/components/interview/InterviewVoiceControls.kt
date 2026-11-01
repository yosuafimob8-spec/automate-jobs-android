package com.example.automatesmartbot.ui.components.interview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.automatesmartbot.ui.viewmodel.InterviewViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

@Composable
@SuppressLint("MissingPermission")
fun InterviewVoiceControls(vm: InterviewViewModel, question: String, language: String, sttAvailable: Boolean, ttsAvailable: Boolean, busy: Boolean, autoSpeak: Boolean = true) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var ttsReady by remember { mutableStateOf(false) }; var listening by remember { mutableStateOf(false) }; var recording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }; var recordingFile by remember { mutableStateOf<File?>(null) }; var playbackFile by remember { mutableStateOf<File?>(null) }; var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val tts = remember { TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS } }
    val recognizer = remember {
        if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else null
    }
    LaunchedEffect(question, language, ttsReady, autoSpeak) {
        if (autoSpeak && ttsReady && question.isNotBlank() && tts.setLanguage(Locale.forLanguageTag(if (language == "en") "en-US" else "id-ID")) >= TextToSpeech.LANG_AVAILABLE) tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "interview-question")
    }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) { listening = false; vm.showError("Suara belum berhasil dibaca (kode $error). Jawaban dapat diketik; tidak diberi skor karena transkripsi gagal.") }
            override fun onResults(results: Bundle?) { listening = false; results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(vm::updateAnswer) }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }
    fun stopRecording() {
        try {
            runCatching { recorder?.stop() }; recorder?.release(); recorder = null; recording = false
            val file = requireNotNull(recordingFile); val bytes = file.readBytes(); require(bytes.size <= 10_000_000) { "Audio melebihi 10 MB." }; file.delete(); recordingFile = null
            vm.transcribe(bytes, "m4a")
        } catch (e: Exception) { recorder?.release(); recorder = null; recording = false; recordingFile?.delete(); recordingFile = null; vm.showError("Rekaman belum dapat diproses. Coba lagi atau ketik jawaban.") }
    }
    fun startVoice() {
        try {
            tts.stop()
            if (recognizer != null) {
                recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == "en") "en-US" else "id-ID")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }); listening = true
            } else if (sttAvailable) {
                val file = File(context.cacheDir, "interview-${UUID.randomUUID()}.m4a")
                @Suppress("DEPRECATION") val next = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
                recorder = next; recordingFile = file
                next.setAudioSource(MediaRecorder.AudioSource.MIC); next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); next.setAudioSamplingRate(16000); next.setAudioEncodingBitRate(48000); next.setOutputFile(file.path); next.setMaxDuration(90000); next.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED && recording) stopRecording() }; next.prepare(); next.start(); recording = true
            } else vm.showError("Pengenalan suara offline tidak tersedia di perangkat dan layanan STT belum terhubung. Gunakan teks.")
        } catch (e: Exception) { recorder?.release(); recorder = null; recordingFile?.delete(); recordingFile = null; recording = false; vm.showError(e.message ?: "Mikrofon belum tersedia.") }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) startVoice() else vm.showError("Izin mikrofon ditolak. Interview teks tetap tersedia.") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Jawaban suara ditranskripsi menjadi teks dan dapat diperiksa sebelum dikirim. Penilaian menggunakan teks, bukan aksen atau penampilan.")
        Button(onClick = {
            if (recording) stopRecording() else if (listening) { recognizer?.stopListening(); listening = false }
            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoice() else permission.launch(Manifest.permission.RECORD_AUDIO)
        }, enabled = !busy && (recognizer != null || sttAvailable)) { Text(if (recording) "Hentikan & Transkripsi" else if (listening) "Hentikan Mendengarkan" else "Jawab dengan Suara") }
        if (recognizer == null && !sttAvailable) Text("Suara belum tersedia: pasang layanan pengenalan offline atau hubungkan STT self-hosted.")
        if (recording || listening) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            val supported = ttsReady && tts.setLanguage(Locale.forLanguageTag(if (language == "en") "en-US" else "id-ID")) >= TextToSpeech.LANG_AVAILABLE
            if (supported) tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "interview-question")
            else if (ttsAvailable) scope.launch {
                try {
                    player?.release(); playbackFile?.delete()
                    val bytes = vm.questionAudio(); val file = File(context.cacheDir, "interview-speech-${UUID.randomUUID()}.wav"); file.writeBytes(bytes); playbackFile = file
                    player = MediaPlayer().apply { setDataSource(file.path); setOnPreparedListener { it.start() }; prepareAsync() }
                } catch (e: CancellationException) { throw e } catch (e: Exception) { vm.showError(e.message ?: "Suara pertanyaan belum tersedia.") }
            } else vm.showError("Suara bahasa yang dipilih belum terpasang. Pertanyaan tetap tersedia sebagai teks.")
        }, enabled = !busy && !recording && !listening && question.isNotBlank()) { Text("Dengarkan Pertanyaan") }
    }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown(); recorder?.runCatching { stop(); release() }; recordingFile?.delete(); player?.release(); playbackFile?.delete() } }
}
