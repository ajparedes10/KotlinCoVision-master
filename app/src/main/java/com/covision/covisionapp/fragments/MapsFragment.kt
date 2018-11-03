package com.covision.covisionapp.fragments

import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

import com.covision.covisionapp.R
import com.covision.covisionapp.structures.GetDirectionsData
import com.covision.covisionapp.structures.GetDirectionsDataRoutes
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task

import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

class MapsFragment : Fragment(), OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener, GoogleMap.OnMapClickListener, com.google.android.gms.location.LocationListener {
    internal var TAG_CODE_PERMISSION_LOCATION = 2
    var rta = arrayOf("")

    //widgets
    private var mSearchText: EditText? = null
    private var mGps: ImageView? = null

    //vars
    private var mLocationPermissionsGranted: Boolean? = false
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mPlaceDetectionClient: PlaceDetectionClient? = null

    lateinit var mapView: MapView
    internal var mMap: GoogleMap? = null
    lateinit var locationManager: LocationManager

    //distance between points
    internal var end_latitude: Double = 0.toDouble()
    internal var end_longitude: Double = 0.toDouble()
    private var lastLocation: Location? = null
    internal var latitude: Double = 0.toDouble()
    internal var longitude: Double = 0.toDouble()
    private var currentMarkerLocation: Marker? = null

    //Duration between two points
    private var dataTransfer: Array<Any?>? = null
    private var url: String? = null

    public fun getDeviceLocation(): String {
            val locationMessage = arrayOf("")
            Log.d(TAG, "getDeviceLocation: getting the devices current location")
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
            try {
                if (mLocationPermissionsGranted!!) {

                    val location: Task<Location>? = mFusedLocationProviderClient!!.lastLocation
                    if(location !=null){
                    location.addOnCompleteListener{task->
                            if (task.isSuccessful) {
                                Log.d(TAG, "onComplete: found location!")
                                var currentLocation = task.result as Location?
                                if (currentLocation == null) {

                                    latitude = 4.627590
                                    longitude = -74.080824
                                    currentLocation = Location("")
                                    currentLocation.longitude = 4.627590
                                    currentLocation.latitude = -74.080824
                                    Toast.makeText(activity,
                                            "Please check your gps signal", Toast.LENGTH_LONG).show()

                                } else {
                                    latitude = currentLocation.latitude
                                    longitude = currentLocation.longitude
                                }
                                moveCamera(LatLng(latitude, longitude), DEFAULT_ZOOM,"My Location")
                                locationMessage[0] = showCurrentPlace()
                                updateLocation(currentLocation)
                            } else {
                                Log.d(TAG, "onComplete: current location is null")
                                Toast.makeText(activity, "unable to get current location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else{
                        getLocationPermission()
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "getDeviceLocation: SecurityException: " + e.message)
            }

            return locationMessage[0]
        }

    private val directionsUrl: String
        get() {
            val googleDirectionsUrl = StringBuilder("https://maps.googleapis.com/maps/api/directions/json?")
            googleDirectionsUrl.append("origin=$latitude,$longitude")
            googleDirectionsUrl.append("&destination=$end_latitude,$end_longitude")
            googleDirectionsUrl.append("&key=" + "AIzaSyD77tEGJBBVl3gwXHS_wBbTRvsinUa1wNE")
            return googleDirectionsUrl.toString()
        }

    fun setRta(text: String) {
        rta[0] = text
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_maps, container, false)
        getLocationPermission()
        mapView = v.findViewById<View>(R.id.mapview) as MapView
        mapView.onCreate(savedInstanceState)
        // Gets to GoogleMap from the MapView and does initialization stuff
        mapView.getMapAsync(this)
        mSearchText = v.findViewById<View>(R.id.input_search) as EditText
        mGps = v.findViewById<View>(R.id.ic_gps) as ImageView
        return v
    }

    override fun onResume() {
        mapView.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        locationManager.removeUpdates(this)

    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
        locationManager.removeUpdates(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {


        Toast.makeText(activity, "Map is Ready", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onMapReady: map is ready")
        mMap = googleMap
        mMap!!.uiSettings.isZoomControlsEnabled = true
        //getDeviceLocation()
        if (mLocationPermissionsGranted!!) {
            getDeviceLocation()

            if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            mMap!!.isMyLocationEnabled = true
            mMap!!.uiSettings.isMyLocationButtonEnabled = true
            init()
        }

        if (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            mMap!!.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true

            // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
            MapsInitializer.initialize(this.activity!!)
            // Updates the location and zoom of the MapView
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(43.1, -87.9), 10f)
            mMap!!.animateCamera(cameraUpdate)
        } else {
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    TAG_CODE_PERMISSION_LOCATION)
        }


        mMap!!.setOnMarkerDragListener(this)
        mMap!!.setOnMapClickListener(this)

    }

    private fun init() {
        Log.d(TAG, "init: initializing")
        mSearchText!!.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || keyEvent.action == KeyEvent.ACTION_DOWN
                    || keyEvent.action == KeyEvent.KEYCODE_ENTER
                    || keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                //execute our method for searching
                Log.d(TAG, "trato de buscar")
                geoLocate("")
                Log.d(TAG, "trate de buscar")
            }
            Log.d(TAG, "no se pudo entrar a buscar")
            false
        }
        mGps!!.setOnClickListener {
            Log.d(TAG, "onClick: clicked gps icon")
            getDeviceLocation()
        }
        hideSoftKeyboard()
    }


    fun geoLocate(searchString : String) : String{
        var searchString = searchString
        Log.d(TAG, "geoLocate: geolocating")
        var dist = ""

        if (searchString === "") searchString = mSearchText!!.text.toString()
        val geocoder = Geocoder(context)
        var list: List<Address> = ArrayList()
        try {
            list = geocoder.getFromLocationName(searchString, 1)
        } catch (e: IOException) {
            Log.e(TAG, "geoLocate: IOException: " + e.message)
        }

        if (list.size > 0) {
            val address = list[0]

            Log.d(TAG, "geoLocate: found a location: " + address.toString())
            moveCamera(LatLng(address.latitude, address.longitude), DEFAULT_ZOOM,
                    address.getAddressLine(0))

            end_latitude = address.latitude
            end_longitude = address.longitude
            dist = putMarkerDistanceOF()
            durationOF()

            paintDirections()

        }
        return if (dist == "") {
            return "error"
        } else
            return dist
    }

    private fun moveCamera(latLng: LatLng, zoom: Float, title: String) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude)

        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

        if (title != "My Location") {
            val options = MarkerOptions()
                    .position(latLng)
                    .title(title)
            mMap!!.addMarker(options)
        }
        hideSoftKeyboard()
    }

    private fun hideSoftKeyboard() {
        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }


    private fun getLocationPermission() {
        locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager;
        Log.d(TAG, "getLocationPermission: getting location permissions")
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ContextCompat.checkSelfPermission(activity!!.applicationContext,
                        FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(activity!!.applicationContext,
                            COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true
                //verificar si el GPS está encendido
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Log.d(TAG, "el GPS no está activado")
                    showSettingsAlert()
                }

            } else {
                ActivityCompat.requestPermissions(activity!!, permissions, LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(activity!!, permissions, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }
    /**
     * Function to show settings alert dialog
     */
    fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(context!!)

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings")

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?")

        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings") { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context!!.startActivity(intent)
        }

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

        // Showing Alert Message
        alertDialog.show()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult: called.")
        mLocationPermissionsGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0) {
                    for (i in grantResults.indices) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false
                            Log.d(TAG, "onRequestPermissionsResult: permission failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted")
                    mLocationPermissionsGranted = true
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
        if (currentMarkerLocation != null) {
            currentMarkerLocation!!.remove()
        }
        latitude = location.latitude
        longitude = location.longitude
        val ltn = LatLng(location.latitude, location.longitude)
        val mop = MarkerOptions()
        mop.position(ltn)
        mop.draggable(true)
        mop.title("Current position")
        mop.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        currentMarkerLocation = mMap!!.addMarker(mop)

        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(ltn))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
        //remove location callback:
        locationManager.removeUpdates(this)
    }

    fun putMarkerDistanceOF(): String {
        val mop = MarkerOptions()
        mop.position(LatLng(end_latitude, end_longitude))
        mop.title("Destination ")
        mop.draggable(true)
        val results = FloatArray(10)
        Location.distanceBetween(latitude, longitude, end_latitude, end_longitude, results)
        mop.snippet("Distance = " + results[0])
        mMap!!.addMarker(mop)
        Log.d("MAMELO", "La distancia" + results[0])
        return results[0].toString()
    }

    fun durationOF(): String {
        dataTransfer = arrayOfNulls<Any>(3)
        url = directionsUrl
        var gtdta = GetDirectionsData()
        dataTransfer!![0] = mMap!!
        dataTransfer!![1] = url!!
        dataTransfer!![2] = LatLng(end_latitude, end_longitude)
        gtdta!!.execute(dataTransfer)
        return gtdta!!.duration!!

    }
    fun updateLocation(location: Location?) {
        lastLocation = location
        if (currentMarkerLocation != null) {
            currentMarkerLocation!!.remove()
        }
        latitude = location!!.latitude
        longitude = location.longitude
        val ltn = LatLng(location.latitude, location.longitude)
        val mop = MarkerOptions()
        mop.position(ltn)
        mop.draggable(true)
        mop.title("Current position")
        mop.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
        currentMarkerLocation = mMap!!.addMarker(mop)

        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(ltn))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {

    }

    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }

    //lugar actual, por ahora este método es llamado por getDeviceLocation() pero puede ser llamado desdde cualquier fragmento
    fun showCurrentPlace(): String {

        if (mMap == null) {
            return rta[0]
        }
        if (mLocationPermissionsGranted!!) {
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return rta[0]
            }

            mPlaceDetectionClient = Places.getPlaceDetectionClient(activity!!, null)
            val placeResult = mPlaceDetectionClient!!.getCurrentPlace(null)
            placeResult.addOnCompleteListener { task ->
                try {
                    val likelyPlaces = task.result
                    var max = -1f
                    for (placeLikelihood in likelyPlaces!!) {
                        Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                                placeLikelihood.place.name,
                                placeLikelihood.likelihood))
                        if (placeLikelihood.likelihood > max) {
                            max = placeLikelihood.likelihood
                            setRta("Te encuentras en " + placeLikelihood.place.name.toString())
                        }
                    }
                    likelyPlaces.release()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return rta[0]
        }
        return rta[0]
    }

    fun paintDirections() {

        mMap!!.clear()
        dataTransfer = arrayOfNulls<Any>(3)
        url = directionsUrl

        dataTransfer!![0] = mMap!!
        dataTransfer!![1] = url!!
        dataTransfer!![2] = LatLng(end_latitude, end_longitude)
        Log.i("GetDirectionsDataTag", "datatransfer size ")
        Log.i("GetDirectionsDataTag", "datatransfer size "+ dataTransfer!!.size.toString())
        val getDirectionsDataRoutes = GetDirectionsDataRoutes()
        getDirectionsDataRoutes.execute(dataTransfer)
    }


    override fun onMarkerClick(marker: Marker): Boolean {
        marker.isDraggable = true
        return false
    }

    override fun onMarkerDragStart(marker: Marker) {

    }

    override fun onMarkerDrag(marker: Marker) {

    }

    override fun onMarkerDragEnd(marker: Marker) {


    }

    override fun onMapClick(latLng: LatLng) {

    }
    companion object {
        private val TAG = "MapActivity"
        private val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
        private val LOCATION_PERMISSION_REQUEST_CODE = 1234
        private val DEFAULT_ZOOM = 15f


        fun newInstance(): Fragment {
            return MapsFragment()
        }
    }
}
