package com.example.agrilinksl

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FarmerProductsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var containerProducts: LinearLayout
    private lateinit var tvNoProducts: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farmer_products)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        containerProducts =
            findViewById(R.id.containerManageProducts)

        tvNoProducts =
            findViewById(R.id.tvNoProducts)

        findViewById<TextView>(
            R.id.tvBackProducts
        ).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(
            R.id.btnAddNewProduct
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddProductActivity::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    private fun loadProducts() {

        val currentUser =
            auth.currentUser ?: return

        containerProducts.removeAllViews()

        firestore.collection("products")
            .whereEqualTo(
                "farmerId",
                currentUser.uid
            )
            .get()
            .addOnSuccessListener { snapshot ->

                containerProducts.removeAllViews()

                if (snapshot.isEmpty) {
                    tvNoProducts.visibility =
                        View.VISIBLE

                    return@addOnSuccessListener
                }

                tvNoProducts.visibility =
                    View.GONE

                for (document in snapshot.documents) {

                    val itemView =
                        LayoutInflater.from(this)
                            .inflate(
                                R.layout.item_manage_product,
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

                    val quantity =
                        document.getDouble("quantity")
                            ?: 0.0

                    val unit =
                        document.getString("unit")
                            ?: "kg"

                    val productId =
                        document.id

                    val tvEmoji =
                        itemView.findViewById<TextView>(
                            R.id.tvManageEmoji
                        )

                    val tvName =
                        itemView.findViewById<TextView>(
                            R.id.tvManageName
                        )

                    val tvPrice =
                        itemView.findViewById<TextView>(
                            R.id.tvManagePrice
                        )

                    val tvQuantity =
                        itemView.findViewById<TextView>(
                            R.id.tvManageQuantity
                        )

                    val btnEdit =
                        itemView.findViewById<MaterialButton>(
                            R.id.btnEditProduct
                        )

                    val btnDelete =
                        itemView.findViewById<MaterialButton>(
                            R.id.btnDeleteProduct
                        )

                    tvEmoji.text =
                        getProductEmoji(
                            name,
                            category
                        )

                    tvName.text = name

                    tvPrice.text =
                        "Rs. ${formatNumber(price)} /$unit"

                    tvQuantity.text =
                        "${formatNumber(quantity)} $unit available"

                    btnEdit.setOnClickListener {

                        val intent =
                            Intent(
                                this,
                                AddProductActivity::class.java
                            )

                        intent.putExtra(
                            "PRODUCT_ID",
                            productId
                        )

                        startActivity(intent)
                    }

                    btnDelete.setOnClickListener {

                        showDeleteDialog(
                            productId,
                            name
                        )
                    }

                    containerProducts.addView(
                        itemView
                    )
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load products: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun showDeleteDialog(
        productId: String,
        productName: String
    ) {

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Product")
            .setMessage(
                "Are you sure you want to delete $productName?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteProduct(productId)
            }
            .show()
    }

    private fun deleteProduct(
        productId: String
    ) {

        firestore.collection("products")
            .document(productId)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Product deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                loadProducts()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Delete failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun getProductEmoji(
        name: String,
        category: String
    ): String {

        val lowerName =
            name.lowercase()

        return when {

            "tomato" in lowerName ->
                "🍅"

            "carrot" in lowerName ->
                "🥕"

            "cucumber" in lowerName ->
                "🥒"

            "potato" in lowerName ->
                "🥔"

            "corn" in lowerName ->
                "🌽"

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

            category.equals(
                "Vegetables",
                true
            ) ->
                "🥬"

            else ->
                "🌱"
        }
    }

    private fun formatNumber(
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