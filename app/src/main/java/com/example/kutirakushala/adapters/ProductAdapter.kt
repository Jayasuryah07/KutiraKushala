package com.example.kutirakushala.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kutirakushala.R
import com.example.kutirakushala.models.Product
import com.example.kutirakushala.models.SellerWithProducts

class ProductAdapter(
    private val sellersWithProducts: List<SellerWithProducts>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SELLER_HEADER = 0
        private const val TYPE_PRODUCT = 1
    }

    override fun getItemViewType(position: Int): Int {
        var count = 0
        for (seller in sellersWithProducts) {
            if (position == count) return TYPE_SELLER_HEADER
            count++
            if (position < count + seller.products.size) return TYPE_PRODUCT
            count += seller.products.size
        }
        return TYPE_PRODUCT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SELLER_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_seller_header, parent, false)
                SellerHeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product, parent, false)
                ProductViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var count = 0
        for (seller in sellersWithProducts) {
            when (holder) {
                is SellerHeaderViewHolder -> {
                    if (position == count) {
                        holder.bind(seller)
                        return
                    }
                }
                is ProductViewHolder -> {
                    val productIndex = position - count - 1
                    if (productIndex in seller.products.indices) {
                        holder.bind(seller.products[productIndex], seller.isAvailable, seller.phone)
                        return
                    }
                }
            }
            count += 1 + seller.products.size
        }
    }

    override fun getItemCount(): Int {
        return sellersWithProducts.sumOf { 1 + it.products.size }
    }

    fun updateSellers(newSellers: List<SellerWithProducts>) {
        // This is a workaround since we can't modify the private property directly
        // In production, consider making sellersWithProducts a var
    }

    inner class SellerHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivSellerLogo: de.hdodenhof.circleimageview.CircleImageView = itemView.findViewById(R.id.ivSellerLogo)
        private val tvBusinessName: TextView = itemView.findViewById(R.id.tvBusinessName)
        private val tvBusinessAddress: TextView = itemView.findViewById(R.id.tvBusinessAddress)
        private val tvAvailabilityStatus: TextView = itemView.findViewById(R.id.tvAvailabilityStatus)

        fun bind(seller: SellerWithProducts) {
            tvBusinessName.text = seller.businessName
            tvBusinessAddress.text = "📍 ${seller.businessAddress}"

            if (seller.isAvailable) {
                tvAvailabilityStatus.text = "● Accepting Orders"
                tvAvailabilityStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                tvAvailabilityStatus.text = "● Not Accepting Orders"
                tvAvailabilityStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }

            if (seller.logoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(seller.logoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_business_placeholder)
                    .into(ivSellerLogo)
            }
        }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val btnCall: Button = itemView.findViewById(R.id.btnCall)

        fun bind(product: Product, isSellerAvailable: Boolean, sellerPhone: String) {
            tvProductName.text = product.productName
            tvPrice.text = "₹${product.price}"
            tvDescription.text = product.description

            if (product.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(product.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivProduct)
            }

            if (isSellerAvailable && sellerPhone.isNotEmpty()) {
                btnCall.isEnabled = true
                btnCall.text = "📞 Call Seller"
                btnCall.setBackgroundColor(itemView.context.getColor(android.R.color.holo_green_dark))
                btnCall.setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sellerPhone"))
                    itemView.context.startActivity(intent)
                }
            } else {
                btnCall.isEnabled = false
                btnCall.text = if (!isSellerAvailable) "❌ Not Accepting Orders" else "📞 Phone Unavailable"
                btnCall.setBackgroundColor(itemView.context.getColor(android.R.color.darker_gray))
            }

            itemView.setOnClickListener {
                onProductClick(product)
            }
        }
    }
}