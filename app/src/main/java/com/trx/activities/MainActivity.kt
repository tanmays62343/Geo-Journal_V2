package com.trx.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.widget.AdapterView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.trx.R
import com.trx.adapters.MainViewAdapter
import com.trx.database.PlacesDatabase
import com.trx.databinding.ActivityMainBinding
import com.trx.miscellaneous.ConnectivityReceiver
import com.trx.models.PlaceModel
import com.trx.miscellaneous.SwipeToDeleteCallback
import com.trx.miscellaneous.SwipeToEditCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private var distance: Double? = null
    private var mPlacesList: LiveData<List<PlaceModel>>? = null
    private var selectedDistance: String = "All" // Default value is "All"

    //for current location
    private val filteredMarkers: ArrayList<PlaceModel> = ArrayList()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var selectedFilter = "all"

    //initializing Database
    private lateinit var database: PlacesDatabase

    private var currentLatLng: LatLng = LatLng(0.0, 0.0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Instantiating the Database
        database = PlacesDatabase.getInstance(applicationContext)
        mPlacesList = database.contactDao().getPlaces()
        //Getting all the places
        getHappyPlacesListFromLocalDB()
        //for Getting current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Handling the Spinner
        binding.spDistance.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            val distanceArray = resources.getStringArray(R.array.Distances_Filter)

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedDistance = distanceArray[position]
                filterMarkersByDistance(selectedDistance)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

        binding.btnAddPlace.setOnClickListener(this)
        binding.btnViewMap.setOnClickListener(this)
        binding.chipAll.setOnClickListener(this)
        binding.chipCommercial.setOnClickListener(this)
        binding.chipResidential.setOnClickListener(this)


    }

    override fun onClick(v: View?) {

        when (v!!.id) {
            R.id.chipAll -> {
                selectedFilter = "all"
                filterList(selectedFilter)

            }

            R.id.chipCommercial -> {
                selectedFilter = "Commercial"
                filterList("Commercial")
            }

            R.id.chipResidential -> {
                selectedFilter = "Residential"
                filterList("Residential")
            }//Handling the ADD Button
            R.id.btn_addPlace -> {
                Intent(this, MapActivity::class.java).also {
                    it.putExtra("ADD", "ADDON_MAP")
                    startActivity(it)
                }
            }//Handling the View Map button
            R.id.btn_viewMap -> {
                Intent(this, MapActivity::class.java).also {
                    val distanceArray = resources.getStringArray(R.array.Distances_Filter)

                    selectedDistance = distanceArray[binding.spDistance.selectedItemPosition]
                    it.putExtra("AllMarker", "VIEW_MAP")
                    it.putExtra("SelectedDistance",selectedDistance)
                    startActivity(it)
                }
            }
        }

    }

    private fun filterList(status: String) {
        selectedFilter = status
        database.contactDao().getPlaces().observe(this@MainActivity) {
            val filteredList: ArrayList<PlaceModel> = ArrayList()
            val newFilteredData = it.filter { item ->
                item.category == status
            }
            val sortedList = if (status == "all") {
                it.sortedBy { placeModel ->
                    calculateDistance(
                        currentLatLng.latitude,
                        currentLatLng.longitude,
                        placeModel.latitude,
                        placeModel.longitude
                    )
                }
            } else {
                newFilteredData.sortedBy { placeModel ->
                    calculateDistance(
                        currentLatLng.latitude,
                        currentLatLng.longitude,
                        placeModel.latitude,
                        placeModel.longitude
                    )
                }
            }

            filteredList.addAll(sortedList)

            val placesAdapter = MainViewAdapter(this, fusedLocationClient, filteredList)
            fusedLocationClient?.let { placesAdapter.setCurrentLocation(it) }
            binding.placesList.adapter = placesAdapter
        }


    }

    private fun getRadiusFromSelectedDistance(selectedDistance: String): Double {


                when (selectedDistance) {
                    "All" -> {
                        distance = Double.MAX_VALUE
                    }

                    "500m" -> {
                        distance = 500.0
                    }

                    "1km" -> {
                        distance = 1000.0
                    }

                    "1.5km" -> {
                        distance = 1500.0
                    }

                    "2km" -> {
                        distance = 2000.0
                    }

                    "2.5km" -> {
                        distance = 2500.0
                    }

                    "3km" -> {
                        distance = 3000.0
                    }
                }



        return distance!!

    }

    private fun filterMarkersByDistance(selectedDistance: String) {
        val radius = getRadiusFromSelectedDistance(selectedDistance)

        // Check if you have the necessary location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        mPlacesList!!.observe(this@MainActivity) {
            fusedLocationClient?.lastLocation?.addOnSuccessListener(this) { location ->
                if (location != null) {
                    // Use the device's actual location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    // Clear existing filtered markers
                    filteredMarkers.clear()
                    for (happyPlaceModel in it!!) {
                        val distance = calculateDistance(
                            currentLatLng.latitude,
                            currentLatLng.longitude,
                            happyPlaceModel.latitude,
                            happyPlaceModel.longitude
                        )

                        if (distance <= radius) {
                            filteredMarkers.add(happyPlaceModel)
                        }
                    }

                    // Update the RecyclerView with the filtered markers
                    setupHappyPlacesRecyclerView(filteredMarkers)
                } else {
                    // Handle the case where location is not available
                    // You can show an error message or use a default location
                }
            }

        }

    }

    //for getting all the places from the database
    private fun getHappyPlacesListFromLocalDB() {

        val getPlacesList = database.contactDao().getPlaces()

        getPlacesList.observe(this@MainActivity) {
            if (!it.isNullOrEmpty()) {
                binding.placesList.visibility = View.VISIBLE
                binding.tvDefaultPlace.visibility = View.GONE
                setupHappyPlacesRecyclerView(it as ArrayList<PlaceModel>?)
            } else {
                binding.placesList.visibility = View.GONE
                binding.tvDefaultPlace.visibility = View.VISIBLE
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient?.lastLocation?.addOnSuccessListener(this) { location ->
            if (location != null) {
                // Use the device's actual location
                currentLatLng = LatLng(location.latitude, location.longitude)
            }
        }


    }

    companion object {
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        internal const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }

    //Function to setup the recycler View
    private fun setupHappyPlacesRecyclerView(happyPlacesList: ArrayList<PlaceModel>?) {
        binding.placesList.layoutManager = LinearLayoutManager(this)
        binding.placesList.setHasFixedSize(true)

        val placesAdapter = MainViewAdapter(this, fusedLocationClient, happyPlacesList!!)
        fusedLocationClient?.let { placesAdapter.setCurrentLocation(it) }
        binding.placesList.adapter = placesAdapter

        placesAdapter.setOnClickListener(object :
            MainViewAdapter.OnClickListener {
            override fun onClick(position: Int, model: PlaceModel) {
                val intent = Intent(this@MainActivity, PlaceDetailActivity::class.java)
                intent.putExtra(
                    EXTRA_PLACE_DETAILS,
                    model
                ) // Passing the complete serializable data class to the detail activity using intent.
                intent.putExtra("SelectedDescription", model.category)
                startActivity(intent)
            }
        })

        //Swipe To edit
        val editSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.placesList.adapter as MainViewAdapter
                adapter.notifyEditItem(
                    this@MainActivity,
                    viewHolder.adapterPosition,
                    ADD_PLACE_ACTIVITY_REQUEST_CODE
                )
            }
        }
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding.placesList)

        //Swipe to Delete
        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.placesList.adapter as MainViewAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                getHappyPlacesListFromLocalDB() // Gets the latest list from the local database after item being delete from it.
            }
        }

        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding.placesList)
    }

    //To filter the places according to the distance
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val radiusOfEarth = 6371 // Earth's radius in kilometers
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(deltaLat / 2) * kotlin.math.sin(deltaLat / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLon / 2) * kotlin.math.sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radiusOfEarth * c * 1000
    }


}