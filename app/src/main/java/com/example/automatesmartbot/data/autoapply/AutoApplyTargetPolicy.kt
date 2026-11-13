package com.example.automatesmartbot.data.autoapply

/** V73: Region-first preview. Position is only a label for an application letter. */
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
        "Bekasi", "Cikarang", "Karawang", "Bandung", "Purwakarta"
    )

    fun matchesCandidate(position: String, location: String, selectedPositions: Set<String>, selectedRegions: Set<String>): Boolean {
        if (location.isBlank() || selectedPositions.isEmpty() || selectedRegions.isEmpty()) return false
        // Intentionally ignore position: it only determines the cover letter's role.
        val city = location.lowercase()
        val inTangsel = listOf("tangerang selatan", "tangsel", "bsd", "serpong", "pamulang", "ciputat")
            .any { token -> city.contains(token) }
        val inCikarang = city.contains("cikarang")
        return selectedRegions.any { selected ->
            when (selected.lowercase()) {
                "tangerang selatan" -> inTangsel
                "tangerang" -> !inTangsel && listOf("tangerang", "karawaci", "cikupa", "balaraja")
                    .any { token -> city.contains(token) }
                "cikarang" -> inCikarang
                "bekasi" -> city.contains("bekasi") || inCikarang
                else -> city.contains(selected.lowercase())
            }
        }
    }
}
