package demo.appfiction.playbilling;

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
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

import demo.appfiction.playbilling.databinding.ActivityMainBinding;


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

    public static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL
            = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";

    /**
     * ID of one-time in-app product
     */
    private final String PRODUCT_1 = "prod1";
    private final String PRODUCT_2 = "prod2";


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
        binding.nonConsumable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buyProduct(PRODUCT_1);
            }
        });

        binding.consumable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
     * Query in-app details from playstore using product_id. You will then use the received details about the product
     * to launch billing flow to buy product
     *
     * @param productID id of product
     */
    private void buyProduct(String productID) {

        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId(productID)
                                                .setProductType(BillingClient.ProductType.INAPP)
                                                .build()))
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                (billingResult, productDetailsList) -> {
                    if (productDetailsList == null) {
                        Toast.makeText(MainActivity.this, "Account unable to access products. Check if your gmail account is a license tester on Google Play Console", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Process the result.
                    ProductDetails selected = selectNonConsumableProduct(productDetailsList);
                    launchBillingFlow(selected);
                }
        );
    }

    /**
     * Restore all active purchased products
     * Call this function to update app with user's purchases
     */
    private void restorePurchases() {
        updateBillingUI(cache.getRemoveAdsData(), cache.getSubscriptionData());
    }

    /**
     * Update system to current purchases. You can change this function to support other app
     *
     * @param nonConsumableData System data to record previous remove_ads purchase.
     * @param consumableData    System data to record subscription.
     */
    private void updateBillingUI(NonConsumableData nonConsumableData, ConsumableData consumableData) {
        if (nonConsumableData != null) {
            //Ads OFF
            binding.nonConsumable.setEnabled(false);
            binding.nonConsumable.setText(getString(R.string.status_no_ads));
        } else {
            //Ads ON
            binding.nonConsumable.setEnabled(true);
            binding.nonConsumable.setText(getString(R.string.status_ads));
        }
    }


    /**
     * Save ProductIDs and purchase tokens to your system data.In this case SharedPreference
     */
    private void recordPurchase(String userID, Purchase purchase) {
        if (purchase.getProducts().get(0).equals(PRODUCT_1)) {
            //Non-consumable (One-time)
            NonConsumableData nonConsumableData = new NonConsumableData();
            nonConsumableData.setPurchaseToken(purchase.getPurchaseToken());
            nonConsumableData.setUserID(purchase.getProducts().get(0));
            nonConsumableData.setProductID(userID);
            cache.setNonConsumableData(nonConsumableData);
            restorePurchases();
            Log.d(TAG, "NonConsumableData: Saved");

        } else if (purchase.getProducts().get(0).equals(PRODUCT_2)) {
            //Consumable product
            ConsumableData consumableData = new ConsumableData();
            consumableData.setSku(purchase.getProducts().get(0));
            consumableData.setUserID(USER_ID);
            cache.setConsumableData(consumableData);
            restorePurchases();

            Log.d(TAG, "ConsumableData: Saved");
        }
    }


    /**
     * Check existing purchases that user has previously made.
     */
    private void queryPurchases() {
        if (billingClient.isReady()) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    (billingResult, purchases) -> {
                        // check billingResult
                        // process returned purchase list, e.g. display the plans user owns
                        for (Purchase p : purchases) {
                            handlePurchase(p);
                        }
                    }
            );
        }
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
     * Launch playstore payment screen to buy using product details you got previously
     *
     * @param productDetails
     */
    private void launchBillingFlow(ProductDetails productDetails) {
        ImmutableList productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                .setProductDetails(productDetails)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        // Launch the billing flow
        BillingResult billingResult = billingClient.launchBillingFlow(this, billingFlowParams);
    }

    /**
     * Select one-time/non-consumable product_details from product_details_list.
     *
     * @param productDetailsList
     */
    private ProductDetails selectNonConsumableProduct(@NonNull List<ProductDetails> productDetailsList) {
        //Assuming we have our app database, we can query.
        for (ProductDetails pd : productDetailsList) {
            if (pd.getProductId().equals(PRODUCT_1)) {
                return pd;
            }
        }
        return null;
    }
}
