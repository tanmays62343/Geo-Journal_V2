package com.trx.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "Places")
data class PlaceModel(

    @PrimaryKey(autoGenerate = true)
    val id: Int,

    val title : String,
    val category : String,
    val date : String,
    val address : String,
    val latitude : Double,
    val longitude : Double

) : Serializable
