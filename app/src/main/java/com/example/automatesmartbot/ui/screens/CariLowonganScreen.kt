package com.example.automatesmartbot.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
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
import com.example.automatesmartbot.data.model.JobFilters
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

    val positions = JobFilters.positions
    val locations = JobFilters.locations

    var selectedPosition by remember { mutableStateOf(positions.first()) }
    var selectedLocation by remember { mutableStateOf(locations.first()) }

    fun triggerSearch() {
        viewModel.fetchJobs(selectedPosition, selectedLocation)
    }

    LaunchedEffect(Unit) {
        triggerSearch()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info & Status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cari Lowongan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Total lowongan ditemukan: ${if (errorMessage != null) "-" else totalJobs}",
                        fontSize = 12.sp,
                        color = Color(0xFF34D399)
                    )
                    Text(
                        text = "Pilih posisi dan lokasi untuk melihat lowongan yang tersedia.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Dropdown Filters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Posisi",
                    selectedOption = selectedPosition,
                    options = positions,
                    onSelected = {
                        selectedPosition = it
                        triggerSearch()
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Lokasi",
                    selectedOption = selectedLocation,
                    options = locations,
                    onSelected = {
                        selectedLocation = it
                        triggerSearch()
                    },
                    modifier = Modifier.weight(1f)
                )
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
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    }
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "Gagal memuat lowongan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFEF4444))
                            Text(text = errorMessage ?: "Terjadi kesalahan", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Button(onClick = { triggerSearch() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                    jobs.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                            Text(text = "Tidak ada lowongan ditemukan", fontSize = 14.sp, color = Color(0xFF94A3B8))
                            Text(text = "Coba ubah filter posisi atau lokasi Anda.", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(jobs) { job ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = job.title ?: "Tanpa Judul",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                            job.matchLabel?.let { label ->
                                                Surface(
                                                    color = Color(0xFF22304A),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = Color(0xFF60A5FA),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                            Text(text = "${job.company ?: "Perusahaan"} â€¢ Sumber: ${job.source ?: "Lowongan kerja"}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = "Lokasi: ${job.location ?: "-"}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                                Text(text = "Update: ${job.updated ?: "-"}", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }

                                            val safeLink = UrlSafety.safeHttpUrl(job.link)
                                            if (safeLink != null) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeLink))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(
                                                                context,
                                                                "Tautan lowongan tidak dapat dibuka.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Lihat Lowongan", fontSize = 12.sp)
                                                }
                                            } else if (!job.link.isNullOrBlank()) {
                                                Text(
                                                    text = "Tautan tidak valid",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFEF4444)
                                                )
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
            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1B263B)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedOption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF60A5FA))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


