package com.example.kutirakushala.models

import android.os.Parcel
import android.os.Parcelable

data class SellerWithProducts(
    val sellerId: String = "",
    val businessName: String = "",
    val businessAddress: String = "",
    val phone: String = "",
    val logoUrl: String = "",
    val isAvailable: Boolean = true,
    val products: List<Product> = emptyList()
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.createTypedArrayList(Product.CREATOR) ?: emptyList()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(sellerId)
        parcel.writeString(businessName)
        parcel.writeString(businessAddress)
        parcel.writeString(phone)
        parcel.writeString(logoUrl)
        parcel.writeByte(if (isAvailable) 1 else 0)
        parcel.writeTypedList(products)
    }
    
    override fun describeContents(): Int = 0
    
    companion object CREATOR : Parcelable.Creator<SellerWithProducts> {
        override fun createFromParcel(parcel: Parcel): SellerWithProducts {
            return SellerWithProducts(parcel)
        }
        
        override fun newArray(size: Int): Array<SellerWithProducts?> {
            return arrayOfNulls(size)
        }
    }
}