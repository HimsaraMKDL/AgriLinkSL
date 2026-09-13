package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FarmerProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var farmerId: String

    private lateinit var tvName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvFarmName: TextView
    private lateinit var tvProductCount: TextView

    private lateinit var containerProducts: LinearLayout
    private lateinit var bottomNav: LinearLayout

    private lateinit var btnEdit: MaterialButton
    private lateinit var btnFarmLocation: MaterialButton
    private lateinit var tvSettings: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farmer_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        farmerId =
            intent.getStringExtra("FARMER_ID")
                ?: auth.currentUser?.uid
                        ?: ""

        if (farmerId.isBlank()) {
            finish()
            return
        }

        tvName =
            findViewById(R.id.tvProfileFarmerName)

        tvAddress =
            findViewById(R.id.tvProfileFarmerAddress)

        tvFarmName =
            findViewById(R.id.tvProfileFarmName)

        tvProductCount =
            findViewById(R.id.tvProfileProductCount)

        containerProducts =
            findViewById(R.id.containerProfileProducts)

        bottomNav =
            findViewById(R.id.farmerProfileBottomNav)

        btnEdit =
            findViewById(R.id.btnEditFarmerProfile)

        btnFarmLocation =
            findViewById(R.id.btnProfileFarmLocation)

        tvSettings =
            findViewById(R.id.tvProfileSettings)

        findViewById<TextView>(
            R.id.tvBackFarmerProfile
        ).setOnClickListener {
            finish()
        }

        setupOwnerMode()

        loadFarmer()
        loadProducts()

        btnFarmLocation.setOnClickListener {

            val intent =
                Intent(
                    this,
                    FarmLocationActivity::class.java
                )

            intent.putExtra(
                "FARMER_ID",
                farmerId
            )

            startActivity(intent)
        }

        tvSettings.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FarmerSettingsActivity::class.java
                )
            )
        }

        btnEdit.setOnClickListener {

            Toast.makeText(
                this,
                "Edit Profile will be added next",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<TextView>(
            R.id.profileNavHome
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FarmerHomeActivity::class.java
                )
            )

            finish()
        }

        findViewById<TextView>(
            R.id.profileNavProducts
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FarmerProductsActivity::class.java
                )
            )
        }
    }

    private fun setupOwnerMode() {

        val isOwner =
            auth.currentUser?.uid == farmerId

        if (!isOwner) {

            tvSettings.visibility =
                View.GONE

            btnEdit.visibility =
                View.GONE

            bottomNav.visibility =
                View.GONE
        }
    }

    private fun loadFarmer() {

        firestore.collection("users")
            .document(farmerId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    return@addOnSuccessListener
                }

                val name =
                    document.getString("name")
                        ?: "Local Farmer"

                val address =
                    document.getString("address")
                        ?: ""

                val farmName =
                    document.getString("farmName")
                        ?: ""

                tvName.text = name

                tvAddress.text =
                    if (address.isBlank()) {
                        "📍 Location not set"
                    } else {
                        "📍 $address"
                    }

                tvFarmName.text =
                    if (farmName.isBlank()) {
                        "Farm / Shop name not set"
                    } else {
                        "$farmName 🌱"
                    }
            }
    }

    private fun loadProducts() {

        containerProducts.removeAllViews()

        firestore.collection("products")
            .whereEqualTo(
                "farmerId",
                farmerId
            )
            .whereEqualTo(
                "status",
                "Available"
            )
            .get()
            .addOnSuccessListener { snapshot ->

                containerProducts.removeAllViews()

                tvProductCount.text =
                    "${snapshot.size()} Active"

                for (document in snapshot.documents) {

                    val view =
                        LayoutInflater.from(this)
                            .inflate(
                                R.layout.item_profile_product,
                                containerProducts,
                                false
                            )

                    val name =
                        document.getString("name")
                            ?: "Product"

                    val category =
                        document.getString("category")
                            ?: ""

                    val price =
                        document.getDouble("price")
                            ?: 0.0

                    val unit =
                        document.getString("unit")
                            ?: "kg"

                    view.findViewById<TextView>(
                        R.id.tvProfileProductEmoji
                    ).text =
                        productEmoji(
                            name,
                            category
                        )

                    view.findViewById<TextView>(
                        R.id.tvProfileProductName
                    ).text = name

                    view.findViewById<TextView>(
                        R.id.tvProfileProductPrice
                    ).text =
                        "Rs.${number(price)}/$unit"

                    containerProducts.addView(view)
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    exception.message,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun productEmoji(
        name: String,
        category: String
    ): String {

        val value =
            name.lowercase()

        return when {

            "tomato" in value ->
                "🍅"

            "carrot" in value ->
                "🥕"

            "cucumber" in value ->
                "🥒"

            "onion" in value ->
                "🧅"

            "chili" in value ||
                    "chilli" in value ->
                "🌶️"

            category.equals(
                "Fruits",
                true
            ) ->
                "🍎"

            else ->
                "🌱"
        }
    }

    private fun number(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {
            value.toInt().toString()
        } else {
            String.format(
                "%.2f",
                value
            )
        }
    }
}