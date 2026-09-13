package com.example.agrilinksl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BuyerHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvBuyerGreeting: TextView
    private lateinit var containerBuyerProducts: LinearLayout
    private lateinit var containerNearbyFarmers: LinearLayout

    private lateinit var etBuyerSearch: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnVegetables: MaterialButton
    private lateinit var btnFruits: MaterialButton
    private lateinit var btnRice: MaterialButton
    private lateinit var btnSpices: MaterialButton

    private var selectedCategory = "Vegetables"
    private val allProducts = mutableListOf<ProductItem>()

    // Location Variables
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var buyerLatitude: Double? = null
    private var buyerLongitude: Double? = null

    // Permission Launcher
    private val buyerLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                getBuyerCurrentLocation()
            } else {
                loadNearbyFarmers()
            }
        }

    data class ProductItem(
        val productId: String,
        val farmerId: String,
        val name: String,
        val category: String,
        val price: Double,
        val quantity: Double,
        val unit: String,
        var farmerName: String = "Local Farmer"
    )

    data class FarmerItem(
        val farmerId: String,
        val name: String,
        val address: String,
        val latitude: Double?,
        val longitude: Double?,
        val distanceKm: Float? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buyer_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        tvBuyerGreeting = findViewById(R.id.tvBuyerGreeting)
        containerBuyerProducts = findViewById(R.id.containerBuyerProducts)
        containerNearbyFarmers = findViewById(R.id.containerNearbyFarmers)

        etBuyerSearch = findViewById(R.id.etBuyerSearch)
        btnVegetables = findViewById(R.id.btnCategoryVegetables)
        btnFruits = findViewById(R.id.btnCategoryFruits)
        btnRice = findViewById(R.id.btnCategoryRice)
        btnSpices = findViewById(R.id.btnCategorySpices)

        setupCategoryButtons()
        setupSearch()

        loadBuyerProfile()
        loadAvailableProducts()

        // Load nearby farmers via location request
        requestBuyerLocation()

        // --- NEW CODE ADDED HERE ---
        findViewById<LinearLayout>(
            R.id.navBuyerOrders
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    BuyerOrdersActivity::class.java
                )
            )
        }

        findViewById<LinearLayout>(
            R.id.navBuyerMarketplace
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    BuyerMarketplaceActivity::class.java
                )
            )
        }

        // --- ADDED BUYER PROFILE NAVIGATION ---
        findViewById<LinearLayout>(
            R.id.navBuyerProfile
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    BuyerProfileActivity::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadAvailableProducts()
    }

    // --- Location Functions ---

    private fun requestBuyerLocation() {
        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            finePermission == PackageManager.PERMISSION_GRANTED ||
            coarsePermission == PackageManager.PERMISSION_GRANTED
        ) {
            getBuyerCurrentLocation()
        } else {
            buyerLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getBuyerCurrentLocation() {
        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

        if (
            finePermission != PackageManager.PERMISSION_GRANTED &&
            coarsePermission != PackageManager.PERMISSION_GRANTED
        ) {
            loadNearbyFarmers()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    buyerLatitude = location.latitude
                    buyerLongitude = location.longitude
                }
                loadNearbyFarmers()
            }
            .addOnFailureListener {
                loadNearbyFarmers()
            }
    }

    private fun calculateDistance(
        farmerLatitude: Double?,
        farmerLongitude: Double?
    ): Float? {

        val buyerLat = buyerLatitude ?: return null
        val buyerLng = buyerLongitude ?: return null

        if (farmerLatitude == null || farmerLongitude == null) {
            return null
        }

        val results = FloatArray(1)

        Location.distanceBetween(
            buyerLat,
            buyerLng,
            farmerLatitude,
            farmerLongitude,
            results
        )

        return results[0] / 1000f
    }

    // --- Existing Functions ---

    private fun loadBuyerProfile() {

        val currentUser = auth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val fullName = document.getString("name") ?: "Buyer"

                val firstName = fullName.trim()
                    .split(" ")
                    .firstOrNull()
                    ?: "Buyer"

                val formattedName = firstName.replaceFirstChar {
                    it.uppercase()
                }

                tvBuyerGreeting.text = "Good Morning, $formattedName 👋"
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadAvailableProducts() {

        firestore.collection("products")
            .whereEqualTo("status", "Available")
            .get()
            .addOnSuccessListener { snapshot ->

                allProducts.clear()

                if (snapshot.isEmpty) {
                    displayProducts(emptyList())
                    return@addOnSuccessListener
                }

                for (document in snapshot.documents) {

                    val product = ProductItem(
                        productId = document.id,
                        farmerId = document.getString("farmerId") ?: "",
                        name = document.getString("name") ?: "Product",
                        category = document.getString("category") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        quantity = document.getDouble("quantity") ?: 0.0,
                        unit = document.getString("unit") ?: "kg"
                    )

                    allProducts.add(product)
                    loadFarmerNameForProduct(product)
                }

                applyFilters()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load products: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadFarmerNameForProduct(product: ProductItem) {

        if (product.farmerId.isBlank()) {
            return
        }

        firestore.collection("users")
            .document(product.farmerId)
            .get()
            .addOnSuccessListener { document ->

                product.farmerName = document.getString("name") ?: "Local Farmer"
                applyFilters()
            }
    }

    private fun loadNearbyFarmers() {

        containerNearbyFarmers.removeAllViews()

        firestore.collection("users")
            .whereEqualTo("role", "Farmer")
            .get()
            .addOnSuccessListener { snapshot ->

                val farmers = mutableListOf<FarmerItem>()

                for (document in snapshot.documents) {

                    val farmerLatitude = document.getDouble("latitude")
                    val farmerLongitude = document.getDouble("longitude")

                    val distance = calculateDistance(
                        farmerLatitude,
                        farmerLongitude
                    )

                    farmers.add(
                        FarmerItem(
                            farmerId = document.id,
                            name = document.getString("name") ?: "Local Farmer",
                            address = document.getString("address") ?: "",
                            latitude = farmerLatitude,
                            longitude = farmerLongitude,
                            distanceKm = distance
                        )
                    )
                }

                val sortedFarmers = farmers.sortedWith(
                    compareBy<FarmerItem> {
                        it.distanceKm == null
                    }.thenBy {
                        it.distanceKm
                    }
                )

                containerNearbyFarmers.removeAllViews()

                if (sortedFarmers.isEmpty()) {
                    showNoFarmersMessage()
                    return@addOnSuccessListener
                }

                for (farmer in sortedFarmers) {
                    addFarmerCard(farmer)
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load farmers: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setupCategoryButtons() {

        btnVegetables.setOnClickListener {
            selectedCategory = "Vegetables"
            updateCategoryUI()
            applyFilters()
        }

        btnFruits.setOnClickListener {
            selectedCategory = "Fruits"
            updateCategoryUI()
            applyFilters()
        }

        btnRice.setOnClickListener {
            selectedCategory = "Rice"
            updateCategoryUI()
            applyFilters()
        }

        btnSpices.setOnClickListener {
            selectedCategory = "Spices"
            updateCategoryUI()
            applyFilters()
        }

        updateCategoryUI()
    }

    private fun setupSearch() {

        etBuyerSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    applyFilters()
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    private fun applyFilters() {

        val searchText = etBuyerSearch.text
            ?.toString()
            ?.trim()
            ?.lowercase()
            ?: ""

        val filteredProducts = allProducts.filter { product ->

            val matchesCategory = product.category.equals(
                selectedCategory,
                ignoreCase = true
            )

            val matchesSearch = searchText.isBlank() ||
                    product.name.lowercase().contains(searchText) ||
                    product.farmerName.lowercase().contains(searchText)

            matchesCategory && matchesSearch
        }

        displayProducts(filteredProducts)
    }

    private fun updateCategoryUI() {

        val green = Color.parseColor("#2E7D32")
        val white = Color.parseColor("#FFFFFF")
        val dark = Color.parseColor("#1F2937")

        val buttons = listOf(
            "Vegetables" to btnVegetables,
            "Fruits" to btnFruits,
            "Rice" to btnRice,
            "Spices" to btnSpices
        )

        for ((category, button) in buttons) {

            if (category == selectedCategory) {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(green)
                button.setTextColor(white)
            } else {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(white)
                button.setTextColor(dark)
            }
        }
    }

    private fun displayProducts(products: List<ProductItem>) {

        containerBuyerProducts.removeAllViews()

        if (products.isEmpty()) {
            showNoProductsMessage()
            return
        }

        for (product in products) {

            val productView = LayoutInflater.from(this)
                .inflate(
                    R.layout.item_buyer_product,
                    containerBuyerProducts,
                    false
                )

            val tvEmoji = productView.findViewById<TextView>(R.id.tvBuyerProductEmoji)
            val tvName = productView.findViewById<TextView>(R.id.tvBuyerProductName)
            val tvPrice = productView.findViewById<TextView>(R.id.tvBuyerProductPrice)
            val tvFarmer = productView.findViewById<TextView>(R.id.tvBuyerProductFarmer)

            tvEmoji.text = getProductEmoji(product.name, product.category)
            tvName.text = product.name
            tvPrice.text = "Rs. ${formatNumber(product.price)} /${product.unit}"
            tvFarmer.text = product.farmerName

            // Added OnClickListener here
            productView.setOnClickListener {
                val intent = Intent(
                    this,
                    ProductDetailsActivity::class.java
                )

                intent.putExtra(
                    "PRODUCT_ID",
                    product.productId
                )

                startActivity(intent)
            }

            containerBuyerProducts.addView(productView)
        }
    }

    private fun addFarmerCard(farmer: FarmerItem) {

        val farmerView = LayoutInflater.from(this)
            .inflate(
                R.layout.item_nearby_farmer,
                containerNearbyFarmers,
                false
            )

        val tvName = farmerView.findViewById<TextView>(R.id.tvDynamicFarmerName)
        val tvLocation = farmerView.findViewById<TextView>(R.id.tvDynamicFarmerLocation)
        val btnViewFarm = farmerView.findViewById<MaterialButton>(R.id.btnDynamicViewFarmer)

        tvName.text = farmer.name

        tvLocation.text = when {
            farmer.distanceKm != null && farmer.address.isNotBlank() -> {
                "📍 ${farmer.address} (${String.format("%.1f", farmer.distanceKm)} km away)"
            }
            farmer.distanceKm != null -> {
                "📍 ${String.format("%.1f", farmer.distanceKm)} km away"
            }
            farmer.address.isNotBlank() -> {
                "📍 ${farmer.address}"
            }
            farmer.latitude != null && farmer.longitude != null -> {
                "📍 Farm location available"
            }
            else -> {
                "📍 Location not set"
            }
        }

        btnViewFarm.setOnClickListener {
            val intent = Intent(this, FarmerProfileActivity::class.java)
            intent.putExtra("FARMER_ID", farmer.farmerId)
            startActivity(intent)
        }

        containerNearbyFarmers.addView(farmerView)
    }

    private fun showNoProductsMessage() {

        val textView = TextView(this)

        textView.text = "No products available right now."
        textView.setTextColor(Color.parseColor("#6B7280"))
        textView.textSize = 12f
        textView.setPadding(8, 25, 8, 25)

        containerBuyerProducts.addView(textView)
    }

    private fun showNoFarmersMessage() {

        val textView = TextView(this)

        textView.text = "No farmers available right now."
        textView.setTextColor(Color.parseColor("#6B7280"))
        textView.textSize = 12f
        textView.setPadding(8, 24, 8, 24)

        containerNearbyFarmers.addView(textView)
    }

    private fun getProductEmoji(name: String, category: String): String {

        val lowerName = name.lowercase()

        return when {
            "tomato" in lowerName -> "🍅"
            "carrot" in lowerName -> "🥕"
            "cucumber" in lowerName -> "🥒"
            "potato" in lowerName -> "🥔"
            "corn" in lowerName -> "🌽"
            "chili" in lowerName || "chilli" in lowerName -> "🌶️"
            "apple" in lowerName -> "🍎"
            "banana" in lowerName -> "🍌"
            category.equals("Vegetables", true) -> "🥬"
            category.equals("Fruits", true) -> "🍎"
            category.equals("Rice", true) -> "🌾"
            category.equals("Spices", true) -> "🌶️"
            else -> "🌱"
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.2f", value)
        }
    }
}