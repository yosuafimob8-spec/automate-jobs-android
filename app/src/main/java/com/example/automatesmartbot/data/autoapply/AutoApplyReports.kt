package com.example.automatesmartbot.data.autoapply

import com.example.automatesmartbot.data.network.ApiService
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response

fun JSONObject.objects(key: String): List<JSONObject> = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()
fun JSONObject.text(key: String) = optString(key).takeUnless { it == "null" }.orEmpty()
fun Response<ResponseBody>.readJson(): JSONObject {
    if (!isSuccessful) {
        val body = errorBody()?.use { it.string() }.orEmpty()
        val reason = runCatching { JSONObject(body) }.getOrNull()
        error("${reason?.text("code").orEmpty()} ${reason?.text("error")?.ifBlank { null } ?: "Permintaan gagal (${code()})."}".trim())
    }
    return JSONObject(body()?.use { it.string() } ?: error("Respons backend kosong."))
}

data class DeliveryReport(val items: List<JSONObject>, val summary: JSONObject, val quota: JSONObject, val nextOffset: Int?) {
    companion object {
        fun parse(body: JSONObject): DeliveryReport {
            require(body.has("items") && body.has("summary") && body.has("auto_apply")) { "Backend laporan perlu diperbarui; rekap belum tersedia. Tidak ada angka pengiriman yang diperkirakan aplikasi." }
            return DeliveryReport(body.objects("items"), body.getJSONObject("summary"), body.getJSONObject("auto_apply"), if (body.isNull("next_offset")) null else body.optInt("next_offset"))
        }
    }
}
object DeliveryStatus {
    fun label(item: JSONObject): String = when (item.text("status")) {
        "sent" -> if (AutoApplyConfirmation.hasReference(item.text("gmail_message_id"))) "Terkirim · dikonfirmasi Gmail" else "Belum pasti · ID Gmail tidak tersedia"
        "ready", "queued", "waiting" -> "Menunggu"
        "sending", "processing" -> "Diproses"
        "failed" -> "Gagal"
        "cancelled" -> "Dibatalkan"
        "blocked", "configuration_blocked" -> "Diblokir konfigurasi"
        "uncertain" -> "Belum pasti · periksa Gmail, tidak diulang otomatis"
        else -> "Status belum dikenali: ${item.text("status")}" // Never turn an unknown state into success.
    }
}
class AutoApplyReportRepository(private val api: ApiService) {
    suspend fun report(period: String, offset: Int = 0): DeliveryReport = DeliveryReport.parse(api.getKarirMateReport(period, offset).readJson())
    suspend fun simulate(): JSONObject = api.simulateAutoApply().readJson().also {
        require(it.text("mode") == "simulation" && it.has("sent") && it.getInt("sent") == 0 && it.has("sending_enabled") && !it.getBoolean("sending_enabled")) { "Backend belum mengonfirmasi hasil simulasi tanpa pengiriman." }
    }
}
