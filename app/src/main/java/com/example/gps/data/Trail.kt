package com.example.gps.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Trail(
    val id: String = "",
    val name: String,
    val location: String,
    val difficulty: String,
    val imageUrl: String,
    var isFavorite: Boolean = false
) : Parcelable