package com.example.kutirakushala.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.kutirakushala.R
import com.example.kutirakushala.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddProductActivity : AppCompatActivity() {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    
    // Views
    private lateinit var ivProductImage: ImageView
    private lateinit var etProductName: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDescription: EditText
    private lateinit var etCategory: EditText
    private lateinit var btnSelectImage: Button
    private lateinit var btnUploadProduct: Button

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            ivProductImage.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        
        // Initialize Views
        ivProductImage = findViewById(R.id.ivProductImage)
        etProductName = findViewById(R.id.etProductName)
        etPrice = findViewById(R.id.etPrice)
        etDescription = findViewById(R.id.etDescription)
        etCategory = findViewById(R.id.etCategory)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnUploadProduct = findViewById(R.id.btnUploadProduct)

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        btnUploadProduct.setOnClickListener {
            uploadProduct()
        }
    }

    private fun uploadProduct() {
        val productName = etProductName.text.toString().trim()
        val priceText = etPrice.text.toString().trim()
        val price = priceText.toDoubleOrNull()
        val description = etDescription.text.toString().trim()
        val category = etCategory.text.toString().trim()

        if (productName.isEmpty()) {
            Toast.makeText(this, "Please enter product name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (price == null) {
            Toast.makeText(this, "Please enter valid price", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (category.isEmpty()) {
            Toast.makeText(this, "Please enter category", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Upload image to Firebase Storage
        val imageRef = storage.reference.child("product_images/${UUID.randomUUID()}.jpg")
        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveProductToFirestore(
                        productName, price, description, category, uri.toString()
                    )
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProductToFirestore(
        productName: String,
        price: Double,
        description: String,
        category: String,
        imageUrl: String
    ) {
        val sellerId = auth.currentUser?.uid
        if (sellerId == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }
        
        val product = Product(
            productId = UUID.randomUUID().toString(),
            sellerId = sellerId,
            productName = productName,
            price = price,
            description = description,
            imageUrl = imageUrl,
            category = category,
            timestamp = System.currentTimeMillis()
        )

        firestore.collection("products").document(product.productId).set(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Product uploaded successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}