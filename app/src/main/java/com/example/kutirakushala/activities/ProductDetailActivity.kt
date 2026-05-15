package com.example.kutirakushala.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.kutirakushala.R
import com.example.kutirakushala.models.Product
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailActivity : AppCompatActivity() {
    
    private lateinit var firestore: FirebaseFirestore
    private var sellerPhone: String = ""
    
    // Views
    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvSellerName: TextView
    private lateinit var btnCallSeller: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        firestore = FirebaseFirestore.getInstance()
        
        // Initialize Views
        ivProductImage = findViewById(R.id.ivProductImage)
        tvProductName = findViewById(R.id.tvProductName)
        tvPrice = findViewById(R.id.tvPrice)
        tvDescription = findViewById(R.id.tvDescription)
        tvCategory = findViewById(R.id.tvCategory)
        tvSellerName = findViewById(R.id.tvSellerName)
        btnCallSeller = findViewById(R.id.btnCallSeller)
        
        val productId = intent.getStringExtra("productId")
        if (productId != null) {
            loadProductDetails(productId)
        } else {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnCallSeller.setOnClickListener {
            if (sellerPhone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$sellerPhone")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Seller phone not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProductDetails(productId: String) {
        firestore.collection("products").document(productId).get()
            .addOnSuccessListener { productDoc ->
                val product = productDoc.toObject(Product::class.java)
                if (product != null) {
                    tvProductName.text = product.productName
                    tvPrice.text = "₹${product.price}"
                    tvDescription.text = product.description
                    tvCategory.text = product.category

                    Glide.with(this)
                        .load(product.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivProductImage)

                    // Load seller info
                    firestore.collection("users").document(product.sellerId).get()
                        .addOnSuccessListener { sellerDoc ->
                            val businessName = sellerDoc.getString("businessName")
                            val name = sellerDoc.getString("name")
                            sellerPhone = sellerDoc.getString("phone") ?: ""
                            
                            tvSellerName.text = if (!businessName.isNullOrEmpty()) {
                                "Seller: $businessName"
                            } else {
                                "Seller: $name"
                            }
                        }
                        .addOnFailureListener {
                            tvSellerName.text = "Seller: Unknown"
                        }
                } else {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load product: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}