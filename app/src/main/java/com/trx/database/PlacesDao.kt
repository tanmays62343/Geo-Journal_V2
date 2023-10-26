package com.trx.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.trx.models.PlaceModel

@Dao
interface PlacesDao {

    @Insert
    fun insertPlace(place : PlaceModel)

    @Update
    fun updatePlace(place : PlaceModel)

    @Delete
    fun deletePlace(place: PlaceModel)

    @Query("SELECT * FROM Places")
    fun getPlaces() : ArrayList<PlaceModel>
}