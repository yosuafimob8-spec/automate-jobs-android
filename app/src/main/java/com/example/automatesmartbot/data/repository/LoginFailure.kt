package com.example.automatesmartbot.data.repository

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException

/** User-facing causes only; never expose credentials, response bodies or tokens. */
internal object LoginFailure {
    fun network(error: IOException, baseUrl: String): String {
        val endpoint = runCatching { URI(baseUrl) }.getOrNull()
        val local = endpoint?.host in setOf("127.0.0.1", "localhost", "::1", "[::1]")
        return when (error) {
            is ConnectException -> if (local)
                "Backend Termux belum merespons di ${endpoint?.host}:${endpoint?.port}. Pastikan layanan login di Termux aktif, lalu coba lagi."
                else "Tidak dapat terhubung ke server login. Periksa koneksi dan ketersediaan layanan."
            is SocketTimeoutException -> "Waktu tunggu layanan login habis. Periksa layanan backend dan coba lagi."
            is UnknownHostException -> "Alamat server login tidak dapat ditemukan. Periksa alamat server dan koneksi."
            else -> "Koneksi layanan login terputus. Periksa backend dan coba lagi."
        }
    }

    fun response(code: Int): String = when (code) {
        401 -> "Email atau sandi tidak sesuai."
        403 -> "Akses login ditolak (403). Periksa status akun Anda."
        429 -> "Terlalu banyak percobaan masuk. Tunggu sebelum mencoba lagi."
        in 500..599 -> "Layanan login sedang bermasalah (HTTP $code). Coba lagi setelah layanan pulih."
        in 200..299 -> "Respons login tidak lengkap. Silakan coba lagi."
        else -> "Login gagal (HTTP $code). Periksa data dan coba lagi."
    }
}
