package com.juaris.app

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

class BillingManager(
    private val context: Context,
    private val skuId: String = "juaris_monats_abo",
    private val onSubscriptionActive: () -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "JuarisBillingKernel"
        private val IS_BETA_BYPASS_ACTIVE: Boolean = BuildConfig.DEBUG
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection(onReady: () -> Unit = {}) {
        if (IS_BETA_BYPASS_ACTIVE) {
            Log.d(TAG, "🚧 Beta-Bypass aktiv: Schalte Premium-Dienste im Debug-Modus frei.")
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
                // Echte Reconnect-Logik für den Live-Betrieb
            }
        })
    }

    private fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchaseList: List<Purchase> = purchases
                for (i in 0 until purchaseList.size) {
                    handlePurchase(purchaseList.get(i))
                }
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
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null && productDetailsList.size > 0) {
                val productDetails: ProductDetails = productDetailsList.get(0)
                val offerDetailsList = productDetails.subscriptionOfferDetails
                
                if (offerDetailsList != null && offerDetailsList.size > 0) {
                    val offerToken = offerDetailsList.get(0).offerToken

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
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            val purchaseList: List<Purchase> = purchases
            for (i in 0 until purchaseList.size) {
                handlePurchase(purchaseList.get(i))
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
               
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "✅ Kauf erfolgreich bei Google bestätigt!")
                        onSubscriptionActive()
                    } else {
                        Log.e(TAG, "❌ Fehler bei der Kaufbestätigung: ${billingResult.debugMessage}")
                    }
                }
            } else {
                onSubscriptionActive()
            }
        }
    }
}

