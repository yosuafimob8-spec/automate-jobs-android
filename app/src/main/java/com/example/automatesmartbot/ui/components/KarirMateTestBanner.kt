package com.example.automatesmartbot.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Career Billboard uses the official Google test banner until AdMob approval.
 * Production Ad Unit ID remains disabled: ca-app-pub-1447746930031525/5293564907.
 */
@Composable
fun KarirMateTestBanner() {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        var loadState by remember(widthDp) { mutableStateOf("loading") }
        var failureCode by remember(widthDp) { mutableStateOf<Int?>(null) }

        key(widthDp) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adSize.height.dp),
                factory = { viewContext ->
                    AdView(viewContext).apply {
                        setAdSize(adSize)
                        adUnitId = "ca-app-pub-3940256099942544/6300978111"
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                loadState = "loaded"
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                loadState = "failed"
                                failureCode = error.code
                                Log.w("KarirMateAdMob",
                                    "Test banner failed: code=${error.code}, domain=${error.domain}, message=${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { it.destroy() }
            )
        }
        if (loadState != "loaded") {
            Text(
                text = if (loadState == "failed") {
                    "Iklan pengujian belum tersedia" + (failureCode?.let { " (kode $it)" } ?: "")
                } else "Memuat iklan pengujian…",
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
