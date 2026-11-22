package com.ciudadalerta.data.config

/**
 * Configuración global y constantes de la aplicación CiudadAlerta.
 */
object CiudadAlertaConfig {
    /** Tiempo en horas para requerir la re-verificación de una incidencia activa. */
    const val TIEMPO_REVERIFICACION_X_HORAS = 48L

    /** Número de marcas de resuelto necesarias para cambiar el estado de una incidencia a RESUELTA. */
    const val MARCAS_PARA_RESOLVER_N = 3

    /** Número de días que una incidencia permanece visible como RESUELTA antes de no mostrarse en el mapa. */
    const val DIAS_VISIBLE_RESUELTA = 4L

    /** Número de denuncias requeridas para ocultar una incidencia del mapa. */
    const val DENUNCIAS_PARA_OCULTAR_M = 3

    /** Límite máximo de reportes que un usuario puede realizar en un día. */
    const val MAX_REPORTES_POR_USUARIO_DIA = 5

    /** Radio por defecto en metros para agrupar o buscar incidencias similares. */
    const val RADIO_AGRUPACION_DEFAULT_METROS = 40.0

    /** Número telefónico de la línea de emergencias. */
    const val LINEA_EMERGENCIAS_123 = "123"
}
