package com.trx.miscellaneous

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.trx.database.PlacesDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val isNetworkAvailable =  isNetworkAvailable(context)
            if (isNetworkAvailable) {
                // Internet connection is available, trigger data synchronization
                val localDatabase = PlacesDatabase.getInstance(context)
                val dao = localDatabase.contactDao()

                // Observe the LiveData
                dao.getPlaces().observe(context as LifecycleOwner, Observer { places ->
                    if (places.isNotEmpty()) {
                        // Upload unsynchronized data to Firebase
                        val database = FirebaseDatabase.getInstance()
                        val myRef = database.getReference("places")
                        for (placesData in places) {
                            val newRef = myRef.push()
                            newRef.setValue(placesData)

                            // Delete synchronized records from the local database
                            context.lifecycleScope.launch {
                                dao.deletePlace(placesData)
                            }
                        }
                    }
                })

            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

}

