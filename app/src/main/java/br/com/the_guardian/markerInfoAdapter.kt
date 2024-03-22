package br.com.the_guardian

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class markerInfoAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter{
    override fun getInfoContents(marker: Marker): View? = null

    override fun getInfoWindow(marker: Marker): View? {

        val place = marker.tag as? homeScreen.Place ?: return null

        val view = LayoutInflater.from(context).inflate(R.layout.activity_marker_info, null)

        view.findViewById<TextView>(R.id.marker_title).text = place.name
        view.findViewById<TextView>(R.id.marker_reference).text = place.reference
        view.findViewById<TextView>(R.id.marker_adress).text = place.address
        view.findViewById<TextView>(R.id.marker_disponibility).text = "Está disponível: ${place.disponibility}"
        view.findViewById<Button>(R.id.btn_consultar)
        return view
    }


}