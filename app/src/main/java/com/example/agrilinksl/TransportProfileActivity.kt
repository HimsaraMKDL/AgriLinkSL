package com.example.agrilinksl

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TransportProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvName: TextView
    private lateinit var tvContact: TextView
    private lateinit var tvVehicle: TextView

    private lateinit var availabilitySwitch: SwitchMaterial
    private lateinit var tvAvailabilityLabel: TextView

    private var isLoadingAvailability = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transport_profile)

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        initializeViews()
        setupBottomNavigation()
        setupProfileActions()
        loadTransportProfile()
    }

    private fun initializeViews() {

        tvName =
            findViewById(
                R.id.tvTransportProfileName
            )

        tvContact =
            findViewById(
                R.id.tvTransportProfileContact
            )

        tvVehicle =
            findViewById(
                R.id.tvProfileVehicleInfo
            )

        availabilitySwitch =
            findViewById(
                R.id.switchTransportAvailability
            )

        tvAvailabilityLabel =
            findViewById(
                R.id.tvTransportAvailabilityLabel
            )
    }

    private fun loadTransportProfile() {

        val uid =
            auth.currentUser?.uid

        if (uid == null) {

            openLogin()
            return
        }

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    return@addOnSuccessListener
                }

                val name =
                    document.getString("name")
                        ?: "Transport Provider"

                val phone =
                    document.getString("phone")
                        ?: ""

                val address =
                    document.getString("address")
                        ?: ""

                val vehicleType =
                    document.getString("vehicleType")
                        ?: "Transport Vehicle"

                val capacity =
                    document.getDouble("capacityKg")
                        ?: 0.0

                val available =
                    document.getBoolean("available")
                        ?: true

                tvName.text =
                    name

                tvContact.text =
                    buildContactText(
                        address,
                        phone
                    )

                tvVehicle.text =
                    if (capacity > 0) {

                        "$vehicleType • Max ${number(capacity)} kg"

                    } else {

                        vehicleType
                    }

                /*
                 * Prevent the listener from writing to
                 * Firestore while initial data is loading.
                 */
                isLoadingAvailability =
                    true

                availabilitySwitch.isChecked =
                    available

                updateAvailabilityLabel(
                    available
                )

                isLoadingAvailability =
                    false
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        setupAvailabilityListener()
    }

    private fun setupAvailabilityListener() {

        availabilitySwitch.setOnCheckedChangeListener {
                _,
                isChecked ->

            if (isLoadingAvailability) {
                return@setOnCheckedChangeListener
            }

            updateAvailability(
                isChecked
            )
        }
    }

    private fun updateAvailability(
        available: Boolean
    ) {

        val uid =
            auth.currentUser?.uid
                ?: return

        availabilitySwitch.isEnabled =
            false

        firestore.collection("users")
            .document(uid)
            .update(
                "available",
                available
            )
            .addOnSuccessListener {

                availabilitySwitch.isEnabled =
                    true

                updateAvailabilityLabel(
                    available
                )

                Toast.makeText(
                    this,
                    if (available)
                        "You are now available"
                    else
                        "You are now unavailable",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                availabilitySwitch.isEnabled =
                    true

                /*
                 * Return switch to previous state.
                 */
                isLoadingAvailability =
                    true

                availabilitySwitch.isChecked =
                    !available

                updateAvailabilityLabel(
                    !available
                )

                isLoadingAvailability =
                    false

                Toast.makeText(
                    this,
                    "Unable to update status: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateAvailabilityLabel(
        available: Boolean
    ) {

        tvAvailabilityLabel.text =
            if (available) {
                "AVAILABLE"
            } else {
                "UNAVAILABLE"
            }

        tvAvailabilityLabel.setTextColor(
            if (available) {
                android.graphics.Color.parseColor(
                    "#2E7D32"
                )
            } else {
                android.graphics.Color.parseColor(
                    "#D32F2F"
                )
            }
        )
    }

    private fun setupProfileActions() {

        findViewById<MaterialCardView>(
            R.id.cardProfileEarnings
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportEarningsActivity::class.java
                )
            )
        }

        findViewById<MaterialCardView>(
            R.id.cardJobHistory
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportRequestsActivity::class.java
                )
            )
        }

        findViewById<MaterialCardView>(
            R.id.cardMyVehicle
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Vehicle details are shown above",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<MaterialCardView>(
            R.id.cardRouteRecords
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Route records will be available from completed deliveries",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<MaterialCardView>(
            R.id.cardVehicleMaintenance
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Vehicle maintenance records coming next",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnTransportLogout
        ).setOnClickListener {

            auth.signOut()
            openLogin()
        }
    }

    private fun setupBottomNavigation() {

        findViewById<TextView>(
            R.id.navProfileDashboard
        ).setOnClickListener {

            val intent =
                Intent(
                    this,
                    TransportDashboardActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        }

        findViewById<TextView>(
            R.id.navProfileRequests
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportRequestsActivity::class.java
                )
            )

            finish()
        }

        findViewById<TextView>(
            R.id.navProfileEarnings
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TransportEarningsActivity::class.java
                )
            )

            finish()
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

                "Profile information not set"
            }
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
                java.util.Locale.getDefault(),
                "%.2f",
                value
            )
        }
    }
}