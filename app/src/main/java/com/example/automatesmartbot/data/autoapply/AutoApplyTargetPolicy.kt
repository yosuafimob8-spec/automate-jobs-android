package com.example.automatesmartbot.data.autoapply

/** Preview matching only; actual delivery filters must be enforced by the authenticated backend. */
object AutoApplyTargetPolicy {
    val positions = listOf(
        "Admin Gudang", "Staff Gudang", "Supervisor Gudang",
        "Admin Warehouse", "Staff Warehouse", "Supervisor Warehouse",
        "Admin Logistik", "Staff Logistik", "Supervisor Logistik",
        "Admin Operasional", "Staff Operasional", "Supervisor Operasional",
        "Operator Produksi", "Staff Purchasing", "Staff Inventory",
        "Driver", "Sales"
    )
    val regions = listOf(
        "Jakarta", "Bogor", "Depok", "Tangerang", "Tangerang Selatan",
        "Bekasi", "Cikarang", "Karawang"
    )

    fun matchesCandidate(position: String, location: String, selectedPositions: Set<String>, selectedRegions: Set<String>): Boolean {
        if (position.isBlank() || location.isBlank() || selectedPositions.isEmpty() || selectedRegions.isEmpty()) return false
        val role = position.lowercase()
        val city = location.lowercase()
        val roleMatches = selectedPositions.any { selected ->
            val key = selected.lowercase()
            val aliases = when (key) {
                "staff warehouse" -> listOf("staff warehouse", "staff gudang")
                "admin warehouse" -> listOf("admin warehouse", "admin gudang")
                "supervisor warehouse" -> listOf("supervisor warehouse", "supervisor gudang")
                "staff gudang" -> listOf("staff gudang", "staff warehouse")
                "admin gudang" -> listOf("admin gudang", "admin warehouse")
                "supervisor gudang" -> listOf("supervisor gudang", "supervisor warehouse")
                else -> listOf(key)
            }
            aliases.any { role.contains(it) }
        }
        // KIRIM_LAMARAN_FIX_V1: avoid crossing distinct region choices.
        val inTangsel = listOf("tangerang selatan", "tangsel", "bsd", "serpong", "pamulang", "ciputat")
            .any { token -> city.contains(token) }
        val inCikarang = city.contains("cikarang")
        val regionMatches = selectedRegions.any { selected ->
            when (selected) {
                "Tangerang Selatan" -> inTangsel
                "Tangerang" -> !inTangsel && listOf("tangerang", "karawaci", "cikupa", "balaraja").any { token -> city.contains(token) }
                "Cikarang" -> inCikarang
                "Bekasi" -> city.contains("bekasi") || inCikarang
                else -> city.contains(selected.lowercase())
            }
        }
        return roleMatches && regionMatches
    }
}
