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
        val regionMatches = selectedRegions.any { selected ->
            when (selected) {
                "Tangerang Selatan" -> city.contains("tangerang selatan") || city.contains("tangsel") || city.contains("bsd") || city.contains("serpong")
                "Tangerang" -> city.contains("tangerang") || city.contains("karawaci") || city.contains("cikupa")
                "Cikarang" -> city.contains("cikarang")
                "Bekasi" -> city.contains("bekasi") || city.contains("cikarang")
                else -> city.contains(selected.lowercase())
            }
        }
        return roleMatches && regionMatches
    }
}
