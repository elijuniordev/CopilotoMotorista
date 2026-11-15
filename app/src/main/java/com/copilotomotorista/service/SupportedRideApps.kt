package com.copilotomotorista.service

/**
 * Pacotes oficiais dos aplicativos de motorista suportados inicialmente.
 * Atualizar somente após confirmar o identificador na distribuição oficial.
 */
internal object SupportedRideApps {
    const val UBER_DRIVER = "com.ubercab.driver"
    const val NINETY_NINE_DRIVER = "com.app99.driver"

    val packages: Set<String> = setOf(UBER_DRIVER, NINETY_NINE_DRIVER)

    fun contains(packageName: String?): Boolean =
        packageName != null && packageName in packages
}
