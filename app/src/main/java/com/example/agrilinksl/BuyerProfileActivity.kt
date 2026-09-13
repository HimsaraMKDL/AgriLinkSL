package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BuyerProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvContact: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvCompletedOrders: TextView

    private var currentName = ""
    private var currentPhone = ""
    private var currentAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_buyer_profile
        )

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        initializeViews()

        setupCards()
        setupBottomNavigation()
        setupButtons()

        loadBuyerProfile()
        loadOrderStatistics()
    }

    override fun onResume() {
        super.onResume()

        loadBuyerProfile()
        loadOrderStatistics()
    }

    private fun initializeViews() {

        tvName =
            findViewById(
                R.id.tvBuyerProfileName
            )

        tvEmail =
            findViewById(
                R.id.tvBuyerProfileEmail
            )

        tvContact =
            findViewById(
                R.id.tvBuyerProfileContact
            )

        tvAddress =
            findViewById(
                R.id.tvBuyerProfileAddress
            )

        tvTotalOrders =
            findViewById(
                R.id.tvBuyerProfileTotalOrders
            )

        tvCompletedOrders =
            findViewById(
                R.id.tvBuyerProfileCompletedOrders
            )
    }

    private fun loadBuyerProfile() {

        val user =
            auth.currentUser

        if (user == null) {

            openLogin()
            return
        }

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                currentName =
                    document.getString("name")
                        ?: "Buyer"

                currentPhone =
                    document.getString("phone")
                        ?: ""

                currentAddress =
                    document.getString("address")
                        ?: ""

                val email =
                    document.getString("email")
                        ?: user.email
                        ?: ""

                tvName.text =
                    currentName

                tvEmail.text =
                    email.ifBlank {
                        "Email not available"
                    }

                tvAddress.text =
                    currentAddress.ifBlank {
                        "Address not set"
                    }

                tvContact.text =
                    buildContactText(
                        currentAddress,
                        currentPhone
                    )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadOrderStatistics() {

        val uid =
            auth.currentUser?.uid
                ?: return

        firestore.collection("orders")
            .whereEqualTo(
                "buyerId",
                uid
            )
            .get()
            .addOnSuccessListener { snapshot ->

                val total =
                    snapshot.size()

                var completed =
                    0

                for (
                document
                in snapshot.documents
                ) {

                    val status =
                        document.getString(
                            "orderStatus"
                        ) ?: ""

                    if (
                        status.equals(
                            "Delivered",
                            ignoreCase = true
                        )
                    ) {

                        completed++
                    }
                }

                tvTotalOrders.text =
                    total.toString()

                tvCompletedOrders.text =
                    completed.toString()
            }
            .addOnFailureListener {

                tvTotalOrders.text =
                    "0"

                tvCompletedOrders.text =
                    "0"
            }
    }

    private fun setupCards() {

        findViewById<MaterialCardView>(
            R.id.cardBuyerProfileOrders
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BuyerOrdersActivity::class.java
                )
            )
        }

        findViewById<MaterialCardView>(
            R.id.cardBuyerProfileMarketplace
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BuyerMarketplaceActivity::class.java
                )
            )
        }
    }

    private fun setupButtons() {

        findViewById<MaterialButton>(
            R.id.btnBuyerEditProfile
        ).setOnClickListener {

            showEditProfileDialog()
        }

        findViewById<MaterialButton>(
            R.id.btnBuyerLogout
        ).setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(
                    "Are you sure you want to logout?"
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Logout"
                ) { _, _ ->

                    auth.signOut()
                    openLogin()
                }
                .show()
        }
    }

    private fun showEditProfileDialog() {

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        val padding =
            (20 * resources.displayMetrics.density)
                .toInt()

        container.setPadding(
            padding,
            5,
            padding,
            0
        )

        val etName =
            EditText(this)

        etName.hint =
            "Full Name"

        etName.setText(
            currentName
        )

        etName.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS

        val etPhone =
            EditText(this)

        etPhone.hint =
            "Phone Number"

        etPhone.setText(
            currentPhone
        )

        etPhone.inputType =
            InputType.TYPE_CLASS_PHONE

        val etAddress =
            EditText(this)

        etAddress.hint =
            "Delivery Address"

        etAddress.setText(
            currentAddress
        )

        etAddress.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        container.addView(
            etName
        )

        container.addView(
            etPhone
        )

        container.addView(
            etAddress
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Edit Profile"
                )
                .setView(
                    container
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Save",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name =
                    etName.text
                        .toString()
                        .trim()

                val phone =
                    etPhone.text
                        .toString()
                        .trim()

                val address =
                    etAddress.text
                        .toString()
                        .trim()

                if (name.isBlank()) {

                    etName.error =
                        "Name is required"

                    return@setOnClickListener
                }

                updateBuyerProfile(
                    name,
                    phone,
                    address,
                    dialog
                )
            }
        }

        dialog.show()
    }

    private fun updateBuyerProfile(
        name: String,
        phone: String,
        address: String,
        dialog: AlertDialog
    ) {

        val uid =
            auth.currentUser?.uid
                ?: return

        val updates:
                Map<String, Any> =
            mapOf(
                "name" to name,
                "phone" to phone,
                "address" to address
            )

        firestore.collection("users")
            .document(uid)
            .update(updates)
            .addOnSuccessListener {

                currentName =
                    name

                currentPhone =
                    phone

                currentAddress =
                    address

                loadBuyerProfile()

                dialog.dismiss()

                Toast.makeText(
                    this,
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Update failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setupBottomNavigation() {

        findViewById<LinearLayout>(
            R.id.navBuyerProfileHome
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    BuyerHomeActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(
            R.id.navBuyerProfileMarketplace
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BuyerMarketplaceActivity::class.java
                )
            )

            finish()
        }

        findViewById<LinearLayout>(
            R.id.navBuyerProfileOrders
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BuyerOrdersActivity::class.java
                )
            )

            finish()
        }
    }

    private fun buildContactText(
        address: String,
        phone: String
    ): String {

        return when {

            address.isNotBlank() &&
                    phone.isNotBlank() -> {

                "📍 $address • ☎ $phone"
            }

            address.isNotBlank() -> {

                "📍 $address"
            }

            phone.isNotBlank() -> {

                "☎ $phone"
            }

            else -> {

                "Contact information not set"
            }
        }
    }

    private fun openLogin() {

        val intent =
            Intent(
                this,
                LoginActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}