package com.example.kutirakushala.models

import android.os.Parcel
import android.os.Parcelable

data class Product(
    val productId: String = "",
    val sellerId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(productId)
        parcel.writeString(sellerId)
        parcel.writeString(productName)
        parcel.writeDouble(price)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeString(category)
        parcel.writeLong(timestamp)
    }
    
    override fun describeContents(): Int = 0
    
    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product {
            return Product(parcel)
        }
        
        override fun newArray(size: Int): Array<Product?> {
            return arrayOfNulls(size)
        }
    }
}