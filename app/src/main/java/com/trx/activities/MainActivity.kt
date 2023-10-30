package com.trx.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
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
    private lateinit var database : PlacesDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Instantiating the Database
        database = PlacesDatabase.getInstance(applicationContext)

        //Getting all the places
        getHappyPlacesListFromLocalDB()

        //for Getting current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Handling the Spinner
        binding.spDistance.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            val distanceArray = resources.getStringArray(R.array.Distances_Filter)
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDistance = distanceArray[position]

                when(selectedDistance){
                    "All" -> {

                    }

                }

            }
            override fun onNothingSelected(adapterView: AdapterView<*>?) {

            }
        }

        //Handling the View Map button
        binding.btnViewMap.setOnClickListener {
            Intent(this,MapActivity::class.java)
                .putExtra("VIEW","VIEW_MAP").also{
                startActivity(it)
            }
        }

        //Handling the ADD Button
        binding.btnAddPlace.setOnClickListener{
            Intent(this,MapActivity::class.java).also {
                it.putExtra("ADD","ADDON_MAP")
                startActivity(it)
            }
        }

    }

    //for getting all the places from the database
    private fun getHappyPlacesListFromLocalDB() {

        val getPlacesList = database.contactDao().getPlaces()

        getPlacesList.observe(this@MainActivity, Observer {
            if(!it.isNullOrEmpty()){
                binding.placesList.visibility = View.VISIBLE
                binding.tvDefaultPlace.visibility = View.GONE
                setupHappyPlacesRecyclerView(it as ArrayList<PlaceModel>?)
            }else {
                binding.placesList.visibility = View.GONE
                binding.tvDefaultPlace.visibility = View.VISIBLE
            }

        })

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