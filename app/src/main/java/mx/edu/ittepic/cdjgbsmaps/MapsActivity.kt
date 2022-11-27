package mx.edu.ittepic.cdjgbsmaps


import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beust.klaxon.*
import com.beust.klaxon.Parser.Companion.default
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import mx.edu.ittepic.cdjgbsmaps.databinding.ActivityMapsBinding
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // declare bounds object to fit whole route in screen
        val LatLongB = LatLngBounds.Builder()

        // Add a marker in Sydney and move the camera
        val hogarJazmin = LatLng(21.475497, -104.866923)
        val hogarChris = LatLng(20.733876, -103.386827)
        mMap.addMarker(MarkerOptions().position(hogarJazmin).title("Jazmin's Home"))
        mMap.addMarker(MarkerOptions().position(hogarChris).title("Christian's Home"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(hogarJazmin))
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL;

        val options = PolylineOptions()
        options.color(Color.RED)
        options.width(5f)

        val app: ApplicationInfo = packageManager
            .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val bundle = app.metaData

        val laLlaave = bundle.getString("com.google.android.geo.API_KEY")

        val url = getURL(hogarJazmin, hogarChris, laLlaave )
        doAsync {
            val result = URL(url).readText()
            uiThread {
                // this will execute in the main thread, after the async call is done }
                val parser: Parser = default()
                val stringBuilder: StringBuilder = StringBuilder(result)
                val json: JsonObject = parser.parse(stringBuilder) as JsonObject

                val routes = json.array<JsonObject>("routes")
                val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject>

                val polypts = points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!)  }
                // Add  points to polyline and bounds
                options.add(hogarJazmin)
                LatLongB.include(hogarJazmin)
                for (point in polypts)  {
                    options.add(point)
                    LatLongB.include(point)
                }
                options.add(hogarChris)
                LatLongB.include(hogarChris)
                // build bounds
                val bounds = LatLongB.build()
                // add polyline to the map
                mMap!!.addPolyline(options)
                // show map with route centered
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

            }

        }


    }
}
    private fun getURL(from: LatLng, to: LatLng, pKey: String?): String? {
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"

        val key = "key=$pKey"

        //val key = "key=AIzaSyD0sCuJQ5gbK_t_K6f_6ah4fn40-VZllgE"
        val params = "$origin&$dest&$sensor&$key"
        return "https://maps.googleapis.com/maps/api/directions/json?$params"
    }
/**
 * Method to decode polyline points
 * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
 */
private fun decodePoly(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(lat.toDouble() / 1E5,
            lng.toDouble() / 1E5)
        poly.add(p)
    }

    return poly
}
