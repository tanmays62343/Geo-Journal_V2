package com.trx.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.trx.R
import com.trx.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    //OnMapReadyCallback interface for implementing google maps

    //Class Members
    private var binding: ActivityMapBinding? = null    //for view binding
    private var mGoogleMap: GoogleMap? = null      //for initializing google map
    private lateinit var autoCompleteFragment: AutocompleteSupportFragment  //auto complete search

    //Current location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        //<----------For adding Google Map---------->
        // can use id with view binding like this
        val mapFragment = supportFragmentManager.findFragmentById(binding?.mapFragment!!.id)
                as SupportMapFragment
        mapFragment.getMapAsync(this)
        //<----------end----------------------------->


        // <----------Google Autocomplete search from places API------------->
        Places.initialize(applicationContext, getString(R.string.google_maps_api_key))
        //normal use of layout id
        autoCompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment
        autoCompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )
        autoCompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {

            override fun onError(place: Status) {
                Toast.makeText(
                    this@MapActivity, "Cannot Fetch Location",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onPlaceSelected(place: Place) {
                val address = place.address
                val latLng = place.latLng

                val latitude = latLng?.latitude
                val longitude = latLng?.longitude

                Intent(this@MapActivity, PlaceFormActivity::class.java).also {
                    it.putExtra("SEARCH_LATITUDE", latitude)
                    it.putExtra("SEARCH_LONGITUDE", longitude)
                    it.putExtra("SEARCH_ADDRESS", address)
                    startActivity(it)
                }
            }
        })
        //<-------------Autocomplete Search ends here----------------->


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        //for zoom on current location
        mGoogleMap?.uiSettings?.isZoomControlsEnabled = true
        setupMap()

        //functions of draggable marker
        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) {
            }

            override fun onMarkerDragStart(marker: Marker) {
            }

            //Starting the placeForm activity on marker Drag end
            override fun onMarkerDragEnd(marker: Marker) {
                val position = marker.position
                val latitude = position.latitude
                val longitude = position.longitude
                val address = getAddress(latitude, longitude)

                Intent(this@MapActivity, PlaceFormActivity::class.java).also {
                    it.putExtra("DRAG_LATITUDE", latitude)
                    it.putExtra("DRAG_LONGITUDE", longitude)
                    it.putExtra("DRAG_ADDRESS", address)
                    startActivity(it)
                }
            }

        })

    }

    private fun setupMap() {

        val requestCode = 69

        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, requestCode)
            setupMap()
        } else {
            mGoogleMap?.isMyLocationEnabled = true
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLang = LatLng(location.latitude, location.longitude)
                    if (intent.hasExtra("ADD")) placeMarkerOnMap(currentLatLang)
                    mGoogleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            currentLatLang,
                            15f
                        )
                    )
                } else {
                    Toast.makeText(this, "Cannot fetch current place",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun placeMarkerOnMap(position: LatLng) {

        val customDraggable = BitmapFactory.decodeResource(resources,R.drawable.img_custom_marker)
        val resizedDraggable = Bitmap.createScaledBitmap(customDraggable,130,130,false)

        val marker = MarkerOptions().position(position)
        marker.title("Drag me to select a location")
        marker.draggable(true)
        marker.icon(BitmapDescriptorFactory.fromBitmap(resizedDraggable))
        mGoogleMap?.addMarker(marker)
    }

    //getting address from latitude and longitude
    private fun getAddress(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this)
        val list = geocoder.getFromLocation(latitude, longitude, 1)
        return list!![0].getAddressLine(0)
    }

    //showing details on marker click
    override fun onMarkerClick(place: Marker) = false

}