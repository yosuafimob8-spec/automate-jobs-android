package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

private val WorkPediaInk = Color(0xFF102B47)

/** Data in the current 70-term JSON often repeats the acronym in `fullName`.
 *  Show verified, human-readable expansions only where they are unambiguous.
 *  Other terms keep their source text; do not manufacture definitions.
 */
internal fun workPediaReadableName(name: String, fullName: String): String? {
    if (fullName.isNotBlank() && !name.equals(fullName, ignoreCase = true)) return fullName
    return when (name.uppercase(java.util.Locale.ROOT)) {
        "PKWT" -> "Perjanjian Kerja Waktu Tertentu"
        "PKWTT" -> "Perjanjian Kerja Waktu Tidak Tertentu"
        else -> null
    }
}

internal fun workPediaCardSummary(definition: String, simple: String): String =
    definition.trim().ifBlank { simple.trim() }.ifBlank { "Ketuk untuk membaca penjelasan istilah ini." }

private data class TermAccent(val color: Color, val icon: ImageVector)

private fun workPediaAccent(category: String): TermAccent = when (category) {
    "Kontrak dan pengupahan" -> TermAccent(Color(0xFF0578BD), Icons.Default.Description)
    "Hak dan administrasi" -> TermAccent(Color(0xFF119C76), Icons.Default.VerifiedUser)
    "Operasional" -> TermAccent(Color(0xFFE18C24), Icons.Default.Settings)
    "Karier dan hubungan kerja" -> TermAccent(Color(0xFF7555D5), Icons.Default.Work)
    else -> TermAccent(Color(0xFF0876AE), Icons.Default.MenuBook)
}

/** The whole card opens the existing WorkPedia detail. The star only toggles a saved favorite. */
@Composable
internal fun WorkPediaTermCard(
    term: JSONObject,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val name = term.optString("name")
    val category = term.optString("category")
    val readableName = workPediaReadableName(name, term.optString("fullName"))
    val summary = workPediaCardSummary(term.optString("definition"), term.optString("simple"))
    val accent = workPediaAccent(category)
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val ink = if (dark) Color(0xFFF4F9FF) else WorkPediaInk
    val muted = if (dark) Color(0xFFBDD0E2) else Color(0xFF4D6276)
    val surface = if (dark) Color(0xFF192D43) else Color(0xFFFFFFFF)
    val border = if (dark) Color(0xFF35526D) else Color(0xFFD5E5F0)
    val iconBackground = if (dark) accent.color.copy(alpha = .24f) else accent.color.copy(alpha = .10f)

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.fillMaxHeight().width(4.dp).background(accent.color))
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(49.dp).clip(RoundedCornerShape(14.dp)).background(iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(accent.icon, contentDescription = null, tint = accent.color, modifier = Modifier.size(27.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(name, color = ink, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
                        if (readableName != null) {
                            Text(
                                readableName, color = muted, fontSize = 12.5.sp, lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = onFavoriteToggle
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "Hapus $name dari favorit" else "Simpan $name ke favorit",
                            tint = if (isFavorite) Color(0xFFF4AE22) else accent.color,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Text(
                    summary,
                    color = muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(color = border.copy(alpha = .8f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        color = iconBackground,
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, accent.color.copy(alpha = .22f))
                    ) {
                        Text(
                            category,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = if (dark) Color(0xFFDFEBF7) else accent.color,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Baca detail", color = accent.color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accent.color, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
