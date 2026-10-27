package com.ciudadalerta.data.model

sealed class Categoria(
    val nombre: String,
    val colorHex: Long
) {
    object Vias : Categoria("Vías", 0xFFE65100)
    object Alumbrado : Categoria("Alumbrado público", 0xFFF57F17)
    object Residuos : Categoria("Residuos y aseo", 0xFF00897B)
    object Parques : Categoria("Parques y zonas verdes", 0xFF2E7D32)
    object Semaforos : Categoria("Semáforos", 0xFFD32F2F)
    object Arboles : Categoria("Árboles (sin riesgo inminente)", 0xFF5D4037)
    object Animales : Categoria("Animales abandonados", 0xFF7B1FA2)
    object Otros : Categoria("Otros", 0xFF546E7A)

    companion object {
        val CATALOGO = listOf(Vias, Alumbrado, Residuos, Parques, Semaforos, Arboles, Animales, Otros)

        val CATEGORIAS_BLOQUEADAS = setOf(
            "EMERGENCIA", "DELITO", "ROBO", "SALUD", "ACCIDENTE_GRAVE",
            "INCENDIO", "HERIDOS", "ARMA", "VIOLENCIA"
        )

        fun fromString(nombreRaw: String?): Categoria {
            if (nombreRaw.isNullOrBlank()) return Otros
            val lower = nombreRaw.lowercase()
            return CATALOGO.firstOrNull { 
                it.nombre.lowercase().contains(lower) || lower.contains(it.nombre.lowercase()) 
            } ?: Otros
        }
    }
}
