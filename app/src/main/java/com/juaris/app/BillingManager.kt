package com.juaris.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(
    private val context: Context,
    private val skuId: String = "juaris_monats_abo",
    private val onSubscriptionActive: () -> Unit
) : PurchasesUpdatedListener {

    // ⚠️ HIER MIT EINEM KLICK AUF TRUE STELLEN ZUM TESTEN AUF DEM HANDY
    // Vor dem Upload in die Google Play Console auf FALSE setzen!
    companion object {
        var IS_BETA_BYPASS_ACTIVE: Boolean = com.juaris.app.BuildConfig.DEBUG
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun startConnection(onReady: () -> Unit = {}) {
        if (IS_BETA_BYPASS_ACTIVE) {
            onSubscriptionActive()
            onReady()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry logic / Neuverbindung bei Bedarf
            }
        })
    }

    private fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
                onSubscriptionActive()
            }
        }
    }

    fun launchBillingFlow(activity: Activity) {
        if (IS_BETA_BYPASS_ACTIVE) {
            onSubscriptionActive()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(skuId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList.first()
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return@queryProductDetailsAsync

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    onSubscriptionActive()
                }
            }
        }
    }
}

