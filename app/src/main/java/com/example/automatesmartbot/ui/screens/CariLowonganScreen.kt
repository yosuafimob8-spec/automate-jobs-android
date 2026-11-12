package com.example.automatesmartbot.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import com.example.automatesmartbot.ui.viewmodel.JobViewModel
import com.example.automatesmartbot.util.UrlSafety

@Composable
fun CariLowonganScreen(
    isPro: Boolean,
    viewModel: JobViewModel = viewModel()
) {
    val context = LocalContext.current
    val jobs by viewModel.jobs.collectAsState()
    val totalJobs by viewModel.totalJobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val locations = listOf("Semua Lokasi Jabodetabek", "Jakarta", "Bogor", "Depok", "Tangerang", "Bekasi")
    var selectedPosition by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf(locations.first()) }

    fun triggerSearch() {
        viewModel.fetchJobs(
            positions = selectedPosition.trim().takeIf { it.isNotEmpty() }?.let(::listOf),
            locations = selectedLocation.takeUnless { it == "Semua Lokasi Jabodetabek" }?.let(::listOf),
            sources = null,
            categories = null
        )
    }

    LaunchedEffect(Unit) {
        triggerSearch()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info & Status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cari Lowongan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = KarirTextDark
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5F7),
                            border = BorderStroke(1.dp, Color(0xFFC6DFE8))
                        ) {
                            Text(
                                text = "Total: ${if (errorMessage != null) "-" else totalJobs}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Temukan peluang kerja terverifikasi sesuai posisi dan lokasi pilihan Anda.",
                        fontSize = 12.sp,
                        color = KarirTextMuted
                    )
                }
            }

            // Dropdown & Search Filters
            OutlinedTextField(
                value = selectedPosition,
                onValueChange = { selectedPosition = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Posisi / Jabatan") },
                placeholder = { Text("Contoh: Admin, Operator, Kurir") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KarirOrange,
                    unfocusedBorderColor = Color(0xFFC6DFE8),
                    focusedLabelColor = KarirOrange,
                    cursorColor = KarirOrange,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                trailingIcon = {
                    Button(
                        onClick = { triggerSearch() },
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Cari", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cari", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterDropdown(
                    label = "Lokasi Kerja",
                    selectedOption = selectedLocation,
                    options = locations,
                    onSelected = {
                        selectedLocation = it
                        triggerSearch()
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        selectedPosition = ""
                        selectedLocation = locations.first()
                        triggerSearch()
                    }
                ) {
                    Text("Reset", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            // Content Area: Loading, Error, Empty, or Job Cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = KarirOrange)
                    }
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Gagal memuat lowongan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = errorMessage ?: "Terjadi kesalahan",
                                fontSize = 12.sp,
                                color = KarirTextMuted
                            )
                            Button(
                                onClick = { triggerSearch() },
                                colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Coba Lagi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    jobs.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = Color(0xFFFFB280),
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = "Tidak ada lowongan ditemukan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = KarirTextDark
                            )
                            Text(
                                text = "Coba ubah kata kunci posisi atau filter lokasi Anda.",
                                fontSize = 12.sp,
                                color = KarirTextMuted
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val liveJobs = jobs.filter { it.resultType != "spontaneous" }
                            if (liveJobs.isNotEmpty()) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF16A34A), shape = RoundedCornerShape(4.dp))
                                        )
                                        Text(
                                            if (liveJobs.all { it.vacancyVerified == true })
                                            "Lowongan Aktif Terverifikasi"
                                        else "Lowongan dari sumber eksternal (cek detail lowongan)",
                                            color = KarirOrangeDeep,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            items(liveJobs) { job ->
                                JobCard(job, context)
                            }
                            if (liveJobs.isEmpty()) {
                                item {
                                    Text(
                                        "Belum ada lowongan aktif yang sesuai",
                                        color = KarirTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (viewModel.hasMore) {
                                item {
                                    OutlinedButton(
                                        onClick = viewModel::loadNextPage,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, KarirOrange),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White,
                                            contentColor = KarirOrange
                                        )
                                    ) {
                                        Text("Muat Halaman Berikutnya", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedCard(
            onClick = { expanded = true },
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = label, fontSize = 10.sp, color = KarirTextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedOption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarirTextDark,
                        maxLines = 1
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = KarirOrange)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = KarirTextDark,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun JobCard(job: com.example.automatesmartbot.data.model.JobsV2Job, context: android.content.Context) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                job.title ?: "Tanpa Judul",
                color = KarirTextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Business, contentDescription = null, tint = KarirTextMuted, modifier = Modifier.size(14.dp))
                Text(
                    "${job.company ?: "Perusahaan"} • ${job.location ?: "Lokasi tidak diketahui"}",
                    color = KarirTextMuted,
                    fontSize = 12.sp
                )
            }
            if (job.resultType == "spontaneous") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5F7),
                    border = BorderStroke(1.dp, Color(0xFFC6DFE8))
                ) {
                    Text(
                        job.opportunityLabel ?: "Peluang Lamaran Spontan",
                        color = Color(0xFFE65100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            val action = job.applicationActions.firstOrNull { UrlSafety.safeHttpUrl(it.url) != null }
            if (action != null) {
                Button(
                    onClick = {
                        val url = UrlSafety.safeHttpUrl(action.url) ?: return@Button
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(action.label ?: "Lihat Sumber Resmi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Tidak ada tindakan lamaran terverifikasi.", color = KarirTextMuted, fontSize = 11.sp)
            }
        }
    }
}
