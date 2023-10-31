package com.trx.adapters

import com.trx.R
import com.trx.models.PlaceModel
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.trx.activities.MainActivity
import com.trx.activities.PlaceDetailActivity
import com.trx.activities.PlaceFormActivity
import com.trx.database.PlacesDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlin.math.atan2
import kotlin.math.sqrt

class MainViewAdapter(
    private val context: Context,
    private var fusedLocationClient: FusedLocationProviderClient? = null,
    private var list: ArrayList<PlaceModel>

) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var currentLocation: LatLng? = null
    private var onClickListener: OnClickListener? = null
    private lateinit var database: PlacesDatabase


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_happy_place,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder) {
            holder.tvTitle.text = model.title
            holder.tvDescription.text = model.category

            var distance = 0.0
            var dist = 0.0

            currentLocation?.let { location ->
                distance = calculateDistance(
                    location.latitude,
                    location.longitude,
                    model.latitude,
                    model.longitude
                )
            }
            if (distance > 1000) {
                dist = distance / 1000
                holder.tvDist.text = String.format("%.2f\nkms", dist)
            } else {
                dist = distance
                holder.tvDist.text = String.format("%.2f meters", dist)
            }

            holder.itemView.setOnClickListener {

                val intent = Intent(context, PlaceDetailActivity::class.java)
                intent.putExtra("MainActivityIntent", model)
                // Passing the complete serializable data class to the detail activity using intent.
                intent.putExtra("SelectedDescription", model.category)
                context.startActivity(intent)
//                    onClickListener!!.onClick(position, model)

            }
        }
    }


    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return list.size
    }

    /**
     * A function to edit the added happy place detail and pass the existing details through intent.
     */
    fun notifyEditItem(activity: Activity, position: Int, requestCode: Int) {
        val intent = Intent(context, PlaceFormActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS, list[position])
        activity.startActivityForResult(
            intent,
            requestCode
        ) // Activity is started with requestCode

        notifyItemChanged(position) // Notify any registered observers that the item at position has changed.
    }

    /**
     * A function to delete the added happy place detail from the local storage.
     */
    fun removeAt(position: Int) {
        if (position >= 0 && position < list.size) {
            database = PlacesDatabase.getInstance(context)

            CoroutineScope(Dispatchers.IO).launch {
                // Perform database operation in a background thread
                database.contactDao().deletePlace(list[position])
                list.removeAt(position)
                notifyItemRemoved(position)
            }


        }
    }

    /**
     * A function to bind the onclickListener.
     */
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    fun setCurrentLocation(locationClient: FusedLocationProviderClient) {
        fusedLocationClient = locationClient

        // Use the location client to get the current location
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
            location?.let {
                currentLocation = LatLng(it.latitude, it.longitude)
                notifyDataSetChanged() // Notify adapter that the current location is available
            }
        }
    }

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

    interface OnClickListener {
        fun onClick(position: Int, model: PlaceModel)
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvDist: TextView = itemView.findViewById(R.id.dist)
    }
}