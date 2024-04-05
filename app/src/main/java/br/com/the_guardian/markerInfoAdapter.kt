package br.com.the_guardian

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class markerInfoAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {
    override fun getInfoContents(marker: Marker): View? = null

    override fun getInfoWindow(marker: Marker): View? {
        val place = marker.tag as? homeScreen.Place ?: return null
        val view = LayoutInflater.from(context).inflate(R.layout.activity_marker_info, null)

        view.findViewById<TextView>(R.id.marker_title).text = place.name
        view.findViewById<TextView>(R.id.marker_reference).text = place.reference
        view.findViewById<TextView>(R.id.marker_adress).text = place.address
        view.findViewById<TextView>(R.id.marker_disponibility).text = if (place.disponibility) "Está disponível: Sim"
        else "Está disponível: Não"

        val btnConsultar = view.findViewById<Button>(R.id.btn_consultar)
        btnConsultar.setOnClickListener {
            val intent = Intent(context, DataScreen::class.java).apply {
                putExtra("name", place.name)
                putExtra("reference", place.reference)
                putExtra("address", place.address)
                putExtra("disponibility", place.disponibility)
            }
            context.startActivity(intent)
        }

        return view
    }

}