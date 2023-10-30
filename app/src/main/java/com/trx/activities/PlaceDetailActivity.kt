package com.trx.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.trx.R
import com.trx.databinding.ActivityPlaceDetailBinding
import com.trx.models.PlaceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.URL

class PlaceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceDetailBinding
    private var detailPlaceModel: PlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setting up toolbar
        setSupportActionBar(binding.toolbar.customToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //intent from MainActivity
        if (intent.hasExtra("MainActivityIntent")) {
            //though getSerializable and getParcelable is deprecated, no other function found
            detailPlaceModel = intent.getSerializableExtra("MainActivityIntent") as PlaceModel
        }

        supportActionBar!!.title = detailPlaceModel!!.title

        setImage(detailPlaceModel!!)

        binding.category.text = detailPlaceModel!!.category
        binding.location.text = detailPlaceModel!!.address

    }

    //for displaying image of the location
    private fun setImage(model: PlaceModel) {

        val latitude = model.latitude
        val longitude = model.longitude

        val zoom = 15
        val imageSize = "400x400"
        val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=$latitude,$longitude" +
                "&zoom=$zoom" +
                "&size=$imageSize" +
                "&markers=color:red%7C$latitude,$longitude" +
                "&key=${getString(R.string.google_maps_api_key)}"

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream: InputStream = URL(staticMapUrl).openStream()
                    val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
                    runOnUiThread {
                        binding.staticMap.setImageBitmap(bitmap)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
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

}