package com.example.kutirakushala.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kutirakushala.R
import com.example.kutirakushala.models.Product
import com.google.firebase.firestore.FirebaseFirestore

class CompanyProductsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    
    // Company data
    private var sellerId: String = ""
    private var businessName: String = ""
    private var businessAddress: String = ""
    private var phone: String = ""
    private var logoUrl: String = ""
    private var isAvailable: Boolean = true
    
    // Views
    private lateinit var ivCompanyLogo: ImageView
    private lateinit var tvCompanyName: TextView
    private lateinit var tvCompanyAddress: TextView
    private lateinit var tvCompanyPhone: TextView
    private lateinit var tvAvailabilityStatus: TextView
    private lateinit var rvProducts: RecyclerView
    private lateinit var btnBack: Button
    
    private var productsList = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_products)

        try {
            firestore = FirebaseFirestore.getInstance()
            
            // Get company data from intent - Using individual fields
            sellerId = intent.getStringExtra("sellerId") ?: ""
            businessName = intent.getStringExtra("businessName") ?: ""
            businessAddress = intent.getStringExtra("businessAddress") ?: ""
            phone = intent.getStringExtra("phone") ?: ""
            logoUrl = intent.getStringExtra("logoUrl") ?: ""
            isAvailable = intent.getBooleanExtra("isAvailable", true)
            
            // Check if data was received
            if (sellerId.isEmpty()) {
                Toast.makeText(this, "Company data not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            Toast.makeText(this, "Loading products for: $businessName", Toast.LENGTH_SHORT).show()
            
            initializeViews()
            setupUI()
            setupRecyclerView()
            loadProducts()
            
            btnBack.setOnClickListener {
                finish()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        ivCompanyLogo = findViewById(R.id.ivCompanyLogo)
        tvCompanyName = findViewById(R.id.tvCompanyName)
        tvCompanyAddress = findViewById(R.id.tvCompanyAddress)
        tvCompanyPhone = findViewById(R.id.tvCompanyPhone)
        tvAvailabilityStatus = findViewById(R.id.tvAvailabilityStatus)
        rvProducts = findViewById(R.id.rvProducts)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupUI() {
        tvCompanyName.text = businessName
        tvCompanyAddress.text = "📍 $businessAddress"
        tvCompanyPhone.text = "📞 $phone"
        
        if (isAvailable) {
            tvAvailabilityStatus.text = "● Accepting Orders"
            tvAvailabilityStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvAvailabilityStatus.text = "● Not Accepting Orders"
            tvAvailabilityStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
        
        if (logoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(logoUrl)
                .placeholder(R.drawable.ic_business_placeholder)
                .into(ivCompanyLogo)
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(productsList, isAvailable, phone) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("productId", product.productId)
            startActivity(intent)
        }
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = productAdapter
    }

    private fun loadProducts() {
        firestore.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { documents ->
                productsList.clear()
                for (document in documents) {
                    val product = document.toObject(Product::class.java)
                    if (product != null) {
                        productsList.add(product)
                    }
                }
                productAdapter.updateProducts(productsList)
                
                if (productsList.isEmpty()) {
                    Toast.makeText(this, "No products available", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "${productsList.size} products found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    // Inner ProductAdapter class
    inner class ProductAdapter(
        private var products: List<Product>,
        private val isSellerAvailable: Boolean,
        private val sellerPhone: String,
        private val onProductClick: (Product) -> Unit
    ) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

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
}