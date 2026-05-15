package com.example.kutirakushala.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kutirakushala.R
import com.example.kutirakushala.adapters.CompanyAdapter
import com.example.kutirakushala.models.Product
import com.example.kutirakushala.models.SellerWithProducts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var companyAdapter: CompanyAdapter
    private val companiesList = mutableListOf<SellerWithProducts>()
    
    private lateinit var tvWelcome: TextView
    private lateinit var rvCompanies: RecyclerView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        try {
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            
            tvWelcome = findViewById(R.id.tvWelcome)
            rvCompanies = findViewById(R.id.rvCompanies)
            btnLogout = findViewById(R.id.btnLogout)
            
            loadCustomerInfo()
            setupRecyclerView()
            loadUniqueCompanies()
            
            btnLogout.setOnClickListener {
                auth.signOut()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun loadCustomerInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "Customer"
                    tvWelcome.text = "Hello, $name!"
                }
                .addOnFailureListener {
                    tvWelcome.text = "Hello, Customer!"
                }
        } else {
            tvWelcome.text = "Hello, Customer!"
        }
    }

    private fun setupRecyclerView() {
        companyAdapter = CompanyAdapter(emptyList()) { company ->
            try {
                // Debug toast to check if company is clicked
                Toast.makeText(this, "Opening: ${company.businessName}", Toast.LENGTH_SHORT).show()
                
                val intent = Intent(this, CompanyProductsActivity::class.java)
                intent.putExtra("sellerId", company.sellerId)
                intent.putExtra("businessName", company.businessName)
                intent.putExtra("businessAddress", company.businessAddress)
                intent.putExtra("phone", company.phone)
                intent.putExtra("logoUrl", company.logoUrl)
                intent.putExtra("isAvailable", company.isAvailable)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        rvCompanies.layoutManager = LinearLayoutManager(this)
        rvCompanies.adapter = companyAdapter
    }

    private fun loadUniqueCompanies() {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { productsDoc ->
                if (productsDoc.isEmpty) {
                    Toast.makeText(this, "No products available", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                val sellerProductsMap = mutableMapOf<String, MutableList<Product>>()
                
                for (document in productsDoc) {
                    val product = document.toObject(Product::class.java)
                    val sellerId = product.sellerId
                    
                    if (!sellerProductsMap.containsKey(sellerId)) {
                        sellerProductsMap[sellerId] = mutableListOf()
                    }
                    sellerProductsMap[sellerId]?.add(product)
                }
                
                val uniqueSellerIds = sellerProductsMap.keys.toList()
                companiesList.clear()
                
                if (uniqueSellerIds.isEmpty()) {
                    Toast.makeText(this, "No companies found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                var pendingRequests = uniqueSellerIds.size
                
                for (sellerId in uniqueSellerIds) {
                    firestore.collection("users").document(sellerId).get()
                        .addOnSuccessListener { userDoc ->
                            val businessName = userDoc.getString("businessName") 
                                ?: userDoc.getString("name") 
                                ?: "Unknown Business"
                            val businessAddress = userDoc.getString("businessAddress") 
                                ?: "Address not available"
                            val phone = userDoc.getString("phone") ?: ""
                            val logoUrl = userDoc.getString("logoUrl") ?: ""
                            val isAvailable = userDoc.getBoolean("isAvailable") ?: true
                            val products = sellerProductsMap[sellerId] ?: emptyList()
                            
                            val seller = SellerWithProducts(
                                sellerId = sellerId,
                                businessName = businessName,
                                businessAddress = businessAddress,
                                phone = phone,
                                logoUrl = logoUrl,
                                isAvailable = isAvailable,
                                products = products
                            )
                            
                            companiesList.add(seller)
                            pendingRequests--
                            
                            if (pendingRequests == 0) {
                                val sortedCompanies = companiesList.sortedBy { it.businessName }
                                companyAdapter.updateCompanies(sortedCompanies)
                                
                                val totalProducts = sortedCompanies.sumOf { it.products.size }
                                Toast.makeText(
                                    this, 
                                    "${sortedCompanies.size} companies, $totalProducts products", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            pendingRequests--
                            if (pendingRequests == 0) {
                                val sortedCompanies = companiesList.sortedBy { it.businessName }
                                companyAdapter.updateCompanies(sortedCompanies)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadUniqueCompanies()
    }
}