package com.example.automatesmartbot.data.model

object JobFilters {
    val positions: List<String> = listOf(
        "Admin Warehouse",
        "Admin Logistic",
        "Admin Operational",
        "Staff Warehouse",
        "Staff Logistic",
        "Supervisor Warehouse",
        "Supervisor Logistic"
    )

    val locations: List<String> = listOf(
        "Tangerang",
        "Bogor",
        "Depok",
        "Jakarta",
        "Bekasi"
    )

    const val VALIDATION_MESSAGE: String =
        "Pilih posisi dan lokasi yang valid. Permintaan tidak dapat diproses."

    fun isValid(position: String?, location: String?): Boolean {
        return position != null &&
            location != null &&
            position in positions &&
            location in locations
    }
}

