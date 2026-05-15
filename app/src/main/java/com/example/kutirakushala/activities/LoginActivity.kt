package com.example.kutirakushala.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kutirakushala.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)

        // Check if already logged in
        if (auth.currentUser != null) {
            checkUserTypeAndNavigate(auth.currentUser!!.uid)
            return
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    checkUserTypeAndNavigate(it.user!!.uid)
                }
                .addOnFailureListener { e ->
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkUserTypeAndNavigate(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
                
                val isSeller = when {
                    document.contains("seller") -> document.getBoolean("seller") ?: false
                    document.contains("isSeller") -> document.getBoolean("isSeller") ?: false
                    else -> false
                }
                
                Toast.makeText(this, "User type: ${if(isSeller) "SELLER" else "CUSTOMER"}", Toast.LENGTH_SHORT).show()
                
                if (isSeller) {
                    checkBusinessProfile(userId)
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener { e ->
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
                Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkBusinessProfile(userId: String) {
        firestore.collection("businessProfiles").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getBoolean("isProfileComplete") == true) {
                    startActivity(Intent(this, SellerHomeActivity::class.java))
                } else {
                    startActivity(Intent(this, BusinessProfileActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, BusinessProfileActivity::class.java))
                finish()
            }
    }
}