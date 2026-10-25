package com.ciudadalerta.data.model

sealed class Categoria(
    val nombre: String,
    val iconName: String,
    val colorHex: Long,
    val radioAgrupacionMetros: Double,
    val descripcionObligatoria: Boolean = false
) {
    object Vias : Categoria("Vías", "warning", 0xFFE65100, 40.0)
    object Alumbrado : Categoria("Alumbrado público", "lightbulb", 0xFFF57F17, 30.0)
    object Residuos : Categoria("Residuos y aseo", "delete_sweep", 0xFF00897B, 35.0)
    object Parques : Categoria("Parques y zonas verdes", "park", 0xFF2E7D32, 50.0)
    object Semaforos : Categoria("Semáforos", "traffic", 0xFFD32F2F, 30.0)
    object Arboles : Categoria("Árboles (sin riesgo inminente)", "nature", 0xFF5D4037, 35.0)
    object Animales : Categoria("Animales abandonados", "pets", 0xFF7B1FA2, 45.0)
    object Otros : Categoria("Otros", "more_horiz", 0xFF546E7A, 30.0, descripcionObligatoria = true)

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
