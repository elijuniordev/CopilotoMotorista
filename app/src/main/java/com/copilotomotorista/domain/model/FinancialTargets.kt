package com.copilotomotorista.domain.model

data class FinancialTargets(
    val excellentNetCentsPerKm: Long = 300L,
    val goodNetCentsPerKm: Long = 200L,
    val warningNetCentsPerKm: Long = 120L,
    val minimumNetCentsPerHour: Long = 0L
) {
    init {
        require(excellentNetCentsPerKm >= goodNetCentsPerKm)
        require(goodNetCentsPerKm >= warningNetCentsPerKm)
        require(warningNetCentsPerKm >= 0)
        require(minimumNetCentsPerHour >= 0)
    }
}
