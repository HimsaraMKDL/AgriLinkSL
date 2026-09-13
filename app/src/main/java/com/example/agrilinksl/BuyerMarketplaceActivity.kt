package com.example.agrilinksl

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class BuyerMarketplaceActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    private lateinit var etSearch:
            com.google.android.material.textfield.TextInputEditText

    private lateinit var containerProducts: LinearLayout
    private lateinit var tvProductCount: TextView

    private lateinit var btnAll: MaterialButton
    private lateinit var btnVegetables: MaterialButton
    private lateinit var btnFruits: MaterialButton
    private lateinit var btnRice: MaterialButton
    private lateinit var btnSpices: MaterialButton

    private var selectedCategory = "All"

    private val allProducts =
        mutableListOf<MarketplaceProduct>()

    data class MarketplaceProduct(
        val productId: String,
        val farmerId: String,
        val name: String,
        val category: String,
        val price: Double,
        val quantity: Double,
        val unit: String,
        var farmerName: String = "Local Farmer"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_buyer_marketplace
        )

        firestore =
            FirebaseFirestore.getInstance()

        etSearch =
            findViewById(
                R.id.etMarketplaceSearch
            )

        containerProducts =
            findViewById(
                R.id.containerMarketplaceProducts
            )

        tvProductCount =
            findViewById(
                R.id.tvMarketplaceCount
            )

        btnAll =
            findViewById(
                R.id.btnMarketAll
            )

        btnVegetables =
            findViewById(
                R.id.btnMarketVegetables
            )

        btnFruits =
            findViewById(
                R.id.btnMarketFruits
            )

        btnRice =
            findViewById(
                R.id.btnMarketRice
            )

        btnSpices =
            findViewById(
                R.id.btnMarketSpices
            )

        setupCategoryButtons()
        setupSearch()
        setupBottomNavigation()

        loadProducts()
    }

    override fun onResume() {
        super.onResume()

        loadProducts()
    }

    private fun setupCategoryButtons() {

        btnAll.setOnClickListener {
            selectCategory("All")
        }

        btnVegetables.setOnClickListener {
            selectCategory("Vegetables")
        }

        btnFruits.setOnClickListener {
            selectCategory("Fruits")
        }

        btnRice.setOnClickListener {
            selectCategory("Rice")
        }

        btnSpices.setOnClickListener {
            selectCategory("Spices")
        }

        updateCategoryUI()
    }

    private fun selectCategory(
        category: String
    ) {

        selectedCategory =
            category

        updateCategoryUI()
        applyFilters()
    }

    private fun setupSearch() {

        etSearch.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    applyFilters()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun loadProducts() {

        firestore.collection("products")
            .whereEqualTo(
                "status",
                "Available"
            )
            .get()
            .addOnSuccessListener { snapshot ->

                allProducts.clear()

                if (snapshot.isEmpty) {

                    applyFilters()
                    return@addOnSuccessListener
                }

                for (
                document
                in snapshot.documents
                ) {

                    val quantity =
                        document.getDouble(
                            "quantity"
                        ) ?: 0.0

                    /*
                     * Do not display products
                     * with zero stock.
                     */
                    if (quantity <= 0.0) {
                        continue
                    }

                    val product =
                        MarketplaceProduct(
                            productId =
                                document.id,

                            farmerId =
                                document.getString(
                                    "farmerId"
                                ) ?: "",

                            name =
                                document.getString(
                                    "name"
                                ) ?: "Product",

                            category =
                                document.getString(
                                    "category"
                                ) ?: "",

                            price =
                                document.getDouble(
                                    "price"
                                ) ?: 0.0,

                            quantity =
                                quantity,

                            unit =
                                document.getString(
                                    "unit"
                                ) ?: "kg"
                        )

                    allProducts.add(
                        product
                    )

                    loadFarmerName(
                        product
                    )
                }

                applyFilters()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load marketplace: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadFarmerName(
        product: MarketplaceProduct
    ) {

        if (product.farmerId.isBlank()) {
            return
        }

        firestore.collection("users")
            .document(product.farmerId)
            .get()
            .addOnSuccessListener { document ->

                val farmName =
                    document.getString(
                        "farmName"
                    ) ?: ""

                val farmerName =
                    document.getString(
                        "name"
                    ) ?: "Local Farmer"

                product.farmerName =
                    farmName.ifBlank {
                        farmerName
                    }

                applyFilters()
            }
    }

    private fun applyFilters() {

        val searchText =
            etSearch.text
                ?.toString()
                ?.trim()
                ?.lowercase()
                ?: ""

        val filtered =
            allProducts.filter { product ->

                val categoryMatches =
                    selectedCategory == "All" ||
                            product.category.equals(
                                selectedCategory,
                                ignoreCase = true
                            )

                val searchMatches =
                    searchText.isBlank() ||
                            product.name
                                .lowercase()
                                .contains(searchText) ||
                            product.farmerName
                                .lowercase()
                                .contains(searchText) ||
                            product.category
                                .lowercase()
                                .contains(searchText)

                categoryMatches &&
                        searchMatches
            }
                .sortedBy {
                    it.name.lowercase()
                }

        displayProducts(
            filtered
        )
    }

    private fun displayProducts(
        products: List<MarketplaceProduct>
    ) {

        containerProducts.removeAllViews()

        tvProductCount.text =
            "${products.size} products"

        if (products.isEmpty()) {

            showEmptyMessage()
            return
        }

        for (product in products) {

            val view =
                LayoutInflater.from(this)
                    .inflate(
                        R.layout.item_marketplace_product,
                        containerProducts,
                        false
                    )

            view.findViewById<TextView>(
                R.id.tvMarketplaceProductEmoji
            ).text =
                productEmoji(
                    product.name,
                    product.category
                )

            view.findViewById<TextView>(
                R.id.tvMarketplaceProductName
            ).text =
                product.name

            view.findViewById<TextView>(
                R.id.tvMarketplaceProductPrice
            ).text =
                "Rs. ${number(product.price)} /${product.unit}"

            view.findViewById<TextView>(
                R.id.tvMarketplaceFarmer
            ).text =
                product.farmerName

            view.findViewById<TextView>(
                R.id.tvMarketplaceStock
            ).text =
                "${number(product.quantity)} ${product.unit} available"

            view.setOnClickListener {

                val intent =
                    Intent(
                        this,
                        ProductDetailsActivity::class.java
                    )

                intent.putExtra(
                    "PRODUCT_ID",
                    product.productId
                )

                startActivity(intent)
            }

            containerProducts.addView(
                view
            )
        }
    }

    private fun showEmptyMessage() {

        val text =
            TextView(this)

        text.text =
            if (
                etSearch.text
                    ?.toString()
                    ?.trim()
                    ?.isNotBlank() == true
            ) {

                "No products match your search."

            } else {

                "No products available in this category."
            }

        text.setTextColor(
            "#6B7280".toColorInt()
        )

        text.textSize =
            12f

        text.setPadding(
            8,
            30,
            8,
            30
        )

        containerProducts.addView(
            text
        )
    }

    private fun updateCategoryUI() {

        val green =
            "#2E7D32".toColorInt()

        val white =
            "#FFFFFF".toColorInt()

        val dark =
            "#1F2937".toColorInt()

        val buttons =
            listOf(
                "All" to btnAll,
                "Vegetables" to btnVegetables,
                "Fruits" to btnFruits,
                "Rice" to btnRice,
                "Spices" to btnSpices
            )

        for (
        (category, button)
        in buttons
        ) {

            if (
                category ==
                selectedCategory
            ) {

                button.backgroundTintList =
                    ColorStateList.valueOf(
                        green
                    )

                button.setTextColor(
                    white
                )

            } else {

                button.backgroundTintList =
                    ColorStateList.valueOf(
                        white
                    )

                button.setTextColor(
                    dark
                )
            }
        }
    }

    private fun setupBottomNavigation() {

        findViewById<LinearLayout>(
            R.id.navMarketplaceHome
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
            R.id.navMarketplaceOrders
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BuyerOrdersActivity::class.java
                )
            )

            finish()
        }

        findViewById<LinearLayout>(
            R.id.navMarketplaceProfile
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    BuyerProfileActivity::class.java
                )
            )

            finish()
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

            "potato" in value ->
                "🥔"

            "corn" in value ->
                "🌽"

            "chili" in value ||
                    "chilli" in value ->
                "🌶️"

            "banana" in value ->
                "🍌"

            "apple" in value ->
                "🍎"

            category.equals(
                "Vegetables",
                true
            ) ->
                "🥬"

            category.equals(
                "Fruits",
                true
            ) ->
                "🍎"

            category.equals(
                "Rice",
                true
            ) ->
                "🌾"

            category.equals(
                "Spices",
                true
            ) ->
                "🌶️"

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

            value.toInt()
                .toString()

        } else {

            String.format(
                Locale.getDefault(),
                "%.2f",
                value
            )
        }
    }
}