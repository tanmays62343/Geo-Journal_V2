package com.trx.activities

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.trx.R
import com.trx.database.PlacesDatabase
import com.trx.databinding.ActivityPlaceFormBinding
import com.trx.models.PlaceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlaceFormActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityPlaceFormBinding

    //initializing our database
    private lateinit var database: PlacesDatabase
    private var mPlaceDetails: PlaceModel? = null

    //Properties of our place object
    private lateinit var title: String
    private lateinit var date: String
    private var latitude: Double = 0.00
    private var longitude: Double = 0.00
    private var address: String = ""
    private var category: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setting up toolbar
        setSupportActionBar(binding.toolbar.customToolbar)
        supportActionBar?.title = "Add place"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Getting current date
        val currentDate = getCurrentDate()
        binding.date.text = "Date : $currentDate"

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        //Getting instance of the database
        database = PlacesDatabase.getInstance(applicationContext)

        //intent from edit item
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mPlaceDetails =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as PlaceModel
        }
        //set the views if edit is asked
        if (mPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"


            binding.tvTitle.setText(mPlaceDetails!!.title)
            binding.tvCategory.text = mPlaceDetails!!.category
            binding.date.text = mPlaceDetails!!.date
            binding.tvAddress.text = mPlaceDetails!!.address

            latitude = mPlaceDetails!!.latitude
            longitude = mPlaceDetails!!.longitude
        }


        //if the intent is coming from the draggable marker
        if (intent.hasExtra("DRAG_LATITUDE") && intent.hasExtra("DRAG_LONGITUDE") &&
            intent.hasExtra("DRAG_ADDRESS")
        ) {
            try {
                latitude = intent.getDoubleExtra("DRAG_LATITUDE", 0.0)
                longitude = intent.getDoubleExtra("DRAG_LONGITUDE", 0.0)
                address = intent.getStringExtra("DRAG_ADDRESS").toString()
                binding.tvAddress.text = address
            } catch (e: Exception) {
                Toast.makeText(
                    this, "Some error in fetching place",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //if intent is coming from the search activity
        if (intent.hasExtra("SEARCH_LATITUDE") && intent.hasExtra("SEARCH_LONGITUDE") &&
            intent.hasExtra("SEARCH_ADDRESS")
        ) {
            try {
                latitude = intent.getDoubleExtra("SEARCH_LATITUDE", 0.0)
                longitude = intent.getDoubleExtra("SEARCH_LONGITUDE", 0.0)
                address = intent.getStringExtra("SEARCH_ADDRESS").toString()
                binding.tvAddress.text = address
            } catch (e: Exception) {
                Toast.makeText(
                    this, "Error in fetching location Details",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //Handling click on radio Buttons
        binding.rbgCategory.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbCommercial.id -> {
                    category = "COMMERCIAL"
                }

                binding.rbResidential.id -> {
                    category = "RESIDENTIAL"
                }
            }
        }

        //Handling date text View
        binding.btnCalendar.setOnClickListener(this)
        //Handling the add button
        binding.btnAdd.setOnClickListener(this)

    }

    override fun onClick(v: View?) {

        when (v!!.id) {

            binding.btnCalendar.id -> {
                showDatePickerDialog()
            }

            binding.btnAdd.id -> {

                //fields validation
                if (binding.tvTitle.text.isEmpty() || category.isEmpty() ||
                    binding.tvAddress.text.isEmpty()
                ) {
                    Toast.makeText(
                        this, "Fields cannot be Empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                savePlaceData()

                val placeObj = PlaceModel(
                    0,
                    binding.tvTitle.text.toString(),
                    category,
                    binding.date.text.toString(),
                    address,
                    latitude,
                    longitude
                )

                //Creating coroutine scope to perform an DB Operation (using lifecycleScope instead of GlobalScope)
                //if new place is added
                if (mPlaceDetails == null) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            database.contactDao().insertPlace(placeObj)
                        }
                        Toast.makeText(
                            this@PlaceFormActivity, "Place Inserted",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
                //if edit is asked
                else {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            database.contactDao().updatePlace(placeObj)
                        }
                    }
                }
                //this will activate after pressing add button
                Intent(
                    this@PlaceFormActivity,
                    MainActivity::class.java
                ).also {
                    //We are using flags in intent to clear the back stack of activities
                    it.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                }
                //finish() Instead of it we have used flags in our intent
            }
        }
    }

    private fun savePlaceToFirebase(place: PlaceModel) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("places")
        val newRef = myRef.push()
        newRef.setValue(place) // Push the place data to Firebase Realtime Database
    }

    // When you want to save the place data
    private fun savePlaceData() {
        if (mPlaceDetails == null) {
            // New place, save it to Firebase and local database
            val placeObj = PlaceModel(
                0,
                binding.tvTitle.text.toString(),
                category,
                binding.date.text.toString(),
                address,
                latitude,
                longitude
            )

            // Save to Firebase
            savePlaceToFirebase(placeObj)

            // Show a message or navigate back
            navigateToMainActivity()
        } else {
            // Existing place, update it in Firebase and local database
            // ...
            // Handle the update logic here

            // Show a message or navigate back
            navigateToMainActivity()
        }
    }

    private fun savePlaceToLocalDatabase(place: PlaceModel) {
        // Create a coroutine to perform database insert operation
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.contactDao().insertPlace(place)
            }
        }
    }

    private fun navigateToMainActivity() {
        Intent(
            this@PlaceFormActivity,
            MainActivity::class.java
        ).also {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(it)
        }
    }



    //Handling back Button on toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //for getting the current date
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    //for showing the date picker dialog after click
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                // Handle the selected date
                onDateSet(year, month, dayOfMonth)
            },
            currentYear,
            currentMonth,
            currentDay
        )
        datePickerDialog.show()
    }

    //After selecting date from dialog and setting it in text view
    private fun onDateSet(year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = Calendar.getInstance()
        selectedDate.set(Calendar.YEAR, year)
        selectedDate.set(Calendar.MONTH, month)
        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy")
        val formattedDate = dateFormat.format(selectedDate.time)

        binding.date.text = "Date : $formattedDate"
    }

}