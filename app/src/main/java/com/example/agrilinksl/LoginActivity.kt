package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvCreateAccount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)

        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvCreateAccount.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }

        tvForgotPassword.setOnClickListener {
            sendPasswordReset()
        }
    }

    private fun loginUser() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Please enter your email address"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email address"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Please enter your password"
            etPassword.requestFocus()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "LOGGING IN..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        getUserRole(userId)
                    } else {
                        resetLoginButton()
                    }

                } else {

                    resetLoginButton()

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun getUserRole(userId: String) {

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val role = document.getString("role") ?: ""

                    Toast.makeText(
                        this,
                        "Login successful - $role",
                        Toast.LENGTH_SHORT
                    ).show()

                    openDashboard(role)

                } else {

                    resetLoginButton()

                    Toast.makeText(
                        this,
                        "User profile not found",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->

                resetLoginButton()

                Toast.makeText(
                    this,
                    "Unable to load user profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun openDashboard(role: String) {

        val intent = when (role) {

            "Farmer" -> {
                Intent(this, FarmerHomeActivity::class.java)
            }

            "Buyer" -> {
                Intent(this, BuyerHomeActivity::class.java)
            }

            "Transport Provider" -> {
                Intent(this, TransportDashboardActivity::class.java)
            }

            else -> {
                Toast.makeText(
                    this,
                    "Unknown user role",
                    Toast.LENGTH_LONG
                ).show()

                auth.signOut()
                resetLoginButton()
                return
            }
        }

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun sendPasswordReset() {

        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {

            etEmail.error =
                "Enter your email address first"

            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            etEmail.error =
                "Please enter a valid email address"

            etEmail.requestFocus()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Password reset email sent",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    exception.message
                        ?: "Unable to send reset email",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun resetLoginButton() {
        btnLogin.isEnabled = true
        btnLogin.text = "LOGIN"
    }
}