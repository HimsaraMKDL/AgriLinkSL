package com.example.agrilinksl

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddProductActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var cardProductImage: MaterialCardView
    private lateinit var ivProductImage: ImageView
    private lateinit var layoutImagePrompt: LinearLayout

    private lateinit var etProductName: TextInputEditText
    private lateinit var actCategory: AutoCompleteTextView
    private lateinit var etPrice: TextInputEditText
    private lateinit var etQuantity: TextInputEditText
    private lateinit var etDescription: TextInputEditText

    private lateinit var btnPublishProduct: MaterialButton
    private lateinit var tvBack: TextView

    private var selectedImageUri: Uri? = null
    private var editingProductId: String? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                if (isImageUnderFiveMb(uri)) {
                    selectedImageUri = uri
                    ivProductImage.setImageURI(uri)
                    ivProductImage.visibility = View.VISIBLE
                    layoutImagePrompt.visibility = View.GONE
                } else {
                    Toast.makeText(
                        this,
                        "Image must be smaller than 5 MB",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews()
        setupCategoryDropdown()
        setupClickListeners()

        editingProductId = intent.getStringExtra("PRODUCT_ID")
        if (editingProductId != null) {
            loadProductForEditing(editingProductId!!)
        }
    }

    private fun initializeViews() {
        cardProductImage = findViewById(R.id.cardProductImage)
        ivProductImage = findViewById(R.id.ivProductImage)
        layoutImagePrompt = findViewById(R.id.layoutImagePrompt)

        etProductName = findViewById(R.id.etProductName)
        actCategory = findViewById(R.id.actCategory)
        etPrice = findViewById(R.id.etPrice)
        etQuantity = findViewById(R.id.etQuantity)
        etDescription = findViewById(R.id.etDescription)

        btnPublishProduct = findViewById(R.id.btnPublishProduct)
        tvBack = findViewById(R.id.tvBackAddProduct)
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf(
            "Vegetables",
            "Fruits",
            "Rice",
            "Spices",
            "Grains",
            "Other"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        actCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        tvBack.setOnClickListener {
            finish()
        }

        cardProductImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnPublishProduct.setOnClickListener {
            validateAndPublishProduct()
        }
    }

    private fun loadProductForEditing(productId: String) {
        firestore.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(
                        this,
                        "Product not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@addOnSuccessListener
                }

                etProductName.setText(document.getString("name") ?: "")
                actCategory.setText(document.getString("category") ?: "", false)

                val price = document.getDouble("price") ?: 0.0
                val quantity = document.getDouble("quantity") ?: 0.0

                etPrice.setText(formatNumber(price))
                etQuantity.setText(formatNumber(quantity))
                etDescription.setText(document.getString("description") ?: "")

                btnPublishProduct.text = "UPDATE PRODUCT"
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Unable to load product",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun validateAndPublishProduct() {
        val productName = etProductName.text.toString().trim()
        val category = actCategory.text.toString().trim()
        val priceText = etPrice.text.toString().trim()
        val quantityText = etQuantity.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (productName.isEmpty()) {
            etProductName.error = "Enter product name"
            etProductName.requestFocus()
            return
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (priceText.isEmpty()) {
            etPrice.error = "Enter price"
            etPrice.requestFocus()
            return
        }

        if (quantityText.isEmpty()) {
            etQuantity.error = "Enter quantity"
            etQuantity.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Enter description"
            etDescription.requestFocus()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            etPrice.error = "Enter a valid price"
            etPrice.requestFocus()
            return
        }

        val quantity = quantityText.toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            etQuantity.error = "Enter a valid quantity"
            etQuantity.requestFocus()
            return
        }

        if (editingProductId == null) {
            publishProduct(
                productName = productName,
                category = category,
                price = price,
                quantity = quantity,
                description = description
            )
        } else {
            updateProduct(
                productId = editingProductId!!,
                productName = productName,
                category = category,
                price = price,
                quantity = quantity,
                description = description
            )
        }
    }

    private fun publishProduct(
        productName: String,
        category: String,
        price: Double,
        quantity: Double,
        description: String
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "Please login again",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnPublishProduct.isEnabled = false
        btnPublishProduct.text = "PUBLISHING..."

        val productDocument = firestore.collection("products").document()

        val product = hashMapOf<String, Any>(
            "productId" to productDocument.id,
            "farmerId" to currentUser.uid,
            "name" to productName,
            "category" to category,
            "description" to description,
            "price" to price,
            "unit" to "kg",
            "quantity" to quantity,
            "imageUrl" to "",
            "status" to "Available",
            "createdAt" to FieldValue.serverTimestamp()
        )

        productDocument
            .set(product)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Product published successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { exception ->
                btnPublishProduct.isEnabled = true
                btnPublishProduct.text = "PUBLISH PRODUCT 🌱"
                Toast.makeText(
                    this,
                    "Failed to publish product: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateProduct(
        productId: String,
        productName: String,
        category: String,
        price: Double,
        quantity: Double,
        description: String
    ) {
        btnPublishProduct.isEnabled = false
        btnPublishProduct.text = "UPDATING..."

        val updates = hashMapOf<String, Any?>(
            "name" to productName,
            "category" to category,
            "price" to price,
            "quantity" to quantity,
            "description" to description,
            "status" to "Available",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("products")
            .document(productId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Product updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { exception ->
                btnPublishProduct.isEnabled = true
                btnPublishProduct.text = "UPDATE PRODUCT"
                Toast.makeText(
                    this,
                    "Update failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.2f", value)
        }
    }

    private fun isImageUnderFiveMb(uri: Uri): Boolean {
        val descriptor = contentResolver.openAssetFileDescriptor(uri, "r")
        val fileSize = descriptor?.length ?: -1L
        descriptor?.close()

        if (fileSize == -1L) {
            return true
        }

        val maxSize = 5L * 1024L * 1024L
        return fileSize <= maxSize
    }
}