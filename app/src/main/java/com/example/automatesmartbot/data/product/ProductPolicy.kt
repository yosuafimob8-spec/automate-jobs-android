package com.example.automatesmartbot.data.product

import android.content.Context
import org.json.JSONObject

/** Product decisions stay explicit and reviewable. Unassigned tiers retain existing access. */
class ProductPolicy(context: Context) {
    private val json = JSONObject(context.assets.open("product_policy.json").bufferedReader().use { it.readText() })
    val approved: Boolean get() = json.optBoolean("approved")
    fun templateTier(id: String): String? = if (approved) json.optJSONObject("cv_templates")?.optString(id)?.takeIf { it in listOf("free", "pro") } else null
    fun trainingTier(id: String): String? = if (approved) json.optJSONObject("training_tiers")?.optString(id)?.takeIf { it in listOf("free", "pro") } else null
    fun templateCategories(id: String, primary: String): List<String> = json.optJSONObject("template_categories")?.optJSONArray(id)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: listOf(primary)
    fun canUseTemplate(id: String, isPro: Boolean): Boolean = templateTier(id) != "pro" || isPro
    val trainingPassScore: Int get() = json.getJSONObject("training").getInt("pass_score").also { require(it in 1..100) }
}
