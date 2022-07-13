package com.neoncoreng.bukkar.model


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "restaurants")
data class Restaurant(
    @SerializedName("is_shutdown")
    val isShutdown: Boolean,
    @ColumnInfo(name = "r_description")
    @SerializedName("r_description")
    val description: String,
    @PrimaryKey
    @ColumnInfo(name = "r_id")
    @SerializedName("r_id")
    val id: Int,
    @ColumnInfo(name = "r_title")
    @SerializedName("r_title")
    val title: String,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)