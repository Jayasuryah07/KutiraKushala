package com.example.kutirakushala.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kutirakushala.R
import com.example.kutirakushala.adapters.ProductAdapter
import com.example.kutirakushala.models.Product
import com.example.kutirakushala.models.SellerWithProducts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class SellerHomeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: ProductAdapter
    private val productsList = mutableListOf<Product>()
    private var sellersWithProductsList = mutableListOf<SellerWithProducts>()

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var ivBusinessLogo: CircleImageView
    private lateinit var tvBusinessName: TextView
    private lateinit var tvBusinessAddress: TextView
    private lateinit var tvBusinessPhone: TextView
    private lateinit var tvBusinessDescription: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var rvProducts: RecyclerView
    private lateinit var switchAvailability: SwitchCompat
    private lateinit var btnAddProduct: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_home)

        try {
            Toast.makeText(this, "Opening Seller Dashboard...", Toast.LENGTH_SHORT).show()

            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()

            if (auth.currentUser == null) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            initializeViews()
            setupToolbar()
            setupRecyclerView()
            loadBusinessProfile()
            loadSellerProducts()
            loadSellerStatus()
            setupClickListeners()

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        ivBusinessLogo = findViewById(R.id.ivBusinessLogo)
        tvBusinessName = findViewById(R.id.tvBusinessName)
        tvBusinessAddress = findViewById(R.id.tvBusinessAddress)
        tvBusinessPhone = findViewById(R.id.tvBusinessPhone)
        tvBusinessDescription = findViewById(R.id.tvBusinessDescription)
        tvWelcome = findViewById(R.id.tvWelcome)
        rvProducts = findViewById(R.id.rvProducts)
        switchAvailability = findViewById(R.id.switchAvailability)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Seller Dashboard"
        } catch (e: Exception) {
            // Toolbar setup failed, continue
        }
    }

    private fun setupClickListeners() {
        btnAddProduct.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, BusinessProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            toggleAvailability(isChecked)
        }
    }

    private fun loadBusinessProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("businessProfiles").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val businessName = document.getString("businessName") ?: "My Business"
                    val businessAddress = document.getString("businessAddress") ?: "Address not set"
                    val phone = document.getString("phone") ?: "Phone not set"
                    val description = document.getString("description") ?: "No description"
                    val logoUrl = document.getString("logoUrl") ?: ""

                    tvBusinessName.text = businessName
                    tvBusinessAddress.text = "📍 $businessAddress"
                    tvBusinessPhone.text = "📞 $phone"
                    tvBusinessDescription.text = description
                    tvWelcome.text = "Welcome, $businessName!"

                    if (logoUrl.isNotEmpty()) {
                        try {
                            Glide.with(this)
                                .load(logoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_business_placeholder)
                                .error(R.drawable.ic_business_placeholder)
                                .into(ivBusinessLogo)
                        } catch (e: Exception) {
                            ivBusinessLogo.setImageResource(R.drawable.ic_business_placeholder)
                        }
                    }
                } else {
                    tvBusinessName.text = "Complete Your Profile"
                    tvBusinessAddress.text = "📍 Click Edit to add address"
                    tvBusinessPhone.text = "📞 Click Edit to add phone"
                    tvBusinessDescription.text = "Add your business description"
                    tvWelcome.text = "Welcome, Seller!"
                    Toast.makeText(this, "Please complete your business profile", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                tvWelcome.text = "Welcome, Seller!"
            }
    }

    private fun loadSellerStatus() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val isAvailable = document.getBoolean("isAvailable") ?: true
                switchAvailability.isChecked = isAvailable
                
                val status = if (isAvailable) "ON" else "OFF"
                Toast.makeText(this, "Current availability: $status", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                switchAvailability.isChecked = true
            }
    }

    private fun toggleAvailability(isAvailable: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("isAvailable", isAvailable)
            .addOnSuccessListener {
                val status = if (isAvailable) "ON" else "OFF"
                Toast.makeText(this, "Availability turned $status", Toast.LENGTH_SHORT).show()
                // Refresh products to update availability status
                loadSellerProducts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update availability", Toast.LENGTH_SHORT).show()
                switchAvailability.isChecked = !isAvailable
            }
    }

    private fun setupRecyclerView() {
        // Initialize with empty list
        adapter = ProductAdapter(emptyList()) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("productId", product.productId)
            startActivity(intent)
        }
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter
    }

    private fun loadSellerProducts() {
        val sellerId = auth.currentUser?.uid ?: return

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
                // Load seller info and update adapter
                loadSellerInfoAndUpdateAdapter(sellerId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSellerInfoAndUpdateAdapter(sellerId: String) {
        firestore.collection("users").document(sellerId).get()
            .addOnSuccessListener { document ->
                val businessName = document.getString("businessName") ?: document.getString("name") ?: "My Business"
                val businessAddress = document.getString("businessAddress") ?: "Address not set"
                val phone = document.getString("phone") ?: ""
                val logoUrl = document.getString("logoUrl") ?: ""
                val isAvailable = document.getBoolean("isAvailable") ?: true
                
                val sellerWithProducts = SellerWithProducts(
                    sellerId = sellerId,
                    businessName = businessName,
                    businessAddress = businessAddress,
                    phone = phone,
                    logoUrl = logoUrl,
                    isAvailable = isAvailable,
                    products = productsList.toList()
                )
                
                sellersWithProductsList.clear()
                sellersWithProductsList.add(sellerWithProducts)
                
                // Create new adapter with updated data
                val newAdapter = ProductAdapter(sellersWithProductsList.toList()) { product ->
                    val intent = Intent(this, ProductDetailActivity::class.java)
                    intent.putExtra("productId", product.productId)
                    startActivity(intent)
                }
                rvProducts.adapter = newAdapter
                
                if (productsList.isEmpty()) {
                    Toast.makeText(this, "No products yet. Add your first product!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading seller info: ${e.message}", Toast.LENGTH_SHORT).show()
                // Fallback - show products without seller card
                val sellerWithProducts = SellerWithProducts(
                    sellerId = sellerId,
                    businessName = "My Business",
                    businessAddress = "Address not set",
                    phone = "",
                    logoUrl = "",
                    isAvailable = true,
                    products = productsList.toList()
                )
                sellersWithProductsList.clear()
                sellersWithProductsList.add(sellerWithProducts)
                
                val newAdapter = ProductAdapter(sellersWithProductsList.toList()) { product ->
                    val intent = Intent(this, ProductDetailActivity::class.java)
                    intent.putExtra("productId", product.productId)
                    startActivity(intent)
                }
                rvProducts.adapter = newAdapter
            }
    }

    override fun onResume() {
        super.onResume()
        loadBusinessProfile()
        loadSellerProducts()
        loadSellerStatus()
    }
}