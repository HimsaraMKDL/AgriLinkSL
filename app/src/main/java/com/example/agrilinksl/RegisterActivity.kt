package com.example.agrilinksl

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var cardFarmer: MaterialCardView
    private lateinit var cardBuyer: MaterialCardView
    private lateinit var cardTransport: MaterialCardView

    private lateinit var tvFarmer: TextView
    private lateinit var tvBuyer: TextView
    private lateinit var tvTransport: TextView

    private lateinit var etFullName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnCreateAccount: MaterialButton
    private lateinit var tvLogin: TextView
    private lateinit var tvBack: TextView

    private var selectedRole = "Farmer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews()
        setupRoleSelection()
        setupClickListeners()

        selectRole("Farmer")
    }

    private fun initializeViews() {

        cardFarmer = findViewById(R.id.cardFarmer)
        cardBuyer = findViewById(R.id.cardBuyer)
        cardTransport = findViewById(R.id.cardTransport)

        tvFarmer = findViewById(R.id.tvFarmer)
        tvBuyer = findViewById(R.id.tvBuyer)
        tvTransport = findViewById(R.id.tvTransport)

        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvLogin = findViewById(R.id.tvLogin)
        tvBack = findViewById(R.id.tvBack)
    }

    private fun setupRoleSelection() {

        cardFarmer.setOnClickListener {
            selectRole("Farmer")
        }

        cardBuyer.setOnClickListener {
            selectRole("Buyer")
        }

        cardTransport.setOnClickListener {
            selectRole("Transport Provider")
        }
    }

    private fun selectRole(role: String) {

        selectedRole = role

        resetRoleCards()

        when (role) {

            "Farmer" -> {
                setSelectedCard(cardFarmer, tvFarmer)
            }

            "Buyer" -> {
                setSelectedCard(cardBuyer, tvBuyer)
            }

            "Transport Provider" -> {
                setSelectedCard(cardTransport, tvTransport)
            }
        }
    }

    private fun resetRoleCards() {

        val normalBackground = Color.parseColor("#FFFFFF")
        val normalStroke = Color.parseColor("#D1D5DB")
        val normalText = Color.parseColor("#6B7280")

        cardFarmer.setCardBackgroundColor(normalBackground)
        cardBuyer.setCardBackgroundColor(normalBackground)
        cardTransport.setCardBackgroundColor(normalBackground)

        cardFarmer.strokeColor = normalStroke
        cardBuyer.strokeColor = normalStroke
        cardTransport.strokeColor = normalStroke

        cardFarmer.strokeWidth = 1
        cardBuyer.strokeWidth = 1
        cardTransport.strokeWidth = 1

        tvFarmer.setTextColor(normalText)
        tvBuyer.setTextColor(normalText)
        tvTransport.setTextColor(normalText)
    }

    private fun setSelectedCard(
        card: MaterialCardView,
        textView: TextView
    ) {

        card.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        card.strokeColor = Color.parseColor("#2E7D32")
        card.strokeWidth = 2

        textView.setTextColor(Color.parseColor("#2E7D32"))
    }

    private fun setupClickListeners() {

        btnCreateAccount.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }

        tvBack.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {

        val fullName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (fullName.isEmpty()) {
            etFullName.error = "Please enter your full name"
            etFullName.requestFocus()
            return
        }

        if (phone.isEmpty()) {
            etPhone.error = "Please enter your phone number"
            etPhone.requestFocus()
            return
        }

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
            etPassword.error = "Please enter a password"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must contain at least 6 characters"
            etPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Please confirm your password"
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }

        btnCreateAccount.isEnabled = false
        btnCreateAccount.text = "CREATING ACCOUNT..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val userId = auth.currentUser?.uid

                    if (userId != null) {

                        saveUserToFirestore(
                            userId = userId,
                            fullName = fullName,
                            phone = phone,
                            email = email
                        )

                    } else {
                        resetButton()

                        Toast.makeText(
                            this,
                            "Unable to get user ID",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    resetButton()

                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore(
        userId: String,
        fullName: String,
        phone: String,
        email: String
    ) {

        val user = hashMapOf<String, Any?>(
            "name" to fullName,
            "email" to email,
            "phone" to phone,
            "role" to selectedRole,
            "address" to "",
            "latitude" to null,
            "longitude" to null,
            "profileImage" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                auth.signOut()

                val intent = Intent(
                    this,
                    LoginActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->

                resetButton()

                Toast.makeText(
                    this,
                    "Account created, but profile could not be saved: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun resetButton() {
        btnCreateAccount.isEnabled = true
        btnCreateAccount.text = "CREATE ACCOUNT"
    }
}