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

class ProductListAdapter(
    private var products: List<Product>,
    private val isSellerAvailable: Boolean,
    private val sellerPhone: String,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductListAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val btnCall: Button = itemView.findViewById(R.id.btnCall)

        fun bind(product: Product) {
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