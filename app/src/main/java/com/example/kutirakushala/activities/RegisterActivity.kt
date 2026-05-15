package com.example.kutirakushala.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kutirakushala.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbIsSeller: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        cbIsSeller = findViewById(R.id.cbIsSeller)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val isSeller = cbIsSeller.isChecked

            if (name.isEmpty()) {
                etName.error = "Name required"
                etName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Email required"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                etPhone.error = "Phone number required"
                etPhone.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password required"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Creating account..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user!!.uid
                    saveUserToFirestore(userId, name, email, phone, isSeller)
                }
                .addOnFailureListener { e ->
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"
                    Toast.makeText(this, "Registration Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun saveUserToFirestore(userId: String, name: String, email: String, phone: String, isSeller: Boolean) {
        val user = hashMapOf(
            "userId" to userId,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "seller" to isSeller,
            "isSeller" to isSeller,
            "isProfileComplete" to !isSeller, // Customers don't need business profile
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                
                if (isSeller) {
                    // Check if business profile is needed
                    checkBusinessProfile(userId)
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener { e ->
                btnRegister.isEnabled = true
                btnRegister.text = "Register"
                Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkBusinessProfile(userId: String) {
        firestore.collection("businessProfiles").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getBoolean("isProfileComplete") == true) {
                    // Profile already exists, go to Seller Home
                    startActivity(Intent(this, SellerHomeActivity::class.java))
                } else {
                    // First time seller - show business profile setup
                    startActivity(Intent(this, BusinessProfileActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                // Error checking profile, show setup anyway
                startActivity(Intent(this, BusinessProfileActivity::class.java))
                finish()
            }
    }
}