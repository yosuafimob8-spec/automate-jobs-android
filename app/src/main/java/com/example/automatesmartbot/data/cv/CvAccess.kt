package com.example.automatesmartbot.data.cv

import android.content.Context
import android.content.ContextWrapper
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.security.SessionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

/** A live authenticated /auth/me response, never a demo or Compose flag, grants local template access. */
interface CvAccessAuthority {
    fun canUse(templateId: String): Boolean
    suspend fun refresh()
    fun workspaceOwner(): String? = null
}

class ServerCvAccess(
    private val token: () -> String? = { ApiClient.getSessionManager()?.getValidToken() },
    private val now: () -> Long = System::currentTimeMillis,
    private val fetch: suspend () -> com.example.automatesmartbot.data.model.UserMeResponse? = {
        withContext(Dispatchers.IO) { ApiClient.apiService.getMe().takeIf { it.isSuccessful }?.body() }
    }
) : CvAccessAuthority {
    private val refreshLock = Mutex()
    @Volatile private var verifiedToken: String? = null
    @Volatile private var ownerToken: String? = null
    @Volatile private var ownerId: String? = null
    override fun workspaceOwner(): String? = ownerId.takeIf { token() == ownerToken && ownerToken != null }
    @Volatile private var validUntil = 0L
    override fun canUse(templateId: String): Boolean = CvTemplates.canUse(templateId,
        !verifiedToken.isNullOrBlank() && token() == verifiedToken && now() < validUntil)
    override suspend fun refresh() = refreshLock.withLock {
        verifiedToken = null; validUntil = 0
        val captured = token()?.takeIf(String::isNotBlank) ?: return@withLock
        val identity = try { fetch() } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null } ?: return@withLock
        if (token() != captured) return@withLock
        val owner = identity.id?.takeIf(String::isNotBlank)?.let { "id:$it" }
            ?: identity.email?.takeIf(String::isNotBlank)?.let { "email:$it" } ?: return@withLock
        ownerToken = captured; ownerId = owner
        if (!identity.hasVerifiedMembership || !identity.hasProMembership) return@withLock
        val expiry = identity.membershipExpiresAt?.let(SessionPolicy::parseExpiryMillis)
        if (identity.membershipExpiresAt != null && expiry == null) return@withLock
        verifiedToken = captured
        validUntil = minOf(expiry ?: Long.MAX_VALUE, now() + 60_000L)
    }
}

private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
private fun bindingKey(token: String) = digest(ApiClient.getBaseUrl() + "::token:" + token)
private fun bindings(context: Context) = context.getSharedPreferences("cv_verified_workspace_bindings", Context.MODE_PRIVATE)
fun cvWorkspaceBound(context: Context, token: String?): Boolean = token.isNullOrBlank() || bindings(context).contains(bindingKey(token))

/** Bind only a server-verified owner. Token renewal must retain the same private directory. */
fun bindCvWorkspace(context: Context, token: String, verifiedOwner: String) {
    require(verifiedOwner.isNotBlank())
    val namespace = digest(ApiClient.getBaseUrl() + "::" + verifiedOwner)
    check(bindings(context).edit().putString(bindingKey(token), namespace).commit()) { "Identitas penyimpanan akun belum tersimpan." }
}

/** Unbound tokens stay isolated and the editor waits for verified identity before writing.
 * Unattributed old device drafts remain preserved, never silently assigned to a new account.
 */
fun cvWorkspace(context: Context, token: String? = ApiClient.getSessionManager()?.getValidToken()): Context {
    if (token.isNullOrBlank()) return context
    val key = bindings(context).getString(bindingKey(token), null) ?: digest("unbound:$token")
    return object : ContextWrapper(context) {
        override fun getFilesDir(): File = File(context.filesDir, "cv_accounts/$key").apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int) = context.getSharedPreferences("cv_${key}_$name", mode)
    }
}

suspend fun ensureCvWorkspace(context: Context): Boolean {
    val token = ApiClient.getSessionManager()?.getValidToken() ?: return true
    if (cvWorkspaceBound(context, token)) return true
    val authority = ServerCvAccess(); authority.refresh()
    val owner = authority.workspaceOwner() ?: return false
    if (token != ApiClient.getSessionManager()?.getValidToken()) return false
    bindCvWorkspace(context, token, owner)
    return true
}

fun cvSessionKey(): String? = ApiClient.getSessionManager()?.getValidToken()?.let {
    MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
}
enum class CvExportFormat { PDF, DOCX }
data class CvExportRequest(val templateId: String, val content: CvContent, val sessionKey: String? = cvSessionKey())
data class CvExportSnapshot(val request: CvExportRequest?, val pages: List<CvPage>, val sessionKey: String? = cvSessionKey())
class CvExportService(private val access: CvAccessAuthority = ServerCvAccess(), private val session: () -> String? = ::cvSessionKey) {
    suspend fun authorize(request: CvExportRequest) {
        require(request.sessionKey == session()) { "Akun berubah. Buka ulang CV sebelum ekspor." }
        access.refresh()
        require(request.sessionKey == session()) { "Akun berubah. Ekspor dibatalkan." }
        require(access.canUse(request.templateId)) { "Akses template PRO tidak aktif. Ekspor dibatalkan." }
    }
    suspend fun export(context: Context, uri: android.net.Uri, request: CvExportRequest, format: CvExportFormat) {
        val bytes = java.io.ByteArrayOutputStream()
        when (format) {
            CvExportFormat.PDF -> writePdf(context, request, bytes)
            CvExportFormat.DOCX -> writeDocx(context, request, bytes)
        }
        // Rendering may outlast a membership/session. Never truncate a destination before the final check.
        authorize(request)
        context.contentResolver.openOutputStream(uri, "wt")?.use { bytes.writeTo(it) }
            ?: error("Lokasi tidak dapat ditulis.")
    }
    suspend fun writePdf(context: Context, request: CvExportRequest, output: java.io.OutputStream) {
        authorize(request)
        CvRenderer.writePdf(context, CvRenderer.layout(request.content, request.templateId), output)
    }
    suspend fun writeDocx(context: Context, request: CvExportRequest, output: java.io.OutputStream) {
        authorize(request)
        CvExporter.writeDocx(CvRenderer.layout(request.content, request.templateId), output)
    }
}
