package com.trx.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.trx.models.PlaceModel

@Database([PlaceModel::class], version = 1)
abstract class PlacesDatabase : RoomDatabase(){

    companion object {
        private var instance: PlacesDatabase? = null


        fun getInstance(context: Context): PlacesDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlacesDatabase::class.java,
                    "PlaceDB"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance!!
        }
    }

    abstract fun contactDao() : PlacesDao

}