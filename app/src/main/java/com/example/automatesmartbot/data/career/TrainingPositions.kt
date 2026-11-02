package com.example.automatesmartbot.data.career

data class TrainingPositionGroup(
    val category: String,
    val positions: List<String>,
    val linkedCourseId: String? = null
)

/** Positions are discovery options; a linked course is not necessarily role-specific. */
object TrainingPositions {
    val groups = listOf(
        TrainingPositionGroup("Logistik & Warehouse", listOf("Admin Warehouse", "Staff Warehouse", "Picker", "Packer", "Inventory Control", "Warehouse Supervisor", "Kepala Gudang"), "warehouse"),
        TrainingPositionGroup("Administrasi Perkantoran", listOf("Staff Administrasi", "Data Entry", "Admin Operasional", "Sekretaris", "Document Control"), "administration"),
        TrainingPositionGroup("Sales & Marketing", listOf("Sales Executive", "Sales Counter", "Marketing Staff", "Digital Marketing", "SPG / SPB", "Sales Supervisor"), "sales"),
        TrainingPositionGroup("Customer Service", listOf("Customer Service", "Call Center Agent", "Customer Relation Officer", "Front Office"), "customer_service"),
        TrainingPositionGroup("Human Resources", listOf("HR Admin", "Recruitment Staff", "HR Generalist", "Payroll Staff", "HR Supervisor")),
        TrainingPositionGroup("Finance & Accounting", listOf("Kasir", "Finance Admin", "Accounting Staff", "Account Payable", "Account Receivable")),
        TrainingPositionGroup("Produksi & Manufaktur", listOf("Operator Produksi", "Quality Control", "Quality Assurance", "Production Leader", "Production Supervisor")),
        TrainingPositionGroup("Teknik & Maintenance", listOf("Teknisi", "Maintenance Staff", "Electrical Technician", "Mechanical Technician", "Engineering Staff")),
        TrainingPositionGroup("IT & Digital", listOf("IT Support", "Helpdesk IT", "Web Developer", "Software Tester", "Data Analyst")),
        TrainingPositionGroup("Retail & Store Operations", listOf("Store Crew", "Pramuniaga", "Kasir Retail", "Store Supervisor", "Kepala Toko")),
        TrainingPositionGroup("Food & Beverage", listOf("Barista", "Waiter", "Kitchen Crew", "Cook", "Baker", "Restaurant Supervisor")),
        TrainingPositionGroup("Transportasi & Pengiriman", listOf("Driver", "Kurir", "Dispatcher", "Transport Coordinator", "Fleet Supervisor")),
        TrainingPositionGroup("Security & General Affairs", listOf("Security", "Office Boy", "General Affairs Staff", "Building Maintenance", "GA Supervisor")),
        TrainingPositionGroup("Hospitality & Perhotelan", listOf("Housekeeping", "Receptionist", "Front Desk Agent", "Room Attendant", "Hotel Supervisor")),
        TrainingPositionGroup("Soft Skills & Pengembangan Karier", listOf("Persiapan Melamar Kerja", "Komunikasi Wawancara", "Leadership", "Komunikasi Profesional", "Manajemen Waktu"))
    )
    val count: Int get() = groups.sumOf { it.positions.size }
}
