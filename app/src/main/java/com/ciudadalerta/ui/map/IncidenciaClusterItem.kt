package com.ciudadalerta.ui.map

import com.ciudadalerta.data.model.Incidencia
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class IncidenciaClusterItem(
    val incidencia: Incidencia
) : ClusterItem {
    override val position: LatLng = LatLng(incidencia.ubicacion.latitud, incidencia.ubicacion.longitud)
    override val title: String = incidencia.titulo
    override val snippet: String = incidencia.descripcion
    override val zIndex: Float? = null
}
