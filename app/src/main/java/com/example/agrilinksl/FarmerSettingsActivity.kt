package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FarmerSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farmer_settings)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val tvName =
            findViewById<TextView>(R.id.tvSettingsName)

        val tvContact =
            findViewById<TextView>(R.id.tvSettingsContact)

        findViewById<TextView>(
            R.id.tvSettingsBack
        ).setOnClickListener {
            finish()
        }

        auth.currentUser?.uid?.let { uid ->

            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->

                    tvName.text =
                        document.getString("name")
                            ?: "Farmer"

                    val address =
                        document.getString("address")
                            ?: ""

                    val phone =
                        document.getString("phone")
                            ?: ""

                    tvContact.text =
                        if (address.isBlank()) {
                            "📍 Location not set  •  ☎ $phone"
                        } else {
                            "📍 $address  •  ☎ $phone"
                        }
                }
        }

        findViewById<LinearLayout>(
            R.id.menuMyProducts
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    FarmerProductsActivity::class.java
                )
            )
        }

        findViewById<LinearLayout>(
            R.id.menuFarmLocation
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    FarmLocationActivity::class.java
                )

            intent.putExtra(
                "FARMER_ID",
                auth.currentUser?.uid
            )

            startActivity(intent)
        }

        findViewById<LinearLayout>(
            R.id.menuMyOrders
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Orders screen will be connected next",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<LinearLayout>(
            R.id.menuWeather
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Weather screen will be connected later",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<MaterialButton>(
            R.id.btnFarmerLogout
        ).setOnClickListener {

            auth.signOut()

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
}