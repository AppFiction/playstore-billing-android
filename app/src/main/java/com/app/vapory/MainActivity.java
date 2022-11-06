package com.app.vapory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.app.vapory.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrate one-time purchase to remove Ads and monthly subscription for app
 * Docs: https://developer.android.com/google/play/billing/integrate
 */

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private ActivityMainBinding binding;
    private BillingClient billingClient;

    /**
     * SharedPreference cache
     */
    private Cache cache;


    /**
     * ID of user who is logged into the app/. For demo purpose we make user constant. Change to your login.
     */
    private String USER_ID = "id_of_the_user_123";

    /**
     * ID of one-time in-app product
     */
    private final String TICKET_PRODUCT_ID = "t1";

    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //Initialise SharedPreference data storage
        cache = new Cache(MainActivity.this);

        //Connect to Google Play billing at start of activity
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        //Connect to google play
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    queryPurchases();
                    Toast.makeText(MainActivity.this, "Billing successfully connected", Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Toast.makeText(MainActivity.this, "Billing disconnected", Toast.LENGTH_LONG).show();

            }
        });

        //Button Clicks Listeners
        binding.removeAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buyProduct(TICKET_PRODUCT_ID);
            }
        });

        binding.restorePurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restorePurchases();
                Toast.makeText(MainActivity.this, "Restored", Toast.LENGTH_LONG).show();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Call this here to update app when billing status changes
        queryPurchases();
    }

    /**
     * Query in-app details from playstore using sku/product_id
     * and launch billing flow to buy product
     *
     * @param productID sku of product
     */
    private void buyProduct(String productID) {
        List<String> skuList = new ArrayList<>();
        skuList.add(productID);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        if (skuDetailsList == null) {
                            Toast.makeText(MainActivity.this, "Account unable to access products. Check if your gmail account is a license tester on Google Play Console", Toast.LENGTH_LONG).show();
                            return;
                        }
                        // Process the result.
                        SkuDetails selectedSkuDetail = selectRemoveAdsProduct(skuDetailsList);
                        launchBillingFlow(selectedSkuDetail);
                    }
                });
    }

    /**
     * Restore all active purchased products
     * Call this function to update app with user's purchases
     */
    private void restorePurchases() {
        updateBillingUI(cache.getRemoveAdsData());
    }

    /**
     * Update system to current purchases. You can change this function to support other app
     *
     * @param ticketData    System data to record previous remove_ads purchase.
     */
    private void updateBillingUI(TicketData ticketData) {
        if (ticketData != null) {
            //Ads OFF
            binding.removeAds.setEnabled(false);
            binding.removeAds.setText(getString(R.string.status_no_ads));
        } else {
            //Ads ON
            binding.removeAds.setEnabled(true);
            binding.removeAds.setText(getString(R.string.status_ads));
        }
    }


    /**
     * Save SKUs and purchase tokens to your system data.In this case SharedPreference
     */
    private void recordPurchase(String userID, Purchase purchase) {
        if (purchase.getSkus().get(0).equals(TICKET_PRODUCT_ID)) {
            //Remove Ads
            TicketData removeAdsData = new TicketData();
            removeAdsData.setPurchaseToken(purchase.getPurchaseToken());
            removeAdsData.setUserID(purchase.getSkus().get(0));
            removeAdsData.setSku(userID);
            cache.setRemoveAdsData(removeAdsData);
            restorePurchases();
            Log.d(TAG, "Ads (Remove): Saved");

        }
    }


    /**
     * Check existing purchases that user has previously made.
     * This will query playstore app cache without making a network request
     */
    private void queryPurchases() {
//        if (billingClient.isReady()) {
//            Purchase.PurchasesResult adsPurchaseResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
//            if ((adsPurchaseResult.getPurchasesList() != null) && adsPurchaseResult.getPurchasesList().size() > 0) {
//                Purchase adsPurchase = adsPurchaseResult.getPurchasesList().get(0);
//                handlePurchase(adsPurchase);
//            }
//        }
    }

    /**
     * Handle purchase
     *
     * @param purchase
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            //Save transaction on system
            recordPurchase(USER_ID, purchase);
            if (!purchase.isAcknowledged()) {
                //Acknowledge purchase to prevent refund to buyer
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
            }
        }
    }

    /**
     * Callback to deliver result of the purchase operation
     *
     * @param billingResult
     * @param purchases
     */
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    @Override
    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {

    }

    /**
     * Launch playstore payment screen to buy remove_ads product id
     *
     * @param skuDetails
     */
    private void launchBillingFlow(SkuDetails skuDetails) {
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();

    }

    /**
     * Select remove_ads product to buy.
     *
     * @param skuDetailsList
     */
    private SkuDetails selectRemoveAdsProduct(@NonNull List<SkuDetails> skuDetailsList) {
        if (skuDetailsList.size() > 0) {
            return skuDetailsList.get(0);
        }
        return null;
    }
}
