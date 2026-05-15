package com.example.kutirakushala.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.kutirakushala.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.util.UUID

class BusinessProfileActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var storageRef: StorageReference
    
    private lateinit var ivBusinessLogo: ImageView
    private lateinit var etBusinessName: EditText
    private lateinit var etBusinessAddress: EditText
    private lateinit var etBusinessPhone: EditText
    private lateinit var etBusinessEmail: EditText
    private lateinit var etBusinessDescription: EditText
    private lateinit var btnSelectLogo: Button
    private lateinit var btnSaveProfile: Button
    private lateinit var progressBar: ProgressBar
    
    private var selectedImageUri: Uri? = null
    private var businessId: String = ""
    private var isSaving = false

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_business_profile)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        storageRef = storage.reference
        
        businessId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        initViews()
        setupClickListeners()
        loadExistingProfile()
    }

    private fun initViews() {
        ivBusinessLogo = findViewById(R.id.ivBusinessLogo)
        etBusinessName = findViewById(R.id.etBusinessName)
        etBusinessAddress = findViewById(R.id.etBusinessAddress)
        etBusinessPhone = findViewById(R.id.etBusinessPhone)
        etBusinessEmail = findViewById(R.id.etBusinessEmail)
        etBusinessDescription = findViewById(R.id.etBusinessDescription)
        btnSelectLogo = findViewById(R.id.btnSelectLogo)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnSelectLogo.setOnClickListener {
            checkStoragePermission()
        }

        btnSaveProfile.setOnClickListener {
            if (!isSaving) {
                saveBusinessProfile()
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                openImagePicker()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                openImagePicker()
            }
        } else {
            openImagePicker()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            try {
                Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_business_placeholder)
                    .into(ivBusinessLogo)
                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadExistingProfile() {
        firestore.collection("businessProfiles").document(businessId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etBusinessName.setText(document.getString("businessName") ?: "")
                    etBusinessAddress.setText(document.getString("businessAddress") ?: "")
                    etBusinessPhone.setText(document.getString("phone") ?: "")
                    etBusinessEmail.setText(document.getString("email") ?: "")
                    etBusinessDescription.setText(document.getString("description") ?: "")
                    
                    val logoUrl = document.getString("logoUrl")
                    if (!logoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(logoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_business_placeholder)
                            .into(ivBusinessLogo)
                    }
                }
            }
    }

    private fun saveBusinessProfile() {
        val businessName = etBusinessName.text.toString().trim()
        val businessAddress = etBusinessAddress.text.toString().trim()
        val phone = etBusinessPhone.text.toString().trim()
        val email = etBusinessEmail.text.toString().trim()
        val description = etBusinessDescription.text.toString().trim()

        if (businessName.isEmpty()) {
            etBusinessName.error = "Business name required"
            return
        }
        if (businessAddress.isEmpty()) {
            etBusinessAddress.error = "Address required"
            return
        }
        if (phone.isEmpty()) {
            etBusinessPhone.error = "Phone required"
            return
        }

        isSaving = true
        showProgress(true)

        // Save to Firestore first
        val profileData = mutableMapOf<String, Any>()
        profileData["businessId"] = businessId
        profileData["businessName"] = businessName
        profileData["businessAddress"] = businessAddress
        profileData["phone"] = phone
        profileData["email"] = email
        profileData["description"] = description
        profileData["isProfileComplete"] = true
        profileData["timestamp"] = System.currentTimeMillis()

        firestore.collection("businessProfiles").document(businessId).set(profileData)
            .addOnSuccessListener {
                updateUserDocument(businessName, phone, email, businessAddress)
                
                if (selectedImageUri != null) {
                    uploadImageToFirebase()
                } else {
                    goToSellerHome()
                }
            }
            .addOnFailureListener { e ->
                isSaving = false
                showProgress(false)
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun uploadImageToFirebase() {
        try {
            val fileName = "business_logos/${businessId}_${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child(fileName)
            
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
            
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
            val data = byteArrayOutputStream.toByteArray()
            
            inputStream?.close()
            byteArrayOutputStream.close()
            
            imageRef.putBytes(data)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        firestore.collection("businessProfiles").document(businessId)
                            .update("logoUrl", uri.toString())
                            .addOnSuccessListener {
                                goToSellerHome()
                            }
                            .addOnFailureListener {
                                goToSellerHome()
                            }
                    }
                    .addOnFailureListener {
                        goToSellerHome()
                    }
                }
                .addOnFailureListener {
                    goToSellerHome()
                }
        } catch (e: Exception) {
            goToSellerHome()
        }
    }

    private fun updateUserDocument(businessName: String, phone: String, email: String, address: String) {
        val updates = mutableMapOf<String, Any>()
        updates["businessName"] = businessName
        updates["phone"] = phone
        updates["email"] = email
        updates["businessAddress"] = address
        updates["isProfileComplete"] = true
        
        firestore.collection("users").document(businessId).update(updates)
    }

    private fun goToSellerHome() {
        isSaving = false
        showProgress(false)
        Toast.makeText(this, "Profile saved! Loading seller page...", Toast.LENGTH_LONG).show()
        
        val intent = Intent(this, SellerHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSaveProfile.isEnabled = !show
        btnSelectLogo.isEnabled = !show
    }
}