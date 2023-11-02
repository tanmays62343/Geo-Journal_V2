package com.trx.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.trx.R
import com.trx.adapters.MainViewAdapter
import com.trx.database.PlacesDatabase
import com.trx.databinding.ActivityMainBinding
import com.trx.models.PlaceModel
import com.trx.swipe.SwipeToDeleteCallback
import com.trx.swipe.SwipeToEditCallback
import kotlin.math.atan2
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    //for current location
    private var fusedLocationClient: FusedLocationProviderClient? = null

    //initializing Database
    private lateinit var database: PlacesDatabase

    //To hold the list of places form Database
    private var placesList: ArrayList<PlaceModel> = ArrayList()

    //Instance of Adapter class to call its methods
    private var placesAdapter: MainViewAdapter? = null

    //for getting the spinner's Distance
    private var distance: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //-----------Ask for Permissions---------------
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
        }
        //-----------------end-------------------------

        //Instantiating the Database
        database = PlacesDatabase.getInstance(applicationContext)

        //Getting places list from Database and setting up the recycler View
        getPlacesListFromDB()


        //for Getting current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Handling the Spinner
        binding.spDistance.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            val distanceArray = resources.getStringArray(R.array.Distances_Filter)
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedDistance = distanceArray[position]
                when (selectedDistance) {

                    "All" -> {
                        distance = null
                    }

                    "500m" -> {
                        distance = 500.00
                    }

                    "1km" -> {
                        distance = 1000.00
                    }

                    "1.5km" -> {
                        distance = 1500.00
                    }

                    "2km" -> {
                        distance = 2000.00
                    }

                    "2.5km" -> {
                        distance = 2500.00
                    }

                    "3km" -> {
                        distance = 3000.00
                    }
                }

            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                return
            }
        }

        //Handling the View Map button
        binding.btnViewMap.setOnClickListener {
            Intent(this, MapActivity::class.java)
                .putExtra("VIEW", "VIEW_MAP").also {
                    startActivity(it)
                }
        }

        //Handling the ADD Button
        binding.btnAddPlace.setOnClickListener {
            Intent(this, MapActivity::class.java).also {
                it.putExtra("ADD", "ADDON_MAP")
                startActivity(it)
            }
        }

        binding.chipAll.setOnClickListener {
            adapterChange(0)
        }

        binding.chipResidential.setOnClickListener {
            adapterChange(1)
        }

        binding.chipCommercial.setOnClickListener {
            adapterChange(2)
        }

    }


    //for getting the places list from database and setting up Recycler view
    private fun getPlacesListFromDB() {

        val getPlacesList = database.contactDao().getPlaces()

        getPlacesList.observe(this@MainActivity) {
            if (!it.isNullOrEmpty()) {
                binding.placesList.visibility = View.VISIBLE
                binding.tvDefaultPlace.visibility = View.GONE
                placesList.clear()
                placesList.addAll(it) //it as ArrayList<PlaceModel>
                setupPlacesRecyclerView()

            } else {
                binding.placesList.visibility = View.GONE
                binding.tvDefaultPlace.visibility = View.VISIBLE
            }
        }

    }

    companion object {
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        internal const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }

    //Used to assign list of Residential or commercial to the adapter
    private fun adapterChange(flag: Int = 0) {
        val tempList: ArrayList<PlaceModel> = ArrayList()
        when (flag) {
            0 -> {
                tempList.clear()
                tempList.addAll(placesList)
                placesAdapter!!.dataList(tempList)
            }

            1 -> {
                tempList.clear()
                val residentialList = placesList.filter { it.category == "RESIDENTIAL" }
                tempList.addAll(residentialList)
                placesAdapter!!.dataList(tempList)
            }

            2 -> {
                tempList.clear()
                val commercialList = placesList.filter { it.category == "COMMERCIAL" }
                tempList.addAll(commercialList)
                placesAdapter!!.dataList(tempList)
            }
        }
    }

    //Function to setup the recycler View
    private fun setupPlacesRecyclerView() {
        binding.placesList.layoutManager = LinearLayoutManager(this)
        binding.placesList.setHasFixedSize(true)

        placesAdapter = MainViewAdapter(this, fusedLocationClient, placesList)
        fusedLocationClient?.let { placesAdapter!!.setCurrentLocation(it) }
        binding.placesList.adapter = placesAdapter
        adapterChange(0)
        placesAdapter!!.setOnClickListener(object :
            MainViewAdapter.OnClickListener {
            override fun onClick(position: Int, model: PlaceModel) {
                val intent = Intent(this@MainActivity, PlaceDetailActivity::class.java)
                intent.putExtra(
                    EXTRA_PLACE_DETAILS,
                    model
                ) // Passing the complete serializable data class to the detail activity using intent.
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
                getPlacesListFromDB() // Gets the latest list from the local database after item being delete from it.
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

    //this method will handel the user's input on asking permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 69){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                recreate()
            }
        }
    }

}