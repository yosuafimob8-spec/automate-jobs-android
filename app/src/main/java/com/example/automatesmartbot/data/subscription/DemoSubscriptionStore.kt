package com.example.automatesmartbot.data.subscription

import android.content.Context
import com.example.automatesmartbot.data.cv.cvWorkspace
import com.example.automatesmartbot.data.cv.cvWorkspaceBound
import org.json.JSONObject

/** Simulated purchases are isolated from SessionManager, API clients and server entitlements. */
data class DemoPlan(val id: String, val name: String, val rupiah: Int, val days: Int)
object DemoPlans {
    val all = listOf(DemoPlan("daily", "Harian", 5000, 1), DemoPlan("weekly", "Mingguan", 25000, 7),
        DemoPlan("monthly", "Bulanan", 79000, 30), DemoPlan("yearly", "Tahunan", 499000, 365))
    fun require(id: String) = all.firstOrNull { it.id == id } ?: error("Paket tidak valid.")
}
data class DemoSubscription(val planId: String?, val activatedAt: Long, val expiresAt: Long) {
    fun active(now: Long) = planId in DemoPlans.all.map { it.id } && expiresAt > now
}
class DemoSubscriptionStore(private val context: Context, private val token: () -> String? = { com.example.automatesmartbot.data.network.ApiClient.getSessionManager()?.getValidToken() }, private val now: () -> Long = System::currentTimeMillis) {
    private val boundToken = token()
    private val file: android.util.AtomicFile get() {
        check(boundToken == token()) { "Akun berubah. Buka ulang halaman langganan." }
        check(cvWorkspaceBound(context, boundToken)) { "Identitas akun belum terverifikasi. Backend harus tersedia untuk menghubungkan akun." }
        return android.util.AtomicFile(java.io.File(cvWorkspace(context, boundToken).filesDir, "pro_demo_subscription.json"))
    }
    private fun read(): JSONObject = if (!file.baseFile.exists()) JSONObject() else
        file.openRead().use { JSONObject(it.readBytes().toString(Charsets.UTF_8)) }
    fun current(): DemoSubscription = synchronized(lock) {
        val data = runCatching { read() }.getOrDefault(JSONObject())
        DemoSubscription(data.optString("plan").takeIf(String::isNotBlank), data.optLong("activated_at"), data.optLong("expires_at"))
    }
    fun clock() = now()
    fun activate(planId: String, requestId: String): DemoSubscription = synchronized(lock) {
        require(requestId.matches(Regex("[A-Za-z0-9_-]{8,100}"))) { "Permintaan simulasi tidak valid." }
        val plan = DemoPlans.require(planId)
        val data = read()
        val requests = data.optJSONObject("requests") ?: JSONObject()
        if (requests.has(requestId)) {
            val previous = requests.getJSONObject(requestId)
            require(previous.getString("plan") == planId) { "Permintaan yang sama tidak boleh mengubah paket." }
            return@synchronized DemoSubscription(planId, previous.getLong("activated_at"), previous.getLong("expires_at"))
        }
        val time = now()
        val expiry = Math.addExact(maxOf(time, current().expiresAt), Math.multiplyExact(plan.days.toLong(), 86_400_000L))
        val result = DemoSubscription(planId, time, expiry)
        requests.put(requestId, JSONObject().put("plan", planId).put("activated_at", time).put("expires_at", expiry))
        data.put("plan", planId).put("activated_at", time).put("expires_at", expiry).put("requests", requests)
        val output = file.startWrite()
        try { output.write(data.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw error }
        check(current().expiresAt == expiry) { "Simulasi belum tersimpan. Coba lagi." }
        result
    }
    companion object { private val lock = Any() }
}
