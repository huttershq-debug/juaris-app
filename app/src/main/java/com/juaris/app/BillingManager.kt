package com.juaris.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(
    private val context: Context,
    private val productID: String = "juaris_monats_abo",
    private val onPurchased: () -> Unit = {}
) {
    private lateinit var billingClient: BillingClient

    companion object {
        // HIER für den Beta-Test auf `true` lassen. 
        // Später vor dem Einreichen bei Google einfach auf `false` stellen!
        var IS_BETA_BYPASS_ACTIVE: Boolean = true
    }

    fun startConnection(onReady: () -> Unit) {
        // Wenn der Beta-Bypass aktiv ist, überspringen wir die echte Billing-Verbindung
        if (IS_BETA_BYPASS_ACTIVE) {
            onPurchased()
            onReady()
            return
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Verbindung wird bei Bedarf neu aufgebaut
            }
        })
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            onPurchased()
        }
    }

    fun launchBillingFlow(activity: Activity) {
        // Wenn der Bypass aktiv ist, triggern wir den Kauf-Erfolg direkt ohne Play Store
        if (IS_BETA_BYPASS_ACTIVE) {
            onPurchased()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
       
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }
}

